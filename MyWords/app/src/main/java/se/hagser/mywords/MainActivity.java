package se.hagser.mywords;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;


public class MainActivity extends ActionBarActivity {

	EditText editText=null;
	CheckBox checkBox=null;
	static String KEY_WORD="word";
	static String NOTIFICATION="se.hagser.mywords";
	private ListView listView=null;
	protected ListAdapter listAdapter=null;
	ArrayList<HashMap<String, String>> rows=new ArrayList<HashMap<String, String>>();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		listView =(ListView)findViewById(R.id.listView);
		checkBox=(CheckBox)findViewById(R.id.checkBox);

		listAdapter = new ListAdapter(this,rows);
		listView.setAdapter(this.listAdapter);
		editText = (EditText)findViewById(R.id.editText);

		editText.setOnKeyListener(new View.OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				Log.i(NOTIFICATION,event.getAction()+"");
				AsyncTask<String,Integer,Boolean> asyncTask=new AsyncTask<String, Integer, Boolean>() {
					@Override
					protected Boolean doInBackground(String... params) {

						String json = GetJson(params[0]);
						rows.clear();
						//Log.i("mywords", json);
						try {
							JSONArray jarr = new JSONArray(json);
							for(int i=0;i<jarr.length();i++)
							{
								HashMap<String, String> map = new HashMap<String, String>();
								String word = jarr.getString(i);
								map.put(KEY_WORD,word);
								//Log.i("mywords", word);
								rows.add(map);
							}

							Intent intent = new Intent(NOTIFICATION);
							sendBroadcast(intent);
						} catch (JSONException e) {
							e.printStackTrace();
						}
						return null;
					}
				};
				if(!oldword.equals(editText.getText()+"")) {
					oldword=editText.getText()+"";
					String url = "http://word.hagser.se/api/"+ (checkBox.isChecked()?"words/": "realwords/") + oldword.replace(" ","$");
					asyncTask.execute(url);
				}
				return false;
			}
		});
	}
	String oldword = "";
	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			listAdapter = new ListAdapter(MainActivity.this,rows);
			listView.setAdapter(MainActivity.this.listAdapter);
		}

	};
	@Override
	protected void onResume() {
		super.onResume();

		registerReceiver(receiver, new IntentFilter(NOTIFICATION));
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(receiver);
	}

	private String GetJson(String url)
	{
		String response="";
		DefaultHttpClient client = new DefaultHttpClient();

		HttpGet httpGet = new HttpGet(url);
		Header header = httpGet.getFirstHeader("accept");
		if(header!=null)
		{
			httpGet.setHeader("accept","text/json");
		}
		else {
			httpGet.addHeader("accept","text/json");
		}
		try {
			//Log.i(url,"execute");
			HttpResponse execute = client.execute(httpGet);
			InputStream content = execute.getEntity().getContent();

			BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
			//Log.i(url,"getting");
			String s;
			while ((s = buffer.readLine()) != null) {
				response += s;
			}
		} catch (Exception e) {
			//Log.i(url,"oops");
			e.printStackTrace();
		}
		//Log.i(url,response);

		return response;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
}
