package se.hagser.myroadchecker;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;

public class UpdateDbTask extends AsyncTask<String, Integer, String> {
	final static String tag = MyAccService.NOTIFICATION+"udt";
	DatabaseHelper mDbHelper=null;
	public int cache=-1;
    private boolean isAdding;

    ArrayList<ContentValues> addThese = new ArrayList<ContentValues>();

    public UpdateDbTask(DatabaseHelper dbh) {
		
		mDbHelper = dbh;
	}
	
	protected void onProgressUpdate(Integer... progress) {
		LogI(tag+" progress", progress[0] + "");
		cache=progress[0];
	}

	protected void onPostExecute(String result) {
		mDbHelper.close();
	}

	@Override
	protected String doInBackground(String... args) {

        addToLocalDatabase(args[1], args[2], args[3], args[4], args[5], args[6], args[7]);
        addToLocalDatabase();
        //this.onPostExecute("OK");

		return null;
	}
    private void addToLocalDatabase(String x,String y,String z, String slat, String slng,String speed,String n) {
        LogI(tag, "addToLocalDatabase-args");

        String dateString = MyAccService.getDefaultDateTime();
        ContentValues values=new ContentValues();
        values.put("at", dateString);
        values.put("x", x);
        values.put("y", y);
        values.put("z", z);
        values.put("lat", slat);
        values.put("lng", slng);
		values.put("s", speed);
        values.put("n", n);
        addThese.add(values);

    }

    private void addToLocalDatabase() {
        if(addThese.size()==0 || isAdding)
            return;
        isAdding=true;
        LogI(tag, "addToLocalDatabase");
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

            LogI(tag, "isReadOnly:" + isReadOnly);
            db.beginTransaction();
            LogI(tag, "beginTransaction");
            try
            {
                if(!isReadOnly)
                {
                    LogI(tag, "isDbLockedByCurrentThread:" + db.isDbLockedByCurrentThread());

                    while(addThese.size()>0) {
                        ContentValues values = addThese.get(0);
						LogI(tag, "find-table:" + mDbHelper.TABLE + "_" + mDbHelper.findTableDB(db,mDbHelper.TABLE));

						long lng = db.replace(mDbHelper.TABLE,null,values);
						if(lng==-1)
							lng = db.insert(mDbHelper.TABLE, null, values);

                        addThese.remove(0);
                        LogI(tag, "insert:" + lng);
                    }
                    db.setTransactionSuccessful();
                }
            }
            catch(Exception ex)
            {
                LogI(tag, ex.toString());
                //ex.printStackTrace();
            }
            finally
            {
                db.endTransaction();
                LogI(tag, "endTransaction");
                //db.close();
                //mDbHelper.close();
                isAdding=false;
            }
        }
		else
		{
			LogI(tag, "find-table:" + mDbHelper.TABLE + "_" + mDbHelper.findTable(mDbHelper.TABLE));
		}
    }
	public void clearDatabase() {
		if(addThese.size()>0 || isAdding)
			return;
		isAdding=true;
		LogI(tag, "clearDatabase");
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

			LogI(tag, "isReadOnly:" + isReadOnly);
			db.beginTransaction();
			LogI(tag, "beginTransaction");
			try
			{
				if(!isReadOnly)
				{
					long lng = db.delete(mDbHelper.TABLE,"",null);
					LogI(tag, "delete:" + lng);
					db.setTransactionSuccessful();
				}
			}
			catch(Exception ex)
			{
				LogI(tag, ex.toString());
				//ex.printStackTrace();
			}
			finally
			{
				db.endTransaction();
				LogI(tag, "endTransaction");
				//db.close();
				//mDbHelper.close();
				isAdding=false;
			}
		}
	}

	private void LogI(String tag,String msg)
    {
        if(MyAccService.bDebug)
            Log.i(tag,msg);

    }
}
