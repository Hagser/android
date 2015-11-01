package se.hagser.mysensors;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEventListener;
import android.hardware.TriggerEvent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewStyle;
import com.jjoe64.graphview.LineGraphView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends Activity implements SensorEventListener {

	public static final String NOTIFICATION = "se.hagser.mysensors";

	TextView sensor_data=null;
    private SensorManager mSensorManager;
    DatabaseHelper mDbHelper=null;
    public static String KEY_ID = "id";
    public static String KEY_NAME = "name";
	public static String KEY_VALUE = "value";
	public static final String RESULT="result";
    private Spinner spinner=null;
    protected ListAdapter listAdapter=null;
    List<Sensor> sensors = null;
    ArrayList<HashMap<String, String>> allrows=new ArrayList<HashMap<String, String>>();

    ArrayList<UpdateDbTask> tasks = new ArrayList<>();

    ArrayList<HashMap<String, String>> rows=new ArrayList<HashMap<String, String>>();
    private Sensor mSensor;
    LinearLayout canvas=null;
    GraphView gv = null;

	private final TriggerEventListener mListener = new MyTriggerListener();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        canvas = (LinearLayout)findViewById(R.id.canvas);
        gv = new LineGraphView(this,"") {};

        canvas.addView(gv);
        mDbHelper = new DatabaseHelper(this.getBaseContext());

        mSensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);
        sensor_data = (TextView)findViewById(R.id.sensor_data);
        sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);

        addMap(0,getString(R.string.choose));
        for(int i=0;i<sensors.size();i++)
        {
            addMap(sensors.get(i));
        }

        spinner =(Spinner)findViewById(R.id.spinner);
        listAdapter = new ListAdapter(this,rows);
        spinner.setAdapter(this.listAdapter);
        CountDownTimer countDownTimer = new CountDownTimer(10000,100) {
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
			@Override
            public void onTick(long millisUntilFinished) {
                HashMap<String, String> map3 = (HashMap<String, String>) spinner.getSelectedItem();
                int sensor_type = Integer.parseInt(map3.get(KEY_ID));
                if(lastsensor!=sensor_type) {
                    allrows.clear();
                    mSensor = mSensorManager.getDefaultSensor(sensor_type);

                    mSensorManager.registerListener(MainActivity.this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);

					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
						try {
							mSensorManager.cancelTriggerSensor(mListener, mSensor);
						} catch (Exception ex) {
						}

						try {
							mSensorManager.requestTriggerSensor(mListener, mSensor);
						} catch (Exception ex) {
						}
					}
                    lastsensor=sensor_type;
                    sensor_data.setText("");
                }
            }

            @Override
            public void onFinish() {
                this.start();
            }
        };
        countDownTimer.start();
    }
int lastsensor = -1;
    private void addMap(Sensor sensor) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(KEY_ID,sensor.getType()+"");
        map.put(KEY_NAME, sensor.getName() + "");
        rows.add(map);
    }

    private void addMap(int sensor,String name) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(KEY_ID,sensor+"");
        map.put(KEY_NAME, name);
        rows.add(map);
    }
    protected void onResume() {
        super.onResume();
		mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.view_details:
				viewDetails();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void viewDetails() {

		Intent intent = new Intent(this,DetailedDataActivity.class);
		startActivity(intent);
	}

    @Override
    public void onSensorChanged(SensorEvent event) {
        HashMap<String, String> map3 = (HashMap<String, String>)spinner.getSelectedItem();
        if((event.sensor.getType()+"").equals(map3.get(KEY_ID)))
        {
            sensor_data.setText("");
            String str_data="";
            HashMap<String, String> map = new HashMap<String, String>();
            float sum=0;
            for(int i=0;i<Math.min(3,event.values.length);i++)
            {
                float f = event.values[i];
                str_data+=f+"\n";
                sum=sum+Math.abs(f);
                map.put(MainActivity.KEY_VALUE + "_" + i, f + "");
            }

            sensor_data.setText(str_data + sum);

            map.put(MainActivity.KEY_ID,event.sensor.getType()+"");
            map.put(MainActivity.KEY_NAME,event.sensor.getName());
            map.put(MainActivity.KEY_VALUE, event.values.length + "");

			String at = UpdateDbTask.getDefaultDateTime();
			String a = map.containsKey(MainActivity.KEY_VALUE+"_0")? map.get(MainActivity.KEY_VALUE+"_0"):"a";
			String b = map.containsKey(MainActivity.KEY_VALUE+"_1")? map.get(MainActivity.KEY_VALUE+"_1"):"b";
			String c = map.containsKey(MainActivity.KEY_VALUE+"_2")? map.get(MainActivity.KEY_VALUE+"_2"):"c";
			String typ = map.containsKey(MainActivity.KEY_ID)? map.get(MainActivity.KEY_ID):"typ";

            int max_length =(typ.equals(Sensor.TYPE_HEART_RATE+"")?30:100);
            LogI(NOTIFICATION + "sensorChange", "a:" + a + ",b:" + b + ",c:" + c + ",typ:" + typ);
            if(!(typ.equals(Sensor.TYPE_HEART_RATE+"") && event.values[0]<40)) {
                execute(at, a, b, c, typ);

                while (allrows.size() > max_length) {
                    allrows.remove(0);
                }
                allrows.add(map);
            }
            drawGraph(2,allrows.size());
        }
    }

    public static void LogI(String tag,String msg)
    {
        if(BuildConfig.DEBUG)
            Log.i(tag, msg);

    }

    private void execute(final String at,final String a,final String b,final String c,final String typ) {
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
                            LogI(NOTIFICATION+"onTick","a:"+a+",b:"+b+",c:"+c+",typ:"+typ);
                            task.execute(at, a, b, c, typ);
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
                            LogI(NOTIFICATION+"onFinish","a:"+a+",b:"+b+",c:"+c+",typ:"+typ);
                            task.execute(at, a, b, c, typ);
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

    private void drawGraph(double viewportstart,double size) {
        float fmin=0,fmax=0;
        float[] ptsx=new float[allrows.size()];
        float[] ptsy=new float[allrows.size()];
        float[] ptsz=new float[allrows.size()];
        GraphView.GraphViewData[] datax = new GraphView.GraphViewData[allrows.size()];
        GraphView.GraphViewData[] datay = new GraphView.GraphViewData[allrows.size()];
        GraphView.GraphViewData[] dataz = new GraphView.GraphViewData[allrows.size()];

        for(int i =0;i<allrows.size();i++)
        {
            if(allrows!=null && allrows.get(i)!=null && allrows.get(i).get(MainActivity.KEY_VALUE)!=null) {
                float vmax = Float.parseFloat(allrows.get(i).get(MainActivity.KEY_VALUE));

				if(allrows.get(i).get(MainActivity.KEY_VALUE + "_0")!=null)
	                ptsx[i] = Float.parseFloat(allrows.get(i).get(MainActivity.KEY_VALUE + "_0"));

                if (vmax > 1 && allrows.get(i).get(MainActivity.KEY_VALUE + "_1")!=null)
                    ptsy[i] = Float.parseFloat(allrows.get(i).get(MainActivity.KEY_VALUE + "_1"));

                if (vmax > 2 && allrows.get(i).get(MainActivity.KEY_VALUE + "_2")!=null)
                    ptsz[i] = Float.parseFloat(allrows.get(i).get(MainActivity.KEY_VALUE + "_2"));

                datax[i] = new GraphView.GraphViewData(i + 1, ptsx[i]);
                datay[i] = new GraphView.GraphViewData(i + 1, ptsy[i]);
                dataz[i] = new GraphView.GraphViewData(i + 1, ptsz[i]);
                fmax = Math.max(ptsz[i], Math.max(ptsy[i], Math.max(fmax, ptsx[i])));
                fmin = Math.min(ptsz[i], Math.min(ptsy[i], Math.min(fmin, ptsx[i])));
            }
        }

        gv.removeAllSeries();
		try {


			GraphViewSeries dvx = new GraphViewSeries("X", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(200, 50, 00), 5), datax);
			if(allrows.get(0).get(MainActivity.KEY_VALUE + "_0")!=null)
				gv.addSeries(dvx);
			GraphViewSeries dvy = new GraphViewSeries("Y", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(50, 200, 00), 5), datay);
			if(allrows.get(0).get(MainActivity.KEY_VALUE + "_1")!=null)
				gv.addSeries(dvy);
			GraphViewSeries dvz = new GraphViewSeries("Z", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(00, 50, 200), 5), dataz);
			if(allrows.get(0).get(MainActivity.KEY_VALUE + "_2")!=null)
				gv.addSeries(dvz);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		GraphViewStyle graphViewStyle = new GraphViewStyle();
		graphViewStyle.useTextColorFromTheme(getApplicationContext());
		gv.setGraphViewStyle(graphViewStyle);

        gv.setScrollable(true);
        gv.setScalable(true);
        gv.setShowLegend(true);
        gv.setViewPort(viewportstart,size);
        gv.redrawAll();

        gv.setDisableTouch(true);

        gv.scrollToEnd();


    }
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	class MyTriggerListener extends TriggerEventListener {
		public void onTrigger(TriggerEvent event) {
			HashMap<String, String> map3 = (HashMap<String, String>)spinner.getSelectedItem();
			if((event.sensor.getType()+"").equals(map3.get(KEY_ID)))
			{
				sensor_data.setText("");
				String str_data="";
				HashMap<String, String> map = new HashMap<String, String>();
				float sum=0;
				for(int i=0;i<Math.min(3,event.values.length);i++)
				{
					float f = event.values[i];
					str_data+=f+"\n";
					sum=sum+Math.abs(f);
					map.put(MainActivity.KEY_VALUE + "_" + i, f + "");
				}

				sensor_data.setText(str_data + sum);

				map.put(MainActivity.KEY_ID,event.sensor.getType()+"");
				map.put(MainActivity.KEY_NAME,event.sensor.getName());
				map.put(MainActivity.KEY_VALUE, event.values.length + "");

				String at = UpdateDbTask.getDefaultDateTime();
				String a = map.containsKey(MainActivity.KEY_VALUE+"_0")? map.get(MainActivity.KEY_VALUE+"_0"):"a";
				String b = map.containsKey(MainActivity.KEY_VALUE+"_1")? map.get(MainActivity.KEY_VALUE+"_1"):"b";
				String c = map.containsKey(MainActivity.KEY_VALUE+"_2")? map.get(MainActivity.KEY_VALUE+"_2"):"c";
				String typ = map.containsKey(MainActivity.KEY_ID)? map.get(MainActivity.KEY_ID):"typ";

				LogI(NOTIFICATION+"onTrigger","a:"+a+",b:"+b+",c:"+c+",typ:"+typ);
				execute(at,a,b,c,typ);

				if (allrows.size()>100)
				{
					allrows.remove(0);
				}
                if((event.sensor.getType() != Sensor.TYPE_HEART_RATE) || ((event.sensor.getType() == Sensor.TYPE_HEART_RATE) && !a.equals("0")))
				    allrows.add(map);

				drawGraph(2,(event.sensor.getType()==Sensor.TYPE_HEART_RATE)?15: allrows.size());
			}

			try
			{mSensorManager.requestTriggerSensor(mListener, mSensor);}
			catch (Exception ex){}
		}
	}
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
