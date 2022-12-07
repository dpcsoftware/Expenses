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

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RadioGroup;
import android.widget.Spinner;

public class Widget1Config extends AppCompatActivity {
    private Intent resultIntent;
    private int wId;
    private SharedPreferences wPrefs;
    private Spinner sp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(RESULT_CANCELED);

        wPrefs = getSharedPreferences(App.WIDGET_PREFS_FNAME, MODE_PRIVATE);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        wId = AppWidgetManager.INVALID_APPWIDGET_ID;
        if (extras != null)
            wId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        else
            finish();

        setContentView(R.layout.widget1_config);

        resultIntent = new Intent();
        resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wId);

        ActionBar abar = getSupportActionBar();
        abar.setTitle(R.string.widget1config_c1);

        SQLiteDatabase db = DatabaseHelper.quickDb(this, DatabaseHelper.MODE_READ);
        Cursor c = db.rawQuery("SELECT " + Db.Table3._ID + "," + Db.Table3.GROUP_NAME +
                " FROM " + Db.Table3.TABLE_NAME + " ORDER BY " + Db.Table3.GROUP_NAME + " ASC", null);

        SimpleCursorAdapter sAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, c, new String[]{Db.Table3.GROUP_NAME}, new int[]{android.R.id.text1}, 0);
        sAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp = ((Spinner) findViewById(R.id.spinner1));
        sp.setAdapter(sAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.widget1_config, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item1:
                //Save Widget Preferences
                SharedPreferences.Editor pEditor = wPrefs.edit();
                int idSelected = ((RadioGroup) findViewById(R.id.radioGroup1)).getCheckedRadioButtonId();
                boolean byMonth = false;
                if (idSelected == R.id.radio0)
                    byMonth = true;
                pEditor.putBoolean(wId + "_BYMONTH", byMonth);
                pEditor.putLong(wId + "_GROUPID", sp.getSelectedItemId());
                pEditor.apply();

                //Update widget
                Intent updateIntent = new Intent(this, Widget1.class);
                updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{wId});
                try {
                    PendingIntent.getBroadcast(this, 0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT).send();
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }

                setResult(RESULT_OK, resultIntent);
                finish();
                break;
        }
        return true;
    }
}