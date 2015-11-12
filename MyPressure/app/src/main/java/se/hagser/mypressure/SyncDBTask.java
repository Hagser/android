package se.hagser.mypressure;

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

	DatabaseHelper mDbHelper = null;
	public SyncDBTask(DatabaseHelper dbh) {
		mDbHelper = dbh;
	}
	protected void onProgressUpdate(Integer... progress) {

	}

	@Override
	protected Boolean doInBackground(Integer... params) {

		String json = syncSQLiteMySQLDB();
		Log.i("doInBack:",json.length()+"");
		if (json.length() < 10) {
			this.cancel(true);
			return false;
		}
		try {
			String ids = "";
			JSONArray jarr = new JSONArray(json);
			for (int j = 0; j < jarr.length(); j++) {
				JSONObject jsonObject = jarr.getJSONObject(j);
				if (jsonObject.has("id") && jsonObject.has("status")) {
					if (jsonObject.getBoolean("status")) {
						ids += "," + jsonObject.getInt("id");
					}
				}
			}
			if (ids.length() > 2) {
				mDbHelper.deleteRows(ids);
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}

		return true;
	}


	private String syncSQLiteMySQLDB() {

		String response = "";
		String url = "http://php.hagser.se/insert_pressure.php";
		Log.i(MyPressureService.NOTIFICATION+"sc",url);
		DefaultHttpClient client = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(url);
		ByteArrayEntity byteArrayEntity;
		String json;
		json = mDbHelper.composeJSONfromSQLite();

		Log.i(MyPressureService.NOTIFICATION+"sc",json.length()+"");

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
			while ((s = buffer.readLine()) != null) {
				response += s;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return response;
	}
}
