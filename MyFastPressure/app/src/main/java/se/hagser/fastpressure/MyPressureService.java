package se.hagser.fastpressure;

import java.util.Calendar;
import java.util.Date;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;

public class MyPressureService extends Service implements SensorEventListener {

	SensorManager mSensorManager =null;
    String pressure=null;
    public static final String KEY_PR="PR";
    public static final String KEY_AT="AT";
    public static final String RESULT="result";
    public static final String NOTIFICATION = "se.hagser.fastpressure";
    @Override
    public void onDestroy()
    {
		try
		{
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

        Sensor prsensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        mSensorManager.registerListener(MyPressureService.this, prsensor, 500);
		
		  return Service.START_STICKY;
	}


	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	long last=0;
    float lastval = 0;
	@Override
	public void onSensorChanged(SensorEvent event) {

        float val = Math.round(event.values[0]);
		long ts = event.timestamp;
        long diff= ts-last;
        long limit = Long.parseLong("1000000000");
        long limitm = Long.parseLong("60000000000");

        if((lastval!=val && diff>limit)||diff>limitm)
		{	
			last=ts;
            lastval=val;
			if(event.sensor.getType()==Sensor.TYPE_PRESSURE)
			{
				pressure=val+"";
				String at = getDefaultDateTime();

			    Intent intent = new Intent(NOTIFICATION);
			    intent.putExtra(KEY_PR, pressure);
			    intent.putExtra(KEY_AT, at);
			    intent.putExtra(RESULT, android.app.Activity.RESULT_OK);
			    sendBroadcast(intent);
				//Log.i(NOTIFICATION,"sendBroadcast");

			}
		}
        else {
            //Log.i("diff",(diff)+"/"+limit);
            //Log.i("val",val+"/"+lastval);
        }
		
	}

	public static String getDefaultDateTime() {

		Date mDate = new Date();

		Calendar c = Calendar.getInstance();
		c.setTime(mDate);

		return getDateString(c.get(Calendar.YEAR), c.get(Calendar.MONTH),
				c.get(Calendar.DAY_OF_MONTH)) + " " + getTimeString(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE),
						c.get(Calendar.SECOND));

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

	public static String getTimeString(int hourOfDay, int minute, int second) {
		String hour = "" + hourOfDay;
		String min = "" + minute;
        String sec = "" + second;

		if (hourOfDay < 10)
			hour = "0" + hourOfDay;
		if (minute < 10)
			min = "0" + minute;
        if (second < 10)
            sec = "0" + second;


        return hour + ":" + min + ":"+sec;
	}
}
