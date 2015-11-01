package se.hagser.mysensors;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class UpdateDbTask extends AsyncTask<String, Integer, String> {
	final static String tag = "udt";

	DatabaseHelper mDbHelper=null;
	public int cache=-1;
    private boolean isAdding;

    ArrayList<ContentValues> addThese = new ArrayList<ContentValues>();

    public UpdateDbTask(DatabaseHelper dbh) {
		
		mDbHelper = dbh;
	}
	
	protected void onProgressUpdate(Integer... progress) {
		MainActivity.LogI(tag+" progress", progress[0] + "");
		cache=progress[0];
	}

	protected void onPostExecute(String result) {
		mDbHelper.close();
	}

	@Override
	protected String doInBackground(String... args) {

        addToLocalDatabase(args[0], args[1], args[2], args[3], args[4]);
        addToLocalDatabase();
        //this.onPostExecute("OK");

		return null;
	}
    private void addToLocalDatabase(String at, String a, String b, String c, String typ) {
        MainActivity.LogI(tag, "addToLocalDatabase-args");
		MainActivity.LogI(tag,"a:"+a+",b:"+b+",c:"+c+",typ:"+typ);

        ContentValues values=new ContentValues();
        values.put("at", at);
        values.put("a", a);
        values.put("b", b);
        values.put("c", c);
        values.put("typ", typ);
        addThese.add(values);

    }

    private void addToLocalDatabase() {
        if(addThese.size()==0 || isAdding)
            return;
        isAdding=true;
        MainActivity.LogI(tag, "addToLocalDatabase");
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

            MainActivity.LogI(tag, "isReadOnly:" + isReadOnly);
            db.beginTransaction();
            MainActivity.LogI(tag, "beginTransaction");
            try
            {
                if(!isReadOnly)
                {
                    MainActivity.LogI(tag, "isDbLockedByCurrentThread:" + db.isDbLockedByCurrentThread());

                    while(addThese.size()>0) {
                        ContentValues values = addThese.get(0);
						MainActivity.LogI(tag, "find-table:" + mDbHelper.TABLE + "_" + mDbHelper.findTableDB(db,mDbHelper.TABLE));

						long lng = db.replace(mDbHelper.TABLE,null,values);
						if(lng==-1)
							lng = db.insert(mDbHelper.TABLE, null, values);

                        addThese.remove(0);
                        MainActivity.LogI(tag, "insert:" + lng);
                    }
                    db.setTransactionSuccessful();
                }
            }
            catch(Exception ex)
            {
                MainActivity.LogI(tag, ex.toString());
                //ex.printStackTrace();
            }
            finally
            {
                db.endTransaction();
                MainActivity.LogI(tag, "endTransaction");
                //db.close();
                //mDbHelper.close();
                isAdding=false;
            }
        }
		else
		{
			MainActivity.LogI(tag, "find-table:" + mDbHelper.TABLE + "_" + mDbHelper.findTable(mDbHelper.TABLE));
		}
    }
	public void clearDatabase() {
		if(addThese.size()>0 || isAdding)
			return;
		isAdding=true;
		MainActivity.LogI(tag, "clearDatabase");
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

			MainActivity.LogI(tag, "isReadOnly:" + isReadOnly);
			db.beginTransaction();
			MainActivity.LogI(tag, "beginTransaction");
			try
			{
				if(!isReadOnly)
				{
					long lng = db.delete(mDbHelper.TABLE,"",null);
					MainActivity.LogI(tag, "delete:" + lng);
					db.setTransactionSuccessful();
				}
			}
			catch(Exception ex)
			{
				MainActivity.LogI(tag, ex.toString());
				//ex.printStackTrace();
			}
			finally
			{
				db.endTransaction();
				MainActivity.LogI(tag, "endTransaction");
				//db.close();
				//mDbHelper.close();
				isAdding=false;
			}
		}
	}

	public static String getDefaultDateTime() {

		Date mDate = new Date();

		Calendar c = Calendar.getInstance();
		c.setTime(mDate);

		return getDateString(c.get(Calendar.YEAR), c.get(Calendar.MONTH),
				c.get(Calendar.DAY_OF_MONTH)) + " " + getTimeString(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE),
				c.get(Calendar.SECOND),c.get(Calendar.MILLISECOND));

	}

	public static String getDateString(int year, int monthOfYear, int dayOfMonth) {

		monthOfYear++;
		String mon = "" + monthOfYear;
		String day = "" + dayOfMonth;

		if (monthOfYear < 10)
			mon = "0" + monthOfYear;
		if (dayOfMonth < 10)
			day = "0" + dayOfMonth;

		return year + "-" + mon + "-" + day;
	}

	public static String getTimeString(int hourOfDay, int minute, int second, int millisecond) {
		String hour = "" + hourOfDay;
		String min = "" + minute;
		String sec = "" + second;
		String millisec = "" + millisecond;

		if (hourOfDay < 10)
			hour = "0" + hourOfDay;
		if (minute < 10)
			min = "0" + minute;
		if (second < 10)
			sec = "0" + second;
		if (millisecond < 10)
			millisec = "00" + millisecond;
		else if (millisecond < 100)
			millisec = "0" + millisecond;


		return hour + ":" + min + ":"+sec+"."+millisec;
	}

}
