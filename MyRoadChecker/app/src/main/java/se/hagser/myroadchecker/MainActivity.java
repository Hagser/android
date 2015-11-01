package se.hagser.myroadchecker;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Set;


public class MainActivity extends Activity {

    TextView tx=null;
    TextView ty=null;
    TextView tz=null;
    TextView tlat=null;
    TextView tlon=null;
    TextView talarm=null;
	TextView text_speed;
	TextView text_count;
	Switch started;
	Switch syncing;
	boolean btactivate=false;
    LinearLayout linear_layout=null;
    Button clickit=null;
    DatabaseHelper mDbHelper=null;
	final static String tag = MyAccService.NOTIFICATION+"ma";
	SharedPreferences keyValues=null;
	final static String devid="deviceid";
	String deviceid=null;
	boolean bOverrideWifi = false;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }
	private String getDeviceId() {
		String ret = null;

		String json = "";
		String url = "http://php.hagser.se/getid.php";
		DefaultHttpClient client = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(url);

		httpGet.setHeader("Accept", "application/json");
		httpGet.setHeader("Content-type", "application/json");
		String androidOS = Build.VERSION.RELEASE;
		httpGet.setHeader("User-Agent", androidOS);
		try {
			HttpResponse execute = client.execute(httpGet);
			InputStream content = execute.getEntity().getContent();

			BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
			String s;
			while ((s = buffer.readLine()) != null) {
				json += s;
			}

			LogI("getStatusCode:" + execute.getStatusLine().getStatusCode() + "");
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			JSONArray jarr = new JSONArray(json);
			for (int j = 0; j < jarr.length(); j++) {
				JSONObject jsonObject = jarr.getJSONObject(j);
				if (jsonObject.has("deviceid")) {
					ret = jsonObject.getString("deviceid");
					break;
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}


		return ret;
	}

	private String fixNow(CharSequence text) {
		String ret = text+"";
		if(ret.contains(":"))
			ret = ret.split(":")[1];
		return ret;
	}

	private BroadcastReceiver getBCReceeiver() {

		BroadcastReceiver bret = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				LogI("btrec:" + intent.getAction());
				LogI("btactivate:" + btactivate);
				if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction())) {
					if (btactivate) {
						startService();
						registerReceiver(getBCReceeiver(), new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
					}
				} else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(intent.getAction())) {
					if (btactivate) {
						stopService();
						registerReceiver(getBCReceeiver(), new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
					}
				} else if("android.bluetooth.devicepicker.action.DEVICE_SELECTED".equals(intent.getAction())) {
					BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					LogI("getAddress:" + device.getAddress());
					keyValues.edit().putString(MyAccService.DEV, device.getAddress()).apply();

					registerReceiver(getBCReceeiver(), new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
				}
				unregisterReceiver(this);
			}
		};
		return bret;
	}

	private BroadcastReceiver getSettingsReceiver() {
		BroadcastReceiver bret = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				LogI("settrec:" + intent.getAction());
				if (MyAccService.BT.equals(intent.getAction())) {
					btactivate = keyValues.getBoolean(MyAccService.BT, btactivate);
					LogI("btact:" + btactivate);
					if (btactivate) {
						String getDevice = keyValues.getString(MyAccService.DEV, "");
						LogI("getDevice:" + getDevice);
						if (getDevice.equals("") || intent.getBooleanExtra("RESET", true)) {
							//registerReceiver(getBCReceeiver(), new IntentFilter("android.bluetooth.devicepicker.action.DEVICE_SELECTED"));
							Intent btintent = new Intent("android.bluetooth.devicepicker.action.LAUNCH")
									.putExtra("android.bluetooth.devicepicker.extra.NEED_AUTH", false)
									.putExtra("android.bluetooth.devicepicker.extra.FILTER_TYPE", 0)
									.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

							//startActivity(btintent);
							String device = intent.getStringExtra("DATA");
							keyValues.edit().putString(MyAccService.DEV, device).apply();
						} else {
							registerReceiver(getBCReceeiver(), new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
							registerReceiver(getBCReceeiver(), new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
						}

					}

				}
			}
		};
		return bret;
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
			if (MyAccService.NOTIFICATION.equals(intent.getAction())) {
				Bundle bundle = intent.getExtras();
				if (bundle != null) {
					String x = bundle.getString(MyAccService.KEY_X);
					String y = bundle.getString(MyAccService.KEY_Y);
					String z = bundle.getString(MyAccService.KEY_Z);
					String lat = bundle.getString(MyAccService.KEY_LAT);
					String lon = bundle.getString(MyAccService.KEY_LON);
					boolean al = bundle.getBoolean(MyAccService.KEY_AL);
					String at = bundle.getString(MyAccService.KEY_AT);
					String s = bundle.getString(MyAccService.KEY_SPEED);
					String cnt = bundle.getString(MyAccService.KEY_CNT);
					int resultCode = bundle.getInt(MyAccService.RESULT);
					if (resultCode == RESULT_OK) {
						tx.setText("X:" + x);
						ty.setText("Y:" + y);
						tz.setText("Z:" + z);
						tlat.setText("Lat:" + lat);
						tlon.setText("Lon:" + lon);
						text_speed.setText("Speed:" + s);
						if (al) {
							talarm.setText(at);
							//linear_layout.setBackgroundColor(Color.RED);
							playSound();
						} else {
							talarm.setText(cnt);
							//linear_layout.setBackgroundColor(Color.BLACK);
						}
					} else {
						Toast.makeText(MainActivity.this, "Update-failed", Toast.LENGTH_LONG).show();
					}
				}
			}
		}
    };
	private void playSound()
	{
		try {
			Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
			r.play();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, new IntentFilter(MyAccService.NOTIFICATION));
		LogI("registerReceiver");
		registerReceiver(syncReceiver, new IntentFilter(MySyncService.SyncNote));

		tx = (TextView) findViewById(R.id.text_x);
		ty = (TextView) findViewById(R.id.text_y);
		tz = (TextView) findViewById(R.id.text_z);
		tlat = (TextView) findViewById(R.id.text_lat);
		tlon = (TextView) findViewById(R.id.text_lon);
		text_speed = (TextView) findViewById(R.id.text_speed);
		text_count = (TextView)findViewById(R.id.text_count);

		started = (Switch)findViewById(R.id.switch_start);

		started.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(started.isChecked())
					startService();
				else
					stopService();
			}
		});

		syncing = (Switch)findViewById(R.id.switch_sync);

		syncing.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				LogI("syncing:"+syncing.isChecked());

				if(syncing.isChecked()) {
					startSyncService();
				}
				else {
					stopSyncService();
				}

			}
		});


		talarm = (TextView) findViewById(R.id.text_alarm);
		linear_layout=(LinearLayout)findViewById(R.id.linear_layout);
		mDbHelper = new DatabaseHelper(this.getBaseContext());
		clickit =(Button)findViewById(R.id.clickit);
		keyValues = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
		bOverrideWifi = keyValues.getBoolean(MyAccService.WO, false);
		String getDevice = keyValues.getString(MyAccService.DEV,"");
		LogI("onresume-getDevice:"+getDevice);
		btactivate = keyValues.getBoolean(MyAccService.BT, false);
		LogI("onresume-btdev:"+btactivate);
		if(btactivate && getDevice!=null && !getDevice.equals("")) {
			registerReceiver(getBCReceeiver(), new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
			registerReceiver(getBCReceeiver(), new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
		}

		final CountDownTimer countDownTimer = new CountDownTimer(10000000,3000) {
			@Override
			public void onTick(long millisUntilFinished) {
				text_count.setText(mDbHelper.countRecords()+"");
				AsyncTask asyncTask = new AsyncTask() {
					@Override
					protected Object doInBackground(Object[] params) {
						if(deviceid==null ||deviceid.equals(""))
						{
							//LogI("doInBack:getDeviceId()");
							deviceid=getDeviceId();
							keyValues.edit().putString(devid,deviceid).apply();
						}
						//LogI("doInBack:"+deviceid);
						return null;
					}
				};
				asyncTask.execute();
			}

			@Override
			public void onFinish() {
				if(deviceid==null ||deviceid.equals("")) {
					LogI("onFinish:"+deviceid);
					this.start();
				}
			}
		};

		try {
			deviceid = keyValues.getString(devid, "");
			LogI(deviceid);
		}
		catch(Exception ex)
		{}
		countDownTimer.start();

		clickit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				UpdateDbTask task = new UpdateDbTask(mDbHelper);
				String at = MyAccService.getDefaultDateTime();
				task.execute(at, fixNow(tx.getText()), fixNow(ty.getText()), fixNow(tz.getText()), fixNow(tlat.getText()), fixNow(tlon.getText()), fixNow(text_speed.getText()), "1");
			}
		});
/*
		if(btactivate && isConnected(getDevice))
		{
			startService();
		}
*/
	}

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
		try {
			//unregisterReceiver(btreceiver);
		}
		catch (Exception ex)
		{}

		try {
			//unregisterReceiver(bdmr);
		}
		catch (Exception ex)
		{}

		try {
			//unregisterReceiver(settingsreceiver);
		}
		catch (Exception ex)
		{}

        mDbHelper.close();

    }
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("bStarted",bStarted);
		outState.putString("deviceid", deviceid);
		outState.putBoolean("isSyncing", isSyncing);
		outState.putBoolean("btactivate",btactivate);

	}
	private void checkIfLocationIsEnabled()
	{
		LocationManager lm = null;
		boolean gps_enabled = false,network_enabled = false;
		if(lm==null)
			lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		try{
			gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
		}catch(Exception ex){}
		try{
			network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		}catch(Exception ex){}

		if(!gps_enabled && !network_enabled){
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			dialog.setMessage(this.getResources().getString(R.string.gps_network_not_enabled));
			dialog.setPositiveButton(this.getResources().getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface paramDialogInterface, int paramInt) {

					Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
					startActivity(myIntent);
				}
			});
			dialog.setNegativeButton(this.getString(R.string.Cancel), new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface paramDialogInterface, int paramInt) {

				}
			});
			dialog.show();

		}
	}
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		bStarted=savedInstanceState.containsKey("bStarted")&&savedInstanceState.getBoolean("bStarted");
		deviceid=savedInstanceState.containsKey("deviceid")?savedInstanceState.getString("deviceid"):"";
		isSyncing=savedInstanceState.containsKey("isSyncing")&&savedInstanceState.getBoolean("isSyncing");
		btactivate=savedInstanceState.containsKey("btactivate")&&savedInstanceState.getBoolean("btactivate");

		if(btactivate)
		{
			if(keyValues==null)
			{
				keyValues = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
			}
			if(keyValues!=null) {
				String getDevice = keyValues.getString(MyAccService.DEV, "");
				if (getDevice != null && !getDevice.equals("")) {
					registerReceiver(getBCReceeiver(), new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
					registerReceiver(getBCReceeiver(), new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
				}
			}
		}
		else
		{
			try {
				//unregisterReceiver(btreceiver);
			}
			catch (Exception ex)
			{}
		}

		if(bStarted)
		{
			startService();
		}
		if(isSyncing)
		{
			startSyncService();
		}
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
			case R.id.reset_data:
				resetData();
				return true;
			case R.id.view_map:
				viewMap();
				return true;
			case R.id.view_settings:
				viewSettings();
				return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
	boolean isSyncing= false;

	private void viewMap() {
		Intent i = new Intent(this,MapsActivity.class);
		startActivity(i);
	}

	private void viewSettings() {
		LogI("viewSettings");
		registerReceiver(getSettingsReceiver(), new IntentFilter(MyAccService.BT));
		Intent i = new Intent(this, SettingsActivity.class);
		startActivity(i);
		LogI("viewSettings-started");
	}

	private void resetData() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				UpdateDbTask updateDbTask = new UpdateDbTask(mDbHelper);
				updateDbTask.clearDatabase();
				keyValues.edit().clear().apply();
				deviceid = "";
			}
		});
		builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				// User cancelled the dialog
			}
		});
		builder.setTitle(R.string.reset);
		builder.setMessage(R.string.reset_db);

		AlertDialog dialog = builder.create();
		dialog.show();

	}
/*
	private void viewFull() {
		Intent i = new Intent(this,FullscreenActivity.class);
		startActivity(i);
	}
*/
	boolean bStarted = false;
    private void stopService() {
		started.setChecked(false);
        if(!bStarted)
            return;
        Intent i = new Intent(this,MyAccService.class);
        LogI("stopService");
        stopService(i);
        bStarted=false;
    }
    private void startService() {
		started.setChecked(true);
        if(bStarted)
            return;
		checkIfLocationIsEnabled();
        Intent i = new Intent(this,MyAccService.class);
        LogI("startService");
        startService(i);
        bStarted=true;
    }
	private void stopSyncService() {
		syncing.setChecked(false);
		if(!isSyncing)
			return;
		Intent i = new Intent(this,MySyncService.class);
		LogI("stopSyncService");
		try {
			unregisterReceiver(syncReceiver);
		}
		catch(Exception e){}
		try {
			stopService(i);
		}
		catch (Exception e)
		{}
		isSyncing=false;
	}
	private void startSyncService() {
		syncing.setChecked(true);
		if(isSyncing)
			return;
		LogI("checkIfWifiIsEnabled");
		checkIfWifiIsEnabled();
		Intent i = new Intent(this,MySyncService.class);
		i.putExtra("device", deviceid);
		LogI("startService");
		startService(i);

		isSyncing=true;
	}
	private BroadcastReceiver syncReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.hasExtra(MySyncService.SyncStop)) {
				syncing.setChecked(false);
				unregisterReceiver(this);
				return;
			}
			else if(intent.hasExtra(MySyncService.Count)) {
				String cntall = intent.getStringExtra(MySyncService.Count);
				text_count.setText(cntall);
			}
			syncing.setChecked(true);
			unregisterReceiver(this);
			registerReceiver(syncReceiver, new IntentFilter(MySyncService.SyncNote));
		}
	};

	private void checkIfWifiIsEnabled() {
		WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		if (!wifi.isWifiEnabled() && !bOverrideWifi){
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			dialog.setMessage(this.getResources().getString(R.string.wifi_not_enabled));
			dialog.setPositiveButton(this.getResources().getString(R.string.open_wifi_settings), new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface paramDialogInterface, int paramInt) {

					Intent myIntent = new Intent( Settings.ACTION_WIFI_SETTINGS);
					startActivity(myIntent);
				}
			});
			dialog.setNegativeButton(this.getString(R.string.Cancel), new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface paramDialogInterface, int paramInt) {

				}
			});
			dialog.show();
		}
	}

	private void LogI(String msg)
    {
        if(MyAccService.bDebug)
            Log.i(tag,msg);

    }
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public boolean isConnected(String address) {
	try {
		BluetoothManager bm = (BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
		BluetoothAdapter bta = bm.getAdapter();
		if(bta!=null) {
			Set<BluetoothDevice> devs = bta.getBondedDevices();//BluetoothAdapter.getDefaultAdapter().getBondedDevices();
			Iterator<BluetoothDevice> its = devs.iterator();
			while (its.hasNext()) {
				BluetoothDevice bd = its.next();
				if (bd.getAddress().equals(address))
					return bd.connectGatt(null,true,null).connect();
			}
		}
	}
	catch (Exception ex)
	{
		ex.printStackTrace();
	}
	return false;
	}

}
