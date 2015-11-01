package se.hagser.mylocationpublisher;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.JsonReader;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MyService extends Service implements LocationListener {
	private android.location.LocationManager locationManager;
	public static String RESULT_RUN="MyServiceRunning";
	public static String RESULT_ADR="MyServiceAddress";
	public static String RESULT_SET="SettingsChanged";
	public static String RESULT_OK="MyServiceOK";
	public static String B="BEAR";
	public static String LA="LAT";
	public static String LO="LNG";
	public static String SP="SPEED";
	public static String TI="TIME";
	public static String TX="TEXT";
	public static String UUID="uuid";
	private boolean bStop = false;
	Location lastLocation=null;
	BroadcastReceiver broadcastReceiver=null;
	public MyService() {

	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		MainActivity.LooG("service","onStartCommand");
		locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		final SharedPreferences keyValues = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
		if(!keyValues.contains(UUID))
			getDeviceId();

		boolean detailed = keyValues.contains(SettingsActivity.detailed_location_key) && keyValues.getBoolean(SettingsActivity.detailed_location_key, false);
		boolean minimal = keyValues.contains(SettingsActivity.minimal_location_key) && keyValues.getBoolean(SettingsActivity.minimal_location_key, false);

		int milliseconds = (minimal?6:(detailed?1:3))*60000;
		float distance = (minimal?1000:(detailed?100:500));
		try
		{
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, milliseconds, distance, this);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		Intent intent_info = new Intent(RESULT_OK);
		intent_info.putExtra(TX,"Service started");
		intent_info.putExtra(TI, Calendar.getInstance().getTimeInMillis());
		sendBroadcast(intent_info);

		BroadcastReceiver broadcastReceiverSettings = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				try
				{
					locationManager.removeUpdates(MyService.this);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}

				boolean detailed = keyValues.contains(SettingsActivity.detailed_location_key) && keyValues.getBoolean(SettingsActivity.detailed_location_key, false);
				boolean minimal = keyValues.contains(SettingsActivity.minimal_location_key) && keyValues.getBoolean(SettingsActivity.minimal_location_key, false);

				int milliseconds = (minimal?6:(detailed?1:3))*60000;
				float distance = (minimal?1000:(detailed?100:500));
				try
				{
					locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, milliseconds, distance, MyService.this);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		};
		broadcastReceiver=broadcastReceiverSettings;
		registerReceiver(broadcastReceiver, new IntentFilter(MyService.RESULT_SET));

		CountDownTimer countDownTimer = new CountDownTimer(1000000000,1000) {
			@Override
			public void onTick(long millisUntilFinished) {
				MainActivity.LooG("service","countDownTimer-onTick");
				Intent intent_started = new Intent(RESULT_RUN);

				if(lastLocation!=null)
				{
					float bearing = lastLocation.getBearing();
					double lat = lastLocation.getLatitude();
					double lng = lastLocation.getLongitude();
					float speed = lastLocation.getSpeed();
					long tim = lastLocation.getTime();

					intent_started.putExtra(B,bearing);
					intent_started.putExtra(LA,lat);
					intent_started.putExtra(LO,lng);
					intent_started.putExtra(SP, speed);
					intent_started.putExtra(TI, tim);
				}
				else
				{
					intent_started.putExtra(TI, Calendar.getInstance().getTimeInMillis());
				}

				sendBroadcast(intent_started);
				if(!keyValues.contains(UUID))
					getDeviceId();
				if(bStop)
					this.cancel();
			}

			@Override
			public void onFinish() {
				MainActivity.LooG("service","countDownTimer-onFinish");
				this.start();
			}
		};
		countDownTimer.start();

		return START_STICKY;
	}
	@Override
	public void onDestroy() {
		MainActivity.LooG("service", "onDestroy");
		bStop=true;
		locationManager.removeUpdates(this);
		try
		{
			if(broadcastReceiver!=null)
				unregisterReceiver(broadcastReceiver);
		}
		catch (Exception e)
		{e.printStackTrace();}

		super.onDestroy();
	}
		@Override
	public IBinder onBind(Intent intent) {
		// TODO: Return the communication channel to the service.
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public void onLocationChanged(Location location) {
		MainActivity.LooG("service","onLocationChanged");

		lastLocation=location;
		float bearing = location.getBearing();
		double lat = location.getLatitude();
		double lng = location.getLongitude();
		float speed = location.getSpeed();
		long tim = location.getTime();
		float acc = location.getAccuracy();


		Intent intent_info = new Intent(RESULT_OK);
		intent_info.putExtra(B,bearing);
		intent_info.putExtra(LA,lat);
		intent_info.putExtra(LO,lng);
		intent_info.putExtra(SP, speed);
		intent_info.putExtra(TI, tim);
		sendBroadcast(intent_info);

		getAddress(lat, lng);
		sendLocation(lat, lng, speed, bearing,acc, new Date(tim));
	}
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
/*
		Intent intent_info = new Intent(RESULT_OK);
		intent_info.putExtra(TX,"onStatusChanged-"+provider);
		intent_info.putExtra(TI, Calendar.getInstance().getTimeInMillis());
		sendBroadcast(intent_info);
*/

	}

	@Override
	public void onProviderEnabled(String provider) {
/*
		Intent intent_info = new Intent(RESULT_OK);
		intent_info.putExtra(TX,"onProviderEnabled-"+provider);
		intent_info.putExtra(TI, Calendar.getInstance().getTimeInMillis());
		sendBroadcast(intent_info);
*/
	}

	@Override
	public void onProviderDisabled(String provider) {
/*
		Intent intent_info = new Intent(RESULT_OK);
		intent_info.putExtra(TX,"onProviderDisabled-"+provider);
		intent_info.putExtra(TI, Calendar.getInstance().getTimeInMillis());
		sendBroadcast(intent_info);
*/
	}
	String minLat="";
	String minLng="";

	private void getDeviceId() {

		String url = "http://php.hagser.se/getid.php";
		final SharedPreferences keyValues = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
		AsyncTask<String,Integer,Integer> asyncTask = new AsyncTask<String, Integer, Integer>() {
			@Override
			protected Integer doInBackground(String... params) {
				boolean bDone = false;
				int iRetries = 0;
				while(!bDone && iRetries<4) {

					try {

						URL url = new URL(params[0]);

						HttpURLConnection connection = (HttpURLConnection) url.openConnection();
						connection.setRequestProperty("Accept", "application/json");
						connection.setRequestProperty("Content-Type", "application/json");
						String androidOS = Build.VERSION.RELEASE;
						connection.setRequestProperty("User-Agent", androidOS);

						String json = "";
						BufferedReader buffer = new BufferedReader(new InputStreamReader(connection.getInputStream()));
						String s;

						while ((s = buffer.readLine()) != null) {
							json += s.trim();
						}

						MainActivity.LooG("getDevice-json", json);
						JSONArray jarrr = new JSONArray(json);
						JSONObject jsonObject = jarrr.getJSONObject(0);
						String devid = "";
						if (jsonObject.has("deviceid")) {
							MainActivity.LooG("getDevice-json", json);
							devid = jsonObject.getString("deviceid");
							keyValues.edit().putString(UUID, devid).apply();
						}
						bDone = true;

					} catch (MalformedURLException e1) {
						e1.printStackTrace();
					} catch (JSONException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						iRetries++;
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				return bDone?1:0;
			}
		};

		asyncTask.execute(url);
	}
//https://maps.googleapis.com/maps/api/geocode/json?address=61430%20S%C3%B6derk%C3%B6ping%20Sverige&key=AIzaSyABRrBsYabaiFnaw6QL4gikjy6KGxN8tGA

	private void getAddress(double lat,double lng) {
		MainActivity.LooG("service","getAddress:"+lat+","+lng);

		String key = "AIzaSyABRrBsYabaiFnaw6QL4gikjy6KGxN8tGA";
		String surl = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + lat + "," + lng + "&key=" + key + "&sensor=false";
		AsyncTask<String,Integer,Integer> asyncTask = new AsyncTask<String, Integer, Integer>() {
			@Override
			protected Integer doInBackground(String... params) {
				boolean bDone = false;
				int iRetries = 0;
				while(!bDone && iRetries<4) {

					try {
						URL url = new URL(params[0]);
						HttpURLConnection connection = (HttpURLConnection) url.openConnection();
						String json = "";
						BufferedReader buffer = new BufferedReader(new InputStreamReader(connection.getInputStream()));
						String s;

						while ((s = buffer.readLine()) != null) {
							json += s.trim();
						}
						JSONObject jsonObject1 = new JSONObject(json);
						JSONArray jarr = jsonObject1.getJSONArray("results");
						//Log.i("getAddress-jarr", jarr.length() + "");
						String address = "";
						for (int j = 0; j < jarr.length(); j++) {
							JSONObject jsonObject = jarr.getJSONObject(j);
							//Log.i("getAddress-jsonObject", "");
							if (jsonObject.has("types") && jsonObject.has("formatted_address")) {
								//Log.i("getAddress-jsonObject", "has");
								JSONArray typearr = jsonObject.getJSONArray("types");
								//Log.i("getAddress-typearr", typearr.length()+"");
								for (int t = 0; t < typearr.length(); t++) {
									if (typearr.getString(t).equals("postal_town")) {
										address = jsonObject.getString("formatted_address");
										if(jsonObject.has("location")) {
											JSONObject jo = jsonObject.getJSONObject("location");
											minLat = jo.has("lat") ? jo.getString("lat") : "";
											minLng = jo.has("lng") ? jo.getString("lng") : "";
										}
										break;
									}
									if (address.equals("") && typearr.getString(t).equals("postal_code")) {
										address += jsonObject.getString("formatted_address");
										if(jsonObject.has("location")) {
											JSONObject jo = jsonObject.getJSONObject("location");
											minLat = jo.has("lat") ? jo.getString("lat") : "";
											minLng = jo.has("lng") ? jo.getString("lng") : "";
										}

									}
									if (address.equals("") && typearr.getString(t).equals("country")) {
										address += jsonObject.getString("formatted_address");
									}
								}

								//Log.i("getAddress-address", address);
							}
						}

						Intent intent_info = new Intent(RESULT_OK);
						intent_info.putExtra(RESULT_ADR, "OK");
						intent_info.putExtra(TX, address);
						sendBroadcast(intent_info);


						int resp_code = connection.getResponseCode();
						bDone=true;

					} catch (Exception e) {
						e.printStackTrace();
					}
					finally {
						iRetries++;
						try {
							Thread.sleep(5000);
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
	private void sendLocation(double lat, double lng, float speed, float bearing,float acc, Date date) {
		MainActivity.LooG("service","sendLocation:"+lat+","+lng);
		final SharedPreferences keyValues = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
		final boolean detailed = keyValues.contains(SettingsActivity.detailed_location_key) && keyValues.getBoolean(SettingsActivity.detailed_location_key, false);
		final boolean minimal = keyValues.contains(SettingsActivity.minimal_location_key) && keyValues.getBoolean(SettingsActivity.minimal_location_key, false);
		AsyncTask<String,Integer,Integer> asyncTask = new AsyncTask<String,Integer,Integer>() {
			@Override
			protected Integer doInBackground(String... params) {
				boolean bDone = false;
				int iRetries = 0;
				while(!bDone && iRetries<4)
				{
					try {
						URL url = new URL("http://php.hagser.se/saveLocation.php");
						//Log.i("sendLocation-proto",url.getProtocol());
						HttpURLConnection connection = (HttpURLConnection) url.openConnection();

						try {
							Date send_date = new Date(Long.parseLong(params[5]));
							String format = "yyyy-MM-dd HH:mm";
							SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.ENGLISH);
							String strDate = sdf.format(send_date);
							String devid = keyValues.getString(UUID, "");
							String lat =minimal&&!minLat.equals("")?minLat: params[0];
							String lng =minimal&&!minLng.equals("")?minLng: params[1];
							if (!detailed) {
								lat = String.format("%.2f", Double.parseDouble(lat));
								lng = String.format("%.2f", Double.parseDouble(lng));
							}

							String data =
									"lat=" + lat +
											"&lng=" + lng +
											"&speed=" + params[2] +
											"&bearing=" + params[3] +
											"&deviceid=" + devid +
											"&minimal=" + (minimal?1:0) +
											"&date=" + strDate +
											"&acc=" + params[4];
							connection.setRequestMethod("POST");
							connection.setDoOutput(true);
							connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
							connection.setFixedLengthStreamingMode(data.getBytes().length);
							OutputStream outputStream = new BufferedOutputStream(connection.getOutputStream());

							outputStream.write(data.getBytes());
							outputStream.flush();
							int resp_code = connection.getResponseCode();
							Log.i("sendLocation-resp_code", resp_code + "");
							bDone=true;
						} catch (Exception ex) {

							try {
								Thread.sleep(5000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
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
		Log.i("sendLocation-asyncTask:", lat + "," + lng + "," + speed + "," + bearing + "," + acc);
		asyncTask.execute(lat + "", lng + "", speed + "", bearing + "",acc+"", date.getTime() + "");
	}

	private String getSubstring(String string, int ilen) {
		return string.substring(0, Math.min(ilen, string.length()));
	}
}
