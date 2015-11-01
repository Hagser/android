package se.hagser.mytouchtest;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

	private static final int MIN_DXDY = 2;

	// Assume no more than 20 simultaneous touches
	final private static int MAX_TOUCHES = 20;

	// Pool of MarkerViews
	final private static LinkedList<MarkerView> mInactiveMarkers = new LinkedList<MarkerView>();
ArrayList<HashMap<String,Float>> mapArrayList = new ArrayList<>();
	// Set of MarkerViews currently visible on the display
	@SuppressLint("UseSparseArrays")
	final private static Map<Integer, MarkerView> mActiveMarkers = new HashMap<Integer, MarkerView>();

	protected static final String TAG = "MainAct";

	private FrameLayout mFrame;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mFrame = (FrameLayout) findViewById(R.id.frame);

		// Initialize pool of View.
		initViews();

		// Create and set on touch listener
		mFrame.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {

				switch (event.getActionMasked()) {

					// Show new MarkerView

					case MotionEvent.ACTION_DOWN:
					case MotionEvent.ACTION_POINTER_DOWN: {

						int pointerIndex = event.getActionIndex();
						int pointerID = event.getPointerId(pointerIndex);
						float pressure = event.getPressure(pointerIndex);

						MarkerView marker = mInactiveMarkers.remove();

						if (null != marker) {

							marker.setP(pressure);
							mActiveMarkers.put(pointerID, marker);
							//x = horiz
							//y = verti


							marker.setXLoc(event.getX(pointerIndex));
							marker.setYLoc(event.getY(pointerIndex));

							updateTouches(mActiveMarkers.size());

							mFrame.addView(marker);
						}
						break;
					}

					// Remove one MarkerView

					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_POINTER_UP: {

						int pointerIndex = event.getActionIndex();
						int pointerID = event.getPointerId(pointerIndex);
						MarkerView marker = mActiveMarkers.remove(pointerID);

						if (null != marker) {

							mInactiveMarkers.add(marker);

							updateTouches(mActiveMarkers.size());

							mFrame.removeView(marker);

							if(mapArrayList.size()>50) {
								Log.d("mapArrayList", mapArrayList.size() + "");
								final ArrayList<HashMap<String,Float>> list = (ArrayList<HashMap<String,Float>>)mapArrayList.clone();
								final Thread thread = new Thread(new Runnable() {
									public void run() {
										GenTone genTone = new GenTone(list);
										genTone.playSound();
									}
								});
								thread.start();
								mapArrayList.clear();
							}

						}
						break;
					}


					// Move all currently active MarkerViews

					case MotionEvent.ACTION_MOVE: {

						for (int idx = 0; idx < event.getPointerCount(); idx++) {

							int ID = event.getPointerId(idx);

							float pressure = event.getPressure(idx);
							MarkerView marker = mActiveMarkers.get(ID);
							if (null != marker) {
								// Redraw only if finger has travel ed a minimum distance
								if (Math.abs(marker.getXLoc() - event.getX(idx)) > MIN_DXDY
										|| Math.abs(marker.getYLoc() - event.getY(idx)) > MIN_DXDY) {

									// Set new location
									marker.setXLoc(event.getX(idx));
									marker.setYLoc(event.getY(idx));
									marker.setP(pressure);

									// Request re-draw
									marker.invalidate();

									final float y = event.getY(idx);
									final float x = event.getX(idx);

									final int h = v.getHeight();
									final int w = v.getWidth();

									final float to = (((w - x) / w) * 1000);
									//final float to = ((((w + h) - (x + y)) / (w + h)) * 20000);
									//final float to = (((h - y) / h) * 100);
									final float vo = (((100 - pressure) / 100) * 100);

									//Log.d(idx+"","h:"+h+",y:"+y+",to:"+to);
									//Log.d(idx+"","w:"+w+",x:"+x+",vo:"+vo);
									HashMap<String, Float> map = new HashMap<>();
									map.put("freq",Math.max(200, to));
									map.put("volu", vo);
									mapArrayList.add(map);

								}
							}
						}

						break;
					}

					default:

						Log.i(TAG, "unhandled action");
				}

				return true;
			}

			// update number of touches on each active MarkerView
			private void updateTouches(int numActive) {
				for (MarkerView marker : mActiveMarkers.values()) {
					marker.setTouches(numActive);
				}
			}
		});
	}

	private void initViews() {
		for (int idx = 0; idx < MAX_TOUCHES; idx++) {
			mInactiveMarkers.add(new MarkerView(this, -1, -1,-1));
		}
	}

	private class GenTone
	{
		final int sampleRate = 8000;
		int numSamples;
		float amplitude;
		private byte generatedSnd[];

		public GenTone(ArrayList<HashMap<String,Float>> mapArrayList){
			int duration = 1;
			numSamples = duration * sampleRate;
			int genSndLen = numSamples*mapArrayList.size();

			Log.d("mapArrayList.size()",mapArrayList.size() +"");
			Log.d("genSndLen",genSndLen +"");
			Log.d("numSamples",numSamples +"");

			generatedSnd = new byte[2 * genSndLen];

			for(int mi=0;mi<mapArrayList.size();mi++) {
				HashMap<String, Float> map = mapArrayList.get(mi);
				float freqOfTone = map.get("freq");
				float _amplitude = map.get("volu");

				amplitude = _amplitude;

				final double sample[] = new double[numSamples];
				// fill out the array
				for (int i = 0; i < numSamples; ++i) {      // Fill the sample array
					sample[i] = Math.sin(freqOfTone * 2 * Math.PI * i / (sampleRate));
				}

				// convert to 16 bit pcm sound array
				// assumes the sample buffer is normalised.
				int idx = 0;
				int i = 0;

				int ramp = numSamples / 20;                                    // Amplitude ramp as a percent of sample count

				for (i = 0; i < ramp; ++i) {                                     // Ramp amplitude up (to avoid clicks)
					double dVal = sample[i];
					// Ramp up to maximum
					final short val = (short) ((dVal * 32767 * i / ramp));
					// in 16 bit wav PCM, first byte is the low order byte
					generatedSnd[idx++] = (byte) (val & 0x00ff);
					generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
				}


				/*for (i = i; i < numSamples - ramp; ++i) {                        // Max amplitude for most of the samples
					double dVal = sample[i];
					// scale to maximum amplitude
					final short val = (short) ((dVal * 32767));
					// in 16 bit wav PCM, first byte is the low order byte
					generatedSnd[idx++] = (byte) (val & 0x00ff);
					generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
				}

				for (i = i; i < numSamples; ++i) {                               // Ramp amplitude down
					double dVal = sample[i];
					// Ramp down to zero
					final short val = (short) ((dVal * 32767 * (numSamples - i) / ramp));
					// in 16 bit wav PCM, first byte is the low order byte
					generatedSnd[idx++] = (byte) (val & 0x00ff);
					generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
				}*/
			}
		}

		public void playSound(){

			AudioTrack audioTrack = null;                                   // Get audio track
			try {
				Log.d("generatedSnd.length",generatedSnd.length+"");
				audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
						sampleRate, AudioFormat.CHANNEL_OUT_MONO,
						AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
						AudioTrack.MODE_STATIC);
				audioTrack.write(generatedSnd, 0, generatedSnd.length);     // Load the track

				audioTrack.setVolume(amplitude);

				audioTrack.play();                                          // Play the track
			}
			catch (Exception e){
				e.printStackTrace();
				//RunTimeError("Error: " + e);
				//return false;
			}

			int x =0;
			do{                                                     // Montior playback to find when done
				if (audioTrack != null)
					x = audioTrack.getPlaybackHeadPosition();
				else
					x = numSamples;
				try {
					Thread.sleep(10,0);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}while (x<numSamples);

			if (audioTrack != null) audioTrack.release();           // Track play done. Release track.
		}
	}

	private class MarkerView extends View {
		private float mX, mY, mP;
		final static private int MAX_SIZE = 400;
		private int mTouches = 0;
		final private Paint mPaint = new Paint();

		public MarkerView(Context context, float x, float y,float p) {
			super(context);
			mX = x;
			mY = y;
			mP = p;
			mPaint.setStyle(Paint.Style.FILL);

			Random rnd = new Random();
			mPaint.setARGB(255, rnd.nextInt(256), rnd.nextInt(256),
					rnd.nextInt(256));
		}

		float getP() {
			return mP;
		}

		void setP(float p) {
			mP = p;
		}

		float getXLoc() {
			return mX;
		}

		void setXLoc(float x) {
			mX = x;
		}

		float getYLoc() {
			return mY;
		}

		void setYLoc(float y) {
			mY = y;
		}

		void setTouches(int touches) {
			mTouches = touches;
		}

		@Override
		protected void onDraw(Canvas canvas) {
			canvas.drawCircle(mX, mY, MAX_SIZE / mTouches, mPaint);
			//canvas.drawCircle(mX, mY,MAX_SIZE/ mP, mPaint);
		}
	}

}
