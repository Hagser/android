package se.hagser.mylocationpublisher;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;

public class SettingsActivity extends AppCompatActivity {
	Switch detailed_location;
	Switch minimal_location;

	public static String detailed_location_key="detailed_switch";

	public static String minimal_location_key="minimal_switch";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		final SharedPreferences keyValues = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

		detailed_location = (Switch)findViewById(R.id.detailed_location);
		boolean detailed = keyValues.contains(SettingsActivity.detailed_location_key) && keyValues.getBoolean(SettingsActivity.detailed_location_key, false);
		detailed_location.setChecked(detailed);
		detailed_location.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				keyValues.edit().putBoolean(detailed_location_key, detailed_location.isChecked()).apply();
				if (detailed_location.isChecked()) {
					minimal_location.setChecked(false);
					keyValues.edit().putBoolean(minimal_location_key, minimal_location.isChecked()).apply();
				}
				sendUpdateBroadCast();
			}
		});
		minimal_location = (Switch)findViewById(R.id.minimal_location);
		boolean minimal = keyValues.contains(SettingsActivity.minimal_location_key) && keyValues.getBoolean(SettingsActivity.minimal_location_key, false);
		minimal_location.setChecked(minimal);
		minimal_location.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				keyValues.edit().putBoolean(minimal_location_key, minimal_location.isChecked()).apply();
				if (minimal_location.isChecked()) {
					detailed_location.setChecked(false);
					keyValues.edit().putBoolean(detailed_location_key, detailed_location.isChecked()).apply();
				}
				sendUpdateBroadCast();
			}
		});
		if(minimal)
			detailed_location.setChecked(false);
	}

	private void sendUpdateBroadCast() {
		Intent intent = new Intent(MyService.RESULT_SET);
		sendBroadcast(intent);
	}
}
