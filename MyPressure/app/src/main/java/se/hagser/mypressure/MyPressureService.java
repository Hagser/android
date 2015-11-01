package se.hagser.mypressure;

import java.util.Calendar;
import java.util.Date;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class MyPressureService extends Service implements SensorEventListener {

	SensorManager mSensorManager =null;
	LocationManager locationManager =null;
	LocationListener locationListener =null;
    DatabaseHelper mDbHelper=null;
    String lat=null;
    String lng=null;
    String alt=null;
    String pressure=null;
	public static final String KEY_PR="PR";
    public static final String KEY_LT="LT";
    public static final String KEY_LN="LN";
    public static final String KEY_AL="AL";
    public static final String KEY_AT="AT";
    public static final String KEY_CH="CH";
    public static final String RESULT="result";
    public static final String NOTIFICATION = "se.hagser.mypressure";
	private boolean bWifi;

	@Override
    public void onDestroy()
    {
		try
		{
	    	mDbHelper.close();
	    	locationManager.removeUpdates(locationListener);
			mSensorManager.unregisterListener(this);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		finally
		{
			super.onDestroy();			
		}
    }
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
		locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
	
		mDbHelper = new DatabaseHelper(this.getBaseContext());
		final WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		bWifi=(wifi.isWifiEnabled() && wifi.getConnectionInfo()!=null);

		Sensor prsensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
		mSensorManager.registerListener(this, prsensor,SensorManager.SENSOR_DELAY_NORMAL);

		makeUseOfNewLocation(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));

		locationListener = new LocationListener() {
		    public void onLocationChanged(Location location) {
		    	makeUseOfNewLocation(location);
		    }
	
			public void onStatusChanged(String provider, int status, Bundle extras) {}
		    public void onProviderEnabled(String provider) {}
		    public void onProviderDisabled(String provider) {}
		  };
	
		  long ihour = 10*60*1000;//hour*minutes*seconds*1000
		  try
		  {
			  locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, ihour, 0, locationListener);
		  }
		  catch(Exception e){e.printStackTrace();}
		  try
		  {
			  locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, ihour, 0, locationListener);
		  }
		  catch(Exception e){e.printStackTrace();}

			try
		  {
			  locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, ihour, 0, locationListener);
		  }
		  catch(Exception e){e.printStackTrace();}

		  return Service.START_STICKY;
	}

	public void makeUseOfNewLocation(Location location) {
        lat=getSubstring(location.getLatitude()+"",10);
		lng=getSubstring(location.getLongitude()+"",10);
		alt=getSubstring(location.getAltitude()+"",10);
		
	}
	private String getSubstring(String string, int ilen) {
		return string.substring(0, Math.min(ilen, string.length()));
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	long last=0;
	float lastval=0;
	@Override
	public void onSensorChanged(SensorEvent event) {

		long ts = event.timestamp;
		float val = Math.round(event.values[0]);
		if(ts-last>100000000||(lastval>0&&Math.abs(lastval-val)>1))
		{	
			last=ts;
			boolean balert = Math.abs(lastval-val)>4;
			lastval=val;
			if(event.sensor.getType()==Sensor.TYPE_PRESSURE)
			{
				pressure=val+"";
				String at = getDefaultDateTime();

				UpdateDbTask task = new UpdateDbTask(mDbHelper);

				final WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
				bWifi=(wifi.isWifiEnabled() && wifi.getConnectionInfo()!=null);

				task.execute(at, pressure, lat, lng, alt, bWifi + "");

			    Intent intent = new Intent(NOTIFICATION);
			    intent.putExtra(KEY_PR, pressure);
			    intent.putExtra(KEY_AT, at);
			    intent.putExtra(KEY_LT, lat);
			    intent.putExtra(KEY_LN, lng);
			    intent.putExtra(KEY_AL, alt);
			    intent.putExtra(KEY_CH, task.cache);
			    intent.putExtra(RESULT, android.app.Activity.RESULT_OK);
				intent.putExtra("ALERT",balert);
			    sendBroadcast(intent);
				Log.i(NOTIFICATION,"sendBroadcast");	

				try
				{
					mSensorManager.unregisterListener(this);
				}
				catch(Exception e){e.printStackTrace();}
			}
		}
		
	}

	public static String getDefaultDateTime() {

		Date mDate = new Date();

		Calendar c = Calendar.getInstance();
		c.setTime(mDate);

		return getDateString(c.get(Calendar.YEAR), c.get(Calendar.MONTH),
				c.get(Calendar.DAY_OF_MONTH)) + " " + getTimeString(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));

	}

	public static String getDateString(int year, int monthOfYear, int dayOfMonth) {

		monthOfYear++;
		String mon = "" + monthOfYear;
		String day = "" + dayOfMonth;

		if (monthOfYear < 10)
			mon = "0" + monthOfYear;
		if (dayOfMonth < 10)
			day = "0" + dayOfMonth;

		return year + "-" + mon + "-" + day;
	}

	public static String getTimeString(int hourOfDay, int minute) {
		String hour = "" + hourOfDay;
		String min = "" + minute;
		if (hourOfDay < 10)
			hour = "0" + hourOfDay;
		if (minute < 10)
			min = "0" + minute;

		return hour + ":" + min + ":00";
	}
}
