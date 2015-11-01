package se.hagser.myspeedcams;

import java.util.ArrayList;
import java.util.HashMap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;

import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.*;

public class MainActivity extends Activity implements SensorEventListener {
    protected ListAdapter mAdapter;
    ListView spinner=null;
    ArrayList<HashMap<String, String>> rows=new ArrayList<HashMap<String, String>>();
    DatabaseHelper mDbHelper=null;
    
    static final String KEY_NAME = "name";
    static final String KEY_LAT = "lat";
    static final String KEY_LNG = "lng";
    static final String KEY_DIST = "dist";
    static final String KEY_MYBEARING = "mybearing";
    static final String KEY_BEARING = "bearing";
    
    protected PowerManager.WakeLock mWakeLock;
    LocationManager locationManager=null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);


        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
        this.mWakeLock.acquire(2*60*60*1000);		
		
		spinner = (ListView) findViewById(R.id.listView1);

        spinner.setAdapter(this.mAdapter);

		mDbHelper = new DatabaseHelper(this.getBaseContext());

		SensorManager mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        Sensor msensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        mSensorManager.registerListener(this, msensor, 0);
        
		locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		LocationListener locationListener = new LocationListener() {
		    public void onLocationChanged(Location location) {
		      // Called when a new location is found by the network location provider.
		      makeUseOfNewLocation(location);
		      //Log.i(location.getProvider(),location.getLatitude()+","+location.getLongitude() );
		    }


			public void onStatusChanged(String provider, int status, Bundle extras) {}

		    public void onProviderEnabled(String provider) {}

		    public void onProviderDisabled(String provider) {}
		  };
		  
		  makeUseOfNewLocation(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));

		// Register the listener with the Location Manager to receive location updates
		  /*
		  try
		  {
			  locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 50, locationListenerP);
		  }
		  catch(Exception e){}
		  try
		  {
			  locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 1000, 50, locationListenerP);
		  }
		  catch(Exception e){}
		  */
		  try
		  {
			  locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 10, locationListener);
		  }
		  catch(Exception e){}
	
	}
	@Override
	public void onResume()
	{
		if(!this.mWakeLock.isHeld())
			this.mWakeLock.acquire(2*60*60*1000);
		
	    super.onResume();	
	}
    @Override
    public void onPause() {
    	try
    	{
    		if(this.mWakeLock.isHeld())
    			this.mWakeLock.release();
    	}
    	catch(Exception e){}
        super.onPause();
    }
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putStringArrayList("alertList",alertList);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		alertList=savedInstanceState.getStringArrayList("alertList");
	}
	ArrayList<String> alertList = new ArrayList<String>();
	boolean balarm=false;
    private void makeUseOfNewLocation(Location location) {

		if(location==null)
			return;

		TextView tv = (TextView) findViewById(R.id.speedtext);
    	tv.setText((location.getSpeed()>0?Math.round(location.getSpeed()*3.6):location.getSpeed())+"");
    	
    	
		rows.clear();
		try
		{
			mDbHelper.openDataBase();
		}
		catch(SQLiteException ex)
		{
			ex.printStackTrace();
			return;
		}
		
		SQLiteDatabase db = mDbHelper.getReadableDatabase();
		db.beginTransaction();
		String sql = "select Name,Lat,Lng,abs(Lat-"+ location.getLatitude() +")+abs(Lng-"+ location.getLongitude() +")LtLn from cameras order by LtLn limit 20";
		String[] selectionArgs = null;
		Cursor c = null;
		try
		{
			c = db.rawQuery(sql, selectionArgs);
			while(c.moveToNext())
			{			
				String name = c.getString(0);
				double Lat = c.getDouble(1);
				double Lng = c.getDouble(2);
				float[] results = {0,0};
				Location.distanceBetween(Lat, Lng, location.getLatitude(), location.getLongitude(), results);

                HashMap<String, String> map = new HashMap<String, String>();
                float fdist = Math.round(results[0]);
                map.put(KEY_NAME, name);
	            map.put(KEY_LAT, Lat+"");
	            map.put(KEY_LNG, Lng+"");
	            map.put(KEY_DIST, fdist+"");
	            map.put(KEY_BEARING,180- (results.length>1? Math.round(results[1]):0)+"");
	            map.put(KEY_MYBEARING,180- (results.length>1? Math.round(results[1]):0)+"");
	    		
	            if(!balarm && fdist<1000 && !alertList.contains(name))
	            {
	            	balarm=true;
	            	alertList.add(name);
	            	
	            	try {
	                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
	                    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
	                    r.play();
	                    
	                } catch (Exception e) {
	                	e.printStackTrace();
	                }	
	                

	            }
	            rows.add(map);
			}

		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		finally
		{
			c.close();
			db.endTransaction();
			db.close();
			mDbHelper.close();
		}

		mAdapter = new ListAdapter(this,rows);
		
        spinner.setAdapter(this.mAdapter);
        // Click event for single list row
        spinner.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos,long id) {
				
			}
        });	
	}

	private float parseFloat(String string) {
		if(string!=null)
			return Float.parseFloat(string);
		return 0;
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}
	
	long last=0;
	float val=0;
	@Override
	public void onSensorChanged(SensorEvent event) {
		long ts = event.timestamp;
		int idx=0;
		if(ts-last>100000000 && val!=Math.round(event.values[idx]))
		{
			last=ts;
			
			//updateList(val);
			
			val = 360-Math.round(event.values[idx]);

			ImageView iv = (ImageView) findViewById(R.id.compass);
			TextView tv = (TextView) findViewById(R.id.compasstext);
			
			
			setRotation(iv,val);
			tv.setText(val+"");
			try
			{
				TextView txtmybear = (TextView)findViewById(R.id.mybearingtext);
				ImageView imgmybear = (ImageView)findViewById(R.id.mybearing);
				setRotation(imgmybear,val);
				txtmybear.setText(val+"");
			}
			catch(Exception e){}
		}
	}
	private void updateList(float val2) {
		for(int i=0;i<rows.size();i++)
		{
			HashMap<String, String> hm = rows.get(i);
			String bear = hm.get(KEY_BEARING);
			hm.put(KEY_BEARING, bear);
		}
		mAdapter = new ListAdapter(this,rows);
		
	}

	private void setRotation(ImageView iv, float rotation) {
		iv.setRotation(rotation);
		
	}

}
