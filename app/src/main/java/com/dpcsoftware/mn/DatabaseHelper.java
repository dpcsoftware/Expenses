/*
 *   Copyright 2013-2015 Daniel Pereira Coelho
 *   
 *   This file is part of the Expenses Android Application.
 *
 *   Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation in version 3.
 *
 *   Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Expenses.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.dpcsoftware.mn;



import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "dpcsoftware.mn";
    public static final int DATABASE_VERSION = 2;
    
    public static final int MODE_READ = 0;
    public static final int MODE_WRITE = 1;
	
	public static final String SQL_CREATE_T1 = "CREATE TABLE " + Db.Table1.TABLE_NAME + " ( "
			+ Db.Table1._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
    		+ Db.Table1.AMOUNT + " REAL,"
    		+ Db.Table1.DATE + " TEXT,"
    		+ Db.Table1.DETAILS + " TEXT,"
    		+ Db.Table1.ID_GROUP + " INTEGER,"
    		+ Db.Table1.ID_CATEGORY + " INTEGER,"
    		+ "FOREIGN KEY(" + Db.Table1.ID_GROUP + ") REFERENCES " + Db.Table3.TABLE_NAME + "(" + Db.Table3._ID + "),"
    		+ "FOREIGN KEY(" + Db.Table1.ID_CATEGORY + ") REFERENCES " + Db.Table2.TABLE_NAME + "(" + Db.Table2._ID + "))";
    
    public static final String SQL_CREATE_T2 = "CREATE TABLE " + Db.Table2.TABLE_NAME + " ( "
    		+ Db.Table2._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
    		+ Db.Table2.CATEGORY_NAME + " TEXT,"
    		+ Db.Table2.CATEGORY_COLOR + " INTEGER)";
        
    public static final String SQL_CREATE_T3 = "CREATE TABLE " + Db.Table3.TABLE_NAME + " ( "
    		+ Db.Table3._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
    		+ Db.Table3.GROUP_NAME + " TEXT,"
			+ Db.Table3.GROUP_TYPE + " INTEGER)";
	public static final String SQL_UPDATE_T3 = "ALTER TABLE " + Db.Table3.TABLE_NAME + " ADD COLUMN "
			+ Db.Table3.GROUP_TYPE + " INTEGER DEFAULT " + Db.Table3.TYPE_MONTH;

    public static final String SQL_CREATE_T4 = "CREATE TABLE " + Db.Table4.TABLE_NAME + " ( "
    		+ Db.Table4._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
    		+ Db.Table4.ID_GROUP + " INTEGER,"
    		+ Db.Table4.ID_CATEGORY + " INTEGER,"
    		+ Db.Table4.AMOUNT + " REAL,"
    		+ Db.Table4.ALERT + " INTEGER)";

	public static final String SQL_CREATE_T5 = "CREATE TABLE " + Db.Table5.TABLE_NAME + " ("
			+ Db.Table5._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ Db.Table5.TAG + " TEXT,"
			+ Db.Table5.VALUE + " TEXT)";
    	
    private Resources rs;
    
    public DatabaseHelper(Context context) {
    	super(context, DATABASE_NAME, null, DATABASE_VERSION);
    	rs = context.getResources();
    }
    
	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(SQL_CREATE_T2);
		insertCategory(database,R.string.dbHelper_c4,R.color.c14);
		insertCategory(database,R.string.dbHelper_c5,R.color.c1);
		insertCategory(database,R.string.dbHelper_c6,R.color.c4);
		insertCategory(database,R.string.dbHelper_c7,R.color.c7);
		insertCategory(database,R.string.dbHelper_c8,R.color.c5);
		
		database.execSQL(SQL_CREATE_T3);
		insertGroup(database,R.string.dbHelper_c1);
		insertGroup(database,R.string.dbHelper_c2);
		insertGroup(database,R.string.dbHelper_c3);
		
		database.execSQL(SQL_CREATE_T1);
		
		database.execSQL(SQL_CREATE_T4);

		database.execSQL(SQL_CREATE_T5);
		insertConfig(database, "db_version", String.valueOf(DATABASE_VERSION));
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
		upgradeDb(database, oldVersion, newVersion);
	}

	public static void upgradeDb(SQLiteDatabase database, int oldVersion, int newVersion) {
		if(oldVersion == 1) {
			database.execSQL(SQL_UPDATE_T3);
			database.execSQL(SQL_CREATE_T4);
			database.execSQL(SQL_CREATE_T5);
			insertConfig(database, "db_version", String.valueOf(DATABASE_VERSION));
		}
	}
	
	private long insertCategory(SQLiteDatabase db, int catName, int catColor) {
		ContentValues cv = new ContentValues();
		cv.put(Db.Table2.CATEGORY_NAME, rs.getString(catName));
		cv.put(Db.Table2.CATEGORY_COLOR, rs.getColor(catColor));
		return db.insert(Db.Table2.TABLE_NAME, null, cv);
	}
	
	private long insertGroup(SQLiteDatabase db, int grName) {
		ContentValues cv = new ContentValues();
		cv.put(Db.Table3.GROUP_NAME, rs.getString(grName));
		return db.insert(Db.Table3.TABLE_NAME,null,cv);
	}

	private static long insertConfig(SQLiteDatabase db, String tag, String value) {
		ContentValues cv = new ContentValues();
		cv.put(Db.Table5.TAG, tag);
		cv.put(Db.Table5.VALUE, value);
		return db.insert(Db.Table5.TABLE_NAME, null, cv);
	}

	public static SQLiteDatabase quickDb(Activity activity, int mode) {
		SQLiteDatabase db;
		DatabaseHelper dbHelper = ((App) activity.getApplication()).dbH;
		if(mode == MODE_READ)
			db = dbHelper.getReadableDatabase();
		else
			db = dbHelper.getWritableDatabase();
		return db;
	}
}

