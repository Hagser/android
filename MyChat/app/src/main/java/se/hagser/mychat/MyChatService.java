package se.hagser.mychat;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Parcel;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class MyChatService extends Service {
	public static String RESULT_CH = "MyChat";
	public static String UUID="uuid";

	public static String RESULT_OK="MyServiceOK";
	public static String RESULT_OL="MyServiceOL";
	private String devid="";
	private String mymsg="";
	String lastDate="0";
	String myid="";
	String myip="";
	private boolean bStop = false;
	private BroadcastReceiver broadcastReceiver;

	public MyChatService() {
	}
	private BroadcastReceiver getBR() {
		return new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.i("service_intent",intent.getAction());
				if(intent.getAction().equals(MyChatService.RESULT_CH))
				{
					if(intent.hasExtra("msg")) {
						sendChat(intent.getStringExtra("msg"));
					}
				}
				unregisterReceiver(this);
				broadcastReceiver=getBR();
				registerReceiver(broadcastReceiver, new IntentFilter(MyChatService.RESULT_CH));
			}
		};
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//MainActivity.LooG("service", "onStartCommand");
		if(intent!=null)
			if(intent.hasExtra("lastDate"))
				lastDate=intent.getStringExtra("lastDate");

		devid = Calendar.getInstance().getTimeInMillis()+"";
		broadcastReceiver = getBR();
		getOnline();

		registerReceiver(broadcastReceiver, new IntentFilter(MyChatService.RESULT_CH));

		CountDownTimer countDownTimer = new CountDownTimer(1000000000,10000) {
			@Override
			public void onTick(long millisUntilFinished) {
				if(bStop)
					this.cancel();

				getChat();
			}

			@Override
			public void onFinish() {
				//MainActivity.LooG("service","countDownTimer-onFinish");
				this.start();
			}
		};
		countDownTimer.start();

		CountDownTimer countDownTimerOnline = new CountDownTimer(1000000000,60000) {
			@Override
			public void onTick(long millisUntilFinished) {
				if(bStop)
					this.cancel();

				getOnline();
			}

			@Override
			public void onFinish() {
				//MainActivity.LooG("service","countDownTimer-onFinish");
				this.start();
			}
		};
		countDownTimerOnline.start();

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		//MainActivity.LooG("service", "onDestroy");
		bStop=true;

		super.onDestroy();
	}
	private void getOnline() {
		//Log.i("service", "getOnline:1");

		String surl = "http://php.hagser.se/jsonc.php?online=1";
		AsyncTask<String,Integer,Integer> asyncTask = new AsyncTask<String, Integer, Integer>() {
			@Override
			protected Integer doInBackground(String... params) {
				boolean bDone = false;
				int iRetries = 0;
				while(!bDone && iRetries<4) {
					bDone=true;
					try {
						URL url = new URL(params[0]);
						HttpURLConnection connection = (HttpURLConnection) url.openConnection();
						String json = "";
						BufferedReader buffer = new BufferedReader(new InputStreamReader(connection.getInputStream()));
						String s;

						while ((s = buffer.readLine()) != null) {
							json += s.trim();
						}
						JSONArray jarr = new JSONArray(json);

						ArrayList<String> onlinelist = new ArrayList<>();

						for (int j = 0; j < jarr.length(); j++) {
							JSONObject jsonObject = jarr.getJSONObject(j);

							if (jsonObject.has("ip")) {
								onlinelist.add(jsonObject.getString("ip"));

							}
						}

						Intent intent_info = new Intent(RESULT_OK);
						intent_info.putExtra(RESULT_OL,"");
						intent_info.putExtra("onlineList", onlinelist);
						sendBroadcast(intent_info);

						//Log.i("service", "sendBroadcast:" + onlinelist.size());

						bDone=true;

					} catch (Exception e) {
						e.printStackTrace();
					}
					finally {
						iRetries++;
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}


				return bDone?1:0;
			}
		};
		asyncTask.execute(surl);
	}
	private void getChat() {
		//Log.i("service", "getChat:" + lastDate);

		String surl = "http://php.hagser.se/jsonc.php?lastDate=" + lastDate.replace(" ","%20");

		AsyncTask<String,Integer,Integer> asyncTask = new AsyncTask<String, Integer, Integer>() {
			@Override
			protected Integer doInBackground(String... params) {
				boolean bDone = false;
				int iRetries = 0;
				while(!bDone && iRetries<4) {
					bDone=true;
					try {
						URL url = new URL(params[0]);
						HttpURLConnection connection = (HttpURLConnection) url.openConnection();
						String json = "";
						BufferedReader buffer = new BufferedReader(new InputStreamReader(connection.getInputStream()));
						String s;

						while ((s = buffer.readLine()) != null) {
							json += s.trim();
						}
						JSONArray jarr = new JSONArray(json);

						ChatItem chatItem = new ChatItem(Parcel.obtain());
						String oldLastDate=lastDate;
						boolean bAlert=(!lastDate.equals("0") && jarr.length()>0);
						String msg="";
						String ip="";
						for (int j = 0; j < jarr.length(); j++) {
							JSONObject jsonObject = jarr.getJSONObject(j);
							HashMap<String,String> map=new HashMap<>();
							if (jsonObject.has("id") && jsonObject.has("at") && jsonObject.has("ip") && jsonObject.has("ms")) {
								ChatItem.Item itm = new ChatItem.Item(Parcel.obtain());
								map.put("id",jsonObject.getString("id"));
								lastDate = jsonObject.getString("at");
								map.put("at",lastDate);
								msg=jsonObject.getString("ms");
								ip=jsonObject.getString("ip");
								if(jsonObject.has("uip"))
									myip = jsonObject.getString("uip");

								map.put("ip",ip);
								map.put("ms",msg);
								itm.map=map;

								chatItem.itemSet.add(itm);
							}
						}
						//Log.i("msg", mymsg+"_"+msg);

						if(bAlert && !oldLastDate.equals(lastDate) && (!mymsg.equals(msg) || (mymsg.equals(msg) && !myip.equals(ip))))
							showNotification(msg);

						//Log.i("service", "chatItem:" + chatItem.itemSet.size());

						Intent intent_info = new Intent(RESULT_OK);
						intent_info.putExtra("chatItem",chatItem);
						sendBroadcast(intent_info);

						//Log.i("service", "sendBroadcast:" + chatItem.itemSet.size());

						bDone=true;

					} catch (Exception e) {
						e.printStackTrace();
					}
					finally {
						iRetries++;

						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}


				return bDone?1:0;
			}
		};
		asyncTask.execute(surl);
	}
	private void showNotification(String message) {
		NotificationCompat.Builder builder =
				new NotificationCompat.Builder(this)
						.setSmallIcon(R.mipmap.ic_launcher)
						.setContentTitle("New MyChat message")
						.setContentText(message);


		Intent notificationIntent = new Intent(this, MainActivity.class);

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		builder.setSound(alarmSound);
		builder.setContentIntent(contentIntent);
		builder.setAutoCancel(true);
		builder.setLights(Color.BLUE, 500, 500);
		long[] pattern = {100,50,100,50,100};
		builder.setVibrate(pattern);
		builder.setStyle(new NotificationCompat.InboxStyle());

		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		manager.notify(1, builder.build());
	}
	long lastSend=0;
	private void sendChat(String message) {
		mymsg=message;
		Log.i("service", "sendChat:" + message);

		AsyncTask<String,Integer,Integer> asyncTask = new AsyncTask<String, Integer, Integer>() {
			@Override
			protected Integer doInBackground(String... params) {
				boolean bDone = false;
				int iRetries = 0;
				while(!bDone && iRetries<4) {
					bDone=true;
					try {
						URL url = new URL("http://php.hagser.se/saveChat.php");
						Log.i("url",url.getPath());
						//Log.i("sendLocation-proto",url.getProtocol());
						HttpURLConnection connection = (HttpURLConnection) url.openConnection();

						try {
							if(devid.equals(""))
								devid = Calendar.getInstance().getTimeInMillis()+"";

							String data = "msg=" + params[0] + "&did="+devid;
							//Log.i("data",data);
							connection.setRequestMethod("POST");
							connection.setDoOutput(true);
							connection.setDoInput(true);

							connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
							connection.setFixedLengthStreamingMode(data.getBytes().length);
							OutputStream outputStream = new BufferedOutputStream(connection.getOutputStream());

							outputStream.write(data.getBytes());
							outputStream.flush();

							int resp_code = connection.getResponseCode();

							String json = "";
							BufferedReader buffer = new BufferedReader(new InputStreamReader(connection.getInputStream()));
							String s;

							while ((s = buffer.readLine()) != null) {
								json += s.trim();
							}
							//Log.i("sendChat-json", json.length() + "");
							if(json.length()>0) {
								JSONArray jarr = new JSONArray(json);

								int j = 0;
								JSONObject jsonObject = jarr.getJSONObject(j);
								if (jsonObject.has("id") && jsonObject.has("at") && jsonObject.has("ip") && jsonObject.has("ms")) {
									myid = jsonObject.getString("id");
									mymsg = jsonObject.getString("ms");
									myip = jsonObject.getString("ip");
								}
							}

							//Log.i("sendLocation-resp_code", resp_code + "");
							bDone=true;
						} catch (Exception ex) {
/*
							try {
								Thread.sleep(5000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
*/
							ex.printStackTrace();
						} finally {
							iRetries++;
							connection.disconnect();
						}

					} catch (MalformedURLException e) {
						//Log.e("sendLocation",e.getMessage());
						e.printStackTrace();
					} catch (IOException e) {
						//Log.e("sendLocation",e.getMessage());
						e.printStackTrace();
					}
				}


				return bDone?1:0;
			}
		};
		long now = Calendar.getInstance().getTimeInMillis();
		if((now-lastSend)>2000) {
			asyncTask.execute(message);
			lastSend=now;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO: Return the communication channel to the service.
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
