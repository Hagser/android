package se.hagser.myraspi;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity  implements SensorEventListener{

	private SensorManager mSensorManager;
	private TextView text_sensor_value;
	private EditText url_text;
	public static boolean bDebug = true;
	public static boolean bLog = true;
int n_val=0;
	CountDownTimer cdt = new CountDownTimer(100000,100) {
		@Override
		public void onTick(long millisUntilFinished) {

			final SendAsync sa = new SendAsync(url_text.getText().toString());
			n_val++;
			if(n_val>255)
				n_val=0;
			double f_val = n_val;
			int i_val = Integer.parseInt((Math.floor(f_val) + "").split("\\.")[0]);
			final String h_val = hex(i_val);
			text_sensor_value.setText(h_val + ":"+f_val + ":"+i_val);
			sa.execute(h_val);
		}

		@Override
		public void onFinish() {

		}
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		text_sensor_value = (TextView) findViewById(R.id.text_sensor_value);
		url_text = (EditText)findViewById(R.id.url_text);
		url_text.setText("http://10.0.0.9:8000/?output_port=");
		final Button start_sensor = (Button) findViewById(R.id.start_sensor);
		start_sensor.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				LogI("onClick", "clicked");
				if(start_sensor.getText().equals("stop"))
				{
					stopSensor();
					LogI("onClick", "started");
					start_sensor.setText(getResources().getText(R.string.app_name));
				}
				else {
					startSensor();
					LogI("onClick", "started");
					start_sensor.setText("stop");
				}
			}
		});

		final Button start_test = (Button) findViewById(R.id.start_test);

		start_test.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				LogI("onClick", "clicked");
				if(start_test.getText().equals("stop")) {
					cdt.cancel();
					start_test.setText(getResources().getText(R.string.test));
				}
				else {
					cdt.start();
					start_test.setText("stop");
				}

				LogI("onClick", "started");
			}
		});
	}

	private void startSensor() {
		LogI("MAstartSens", "start");
		Sensor mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
		LogI("MAstartSens", "mSensor:" + mSensor.getName());
		mSensorManager.registerListener(MainActivity.this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
		LogI("MAstartSens", "started");
	}
	private void stopSensor() {
		LogI("MAstopSens", "stop");
		mSensorManager.unregisterListener(MainActivity.this);
		LogI("MAstopSens", "stopped");
	}
long last_timestamp=0;
	String old_h_val="";
	@Override
	public void onSensorChanged(SensorEvent event) {
		if((event.timestamp-last_timestamp)>(100000000)) {
			last_timestamp=event.timestamp;
			Double f_val = (Math.abs(event.values[1])/9.82) * 256;
			int i_val = Integer.parseInt((Math.floor(f_val) + "").split("\\.")[0]);
			final String h_val = hex(i_val);
			text_sensor_value.setText(h_val);
			if(!old_h_val.equals(h_val)) {
				old_h_val=h_val;
				final SendAsync sa = new SendAsync(url_text.getText().toString());
				sa.execute(h_val);
			}
		}
	}
	public void LogI(String tag,String msg)
	{
		if(bDebug)
			text_sensor_value.setText(tag + ":" + msg);
		if(bLog)
			Log.i(tag,msg);
	}
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	public static String hex(int n) {
		return String.format("0x%2s", Integer.toHexString(n)).replace(' ', '0');
	}
}
