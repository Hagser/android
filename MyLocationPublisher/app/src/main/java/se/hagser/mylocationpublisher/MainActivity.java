package se.hagser.mylocationpublisher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

private TextView all_text;
	private Button button_toggle;
	private Button button_open_web;
	private TextView address_text;
	public static String TAG = "se.mylocation";

	BroadcastReceiver broadcastReceiver=null;
	BroadcastReceiver broadcastReceiverRunning=null;
	boolean bStarted=false;
	String device_id="";
	private boolean bAutoStart=false;

	public static void LooG(String tag,String msg) {
		if(BuildConfig.DEBUG)
			Log.i(tag,msg);
	}
	public static void LooG(String msg) {
		LooG(TAG,msg);
	}

	@Override
	public void onDestroy() {
		try
		{
			//Intent i = new Intent(this,MyService.class);
			//stopService(i);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		finally
		{
			super.onDestroy();
		}
	}
	private BroadcastReceiver getBR() {
		return new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			LooG("onReceiveBR");
			if(intent.getAction().equals(MyService.RESULT_OK))
			{
				if(intent.hasExtra(MyService.RESULT_ADR))
				{
					String address = intent.getStringExtra(MyService.TX);
					if(address_text==null)
					{
						address_text = (TextView)findViewById(R.id.address_text);
					}
					address_text.setText("ADR:"+address);
				}
				else
				{
					if(all_text==null)
						all_text = (TextView)findViewById(R.id.all_text);

					float bearing = intent.getFloatExtra(MyService.B, -1);
					double lat = intent.getDoubleExtra(MyService.LA, -1);
					double lng = intent.getDoubleExtra(MyService.LO, -1);
					float speed = intent.getFloatExtra(MyService.SP, -1);
					long tim = intent.getLongExtra(MyService.TI, -1);
					String text = intent.getStringExtra(MyService.TX);
					Date dt = new Date(tim);
					String format="yyyy-MM-dd HH:mm";
					SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.ENGLISH);
					String strDate = sdf.format(dt);

					String strText = "";
					if(bearing>0)
						strText+="BEAR:" + bearing + "\r\n";
					if(lat>0)
						strText+="LAT:" + lat + "\r\n";
					if(lng>0)
						strText+="LNG:" + lng + "\r\n";
					if(speed>0)
						strText+="SPEED:" + speed + "\r\n";
					if(tim>0)
						strText+="TIM:" + strDate + "\r\n";
					if(text!=null && !text.equals(""))
						strText+="TEXT:" + text;

					all_text.setText(strText);
				}
			}
			unregisterReceiver(this);
			broadcastReceiver=getBR();
			registerReceiver(broadcastReceiver, new IntentFilter(MyService.RESULT_OK));
		}
	};

	}
	private BroadcastReceiver getRunBR() {
		return new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				LooG("onReceiveRunBR");
				if(intent.getAction().equals(MyService.RESULT_RUN))
				{
					bStarted=true;
					if(button_toggle==null)
						button_toggle = (Button)findViewById(R.id.button_toggle);
					button_toggle.setText(getResources().getText(R.string.stop));

					if(all_text==null)
						all_text=(TextView)findViewById(R.id.all_text);

					float bearing = intent.getFloatExtra(MyService.B, -1);
					double lat = intent.getDoubleExtra(MyService.LA, -1);
					double lng = intent.getDoubleExtra(MyService.LO, -1);
					float speed = intent.getFloatExtra(MyService.SP, -1);
					long tim = intent.getLongExtra(MyService.TI, -1);
					String text = intent.getStringExtra(MyService.TX);
					Date dt = new Date(tim);
					String format="yyyy-MM-dd HH:mm";
					SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.ENGLISH);
					String strDate = sdf.format(dt);

					String strText = "";
					if(bearing>0)
						strText+="BEAR:" + bearing + "\r\n";
					if(lat>0)
						strText+="LAT:" + lat + "\r\n";
					if(lng>0)
						strText+="LNG:" + lng + "\r\n";
					if(speed>0)
						strText+="SPEED:" + speed + "\r\n";
					if(tim>0)
						strText+="TIM:" + strDate + "\r\n";
					if(text!=null && !text.equals(""))
						strText+="TEXT:" + text;

					all_text.setText(strText);
					unregisterReceiver(this);
					broadcastReceiverRunning=null;
				}
			}
		};
	}
	private void startMyService() {
		LooG("startMyService-bStarted",bStarted+"");
		if (button_toggle == null)
			button_toggle = (Button) findViewById(R.id.button_toggle);
		button_toggle.setText(getResources().getText(R.string.stop));

		if(bStarted) {
			return;
		}
		try {
			if (broadcastReceiverRunning != null)
				unregisterReceiver(broadcastReceiverRunning);
		}
		catch (IllegalArgumentException e)
		{

		}
		startService(new Intent(MainActivity.this,MyService.class));
		bStarted=true;


	}
	private void stopMyService() {
		LooG("stopMyService-bStarted",bStarted+"");
		if (button_toggle == null)
			button_toggle = (Button) findViewById(R.id.button_toggle);
		button_toggle.setText(getResources().getText(R.string.start));

		if(!bStarted) {
			return;
		}

		stopService(new Intent(MainActivity.this,MyService.class));
		bStarted=false;
		all_text.setText("");
		address_text.setText("");
	}
	@Override
	protected void onPause() {
		LooG("onPause");
		try
		{
			if(broadcastReceiver!=null)
				unregisterReceiver(broadcastReceiver);
		}
		catch (Exception e) {}

		try
		{
			if(broadcastReceiverRunning!=null)
				unregisterReceiver(broadcastReceiverRunning);
		}
		catch (Exception e) {}

		super.onPause();
	}
	@Override
	protected void onResume() {
		LooG("onResume");

		broadcastReceiverRunning=getRunBR();
		registerReceiver(broadcastReceiverRunning, new IntentFilter(MyService.RESULT_RUN));
		LooG("onResume","before CDT");
		if(bAutoStart) {
			final long beforeCDT = SystemClock.elapsedRealtime();
			CountDownTimer countDownTimer = new CountDownTimer(20000, 6000) {
				@Override
				public void onTick(long millisUntilFinished) {
					if ((SystemClock.elapsedRealtime() - beforeCDT) > 6000) {
						LooG("onResume", "onTick");
						if (!bStarted) {
							startMyService();
						}
						this.cancel();
					}
				}

				@Override
				public void onFinish() {
					this.start();
				}
			};
			LooG("onResume", "before start");
			countDownTimer.start();
		}
		all_text = (TextView)findViewById(R.id.all_text);
		all_text.setText("");

		address_text = (TextView)findViewById(R.id.address_text);
		address_text.setText("");

		button_toggle = (Button)findViewById(R.id.button_toggle);
		button_toggle.setText(getResources().getText(R.string.start));

		button_toggle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (bStarted)
					stopMyService();
				else
					startMyService();
			}
		});

		button_open_web = (Button)findViewById(R.id.button_open_web);
		button_open_web.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final SharedPreferences keyValues = PreferenceManager.getDefaultSharedPreferences(MainActivity.this.getApplicationContext());
				if(keyValues.contains(MyService.UUID))
				{
					device_id=keyValues.getString(MyService.UUID,"");
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://php.hagser.se/showLocation.php?device_id="+device_id));
					startActivity(browserIntent);
				}
			}
		});

		broadcastReceiver=getBR();
		registerReceiver(broadcastReceiver, new IntentFilter(MyService.RESULT_OK));
		super.onResume();
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		LooG("onRestoreInstanceState");
		if(savedInstanceState.containsKey("bStarted"))
			bStarted=savedInstanceState.getBoolean("bStarted");
		super.onRestoreInstanceState(savedInstanceState);
	}
	@Override
	protected void onSaveInstanceState(Bundle saveInstanceState) {
		LooG("onSaveInstanceState");
		saveInstanceState.putBoolean("bStarted",bStarted);
		super.onSaveInstanceState(saveInstanceState);

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
			startActivity(new Intent(this,SettingsActivity.class));
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
}
