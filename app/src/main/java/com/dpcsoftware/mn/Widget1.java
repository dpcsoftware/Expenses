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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;

public class Widget1 extends AppWidgetProvider {
	SharedPreferences prefs, wPrefs;
	Resources rs;

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        wPrefs = context.getSharedPreferences(App.WIDGET_PREFS_FNAME, Context.MODE_PRIVATE);
        rs = context.getResources();
        		
        for (int appWidgetId : appWidgetIds) {
            long groupId = wPrefs.getLong(appWidgetId + "_GROUPID", 0);

            Intent intent = new Intent(context, AddEx.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            intent.putExtra("SET_GROUP_ID", groupId);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget1);
            views.setOnClickPendingIntent(R.id.imageButton1, pendingIntent);
            
            Intent intent2 = new Intent(context, ExpensesList.class);
            intent2.putExtra("SET_GROUP_ID", groupId);
            PendingIntent pendingIntent2 = PendingIntent.getActivity(context, appWidgetId, intent2, PendingIntent.FLAG_UPDATE_CURRENT);
            
            views.setOnClickPendingIntent(R.id.LinearLayout1, pendingIntent2);
            
            
            DatabaseHelper dbHelper = new DatabaseHelper(context);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
   			Date date = Calendar.getInstance().getTime();
    		
   			boolean isByMonth = wPrefs.getBoolean(appWidgetId + "_BYMONTH", true);
    		String queryModifier = "";
    		
    		SimpleDateFormat dateDbFormater = new SimpleDateFormat("yyyy-MM", Locale.US);
    		dateDbFormater.setTimeZone(TimeZone.getDefault());    		
    		
    		if(isByMonth)
    			queryModifier = " AND strftime('%Y-%m'," + Db.Table1.DATE + ") = '" + dateDbFormater.format(date) + "'";
    		
    		Cursor c = db.rawQuery("SELECT " +
    				"SUM(" + Db.Table1.AMOUNT + ")" +
    				" FROM " +
    				Db.Table1.TABLE_NAME +
    				" WHERE " + Db.Table1.ID_GROUP + " = " + groupId +
    				queryModifier, null);    		
    		c.moveToFirst();
    		
    		Cursor c2 = db.rawQuery("SELECT " + Db.Table3.GROUP_NAME + " FROM " + Db.Table3.TABLE_NAME +
    				" WHERE " + Db.Table3._ID + " = " + groupId ,null);
    		c2.moveToFirst();
    		
    		if(c2.getCount() == 1)
    			views.setTextViewText(R.id.textView1, c2.getString(0));
    		else
    			views.setTextViewText(R.id.textView1, "Erro");
    		
    		if(isByMonth) {
    			views.setTextViewText(R.id.textView2, (new SimpleDateFormat("LLL/yy",Locale.getDefault())).format(date));
    			views.setViewVisibility(R.id.textView2, View.VISIBLE);
    		}
    		else
    			views.setViewVisibility(R.id.textView2, View.GONE);
    		
    		views.setTextViewText(R.id.textView3, printMoney(c.getFloat(0)));
    		
            appWidgetManager.updateAppWidget(appWidgetId, views);
            
            db.close();
        }
    }
    
	public String printMoney(float value) {
		int nFractionDigits = Currency.getInstance(Locale.getDefault()).getDefaultFractionDigits();
		String val = String.format("%."+nFractionDigits+"f", value);
		String symbol = prefs.getString("currencySymbol",rs.getString(R.string.standard_currency));
		if(prefs.getBoolean("cSymbolBefore",rs.getBoolean(R.bool.standard_currency_pos)))
			return symbol + " " + val;
		else
			return val + " " + symbol;
	}
}