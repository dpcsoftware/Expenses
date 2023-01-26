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

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class App extends Application {
    public static final PorterDuff.Mode colorFilterMode = PorterDuff.Mode.MULTIPLY;

    public DatabaseHelper dbH;
    public long activeGroupId;
    public int activeGroupPos = -1;
    public boolean mnUpdateList = false;
    public boolean mnUpdateMenu = false;
    public boolean addExUpdateCategories = false;
    public boolean editCategoriesUpdateList = false;
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

    public static void Toast(Context context, String msg) {
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show();
    }

    public static void Toast(Context context, int resourceId) {
        android.widget.Toast.makeText(context, context.getResources().getString(resourceId), android.widget.Toast.LENGTH_SHORT).show();
    }

    public static void Log(String msg) {
        Log.d("Expenses-Debug", msg);
    }

    public static final String WIDGET_PREFS_FNAME = "WigetPreferences";

    public static final String BUDGET_NOTIFICATIONS_CHANNEL_ID = "budget";

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
            showChangesDialog = savedVersion < appVersion;
        } catch (Exception e) {
            showChangesDialog = false;
        }

        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(BUDGET_NOTIFICATIONS_CHANNEL_ID,
                    getString(R.string.app_c2), NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void setFlag(int tableId) {
        switch (tableId) {
            case 1:
                mnUpdateList = true;

                //Update widgets if anyone is being used
                AppWidgetManager wManager = AppWidgetManager.getInstance(this);
                ComponentName cWidgetProvider = new ComponentName(this, Widget1.class);
                int wIds[] = wManager.getAppWidgetIds(cWidgetProvider);
                if (wIds.length != 0) {
                    Intent updateIntent = new Intent(this, Widget1.class);
                    updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, wIds);
                    try {
                        PendingIntent.getBroadcast(this, 0, updateIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)
                                .send();
                    } catch (PendingIntent.CanceledException e) {
                        e.printStackTrace();
                    }
                }

                //Check if any budget item was surpassed
                if (prefs.getBoolean("budgetAlert", true)) {
                    boolean notified = false;
                    int flag, budgetType;
                    String queryModifier = "";

                    NotificationManager notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    Intent it = new Intent(this, Budget.class);
                    it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    SQLiteDatabase db = dbH.getWritableDatabase();

                    Cursor cType = db.rawQuery("SELECT " +
                            Db.Table3.GROUP_TYPE +
                            " FROM " + Db.Table3.TABLE_NAME +
                            " WHERE " + Db.Table3._ID + " = " + activeGroupId, null);
                    if (cType.getCount() > 0) {
                        cType.moveToFirst();
                        budgetType = cType.getInt(0);
                        cType.close();

                        if (budgetType == Db.Table3.TYPE_MONTH)
                            queryModifier = " AND strftime('%Y-%m'," + Db.Table1.T_DATE + ") = '" + dateToDb("yyyy-MM", Calendar.getInstance().getTime()) + "'";

                        Cursor c = db.rawQuery("SELECT "
                                + Db.Table4.T_ID + ","
                                + Db.Table4.T_AMOUNT + ","
                                + Db.Table4.T_ALERT + ","
                                + "SUM(" + Db.Table1.T_AMOUNT + ")" +
                                " FROM " + Db.Table4.TABLE_NAME +
                                " LEFT OUTER JOIN " + Db.Table1.TABLE_NAME + " ON " + Db.Table4.T_ID_CATEGORY + " = " + Db.Table1.T_ID_CATEGORY +
                                " AND " + Db.Table4.T_ID_GROUP + " = " + Db.Table1.T_ID_GROUP +
                                queryModifier +
                                " WHERE " + Db.Table4.T_ID_GROUP + " = " + activeGroupId +
                                " GROUP BY " + Db.Table4.T_ID, null);
                        c.moveToFirst();
                        while (!c.isAfterLast()) {
                            if (c.getFloat(3) > c.getFloat(1)) {
                                if (!notified && c.getInt(2) == 0) {
                                    PendingIntent pi = PendingIntent.getActivity(this, 0, it,
                                            PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
                                    NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(this, BUDGET_NOTIFICATIONS_CHANNEL_ID)
                                            .setContentTitle(rs.getString(R.string.app_name))
                                            .setContentText(rs.getString(R.string.app_c1))
                                            .setSmallIcon(R.drawable.money_white)
                                            .setLargeIcon(drawableToBitmap(ContextCompat.getDrawable(getApplicationContext(), R.drawable.logo)))
                                            .setContentIntent(pi)
                                            .setAutoCancel(true);
                                    notifManager.notify(1, notifBuilder.build());
                                    notified = true;
                                }
                                flag = 1;
                            } else
                                flag = 0;

                            //set flag alert on database
                            ContentValues values = new ContentValues();
                            values.put(Db.Table4.ALERT, flag);
                            db.update(Db.Table4.TABLE_NAME, values, Db.Table4._ID + " = " + c.getLong(0), null);

                            c.moveToNext();
                        }
                        c.close();
                    } else
                        cType.close();

                    db.close();
                }

                break;
            case 2:
                mnUpdateList = true;
                break;
            case 3:
                mnUpdateMenu = true;
                break;
            case 4:
                break;
        }
    }

    public class SpinnerMenu {
        private Spinner gSpinner;
        private SimpleCursorAdapter mAdapter;
        private Runnable callback;
        private AppCompatActivity activity;
        private EditText searchEditText;
        private boolean searchBar;

        public SpinnerMenu(AppCompatActivity act, Runnable updateFunction) {
            callback = updateFunction;
            activity = act;
            searchBar = false;
            renderMenu();
        }

        public SpinnerMenu(AppCompatActivity act, Runnable updateFunction, boolean sBar) {
            callback = updateFunction;
            activity = act;
            searchBar = sBar;
            renderMenu();
        }

        public void renderMenu() {
            SQLiteDatabase db = DatabaseHelper.quickDb(activity, 0);
            Cursor mCursor = db.query(true, Db.Table3.TABLE_NAME, null, null, null, null, null, Db.Table3.GROUP_NAME + " ASC", null);
            if (mAdapter == null) {
                mAdapter = new SimpleCursorAdapter(activity, R.layout.menu_spinner_item, mCursor, new String[]{Db.Table3.GROUP_NAME}, new int[]{android.R.id.text1}, 0);
                mAdapter.setDropDownViewResource(R.layout.menu_spinner_dd_item);

                ActionBar abar = activity.getSupportActionBar();
                View menuView;
                if (!searchBar)
                    menuView = activity.getLayoutInflater().inflate(R.layout.menu_spinner, null);
                else {
                    menuView = activity.getLayoutInflater().inflate(R.layout.menu_spinner_search, null);
                    searchEditText = (EditText) menuView.findViewById(R.id.searchEditText);
                }
                gSpinner = (Spinner) menuView.findViewById(R.id.icsSpinner1);
                gSpinner.setAdapter(mAdapter);

                gSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        activeGroupId = id;
                        activeGroupPos = position;
                        if (callback != null) callback.run();
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });

                if (activeGroupPos == -1)
                    activeGroupPos = prefs.getInt("ACTIVE_GROUP_POS", 0);

                if (activeGroupPos >= gSpinner.getCount())
                    activeGroupPos = 0;

                gSpinner.setSelection(activeGroupPos);

                abar.setCustomView(menuView);
                abar.setDisplayShowCustomEnabled(true);
                abar.setDisplayShowTitleEnabled(false);
            } else {
                mAdapter.swapCursor(mCursor);
                mAdapter.notifyDataSetChanged();
                if (activeGroupPos >= mAdapter.getCount()) {
                    activeGroupPos = 0;
                    gSpinner.setSelection(activeGroupPos);
                }
            }
            db.close();
        }

        public Spinner getSpinner() {
            return gSpinner;
        }

        public EditText getEditText() {
            return searchEditText;
        }

        public void setSelectedById(long id) {
            int i;

            for (i = 0; i < gSpinner.getCount(); i++) {
                if (gSpinner.getItemIdAtPosition(i) == id) {
                    gSpinner.setSelection(i);
                    return;
                }
            }
        }
    }

    public String printMoney(float value) {
        return printMoney(value, true);
    }

    public String printMoney(float value, boolean withSymbol) {
        int nFractionDigits;

        try {
            nFractionDigits = Currency.getInstance(Locale.getDefault()).getDefaultFractionDigits();
        } catch (IllegalArgumentException e) {
            nFractionDigits = 2;
        }

        String val = String.format("%." + nFractionDigits + "f", value);
        if (withSymbol) {
            String symbol = prefs.getString("currencySymbol", rs.getString(R.string.standard_currency));
            if (prefs.getBoolean("cSymbolBefore", rs.getBoolean(R.bool.standard_currency_pos)))
                return symbol + " " + val;
            else
                return val + " " + symbol;
        }
        else {
            return val;
        }
    }

    public static SimpleDateFormat getDateDbFormater(String pattern) {
        SimpleDateFormat dateDbFormater = new SimpleDateFormat(pattern, Locale.US);
        dateDbFormater.setTimeZone(TimeZone.getDefault());
        return dateDbFormater;
    }

    public static String dateToDb(String pattern, int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day);
        return getDateDbFormater(pattern).format(cal.getTime());
    }

    public static String dateToDb(String pattern, Date date) {
        return getDateDbFormater(pattern).format(date);
    }

    public static String dateToUser(String pattern, String date) {
        SimpleDateFormat localFormater;
        if (pattern == null) {
            localFormater = (SimpleDateFormat) DateFormat.getDateInstance();
        } else
            localFormater = new SimpleDateFormat(pattern, Locale.getDefault());

        try {
            return localFormater.format(getDateDbFormater("yyyy-MM-dd").parse(date));
        } catch (Exception e) {
            return null;
        }
    }

    public static String dateToUser(String pattern, Date date) {
        SimpleDateFormat formater;
        if (pattern == null)
            formater = (SimpleDateFormat) DateFormat.getDateInstance();
        else
            formater = new SimpleDateFormat(pattern, Locale.getDefault());
        return formater.format(date);
    }

    public static boolean copyFile(String sourcePath, String destinationPath) {
        try {
            OutputStream output = new FileOutputStream(destinationPath);
            FileInputStream input = new FileInputStream(sourcePath);
            return copyStream(input, output);
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean copyStream(InputStream input, OutputStream output) {
        try {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }

            output.flush();
            output.close();
            input.close();

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public int dpToPx(float dp) {
        return Math.round(getApplicationContext().getResources().getDisplayMetrics().density * dp);
    }

    public static boolean isAndroidQOrAbove() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }
}