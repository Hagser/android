package se.hagser.mypressure;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
	public BootReceiver() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		//Intent myIntent = new Intent(context, MainActivity.class);
		//myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		//context.startActivity(myIntent);

		Intent i = new Intent(context,MyPressureService.class);
		context.startService(i);
	}
}
