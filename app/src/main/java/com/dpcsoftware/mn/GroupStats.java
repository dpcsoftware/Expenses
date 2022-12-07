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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.Calendar;

public class GroupStats extends AppCompatActivity {
    private App app;
    private GroupStatsAdapter adapter;
    private View footer;
    private Calendar date;
    private boolean isByMonth = true;
    private View.OnClickListener monthButtonCListener = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.imageButton1:
                    date.add(Calendar.MONTH, -1);
                    break;
                case R.id.imageButton2:
                    date.add(Calendar.MONTH, 1);
                    break;
            }
            renderGraph();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (App) getApplication();
        date = Calendar.getInstance();

        ActionBar abar = getSupportActionBar();
        if (abar != null) {
            abar.setTitle(R.string.groupstats_c1);
        }
        setContentView(R.layout.groupstats);

        ListView lv = ((ListView) findViewById(R.id.listView1));
        LayoutInflater inflater = LayoutInflater.from(this);
        footer = inflater.inflate(R.layout.groupstats_listitem, null);
        TextView footerText = (TextView) footer.findViewById(R.id.textView1);
        footerText.setText(R.string.gp_10);
        footerText.setTypeface(null, Typeface.BOLD);
        lv.addFooterView(footer);

        findViewById(R.id.imageButton1).setOnClickListener(monthButtonCListener);
        findViewById(R.id.imageButton2).setOnClickListener(monthButtonCListener);

        ((RadioGroup) findViewById(R.id.radioGroup1)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                LinearLayout monthPicker = (LinearLayout) findViewById(R.id.LinearLayoutMonthPicker);
                if (checkedId == R.id.radio0) {
                    isByMonth = true;
                    monthPicker.setVisibility(View.VISIBLE);
                } else {
                    isByMonth = false;
                    monthPicker.setVisibility(View.GONE);
                }
                renderGraph();
            }
        });

        renderGraph();
    }

    public void renderGraph() {
        SQLiteDatabase db = DatabaseHelper.quickDb(this, DatabaseHelper.MODE_READ);

        String queryModifier = "";
        if (isByMonth)
            queryModifier = " AND strftime('%Y-%m'," + Db.Table1.T_DATE + ") = '" + App.dateToDb("yyyy-MM", date.getTime()) + "'";

        Cursor c = db.rawQuery("SELECT " +
                Db.Table3.T_ID + "," +
                Db.Table3.T_GROUP_NAME + "," +
                "SUM(" + Db.Table1.T_AMOUNT + ")" +
                " FROM " +
                Db.Table3.TABLE_NAME +
                " LEFT JOIN " +
                Db.Table1.TABLE_NAME +
                " ON " +
                Db.Table3.T_ID + " = " + Db.Table1.T_ID_GROUP +
                queryModifier +
                " GROUP BY " + Db.Table3.T_ID +
                " ORDER BY " + Db.Table3.T_GROUP_NAME, null);

        float total = 0;
        while (c.moveToNext()) {
            total += c.getFloat(2);
        }

        ListView lv = ((ListView) findViewById(R.id.listView1));
        ((TextView) footer.findViewById(R.id.textView2)).setText(app.printMoney(total));

        ((TextView) findViewById(R.id.textViewMonth)).setText(App.dateToUser("MMMM / yyyy", date.getTime()));

        if (adapter == null) {
            adapter = new GroupStatsAdapter(this, c);
            lv.setAdapter(adapter);
        } else {
            adapter.changeCursor(c);
            adapter.notifyDataSetChanged();
        }
    }

    private class GroupStatsAdapter extends CursorAdapter {
        private LayoutInflater mInflater;

        public GroupStatsAdapter(Context context, Cursor c) {
            super(context, c, 0);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mInflater.inflate(R.layout.groupstats_listitem, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ((TextView) view.findViewById(R.id.textView1)).setText(cursor.getString(1));
            ((TextView) view.findViewById(R.id.textView2)).setText(app.printMoney(cursor.getFloat(2)));
        }
    }
}
