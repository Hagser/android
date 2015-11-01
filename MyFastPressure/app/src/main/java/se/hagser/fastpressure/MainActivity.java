package se.hagser.fastpressure;
import android.os.Bundle;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends Activity {

    protected ListAdapter mAdapter;
    ListView spinner=null;
    ArrayList<HashMap<String, String>> rows=new ArrayList<HashMap<String, String>>();
    Button menubutton=null;
	TextView pressure = null;
    TextView cal_press = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		pressure = (TextView) findViewById(R.id.pressure);
        cal_press = (TextView) findViewById(R.id.cal_press);

        menubutton = (Button)findViewById(R.id.menubutton);

        menubutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.openOptionsMenu();
            }
        });

        spinner = (ListView) findViewById(R.id.thelist);

        spinner.setAdapter(this.mAdapter);

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
    float pr_old=0;
    float pr_cal=0;
	  private BroadcastReceiver receiver = new BroadcastReceiver() {

	    @Override
	    public void onReceive(Context context, Intent intent) {
	      Bundle bundle = intent.getExtras();
	      if (bundle != null) {
	        String pr = bundle.getString(MyPressureService.KEY_PR);
              String at = bundle.getString(MyPressureService.KEY_AT);
	        int resultCode = bundle.getInt(MyPressureService.RESULT);
	        if (resultCode == RESULT_OK) {
	        	pressure.setText(pr);
                pr_old=Float.parseFloat(pr);
                if(pr_cal>0)
                {
                    cal_press.setText((pr_old-pr_cal)+"");
                }

                HashMap<String, String> map = new HashMap<String, String>();
                map.put(MyPressureService.KEY_PR, pr);
                map.put(MyPressureService.KEY_AT, at);
                if(rows.size()>15)
                {rows.remove(0);}
                rows.add(map);

                mAdapter = new ListAdapter(MainActivity.this,rows);

                spinner.setAdapter(mAdapter);

                //Log.i(MyPressureService.KEY_PR,pr);
		          //Toast.makeText(MainActivity.this, "Updated-"+sat,Toast.LENGTH_LONG).show();
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
            case R.id.calibrate:
                calibrate();
                return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
    private void calibrate()
    {
        CountDownTimer cdt = new CountDownTimer(5000,100) {
            @Override
            public void onTick(long millisUntilFinished) {
                int iv = pressure.getVisibility();
                pressure.setVisibility(iv==View.VISIBLE?View.INVISIBLE:View.VISIBLE);
            }

            @Override
            public void onFinish() {
                pr_cal=pr_old;
                cal_press.setText("");
                pressure.setVisibility(View.VISIBLE);
            }
        };
        cdt.start();
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
		outState.putString("p", spressure);
        outState.putString("pr_old", pr_old+"");
        outState.putString("pr_cal", pr_cal+"");
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {

		pressure.setText(savedInstanceState.getString("p"));
        String strpold =savedInstanceState.getString("pr_old");
        if(strpold!=null) {
            pr_old = Float.parseFloat(strpold);
        }
        String strpcal =savedInstanceState.getString("pr_cal");
        if(strpcal!=null) {
            pr_cal = Float.parseFloat(strpcal);
        }

        if(pr_cal>0)
        {
            cal_press.setText((pr_old-pr_cal)+"");
        }


		super.onRestoreInstanceState(savedInstanceState);

	}	
}


