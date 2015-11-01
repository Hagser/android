package se.hagser.myroadchecker;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.test.ActivityTestCase;
import android.test.ActivityUnitTestCase;

/**
 * Created by jh on 2015-04-29.
 */
public class ActivityTest extends ActivityUnitTestCase {
	Activity mActivity;

	public ActivityTest(Class activityClass) {
		super(activityClass);
		mActivity=getActivity();
	}
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mActivity = getActivity();

	}
	public void TestSettingsIntent()
	{
		assertNotNull("mActivity is null", mActivity);
		SharedPreferences keyValues=null;
Intent intent = new Intent(MyAccService.BT);
		intent.putExtra("KEY", "");
		intent.putExtra("RESET",false);
		mActivity.sendBroadcast(intent);
	}
}
