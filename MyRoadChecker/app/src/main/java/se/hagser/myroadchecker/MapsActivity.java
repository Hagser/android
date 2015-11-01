package se.hagser.myroadchecker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.HashMap;

public class MapsActivity extends FragmentActivity {

	private GoogleMap mMap; // Might be null if Google Play services APK is not available.
	LocationManager locationManager =null;
	LocationListener locationListener =null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);
		setUpMapIfNeeded();
	}
boolean bMoveCam=true;
	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(receiver, new IntentFilter(MyAccService.NOTIFICATION));
		setUpMapIfNeeded();

		mDbHelper = new DatabaseHelper(this.getBaseContext());

		keyValues = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

		locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

		locationListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				if(mMap!=null) {

					if((location.getLatitude()!=0.0||location.getLongitude()!=0.0) && bMoveCam) {
						lastlatlng = new LatLng(location.getLatitude(), location.getLongitude());
						CameraUpdate update = CameraUpdateFactory.newLatLng(lastlatlng);
						mMap.moveCamera(update);
					}
				}
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {}
			public void onProviderEnabled(String provider) {}
			public void onProviderDisabled(String provider) {}
		};

		//long ihour = 1*60*60*1000;
		Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if(location!=null) {
			if (location.getLatitude() != 0.0 || location.getLongitude() != 0.0) {
				lastlatlng = new LatLng(location.getLatitude(), location.getLongitude());
			}
		}
		long ihour = 1000;
		try
		{
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, ihour, 0, locationListener);
		}
		catch(Exception e){}
		try
		{
			locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, ihour, 0, locationListener);
		}
		catch(Exception e){}
		try
		{
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, ihour, 0, locationListener);
		}
		catch(Exception e){}
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(receiver);
	}
	@Override
	public void onDestroy()
	{
		try
		{
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

	private void setUpMapIfNeeded() {
		// Do a null check to confirm that we have not already instantiated the map.
		if (mMap == null) {
			// Try to obtain the map from the SupportMapFragment.
			mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
					.getMap();
			// Check if we were successful in obtaining the map.
			if (mMap != null) {
				mMap.setMyLocationEnabled(true);
				setUpMap();
			}
		}
	}

	/**
	 * This is where we can add markers or lines, add listeners or move the camera. In this case, we
	 * just add a marker near Africa.
	 * <p/>
	 * This should only be called once and when we are sure that {@link #mMap} is not null.
	 */
	private void setUpMap() {
		//mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
	}

	ArrayList<HashMap<String, String>> rows=new ArrayList<HashMap<String, String>>();
	private LatLng lastlatlng=null;
	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle bundle = intent.getExtras();
			if (bundle != null) {
				String x = bundle.getString(MyAccService.KEY_X);
				String y = bundle.getString(MyAccService.KEY_Y);
				String z = bundle.getString(MyAccService.KEY_Z);
				String lat = bundle.getString(MyAccService.KEY_LAT);
				String lon = bundle.getString(MyAccService.KEY_LON);
				boolean al = bundle.getBoolean(MyAccService.KEY_AL);
				String at = bundle.getString(MyAccService.KEY_AT);
				String cnt = bundle.getString(MyAccService.KEY_CNT);
				double s = bundle.getFloat(MyAccService.KEY_SPEED);
				int resultCode = bundle.getInt(MyAccService.RESULT);
				if (resultCode == RESULT_OK) {
					HashMap<String, String> map = new HashMap<String, String>();
					map.put(MyAccService.KEY_X,x);
					map.put(MyAccService.KEY_Y,y);
					map.put(MyAccService.KEY_Z,z);
					map.put(MyAccService.KEY_LAT,lat);
					map.put(MyAccService.KEY_LON, lon);
					map.put(MyAccService.KEY_AL, al+"");
					map.put(MyAccService.KEY_SPEED, s+"");
					rows.add(map);

					PolylineOptions polyline = new PolylineOptions();

					LatLng latlng=new LatLng(Float.parseFloat(lat),Float.parseFloat(lon));

					float ptsx = Float.parseFloat(map.get(MyAccService.KEY_X));
					float ptsy = Float.parseFloat(map.get(MyAccService.KEY_Y));
					float ptsz=Float.parseFloat(map.get(MyAccService.KEY_Z));
					double fmax = Math.max(ptsz, Math.max(ptsy, Math.max(1, ptsx)));
					if(al)
					{
						CircleOptions options = new CircleOptions();
						options.center(latlng);
						options.strokeColor(getColor(s));
						options.radius(fmax);
						options.fillColor(getColor(s));
						mMap.addCircle(options);
					}
					if(lastlatlng!=null && lastlatlng!=latlng)
					{
						if(lastlatlng.latitude!=0.0 && lastlatlng.longitude!=0.0)
							polyline.add(lastlatlng);
					}
					polyline.add(latlng);
					lastlatlng=latlng;
				}
			}
		}

	};
	DatabaseHelper mDbHelper=null;
	SharedPreferences keyValues=null;
	private void testDB()
	{
		rows.clear();
		mMap.clear();
		bMoveCam=false;
		stopService();

		long difflimit=0;
		difflimit = Long.parseLong(keyValues.getString(MyAccService.DL, difflimit+""));
		long speedlimit=0;
		speedlimit = Long.parseLong(keyValues.getString(MyAccService.SL, speedlimit+""));


		try
		{
			mDbHelper.openDataBase();
		}
		catch(SQLiteException ex)
		{
			ex.printStackTrace();
			return;
		}

		if(mDbHelper.findTable(mDbHelper.TABLE).equals(mDbHelper.TABLE)) {
			SQLiteDatabase db = mDbHelper.getMyDB();
			db.beginTransaction();
			String sql = "select max(abs(x))x, max(abs(y))y, max(abs(z))z,lat,lng,s from [AccLoc] where at>= date('now','-7 day') and s>="+ (speedlimit/3.6) +" and (((x > "+difflimit+") or (y > "+difflimit+") or (z > "+difflimit+"))) group by lat,lng order by at limit 2000";
			Log.i(MyAccService.NOTIFICATION,sql+"");
			String[] selectionArgs = null;
			Cursor c = null;
			try {
				c = db.rawQuery(sql, selectionArgs);
				while (c.moveToNext()) {
					double x = c.getDouble(0);
					double y = c.getDouble(1);
					double z = c.getDouble(2);
					double lat = c.getDouble(3);
					double lon = c.getDouble(4);
					double s = c.getDouble(5);

					boolean al = ((x > difflimit) || (y > difflimit) || (z > difflimit));

					if(al) {
						double fmax = Math.max(z, Math.max(y, Math.max(1, x)));
						LatLng latlng=new LatLng(lat, lon);
						CircleOptions options = new CircleOptions();
						options.center(latlng);
						options.strokeColor(getColor(s));
						options.radius(fmax);
						options.fillColor(getColor(s));
						mMap.addCircle(options);

					}
				}

			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				if (c != null)
					c.close();
				db.endTransaction();
				db.close();
				mDbHelper.close();
			}
		}
		Log.i(MyAccService.NOTIFICATION,rows.size()+"");
	}

	private int getColor(double speed) {
		return Color.argb(Integer.parseInt(Math.round(speed*6.375)+""),255,30,30);
	}

	private void updateMap() {
		PolylineOptions polyline = new PolylineOptions();

		mMap.clear();
		for (int i = 0; i < rows.size(); i++) {
			float lat = Float.parseFloat(rows.get(i).get(MyAccService.KEY_LAT));
			float lon = Float.parseFloat(rows.get(i).get(MyAccService.KEY_LON));
			boolean al = Boolean.parseBoolean(rows.get(i).get(MyAccService.KEY_AL));
			float ptsx = Float.parseFloat(rows.get(i).get(MyAccService.KEY_X));
			float ptsy = Float.parseFloat(rows.get(i).get(MyAccService.KEY_Y));
			float ptsz=Float.parseFloat(rows.get(i).get(MyAccService.KEY_Z));
			float s=Float.parseFloat(rows.get(i).get(MyAccService.KEY_SPEED));
			double fmax = Math.max(ptsz, Math.max(ptsy, Math.max(1, ptsx)));
			LatLng latlng=new LatLng(lat, lon);
			if(al)
			{
				CircleOptions options = new CircleOptions();
				options.center(latlng);
				options.strokeColor(getColor(s));
				options.radius(fmax);
				options.fillColor(getColor(s));
				mMap.addCircle(options);
			}
			polyline.add(latlng);
		}
		mMap.addPolyline(polyline);
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_map, menu);
		return true;
	}
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean("bStarted",bStarted);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		bStarted=savedInstanceState.getBoolean("bStarted");
	}

	boolean bStarted = false;
	private void stopService() {
		if(!bStarted)
			return;
		Intent i = new Intent(this,MyAccService.class);
		stopService(i);
		bStarted=false;
	}
	private void startService() {
		if(bStarted)
			return;
		bMoveCam=true;
		Intent i = new Intent(this,MyAccService.class);
		startService(i);
		bStarted=true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.start_service:
				startService();
				return true;
			case R.id.stop_service:
				stopService();
				return true;
			case R.id.reset_data:
				resetData();
				return true;
			case R.id.view_settings:
				viewSettings();
				return true;
			case R.id.test_db:
				testDB();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	private void viewSettings() {
		Intent i = new Intent(this,SettingsActivity.class);
		startActivity(i);
	}

	private void resetData() {
		rows.clear();
		mMap.clear();
	}
}
