package se.hagser.myroadchecker;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class MyAccService extends Service implements SensorEventListener {

    SensorManager mSensorManager =null;

    LocationManager locationManager =null;
    LocationListener locationListener =null;
    DatabaseHelper mDbHelper=null;
    String lat=null;
    String lng=null;
	long loglimit = 1;
	long speedlimit = 1;
	long difflimit = 1;
	long timelimit = 60;
	public static boolean bDebug=false;
    public static final String KEY_X="X";
    public static final String KEY_Y="Y";
    public static final String KEY_Z="Z";
    public static final String KEY_LAT="LAT";
    public static final String KEY_LON="LON";
    public static final String KEY_AT="AT";
    public static final String KEY_AL="AL";
    public static final String KEY_CNT="CNT";
	public static final String KEY_SPEED="SPEED";
	public static final String SL="speedlimit";
	public static final String DL="difflimit";
	public static final String TL="timelimit";
	public static final String BT="btactivate";
    public static final String WO="wifioverride";
	public static final String DEV="device";
    public static final String RESULT="result";
    public static final String NOTIFICATION = "se.hagser.myrc";
	SharedPreferences keyValues = null;
	private float speed=0;
	ArrayList<UpdateDbTask> tasks = new ArrayList<>();

	long last=0;
	float lastvalx = 0;
	float lastvaly = 0;
	float lastvalz = 0;
	float vallat=0;
	float vallon=0;

	@Override
    public void onDestroy()
    {
        try
        {
            //mDbHelper.close();

			mSensorManager.unregisterListener(this);
            locationManager.removeUpdates(locationListener);
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

        //Sensor prsensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Sensor gyrosensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Sensor gravitysensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        //mSensorManager.registerListener(MyAccService.this, prsensor, 100);
        mSensorManager.registerListener(MyAccService.this, gyrosensor,SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(MyAccService.this, gravitysensor, SensorManager.SENSOR_DELAY_NORMAL);


        mDbHelper = new DatabaseHelper(this.getBaseContext());

		keyValues = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

		try {
			difflimit = Long.parseLong(keyValues.getString(DL, difflimit+""));
			timelimit = Long.parseLong(keyValues.getString(TL, timelimit+""));
			speedlimit = Long.parseLong(keyValues.getString(SL, speedlimit+""));
		}
		catch(Exception ex)
		{
			LogI(NOTIFICATION,"difflimit:"+keyValues.getString(DL,"sososodde"));
			ex.printStackTrace();
		}
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                makeUseOfNewLocation(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
        };

        //long ihour = 1*60*60*1000;
        long ihour = 1000;
        try
        {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, ihour, 10, locationListener);
        }
        catch(Exception e){}
        try
        {
            locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, ihour, 10, locationListener);
        }
        catch(Exception e){}
        try
        {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, ihour, 10, locationListener);
        }
        catch(Exception e){}

        return Service.START_STICKY;
    }

    public void makeUseOfNewLocation(Location location) {
		speed = location.getSpeed();
        lat=getSubstring(location.getLatitude()+"",10);
        lng=getSubstring(location.getLongitude()+"",10);
        vallat=Float.parseFloat(lat);
        vallon=Float.parseFloat(lng);
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
    boolean bDeviceIsStill=false;
    int idx_axel=0;
    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType()==Sensor.TYPE_GYROSCOPE && bDeviceIsStill) {
            try {
                difflimit = Long.parseLong(keyValues.getString(DL, difflimit + ""));
                timelimit = Long.parseLong(keyValues.getString(TL, timelimit + ""));
                speedlimit = Long.parseLong(keyValues.getString(SL, speedlimit + ""));
            } catch (Exception ex) {
                //ex.printStackTrace();
            }
            //float valx = Math.round(event.values[idx_axel]*100);
            float valx = Math.round(event.values[0]*100);
            float valy = Math.round(event.values[1]*100);
            float valz = Math.round(event.values[2]*100);
            long ts = event.timestamp;//1000000000=1 sec
            long diff = ts - last;
            long limit = bDebug ? Long.parseLong("1000000000") : timelimit * Long.parseLong("1000000000");
            long limitm = Long.parseLong("60000000000");

            boolean balarm = ((diff(lastvalx, valx) > difflimit) || (diff(lastvaly, valy) > difflimit) || (diff(lastvalz, valz) > difflimit));
            boolean lalarm = bDebug && ((diff(lastvalx, valx) > loglimit) || (diff(lastvaly, valy) > loglimit) || (diff(lastvalz, valz) > loglimit));
            //boolean balarm = ((diff(lastvalx, valx) > difflimit));
            //boolean lalarm = bDebug && ((diff(lastvalx, valx) > loglimit) );

            boolean salarm = ((speed * 3.6) >= speedlimit);
            if (
                    (((diff > limit) || (diff > limitm)) || balarm || lalarm) && salarm
                    )
            {
                last = ts;
                lastvalx = valx;
                lastvaly = valy;
                lastvalz = valz;

                String at = getDefaultDateTime();
                if ((balarm || lalarm) && salarm) {
                    //execute(at, valx + "", valy + "", valz + "", vallat + "", vallon + "", speed + "", "0");
                    execute(at, valx + "", "", "", vallat + "", vallon + "", speed + "", "0");
                }
                if ((diff > limit) || (diff > limitm)) {
                    Intent intent = new Intent(NOTIFICATION);
                    intent.putExtra(KEY_X, valx + "");
                    intent.putExtra(KEY_Y, valy + "");
                    intent.putExtra(KEY_Z, valz + "");
                    intent.putExtra(KEY_LAT, vallat + "");
                    intent.putExtra(KEY_LON, vallon + "");
                    intent.putExtra(KEY_SPEED, speed + "");
                    intent.putExtra(KEY_AL, balarm);
                    intent.putExtra(KEY_AT, at);
                    intent.putExtra(KEY_CNT, "");
                    intent.putExtra(RESULT, android.app.Activity.RESULT_OK);
                    sendBroadcast(intent);
                }
                LogI(NOTIFICATION, "sendBroadcast-difflimit:" + difflimit);


            }
        }
        else if(event.sensor.getType()==Sensor.TYPE_GYROSCOPE)
        {
            float valx = event.values[0];
            float valy = event.values[1];
            float valz = event.values[2];

            float val = Math.abs(valx)+Math.abs(valy)+Math.abs(valz);
            bDeviceIsStill=(val<0.5);
        }
        else if(event.sensor.getType()==Sensor.TYPE_GRAVITY)
        {
            float valx = Math.abs(event.values[0]);
            float valy = Math.abs(event.values[1]);
            float valz = Math.abs(event.values[2]);

            if(valx>=valy) {
                if (valx >= valz) {
                    idx_axel = 0;
                } else {
                    idx_axel = 2;
                }
            }
            else {
                if (valy >= valz) {
                    idx_axel = 1;
                } else {
                    idx_axel = 2;
                }
            }

        }
    }
    private void execute(final String at,final String s,final String s1,final String s2,final String s3,final String s4,final String s5,final String s6) {
        LogI(NOTIFICATION + " execute",at);
        tasks.add(new UpdateDbTask(mDbHelper));
        LogI(NOTIFICATION + " execute-tasks", tasks.size() + "");
        final CountDownTimer countDownTimer = new CountDownTimer(10000,500) {
            @Override
            public void onTick(long millisUntilFinished) {
                if(tasks.size()>0) {
                    UpdateDbTask task = tasks.get(0);
                    AsyncTask.Status sts = task.getStatus();
                    LogI(NOTIFICATION + " execute-sts", sts.name() + " " + at);
                    if (!sts.equals(AsyncTask.Status.RUNNING) && !sts.equals(AsyncTask.Status.FINISHED)) {
                        LogI(NOTIFICATION + " execute-start", at);
                        try {
                            task.execute(at, s, s1, s2, s3, s4, s5,s6);
                        } catch (Exception ex) {
                            LogI(NOTIFICATION + " execute-err", at + " " + ex.getMessage());
                        }
                    } else if (sts.equals(AsyncTask.Status.FINISHED)) {
                        tasks.remove(0);
                        LogI(NOTIFICATION + " execute-tasks", tasks.size() + "");
                    }
                }
            }

            @Override
            public void onFinish() {
                if(tasks.size()>0) {
                    UpdateDbTask task = tasks.get(0);
                    AsyncTask.Status sts = task.getStatus();
                    LogI(NOTIFICATION + " execute-sts", sts.name() + " " + at);
                    if (!sts.equals(AsyncTask.Status.RUNNING) && !sts.equals(AsyncTask.Status.FINISHED)) {
                        LogI(NOTIFICATION + " execute-start", at);
                        try {
                            task.execute(at, s, s1, s2, s3, s4, s5,s6);
                        } catch (Exception ex) {
                            LogI(NOTIFICATION + " execute-err", at + " " + ex.getMessage());
                        }
                    } else if (sts.equals(AsyncTask.Status.FINISHED)) {
                        tasks.remove(0);
                        LogI(NOTIFICATION + " execute-tasks", tasks.size() + "");
                    }
                }
            }
        };
        countDownTimer.start();
    }

    public static float diff(float d1,float d2)
    {
		return (Math.max(d1,d2)-Math.min(d1,d2));
    }
    public static String getDefaultDateTime() {

        Date mDate = new Date();

        Calendar c = Calendar.getInstance();
        c.setTime(mDate);

        return getDateString(c.get(Calendar.YEAR), c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)) + " " + getTimeString(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE),
                c.get(Calendar.SECOND),c.get(Calendar.MILLISECOND));

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

    public static String getTimeString(int hourOfDay, int minute, int second, int millisecond) {
        String hour = "" + hourOfDay;
        String min = "" + minute;
        String sec = "" + second;
        String millisec = "" + millisecond;

        if (hourOfDay < 10)
            hour = "0" + hourOfDay;
        if (minute < 10)
            min = "0" + minute;
        if (second < 10)
            sec = "0" + second;
        if (millisecond < 10)
            millisec = "00" + millisecond;
        else if (millisecond < 100)
            millisec = "0" + millisecond;


        return hour + ":" + min + ":"+sec+"."+millisec;
    }
    private void LogI(String tag,String msg)
    {
        if(bDebug)
            Log.i(tag,msg);

    }
}
