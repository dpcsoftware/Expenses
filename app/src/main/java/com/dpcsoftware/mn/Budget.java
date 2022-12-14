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

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;

import java.util.Calendar;

public class Budget extends AppCompatActivity implements View.OnClickListener,
        AdapterView.OnItemClickListener, RadioGroup.OnCheckedChangeListener, AdapterView.OnItemLongClickListener {
    private App app;
    private Runnable menuCallback = new Runnable() {
        public void run() {
            SQLiteDatabase db = DatabaseHelper.quickDb(Budget.this, DatabaseHelper.MODE_READ);

            Cursor c = db.rawQuery("SELECT " + Db.Table3.GROUP_TYPE +
                    " FROM " + Db.Table3.TABLE_NAME +
                    " WHERE " + Db.Table3._ID + " = " + app.activeGroupId, null);
            c.moveToFirst();
            gType = c.getInt(0);
            c.close();
            if (gType == Db.Table3.TYPE_MONTH)
                rg.check(R.id.radio0);
            else
                rg.check(R.id.radio1);
            renderList();
        }
    };
    private Resources res;
    private Calendar date;
    private ListView lv;
    private View header;
    private BudgetAdapter adapter;
    private int gType;
    private RadioGroup rg;
    private ActionMode.Callback actModeCallback;
    private ActionMode actMode;
    private long selectedId;
    private boolean pickMode = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (App) getApplication();

        setContentView(R.layout.budget);

        res = getResources();

        actModeCallback = new BudActionModeCallback();
        actMode = null;

        date = Calendar.getInstance();
        findViewById(R.id.imageButton1).setOnClickListener(this);
        findViewById(R.id.imageButton2).setOnClickListener(this);
        ((TextView) findViewById(R.id.dateView)).setText(App.dateToUser("MMMM / y", date.getTime()));

        rg = (RadioGroup) findViewById(R.id.radioGroup1);
        rg.setOnCheckedChangeListener(this);

        lv = (ListView) findViewById(android.R.id.list);

        ((TextView) findViewById(R.id.textView1)).setText(R.string.budget_c1);
        lv.setEmptyView(findViewById(R.id.empty));
        lv.setOnItemClickListener(this);
        lv.setOnItemLongClickListener(this);

        header = LayoutInflater.from(this).inflate(R.layout.budget_listitem, null);
        ((TextView) header.findViewById(R.id.textViewLabel)).setText(R.string.gp_10);
        ((TextView) header.findViewById(R.id.textViewLabel)).setTypeface(Typeface.DEFAULT_BOLD);
        ((TextView) header.findViewById(R.id.textViewPercentage)).setTypeface(Typeface.DEFAULT_BOLD);
        ((TextView) header.findViewById(R.id.textViewValues)).setTypeface(Typeface.DEFAULT_BOLD);
        header.findViewById(R.id.imageView1).setVisibility(View.GONE);
        header.setBackgroundColor(getResources().getColor(R.color.yellow));
        header.setOnClickListener(null);
        lv.addHeaderView(header);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.budget, menu);

        app.new SpinnerMenu(this, menuCallback);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item1:
                Bundle args = new Bundle();
                args.putInt("MODE", BudgetDialog.ADD);
                BudgetDialog addDg = new BudgetDialog();
                addDg.setArguments(args);
                addDg.show(getSupportFragmentManager(), null);
                break;
        }

        return true;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.imageButton1)
            date.add(Calendar.MONTH, -1);
        else
            date.add(Calendar.MONTH, 1);

        ((TextView) findViewById(R.id.dateView)).setText(App.dateToUser("MMMM / y", date.getTime()));
        renderList();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        Bundle args = new Bundle();
        args.putLong("EDIT_ID", id);
        args.putInt("MODE", BudgetDialog.EDIT);
        BudgetDialog edtDg = new BudgetDialog();
        edtDg.setArguments(args);
        edtDg.show(getSupportFragmentManager(), null);

        if (pickMode)
            actMode.finish();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (!pickMode) {
            pickMode = true;
            selectItem(view, id);
            actMode = startSupportActionMode(actModeCallback);
        }
        return true;
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        SQLiteDatabase db = DatabaseHelper.quickDb(this, DatabaseHelper.MODE_WRITE);
        App.Log("here");
        if (checkedId == R.id.radio0) {
            gType = Db.Table3.TYPE_MONTH;
            findViewById(R.id.dateLayout).setVisibility(View.VISIBLE);
        } else {
            gType = Db.Table3.TYPE_TOTAL;
            findViewById(R.id.dateLayout).setVisibility(View.GONE);
        }
        ContentValues cv = new ContentValues();
        cv.put(Db.Table3.GROUP_TYPE, gType);
        db.update(Db.Table3.TABLE_NAME, cv, Db.Table3._ID + " = " + app.activeGroupId, null);
        db.close();
        renderList();
    }

    public void renderList() {
        SQLiteDatabase db = DatabaseHelper.quickDb(this, DatabaseHelper.MODE_READ);

        String queryModifier = "";
        if (gType == Db.Table3.TYPE_MONTH)
            queryModifier = " AND strftime('%Y-%m'," + Db.Table1.T_DATE + ") = '" + App.dateToDb("yyyy-MM", date.getTime()) + "'";

        Cursor c = db.rawQuery("SELECT "
                + Db.Table4.T_ID + ","
                + Db.Table4.T_AMOUNT + ","
                + Db.Table2.T_CATEGORY_NAME + ","
                + "SUM(" + Db.Table1.T_AMOUNT + "),"
                + Db.Table2.T_CATEGORY_COLOR +
                " FROM " + Db.Table4.TABLE_NAME +
                " INNER JOIN " + Db.Table2.TABLE_NAME + " ON " + Db.Table4.T_ID_CATEGORY + " = " + Db.Table2.T_ID +
                " LEFT OUTER JOIN " + Db.Table1.TABLE_NAME + " ON " + Db.Table4.T_ID_CATEGORY + " = " + Db.Table1.T_ID_CATEGORY +
                " AND " + Db.Table4.T_ID_GROUP + " = " + Db.Table1.T_ID_GROUP +
                queryModifier +
                " WHERE " + Db.Table4.T_ID_GROUP + " = " + app.activeGroupId +
                " GROUP BY " + Db.Table4.T_ID +
                " ORDER BY " + Db.Table4.T_ALERT + " DESC, " + Db.Table2.T_CATEGORY_NAME + " ASC", null);
        if (adapter == null) {
            adapter = new BudgetAdapter(this, c);
            lv.setAdapter(adapter);
        } else {
            adapter.swapCursor(c);
            adapter.notifyDataSetChanged();
        }

        float totalSpent = 0;
        float totalBudget = 0;

        c.moveToFirst();
        while (!c.isAfterLast()) {
            totalSpent += c.getFloat(3);
            totalBudget += c.getFloat(1);
            c.moveToNext();
        }

        float rate = totalSpent / totalBudget;

        ((TextView) header.findViewById(R.id.textViewPercentage)).setText(String.format("%.1f", (totalSpent / totalBudget) * 100) + "%");
        ProgressBar pb = (ProgressBar) header.findViewById(R.id.progressBar1);
        pb.setProgress(Math.round(rate * pb.getMax()));
        GradientDrawable gd = (GradientDrawable) ((ScaleDrawable) ((LayerDrawable) pb.getProgressDrawable()).findDrawableByLayerId(android.R.id.progress)).getDrawable();
        if (pb.getProgress() == pb.getMax())
            gd.setColor(res.getColor(R.color.red));
        else
            gd.setColor(res.getColor(R.color.green));
        ((TextView) header.findViewById(R.id.textViewValues)).setText(app.printMoney(totalSpent) + " / " + app.printMoney(totalBudget));

        db.close();
    }

    private class BudActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.budget_actionmode, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.item1:
                    deleteItem();
                    renderList();
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            pickMode = false;
            clearSelections();
            actMode = null;
        }
    }

    private void selectItem(View v, long id) {
        v.setBackgroundColor(getResources().getColor(R.color.gray));
        if (id >= 0)
            selectedId = id;
    }

    private void unselectItem(View v, long id) {
        v.setBackgroundResource(R.drawable.statelist_normal);
        if (id >= 0)
            selectedId = -1;
    }

    private void clearSelections() {
        int i, max, start;
        if (selectedId >= 0) {
            max = lv.getChildCount();
            if (lv.getHeaderViewsCount() == 0)
                start = 0;
            else if (lv.getFirstVisiblePosition() == 0)
                start = 1;
            else
                start = 0;
            for (i = start; i < max; i++)
                unselectItem(lv.getChildAt(i), -1);
            selectedId = -1;
        }
    }

    private void deleteItem() {
        SQLiteDatabase db = DatabaseHelper.quickDb(this, DatabaseHelper.MODE_WRITE);
        int toastString;
        int result = db.delete(Db.Table4.TABLE_NAME, Db.Table4._ID + " = " + selectedId, null);

        if (result == 1) {
            toastString = R.string.budget_c9;
            app.setFlag(4);
        } else
            toastString = R.string.gp_11;

        App.Toast(this, toastString);
        db.close();
    }

    private class BudgetAdapter extends CursorAdapter {
        private LayoutInflater mInflater;

        public BudgetAdapter(Context context, Cursor c) {
            super(context, c, false);

            mInflater = LayoutInflater.from(context);
        }

        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mInflater.inflate(R.layout.budget_listitem, parent, false);
        }

        public void bindView(View view, Context context, Cursor cursor) {
            ((ImageView) view.findViewById(R.id.imageView1)).getDrawable().setColorFilter(cursor.getInt(4), App.colorFilterMode);
            ((TextView) view.findViewById(R.id.textViewLabel)).setText(cursor.getString(2));

            float spent = cursor.getFloat(3);
            float budget = cursor.getFloat(1);
            float rate = spent / budget;

            ((TextView) view.findViewById(R.id.textViewPercentage)).setText(String.format("%.1f", rate * 100) + "%");
            ProgressBar pb = (ProgressBar) view.findViewById(R.id.progressBar1);
            pb.setProgress((int) Math.floor(rate * pb.getMax()));
            GradientDrawable gd = (GradientDrawable) ((ScaleDrawable) ((LayerDrawable) pb.getProgressDrawable()).findDrawableByLayerId(android.R.id.progress)).getDrawable();
            if (pb.getProgress() == pb.getMax())
                gd.setColor(res.getColor(R.color.red));
            else
                gd.setColor(res.getColor(R.color.green));
            ((TextView) view.findViewById(R.id.textViewValues)).setText(app.printMoney(spent) + " / " + app.printMoney(budget));

            long id = cursor.getLong(0);
            if (id == selectedId)
                selectItem(view, -1);
            else
                unselectItem(view, -1);
        }
    }

    public static class BudgetDialog extends DialogFragment implements DialogInterface.OnClickListener {
        public static final int ADD = 0, EDIT = 1;

        private long editId;
        private int mode;
        private Bundle params;
        private EditText edtValue;
        private Budget act;
        private App app;

        @NonNull
        public Dialog onCreateDialog(Bundle savedInstance) {
            int titleResource;

            params = getArguments();
            mode = params.getInt("MODE", ADD);

            act = (Budget) getActivity();
            app = (App) act.getApplication();

            LayoutInflater li = LayoutInflater.from(act);
            View v = li.inflate(R.layout.budget_dialog, null);

            edtValue = (EditText) v.findViewById(R.id.editText1);

            Spinner sp = (Spinner) v.findViewById(R.id.spinner1);
            SQLiteDatabase db = DatabaseHelper.quickDb(act, DatabaseHelper.MODE_READ);

            if (mode == ADD) {
                titleResource = R.string.budget_c3;

                Cursor c = db.rawQuery("SELECT "
                        + Db.Table2.T_ID + ","
                        + Db.Table2.T_CATEGORY_NAME +
                        " FROM " + Db.Table2.TABLE_NAME +
                        " WHERE NOT EXISTS(" +
                        "SELECT * FROM " + Db.Table4.TABLE_NAME +
                        " WHERE " + Db.Table4.T_ID_CATEGORY + " = " + Db.Table2.T_ID +
                        " AND " + Db.Table4.T_ID_GROUP + " = " + app.activeGroupId + ")" +
                        " ORDER BY " + Db.Table2.T_CATEGORY_NAME + " ASC", null);

                if (c.getCount() == 0) {
                    App.Toast(act, R.string.budget_c8);
                    c.close();
                    dismiss();
                } else {
                    SimpleCursorAdapter adapter = new SimpleCursorAdapter(act, android.R.layout.simple_spinner_item, c, new String[]{Db.Table2.CATEGORY_NAME}, new int[]{android.R.id.text1}, 0);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    sp.setAdapter(adapter);
                }
            } else {
                titleResource = R.string.budget_c4;
                editId = params.getLong("EDIT_ID");

                Cursor c2 = db.rawQuery("SELECT "
                        + Db.Table4.AMOUNT +
                        " FROM " + Db.Table4.TABLE_NAME +
                        " WHERE " + Db.Table4._ID + " = " + editId, null);
                c2.moveToFirst();

                edtValue.setText(c2.getString(0));
                c2.close();
                sp.setVisibility(View.GONE);
                v.findViewById(R.id.textView1).setVisibility(View.GONE);
            }
            db.close();

            Dialog dg = new AlertDialog.Builder(act)
                    .setView(v)
                    .setTitle(titleResource)
                    .setPositiveButton(R.string.gp_2, this)
                    .setNegativeButton(R.string.gp_3, this)
                    .create();

            Window dgW = dg.getWindow();
            dgW.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM|WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            dgW.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

            edtValue.requestFocus();

            return dg;
        }

        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE)
                saveItem();
            else
                dismiss();
        }

        private void saveItem() {
            float amount;
            Spinner sp = (Spinner) getDialog().findViewById(R.id.spinner1);

            try {
                amount = Float.parseFloat(edtValue.getText().toString());
                if (amount == 0)
                    throw new Exception();
            } catch (Exception e) {
                App.Toast(act, R.string.budget_c5);
                return;
            }

            SQLiteDatabase db = DatabaseHelper.quickDb(act, DatabaseHelper.MODE_WRITE);
            ContentValues cv = new ContentValues();
            cv.put(Db.Table4.AMOUNT, amount);
            if (mode == ADD) {
                cv.put(Db.Table4.ID_CATEGORY, sp.getSelectedItemId());
                cv.put(Db.Table4.ALERT, 0);
            }
            cv.put(Db.Table4.ID_GROUP, app.activeGroupId);
            long result;
            if (mode == EDIT)
                result = db.update(Db.Table4.TABLE_NAME, cv, Db.Table4._ID + " = " + editId, null);
            else
                result = db.insert(Db.Table4.TABLE_NAME, null, cv);
            int toastText;
            if ((mode == EDIT && result == 1) | (mode == ADD && result != -1)) {
                if (mode == EDIT)
                    toastText = R.string.budget_c6;
                else
                    toastText = R.string.budget_c7;
                act.renderList();
                this.dismiss();
            } else
                toastText = R.string.gp_11;
            App.Toast(act, toastText);
            db.close();
        }
    }
}