/*
 *   Copyright 2013, 2014 Daniel Pereira Coelho
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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.app.Application;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PorterDuff;
import android.preference.PreferenceManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;

public class App extends Application {
	public static final PorterDuff.Mode colorFilterMode = PorterDuff.Mode.MULTIPLY;

	public DatabaseHelper dbH;
	public long activeGroupId;
	public int activeGroupPos = -1;
	public boolean mnUpdateList = false;
	public boolean mnUpdateMenu = false;
	public boolean addExUpdateCategories = false;
	public long addExUpdateCategoryId;
	public OnSharedPreferenceChangeListener prefsListener = new OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        	mnUpdateList = true;
        }
	};
	public boolean showChangesDialog;
	public int appVersion;
	
	
	private Resources rs;
	private SharedPreferences prefs;

	public static final void Toast(Context context, String msg) {
		android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show();
	}
	
	public static final void Toast(Context context, int resourceId) {
		android.widget.Toast.makeText(context, context.getResources().getString(resourceId), android.widget.Toast.LENGTH_SHORT).show();
	}
	
	public static final void Log(String msg) {
		Log.i("MN-Debug",msg);
	}
	
	public static final String WIDGET_PREFS_FNAME = "WigetPreferences";
	
	public void onCreate() {
		super.onCreate();
		dbH = new DatabaseHelper(this);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(prefsListener);
		rs = getResources();
		
		//Check if changes dialog must be shown
		int savedVersion = prefs.getInt("APP_VERSION", -1);
		try {
			appVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
			if(savedVersion < appVersion)
				showChangesDialog = true;
			else
				showChangesDialog = false;
		}
		catch (Exception e) {
			showChangesDialog = false;
		}
	}
	
	public void setFlag(int tableId) {
		switch(tableId) {
			case 1:
				mnUpdateList = true;
				
				//Update widgets if anyone is being used
				AppWidgetManager wManager = AppWidgetManager.getInstance(this);
				ComponentName cWidgetProvider = new ComponentName(this, Widget1.class);
				int wIds[] = wManager.getAppWidgetIds(cWidgetProvider);
				if(wIds.length != 0) {
					Intent updateIntent = new Intent(this, Widget1.class);
					updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
				    updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, wIds);
				    try {
				    	PendingIntent.getBroadcast(this, 0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT).send();
				    }
				    catch (PendingIntent.CanceledException e) {
				    	e.printStackTrace();
				    }
				}
				break;
			case 2:
				mnUpdateList = true;
				break;
			case 3:
				mnUpdateMenu = true;
				break;
		}
	}
		
	public class SpinnerMenu {
		private Spinner gSpinner;
		private SimpleCursorAdapter mAdapter;
		private Runnable callback;
		private ActionBarActivity activity;
	
		public SpinnerMenu(ActionBarActivity act, Runnable updateFunction) {
			callback = updateFunction;
			activity = act;
			renderMenu();			
		}
		
		public void renderMenu() {
			SQLiteDatabase db = DatabaseHelper.quickDb(activity, 0);
			Cursor mCursor = db.query(true, Db.Table3.TABLE_NAME, null, null, null, null, null, Db.Table3.GROUP_NAME + " ASC", null);
	    	if(mAdapter == null) {			
	    		mAdapter = new SimpleCursorAdapter(activity, R.layout.menu_spinner_item, mCursor, new String[] {Db.Table3.GROUP_NAME}, new int[] {android.R.id.text1},0);
	    		mAdapter.setDropDownViewResource(R.layout.menu_spinner_dd_item);
		    	
		        ActionBar abar = activity.getSupportActionBar();
		        View spview = activity.getLayoutInflater().inflate(R.layout.menu_spinner,null);
		        gSpinner = (Spinner) spview.findViewById(R.id.icsSpinner1);
		        gSpinner.setAdapter(mAdapter);
		        
		    	gSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
		        	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		        		activeGroupId = id;
		        		activeGroupPos = position;
		        		if(callback != null) callback.run();
		        	}
		        	public void onNothingSelected(AdapterView<?> parent) {        		
		        	}
		        });
		    	
		    	if(activeGroupPos == -1)
		    		activeGroupPos = prefs.getInt("ACTIVE_GROUP_POS", 0);
		    	
		    	if(activeGroupPos >=0 && activeGroupPos < gSpinner.getCount()) 
		    		gSpinner.setSelection(activeGroupPos);
		    	else
		    		activeGroupPos = 0;
		
		        abar.setCustomView(spview);
		        abar.setDisplayShowCustomEnabled(true);
		    	abar.setDisplayShowTitleEnabled(false);
	    	}
	    	else {
	    		mAdapter.swapCursor(mCursor);
	    		mAdapter.notifyDataSetChanged();
	    		if(activeGroupPos >= mAdapter.getCount()) {
	    			activeGroupPos = 0;
	    			gSpinner.setSelection(activeGroupPos);
	    		}
	    	}
	    	db.close();
		}
		
		public Spinner getSpinner() {
			return gSpinner;
		}
		
		public void setSelectedById(long id) {
			int i;
			
			for(i = 0;i < gSpinner.getCount();i++) {
				if(gSpinner.getItemIdAtPosition(i) == id) {
					gSpinner.setSelection(i);
					return;
				}
			}
		}
	}
	
	public String printMoney(float value) {
		int nFractionDigits = Currency.getInstance(Locale.getDefault()).getDefaultFractionDigits();
		String val = String.format("%."+nFractionDigits+"f", value);
		String symbol = prefs.getString("currencySymbol",rs.getString(R.string.standard_currency));
		if(prefs.getBoolean("cSymbolBefore",rs.getBoolean(R.bool.standard_currency_pos)) == true)
			return symbol + " " + val;
		else
			return val + " " + symbol;
	}
	
	public SimpleDateFormat getDateDbFormater(String pattern) {
		SimpleDateFormat dateDbFormater = new SimpleDateFormat(pattern, Locale.US);
		dateDbFormater.setTimeZone(TimeZone.getDefault());
		return dateDbFormater;
	}
	
	public String dateToDb(String pattern, int year, int month, int day) {
		Calendar cal = Calendar.getInstance();
		cal.set(year, month, day);
		return getDateDbFormater(pattern).format(cal.getTime());
	}
	
	public String dateToDb(String pattern, Date date) {
		return getDateDbFormater(pattern).format(date);
	}
	
	public String dateToUser(String pattern, String date) {
		SimpleDateFormat localFormater;
		if(pattern == null) {
			localFormater = (SimpleDateFormat) DateFormat.getDateInstance();
		}
		else 
			localFormater = new SimpleDateFormat(pattern, Locale.getDefault());
		
		try {
			return localFormater.format(getDateDbFormater("yyyy-MM-dd").parse(date));
		}
		catch (Exception e) {
			return null;
		}
	}
	
	public String dateToUser(String pattern, Date date) {
		SimpleDateFormat formater;
		if(pattern == null)
			formater = (SimpleDateFormat) DateFormat.getDateInstance();
		else
			formater = new SimpleDateFormat(pattern, Locale.getDefault());
		return formater.format(date);
	}
	
	public static boolean copyFile(String sourcePath, String destinationPath) {
		try {
			FileInputStream input = new FileInputStream(sourcePath);
			
			OutputStream output = new FileOutputStream(destinationPath);
			
			byte[] buffer = new byte[1024];
			int length;
			while ((length = input.read(buffer))>0)
			{
			    output.write(buffer, 0, length);
			}
	
			output.flush();
			output.close();
			input.close();
		
			return true;
		}
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
}