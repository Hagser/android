package se.hagser.mysensors;

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
	private static String tag="db";
    private static String DB_PATH = "";
    private static String DB_NAME = "MySensors.sqlite";
 
    private SQLiteDatabase myDB; 
    public String TABLE="Sensors";

    public DatabaseHelper(Context context) {
 
    	super(context, DB_NAME, null, 1);
    	DB_PATH = context.getFilesDir().getPath()+"/";
    	DB_PATH = DB_PATH.replace("/files/","/databases/");
		DB_PATH = getStorageDir(DB_NAME.replace(".sqlite", "")).getAbsolutePath()+"/";
        createDirectory(DB_PATH);
        LogI(tag+" createDataBase", DB_PATH);
        try {
			createDataBase();
		} catch (Exception e) {
			LogI(tag, "Couldn't create DB!");
			e.printStackTrace();
		}
    }

	public File getStorageDir(String albumName) {
		File file = new File(Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_DOWNLOADS), albumName);
		if (!file.mkdirs()) {
			LogI(tag, "Directory not created");
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

    	LogI(tag, "createDataBase");
        String myPath = DB_PATH + DB_NAME;
    	LogI(tag, myPath);
    	File f = new File(myPath);
    	if(f.exists())
    	{

        	LogI(tag+"1", "f.exists():"+f.getPath());
    	}
    	myDB = SQLiteDatabase.openDatabase(myPath, null,SQLiteDatabase.CREATE_IF_NECESSARY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);

    	//myDB.enableWriteAheadLogging();
		String strTable = findTable(TABLE);
    	LogI(tag,"findTable:"+strTable);
		if(strTable.equals(""))
		{
	    	try
	    	{
		    	myDB.beginTransaction();
		    	final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS "+ TABLE +" ("
	                + "id INTEGER primary key AUTOINCREMENT,"
	                + "at DATETIME,"
	                + "a FLOAT,"
	                + "b FLOAT,"
                    + "c FLOAT,"
					+ "typ TEXT);";
		    	LogI(tag, "CREATE_TABLE:"+CREATE_TABLE);
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
		LogI(tag,ex.getMessage());
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
			LogI(tag, ex.getMessage());
		}
		finally
		{
			if(c!=null)
				c.close();
		}
		return strRet;
	}

	/**
	 * Compose JSON out of SQLite records
	 * @return
	 */
	public String composeJSONfromSQLite(int skip,int limit){
		Cursor cursor = null;
		try
		{
			JSONArray jsonArray = new JSONArray();

			this.openDataBase();
			if(findTable(TABLE).equals(TABLE)) {
				String selectQuery = "SELECT distinct at,a,b,c,typ,id FROM " + TABLE + " limit "+skip+","+limit;
				SQLiteDatabase database = this.getMyDB();
				cursor = database.rawQuery(selectQuery, null);
				if (cursor.moveToFirst()) {
					do {
						JSONObject map = new JSONObject();
						try {
							map.put("at", cursor.getString(0));
							map.put("a", cursor.getString(1));
							map.put("b", cursor.getString(2));
							map.put("c", cursor.getString(3));
							map.put("typ", cursor.getString(4));
							map.put("id", cursor.getString(5));
						} catch (JSONException e) {
							e.printStackTrace();
						}
						jsonArray.put(map);
					} while (cursor.moveToNext());
				}
				database.close();
				LogI("jsonArray", jsonArray.length() + "");
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

	/**
	 * Compose JSON out of SQLite records
	 * @return
	 */
	public long countRecords(){

		long ret=0;
		Cursor cursor =null;
		try {
			this.openDataBase();
			if (findTable(TABLE).equals(TABLE)) {
				String selectQuery = "SELECT count(id) FROM " + TABLE+"";
				SQLiteDatabase database = this.getMyDB();
				cursor = database.rawQuery(selectQuery, null);
				if (cursor.moveToFirst()) {
					ret = cursor.getLong(0);
				}
				database.close();
			}
		}catch(Exception ex)
		{
			ex.printStackTrace();
		}
		finally {
			if(cursor!=null)
				cursor.close();
		}
		return ret;

	}
	/**
	 * Compose JSON out of SQLite records
	 * @return
	 */
	public long deleteRows(String ids){

		long lret=-1;
		try
		{
		this.openDataBase();
		if(findTable(TABLE).equals(TABLE)) {

			SQLiteDatabase database = this.getMyDB();
			lret=database.delete(TABLE,"id in ("+ids.substring(1)+")",null);
			LogI(tag,"deleted:"+lret);
			database.close();

		}
		}catch(Exception ex)
		{
			ex.printStackTrace();
		}

		return lret;
	}
	public SQLiteDatabase getMyDB()
	{
		return myDB;
	}
	public void openDataBase() throws SQLException{

    	LogI(tag, "openDataBase");
    	if(myDB!=null && myDB.isOpen())
    	{
        	LogI(tag, "isOpen");
    		return;
    	}
    	//Open the database
        String myPath = DB_PATH + DB_NAME;
    	LogI(tag, myPath);
    	File f = new File(myPath);
    	if(f.exists())
    	{
			LogI(tag+"2", "f.exists():"+f.getPath());
        	myDB = SQLiteDatabase.openDatabase(myPath, null,SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        	//myDB.enableWriteAheadLogging();
    	}
        else
        {
            LogI(tag, "f.not exists()");
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

    	LogI(tag, "onCreate");
	}
 
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    	LogI(tag, "onUpgrade");
	}

    private void LogI(String tag,String msg)
    {
        if(BuildConfig.DEBUG)
            Log.i(tag,msg);

    }
}