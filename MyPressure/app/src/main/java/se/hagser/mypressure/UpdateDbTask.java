package se.hagser.mypressure;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.util.Log;


public class UpdateDbTask extends AsyncTask<String, Integer, String> {
	final static String tag = MyPressureService.NOTIFICATION; 
	DatabaseHelper mDbHelper=null;
	public int cache=-1;
	public UpdateDbTask(DatabaseHelper dbh) {
		
		mDbHelper = dbh;
	}
	
	protected void onProgressUpdate(Integer... progress) {
		Log.i(tag,progress[0]+"");
		cache=progress[0];
	}

	protected void onPostExecute(String result) {
		mDbHelper.close();
	}

	@Override
	protected String doInBackground(String... args) {
			addToLocalDatabase(args[1], args[2], args[3], args[4]);
			boolean bWifi = Boolean.parseBoolean(args[5]);
			if(bWifi)
				syncData();

		return null;
	}
	private void syncData() {
		if(mDbHelper==null)
			return;

		Log.i(tag,"syncData");
		final SyncDBTask task = new SyncDBTask(mDbHelper);

		task.execute();

	}

	private void addToLocalDatabase(String spressure, String slat, String slng,String salt) {
		Log.i(tag,"addToLocalDatabase");
		try
		{
			mDbHelper.openDataBase();
		}
		catch(SQLiteException ex)
		{
			ex.printStackTrace();
			return;
		}

		if(mDbHelper.findTable(mDbHelper.TABLE).equals(mDbHelper.TABLE))
		{
			SQLiteDatabase db = mDbHelper.getMyDB();
			Boolean isReadOnly = db.isReadOnly();

			Log.i(tag, "isReadOnly:" + isReadOnly);
			db.beginTransaction();
			Log.i(tag, "beginTransaction");
			try
			{
				if(!isReadOnly)
				{
					String dateString = MyPressureService.getDefaultDateTime();
					
					ContentValues values=new ContentValues();
					values.put("at", dateString);
					values.put("val", spressure);
					values.put("lat", slat);
					values.put("lng", slng);
					values.put("alt", salt);
				    Log.i(tag,"isDbLockedByCurrentThread:"+db.isDbLockedByCurrentThread());


					Log.i(tag, "find-table:" + mDbHelper.TABLE + "_" + mDbHelper.findTableDB(db,mDbHelper.TABLE));

					long lng = db.replace(mDbHelper.TABLE,null,values);
					if(lng==-1)
						lng = db.insert(mDbHelper.TABLE, null, values);

					Log.i(tag, "insert:" + lng);

					db.setTransactionSuccessful();
				}
			}
			catch(Exception ex)
			{
				Log.i(tag, ex.toString());
				ex.printStackTrace();
			} finally
			{
				db.endTransaction();
				Log.i(tag,"endTransaction");
			}
		}
	}
}
