package se.hagser.myroadchecker;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity {
	/**
	 * Determines whether to always show the simplified settings UI, where
	 * settings are presented in a single list. When false, settings are shown
	 * as a master/detail two-pane view on tablets. When true, a single pane is
	 * shown on tablets.
	 */
	private static final boolean ALWAYS_SIMPLE_PREFS = false;


	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		setupSimplePreferencesScreen();
		//EditTextPreference etp = (EditTextPreference)findPreference(R.id.seldev);
	}

	/**
	 * Shows the simplified settings UI if the device configuration if the
	 * device configuration dictates that a simplified, single-pane UI should be
	 * shown.
	 */
	private void setupSimplePreferencesScreen() {
		if (!isSimplePreferences(this)) {
			return;
		}

		// In the simplified UI, fragments are not used at all and we instead
		// use the older PreferenceActivity APIs.

		// Add 'general' preferences.
		addPreferencesFromResource(R.xml.pref_general);

		bindPreferenceSummaryToValue(findPreference(MyAccService.SL));
		bindPreferenceSummaryToValue(findPreference(MyAccService.DL));
		bindPreferenceSummaryToValue(findPreference(MyAccService.TL));
		bindPreferenceSummaryToBoolValue(findPreference(MyAccService.BT));
		bindPreferenceSummaryToValue(findPreference(MyAccService.DEV));
		bindPreferenceSummaryToBoolValue(findPreference(MyAccService.WO));
		bindPreferenceSummaryToValue(findPreference(MyAccService.DEV));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onIsMultiPane() {
		return isXLargeTablet(this) && !isSimplePreferences(this);
	}

	/**
	 * Helper method to determine if the device has an extra-large screen. For
	 * example, 10" tablets are extra-large.
	 */
	private static boolean isXLargeTablet(Context context) {
		return (context.getResources().getConfiguration().screenLayout
				& Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	/**
	 * Determines whether the simplified settings UI should be shown. This is
	 * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
	 * doesn't have newer APIs like {@link PreferenceFragment}, or the device
	 * doesn't have an extra-large screen. In these cases, a single-pane
	 * "simplified" settings UI should be shown.
	 */
	private static boolean isSimplePreferences(Context context) {
		return ALWAYS_SIMPLE_PREFS
				|| Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
				|| !isXLargeTablet(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void onBuildHeaders(List<Header> target) {
		if (!isSimplePreferences(this)) {
			loadHeadersFromResource(R.xml.pref_headers, target);
		}
	}
	private static Preference.OnPreferenceClickListener sPreferenceClickListener = new Preference.OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {

			Log.i(MyAccService.NOTIFICATION + "SA", "onPrefClick");

			BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {

				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				Log.i("SA","getAddress:" + device.getAddress());

				//keyValues.edit().putString(MyAccService.DEV, device.getAddress()).apply();

				context.unregisterReceiver(this);

				Intent btintent = new Intent(MyAccService.BT);
				btintent.putExtra("KEY", MyAccService.DEV);
				btintent.putExtra("DATA", device.getAddress());
				btintent.putExtra("RESET", true);
				context.sendBroadcast(btintent);

				}
			};

			preference.getContext().registerReceiver(broadcastReceiver, new IntentFilter("android.bluetooth.devicepicker.action.DEVICE_SELECTED"));
			Intent btintent = new Intent("android.bluetooth.devicepicker.action.LAUNCH")
					.putExtra("android.bluetooth.devicepicker.extra.NEED_AUTH", false)
					.putExtra("android.bluetooth.devicepicker.extra.FILTER_TYPE", 0)
					.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

			preference.getContext().startActivity(btintent);



			return false;
		}
	};
		/**
	 * A preference value change listener that updates the preference's summary
	 * to reflect its new value.
	 */

	private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			String stringValue = value.toString();

			if (preference instanceof ListPreference) {
				// For list preferences, look up the correct display value in
				// the preference's 'entries' list.
				ListPreference listPreference = (ListPreference) preference;
				int index = listPreference.findIndexOfValue(stringValue);

				// Set the summary to reflect the new value.
				preference.setSummary(
						index >= 0
								? listPreference.getEntries()[index]
								: null);

			} else {
				// For all other preferences, set the summary to the value's
				// simple string representation.
				preference.setSummary(stringValue);
			}
			return true;
		}
	};

	private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToBoolValueListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			boolean boolValue = Boolean.parseBoolean(value.toString());
			Log.i(MyAccService.NOTIFICATION + "sa", value.toString());
			if(boolValue) {
				String device = PreferenceManager
				.getDefaultSharedPreferences(preference.getContext())
				.getString(MyAccService.DEV, "");
				if(device==null || device.equals("")) {
					Intent intent = new Intent(MyAccService.BT);
					intent.putExtra("KEY", preference.getKey());
					//preference.getContext().sendBroadcast(intent);
				}
			}
			if (preference instanceof ListPreference) {
				// For list preferences, look up the correct display value in
				// the preference's 'entries' list.
				ListPreference listPreference = (ListPreference) preference;
				int index = listPreference.findIndexOfValue(boolValue+"");

				// Set the summary to reflect the new value.
				preference.setSummary(
						index >= 0
								? listPreference.getEntries()[index]
								: null);

			} else {
				// For all other preferences, set the summary to the value's
				// simple string representation.
				preference.setSummary( boolValue+"");
			}
			return true;
		}
	};
	/**
	 * Binds a preference's summary to its value. More specifically, when the
	 * preference's value is changed, its summary (line of text below the
	 * preference title) is updated to reflect the value. The summary is also
	 * immediately updated upon calling this method. The exact display format is
	 * dependent on the type of preference.
	 *
	 * @see #sBindPreferenceSummaryToValueListener
	 */
	private static void bindPreferenceSummaryToValue(Preference preference) {
		// Set the listener to watch for value changes.
		preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

		// Trigger the listener immediately with the preference's
		// current value.
		sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
				PreferenceManager
						.getDefaultSharedPreferences(preference.getContext())
						.getString(preference.getKey(), ""));
	}

	private static void bindPreferenceSummaryToBoolValue(Preference preference) {
		// Set the listener to watch for value changes.
		preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToBoolValueListener);

		// Trigger the listener immediately with the preference's
		// current value.
		sBindPreferenceSummaryToBoolValueListener.onPreferenceChange(preference,
				PreferenceManager
						.getDefaultSharedPreferences(preference.getContext())
						.getBoolean(preference.getKey(), false));
	}

	private static void bindPreferenceClickListener(Preference preference) {
		// Set the listener to watch for value changes.
		preference.setOnPreferenceClickListener(sPreferenceClickListener);

		// Trigger the listener immediately with the preference's
		// current value.
		//sPreferenceClickListener.onPreferenceClick(preference);
	}

	/**
	 * This fragment shows general preferences only. It is used when the
	 * activity is showing a two-pane settings UI.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class GeneralPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_general);

			bindPreferenceSummaryToValue(findPreference(MyAccService.SL));
			bindPreferenceSummaryToValue(findPreference(MyAccService.TL));
			bindPreferenceSummaryToValue(findPreference(MyAccService.DL));
			bindPreferenceSummaryToBoolValue(findPreference(MyAccService.BT));
			bindPreferenceSummaryToValue(findPreference(MyAccService.DL));
			bindPreferenceSummaryToBoolValue(findPreference(MyAccService.WO));
			bindPreferenceClickListener(findPreference(MyAccService.DEV));
		}
	}

}
