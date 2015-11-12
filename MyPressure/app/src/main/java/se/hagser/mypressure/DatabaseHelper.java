package se.hagser.mypressure;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class DatabaseHelper extends SQLiteOpenHelper{
	 final static String tag=MyPressureService.NOTIFICATION;
    //The Android's default system path of your application database.
    private static String DB_PATH = "";//"/data/data/com.example.myspeedcams/databases/";
 
    private static String DB_NAME = "MyPressure.sqlite";
 
    private SQLiteDatabase myDB; 
    public String TABLE="pressure";
 
    //private final Context myContext;
 	/*
    public File getStorageDir(String albumName) {
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), albumName);
        if (!file.mkdirs()) {
            Log.e(tag, "Directory not created");
        }
        return file;
    }
	*/
    public DatabaseHelper(Context context) {
 
    	super(context, DB_NAME, null, 1);
    	DB_PATH = context.getFilesDir().getPath()+"/";
    	DB_PATH = DB_PATH.replace("/files/","/databases/");
    	DB_PATH = getStorageDir(DB_NAME.replace(".sqlite", "")).getAbsolutePath()+"/";
		createDirectory(DB_PATH);
    	Log.i(tag, DB_PATH);
        try {
			createDataBase();
		} catch (Exception e) {
			Log.i(tag, "Couldn't create DB!");
			e.printStackTrace();
		}
    }

	public File getStorageDir(String albumName) {
		File file = new File(Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_DOWNLOADS), albumName);
		if (!file.mkdirs()) {
			Log.i(tag, "Directory not created");
		}
		return file;
	}

	private  void createDirectory(String path)
	{
		if(!new File(path).exists())
			new File(path).mkdirs();

	}
  /**
     * Creates a empty database on the system and rewrites it with your own database.
     * */
    public void createDataBase(){

    	Log.i(tag, "createDataBase");
        String myPath = DB_PATH + DB_NAME;
    	Log.i(tag, myPath);
    	File f = new File(myPath);
    	if(f.exists())
    	{
        	Log.i(tag, "f.exists()");
    	}
    	myDB = SQLiteDatabase.openDatabase(myPath, null,SQLiteDatabase.CREATE_IF_NECESSARY |SQLiteDatabase.NO_LOCALIZED_COLLATORS);
    	
    	//myDB.enableWriteAheadLogging();
		String strTable = findTable(TABLE);
    	Log.i(tag,"findTable:"+strTable);
		if(strTable.equals(""))
		{
	    	try
	    	{
		    	myDB.beginTransaction();
		    	final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS "+ TABLE +" ("
	                + "id INTEGER primary key AUTOINCREMENT,"
	                + "at DATETIME,"
	                + "val FLOAT,"
	                + "lat FLOAT,"
	                + "lng FLOAT,"
	                + "alt FLOAT,sent BIT);";
		    	Log.i(tag, "CREATE_TABLE:"+CREATE_TABLE);
		    	myDB.execSQL(CREATE_TABLE);
		    	myDB.setTransactionSuccessful();
	    	}
	    	catch(SQLException ex)
	    	{
	    		ex.printStackTrace();
	    	}
	    	finally
	    	{
		    	myDB.endTransaction();
		    	myDB.close();
	    	}
		}    	
    }

	public SQLiteDatabase getMyDB()
	{
		return myDB;
	}
	public long deleteRows(String ids){

		long lret=-1;
		try
		{
			this.openDataBase();
			if(findTable(TABLE).equals(TABLE)) {

				SQLiteDatabase database = this.getMyDB();
				ContentValues contentValues = new ContentValues();
				contentValues.put("sent","1");
				lret=database.update(TABLE,contentValues, "id in (" + ids.substring(1) + ")",null);
				//lret=database.delete(TABLE,"id in ("+ids.substring(1)+")",null);
				database.close();

			}
		}catch(Exception ex)
		{
			ex.printStackTrace();
		}

		return lret;
	}

	/**
	 * Compose JSON out of SQLite records
	 * @return
	 */
	public String composeJSONfromSQLite(){
		Cursor cursor = null;
		try
		{
			JSONArray jsonArray = new JSONArray();

			this.openDataBase();
			if(findTable(TABLE).equals(TABLE)) {
				String selectQuery = "SELECT distinct id,at,val,lat,lng,alt FROM " + TABLE + " where sent is null";
				SQLiteDatabase database = this.getMyDB();
				cursor = database.rawQuery(selectQuery, null);
				if (cursor.moveToFirst()) {
					do {
						JSONObject map = new JSONObject();
						try {
							map.put("id", cursor.getString(0));
							map.put("at", cursor.getString(1));
							map.put("val", cursor.getString(2));
							map.put("lat", cursor.getString(3));
							map.put("lng", cursor.getString(4));
							map.put("alt", cursor.getString(5));
						} catch (JSONException e) {
							e.printStackTrace();
						}
						jsonArray.put(map);
					} while (cursor.moveToNext());
				}
				database.close();
				return jsonArray.toString();
			}
		}catch(Exception ex)
		{
			ex.printStackTrace();
		}
		finally {
			if(cursor!=null)
				cursor.close();
		}
		return "";

	}

 public String findTable(String table)
 {
	String strRet = "";
	Cursor c = null;
	try
	{
		c = myDB.rawQuery("select tbl_name from sqlite_master where type='table' and tbl_name = '"+ table +"'", null);
		if(c.moveToFirst())
		{
			strRet = c.getString(0);
		}
	}
	catch(Exception ex)
	{
		ex.printStackTrace();
	}
	finally
	{
		if(c!=null)
			c.close();
	}
	return strRet;
 }

	public String findTableDB(SQLiteDatabase db, String table)
	{
		String strRet = "";
		Cursor c = null;
		try
		{
			c = db.rawQuery("select tbl_name from sqlite_master where type='table' and tbl_name = '"+ table +"'", null);
			if(c.moveToFirst())
			{
				strRet = c.getString(0);
			}
		}
		catch(Exception ex)
		{
			Log.i(tag, ex.getMessage());
		}
		finally
		{
			if(c!=null)
				c.close();
		}
		return strRet;
	}
	public void openDataBase() throws SQLException{

    	Log.i(tag, "openDataBase");
    	if(myDB!=null && myDB.isOpen())
    	{
        	Log.i(tag, "isOpen");
    		return;
    	}
    	//Open the database
        String myPath = DB_PATH + DB_NAME;
    	Log.i(tag, myPath);
    	File f = new File(myPath);
    	if(f.exists())
    	{
        	Log.i(tag, "f.exists()");
        	myDB = SQLiteDatabase.openDatabase(myPath, null,SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        	//myDB.enableWriteAheadLogging();
    	}
    }
 
    @Override
	public synchronized void close() {
		if(myDB != null)
			myDB.close();
		
		super.close();
	}
    
	@Override
	public void onCreate(SQLiteDatabase db) {

    	Log.i(tag, "onCreate");
	}
 
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    	Log.i(tag, "onUpgrade");
	}

}