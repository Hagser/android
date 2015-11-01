package se.hagser.myraspi;

import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by jh on 2015-09-26.
 */
public class SendAsync extends AsyncTask<String, Integer, String> {
	public int Progress;
	@Override
	protected String doInBackground(String... params) {
		Send(init_url + params[0]);
		return null;
	}

	protected void onProgressUpdate(Integer... progress) {
		Progress=progress[0];
	}
	private String init_url="";
	public String getUrl(){return init_url;}
	public SendAsync(String in_url){
		init_url=in_url;
	}
	private void Send(String surl)
	{
this.onProgressUpdate(1);
		LogI("SA", "Send:" + surl);
		try {
			URL url = new URL(surl);

			final HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
					/*
					new HttpURLConnection(url) {
				@Override
				public void disconnect() {

				}

				@Override
				public boolean usingProxy() {
					return false;
				}

				@Override
				public void connect() throws IOException {
					SendAsync.this.onProgressUpdate(4);
					//this.getInputStream();
					LogI("Response:", this.getResponseCode() + "");
					SendAsync.this.onProgressUpdate(5);
				}
			};
*/
			this.onProgressUpdate(2);
			//httpURLConnection.setRequestMethod("GET");
			//httpURLConnection.setDoInput(true);
			//httpURLConnection.setDoOutput(true);
			LogI("SA", "connecting");
			httpURLConnection.connect();
			LogI("Response:", httpURLConnection.getResponseCode() + "");


			this.onProgressUpdate(3);

		} catch (MalformedURLException e) {
			e.printStackTrace();
			LogI("SA",e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			LogI("SA", e.getMessage());
		}

		LogI("SA", "start");

	}

	private void LogI(String tag,String msg)
	{
		if(MainActivity.bLog)
			Log.i(tag, msg);
	}
}
