package se.hagser.mypressure;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	TextView pressure = null;
	TextView lat = null;
	TextView lng = null;
	TextView alt = null;
	TextView at = null;
	TextView cache = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		pressure = (TextView) findViewById(R.id.pressure);
		lat = (TextView) findViewById(R.id.lat);
		lng = (TextView) findViewById(R.id.lng);
		alt = (TextView) findViewById(R.id.alt);
		at = (TextView) findViewById(R.id.at);
		cache = (TextView) findViewById(R.id.cache);

		Intent i = new Intent(this,MyPressureService.class);
		Log.i(MyPressureService.NOTIFICATION,"startService");
		startService(i);
	}
	  @Override
	  protected void onResume() {
	    super.onResume();
	    registerReceiver(receiver, new IntentFilter(MyPressureService.NOTIFICATION));
	  }
	  @Override
	  protected void onPause() {
	    super.onPause();
	    unregisterReceiver(receiver);
	  }
	  private BroadcastReceiver receiver = new BroadcastReceiver() {

	    @Override
	    public void onReceive(Context context, Intent intent) {
	      Bundle bundle = intent.getExtras();
	      if (bundle != null) {
	        String pr = bundle.getString(MyPressureService.KEY_PR);
	        String sat = bundle.getString(MyPressureService.KEY_AT);
	        String lt = bundle.getString(MyPressureService.KEY_LT);
	        String ln = bundle.getString(MyPressureService.KEY_LN);
	        String al = bundle.getString(MyPressureService.KEY_AL);
	        String ch = bundle.getInt(MyPressureService.KEY_CH)+"";
	        int resultCode = bundle.getInt(MyPressureService.RESULT);
			  boolean alert = bundle.getBoolean("ALERT");
	        if (resultCode == RESULT_OK) {
	        	pressure.setText(pr);
	        	lat.setText(lt);
	        	lng.setText(ln);
	        	alt.setText(al);
	        	at.setText(sat);
	        	cache.setText(ch);
				if(alert) {
					Toast.makeText(MainActivity.this, "Pressure change more than 4 point!", Toast.LENGTH_LONG).show();
				}
	        } else {
	          Toast.makeText(MainActivity.this, "Update-failed",Toast.LENGTH_LONG).show();
	        }
	      }
	    }
	  };

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.start_service:
	    	startService();
	        return true;
	    case R.id.stop_service:
	    	stopService();
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	private void stopService() {
		Intent i = new Intent(this,MyPressureService.class);
		Log.i(MyPressureService.NOTIFICATION,"stopService");
		stopService(i);
	}
	private void startService() {
		Intent i = new Intent(this,MyPressureService.class);
		Log.i(MyPressureService.NOTIFICATION,"startService");
		startService(i);
	}
	@Override
	protected void onSaveInstanceState(Bundle outState) {

		String spressure = (String) pressure.getText();
		String slat = (String) lat.getText();
		String slng = (String) lng.getText();
		String salt = (String) alt.getText();
		String sat = (String) at.getText();
		String sch = (String) cache.getText();
		outState.putString("p", spressure);
		outState.putString("lt", slat);
		outState.putString("ln", slng);
		outState.putString("a", salt);
		outState.putString("at", sat);
		outState.putString("ch", sch);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {

		pressure.setText(savedInstanceState.getString("p"));
		lat.setText(savedInstanceState.getString("lt"));
		lng.setText(savedInstanceState.getString("ln"));
		alt.setText(savedInstanceState.getString("a"));
		at.setText(savedInstanceState.getString("at"));
		cache.setText(savedInstanceState.getString("ch"));

		super.onRestoreInstanceState(savedInstanceState);

	}	
}


