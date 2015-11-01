package se.hagser.myroadchecker;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPOutputStream;

public class SyncDBTask extends AsyncTask<Integer, Integer, Boolean> {
	final static String tag = MyAccService.NOTIFICATION + "udt";
	DatabaseHelper mDbHelper = null;
	String deviceid = "";


	public SyncDBTask(DatabaseHelper dbh,String _deviceid) {
		mDbHelper = dbh;
		deviceid=_deviceid;
	}
	protected void onProgressUpdate(Integer... progress) {
		LogI(progress[0] + "");
	}

	protected void onPostExecute(String result) {
		mDbHelper.close();
	}

	protected Boolean doInBackground(Integer... params) {

		LogI("doinback");
		int limit = params[1];
		int i = params[0];
		String json = syncSQLiteMySQLDB(i, limit);
		if (json.length() < 10) {
			this.cancel(true);
			return false;
		}
		LogI("gotjson");
		try {
			String ids = "";
			JSONArray jarr = new JSONArray(json);
			for (int j = 0; j < jarr.length(); j++) {
				JSONObject jsonObject = jarr.getJSONObject(j);
//				if (jsonObject.has("id") && jsonObject.has("status")) {
//					if (jsonObject.getBoolean("status")) {
//						ids += "," + jsonObject.getInt("id");
//					}
//				}
				if (jsonObject.has("id")) {
					ids += "," + jsonObject.getInt("id");
				}

			}
			LogI("ids:" + ids);
			if (ids.length() > 2) {
				long lrows = mDbHelper.deleteRows(ids);
				lrows += mDbHelper.deleteZeroSpeed();
				LogI("numrowsdeleted:" + lrows);
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
		LogI("syncdbtask-done");

		return true;
	}


	private String syncSQLiteMySQLDB(int skip, int limit) {

		LogI("limit " + skip + "," + limit);
		String response = "";
		String url = "http://php.hagser.se/insert.php";


		DefaultHttpClient client = new DefaultHttpClient();

		HttpPost httpPost = new HttpPost(url);
		StringEntity se = null;
		ByteArrayEntity byteArrayEntity = null;

		String json = "";

		json = mDbHelper.composeJSONfromSQLite(skip, limit, deviceid);
		LogI("json:" + json.length() + "");
		if (json.length() < 100) {
			return "";
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPOutputStream gzos = null;

		try {
			gzos = new GZIPOutputStream(baos);
			gzos.write(json.getBytes());

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (gzos != null) try {
				gzos.close();
			} catch (IOException ignore) {
			}
		}

		byte[] fooGzippedBytes = baos.toByteArray();
		byteArrayEntity = new ByteArrayEntity(fooGzippedBytes);


		//sets the post request as the resulting string

		httpPost.setEntity(byteArrayEntity);

		httpPost.setHeader("Accept", "application/json");
		//httpPost.setHeader("Accept", "gzip");
		//httpPost.setHeader("Content-type", "application/json");
		httpPost.setHeader("Content-type", "gzip");

		try {
			HttpResponse execute = client.execute(httpPost);
			InputStream content = execute.getEntity().getContent();

			BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
			String s;
			/*
			while ((s = buffer.readLine()) != null) {
				response += s;
			}
			*/
			response=json;
			LogI("getStatusCode:" + execute.getStatusLine().getStatusCode() + "");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return response;

	}

	private void LogI(String msg) {
		if (MyAccService.bDebug)
			Log.i(tag, msg);

	}

}