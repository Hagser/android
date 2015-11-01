package se.hagser.myroadchecker;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class MySyncService extends Service {

	private DatabaseHelper mDbHelper=null;
	public static String SyncNote = "SyncNote";
	public static String SyncStop = "SyncStop";
	public static String Count = "Count";
	String deviceid=null;
	boolean isSyncing = false;
	static String tag = "mysyncservice";
	private int cnt = 0;
	SharedPreferences keyValues;
	boolean bOverrideWifi = false;

	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent==null)
			return Service.START_FLAG_RETRY;

		keyValues = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
		bOverrideWifi = keyValues.getBoolean(MyAccService.WO,false);
		deviceid = intent.getStringExtra("device");
		isSyncing=true;
		LogI("start");
		mDbHelper = new DatabaseHelper(this.getApplicationContext());
		LogI("mdbhelper");
		final WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);

		LogI("wifi");
		final CountDownTimer countDownTimer = new CountDownTimer(Integer.MAX_VALUE,10000) {
			@Override
			public void onTick(long millisUntilFinished) {
				if(!isSyncing) {
					this.cancel();
					this.onFinish();
					LogI("stopself");

					Intent intent = new Intent(SyncNote);
					intent.putExtra(SyncStop,SyncStop);
					sendBroadcast(intent);

					stopSelf();

				}
				bOverrideWifi = keyValues.getBoolean(MyAccService.WO,false);
				LogI("in loop");
				sendIntentInfo();

				if ((wifi.isWifiEnabled() && wifi.getConnectionInfo()!=null)||bOverrideWifi) {
					LogI("sync");
					syncData();
				}

			}

			@Override
			public void onFinish() {

			}

		};
		countDownTimer.start();

		return Service.START_STICKY;
	}

	private void sendIntentInfo() {
		Intent intent = new Intent(SyncNote);
		sendBroadcast(intent);
	}

	private static void LogI(String msg)
{
	if(MyAccService.bDebug)
		Log.i(tag,msg);
}
	@Override
	public void onDestroy() {
		LogI("sync destroy");

		isSyncing=false;
		mDbHelper=null;
		super.onDestroy();

	}

	private void syncData() {
		LogI("in syncdata");
		if(mDbHelper==null || !isSyncing)
			return;

		final int limit=5000;
		final SyncDBTask task = new SyncDBTask(mDbHelper,deviceid);

		final CountDownTimer countDownTimer = new CountDownTimer(Integer.MAX_VALUE,2000) {

			@Override
			public void onTick(long millisUntilFinished) {
				long cntall=0;
				if(mDbHelper!=null) {
					cntall = mDbHelper.countRecords();
					if (task != null && cntall>0) {
						AsyncTask.Status sts = task.getStatus();

						if (!sts.equals(AsyncTask.Status.RUNNING) && !sts.equals(AsyncTask.Status.FINISHED)) {
							try {
								LogI("task.execute");
								task.execute(cnt, limit);

							} catch (Exception ex) {
							}
							finally {
								//cnt=cnt+limit;
							}
						}
						Intent intent = new Intent(SyncNote);
						intent.putExtra(Count, cntall + "");
						sendBroadcast(intent);
					}
				}
				if(cntall<1)
					isSyncing=false;
				if(!isSyncing)
				{
					this.onFinish();
				}
			}

			@Override
			public void onFinish() {

				this.cancel();


			}
		};
		countDownTimer.start();


	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO: Return the communication channel to the service.
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
