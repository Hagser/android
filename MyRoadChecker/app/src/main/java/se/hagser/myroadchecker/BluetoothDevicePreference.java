package se.hagser.myroadchecker;

import android.bluetooth.*;
import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

import java.util.Set;

public class BluetoothDevicePreference extends ListPreference {

	public BluetoothDevicePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		try {

			BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
			if(bta!=null) {
				Set<BluetoothDevice> pairedDevices = bta.getBondedDevices();
				CharSequence[] entries = new CharSequence[pairedDevices.size()];
				CharSequence[] entryValues = new CharSequence[pairedDevices.size()];
				int i = 0;
				for (BluetoothDevice dev : pairedDevices) {
					entries[i] = dev.getName();
					entryValues[i] = dev.getAddress();
					i++;
				}
				setEntries(entries);
				setEntryValues(entryValues);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public BluetoothDevicePreference(Context context) {
		this(context, null);
	}

}