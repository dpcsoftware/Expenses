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
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;


public class EditCategories extends AppCompatActivity {
    private App app;
    private ListView lv;
    private CategoriesAdapter adapter;
    private boolean comingFromAddEx = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listview);
        lv = (ListView) findViewById(R.id.listView1);
        app = (App) getApplication();

        renderCategories();

        getSupportActionBar().setTitle(R.string.editcategories_c1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.categories, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item1:
                Bundle params = new Bundle();
                params.putInt("MODE", EditCategoryActivity.ADD);
                Intent it = new Intent(EditCategories.this, EditCategoryActivity.class);
                it.putExtras(params);
                startActivity(it);
                break;
        }
        return true;
    }

    public void onResume() {
        super.onResume();

        if (app.editCategoriesUpdateList) {
            renderCategories();
            app.editCategoriesUpdateList = false;
        }
    }

    private void renderCategories() {
        SQLiteDatabase db = DatabaseHelper.quickDb(this, 0);
        Cursor c = db.rawQuery("SELECT "
                + Db.Table2._ID + ","
                + Db.Table2.CATEGORY_NAME + ","
                + Db.Table2.CATEGORY_COLOR +
                " FROM " + Db.Table2.TABLE_NAME +
                " ORDER BY " + Db.Table2.CATEGORY_NAME + " ASC", null);
        if (adapter == null) {
            adapter = new CategoriesAdapter(this, c);
            lv.setAdapter(adapter);
        } else {
            adapter.swapCursor(c);
            adapter.notifyDataSetChanged();
        }
        db.close();
    }

    private class CategoriesAdapter extends CursorAdapter implements OnClickListener {
        private LayoutInflater mInflater;

        public CategoriesAdapter(Context context, Cursor c) {
            super(context, c, 0);
            mInflater = LayoutInflater.from(context);
        }

        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mInflater.inflate(R.layout.editcategories_listitem, parent, false);
        }

        public void bindView(View view, Context context, Cursor cursor) {
            ((ImageView) view.findViewById(R.id.imageViewCategory)).getDrawable().setColorFilter(cursor.getInt(2), App.colorFilterMode);
            ((TextView) view.findViewById(R.id.textViewCategory)).setText(cursor.getString(1));
            ImageButton btEdit = (ImageButton) view.findViewById(R.id.imageButtonEdit);
            btEdit.setOnClickListener(this);
            btEdit.setTag(cursor.getPosition());
            ImageButton btDelete = (ImageButton) view.findViewById(R.id.imageButtonDelete);
            btDelete.setOnClickListener(this);
            btDelete.setTag(cursor.getPosition());
        }

        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.imageButtonDelete:
                    if (getCursor().getCount() == 1) {
                        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(EditCategories.this);
                        dialogBuilder.setTitle(R.string.editcategories_c2);
                        dialogBuilder.setMessage(R.string.editcategories_c3);
                        dialogBuilder.create().show();
                    } else {
                        Bundle args = new Bundle();
                        args.putLong("DELETE_ID", getItemId((Integer) v.getTag()));
                        DeleteDialog delDg = new DeleteDialog();
                        delDg.setArguments(args);
                        delDg.show(getSupportFragmentManager(), null);
                    }
                    break;
                case R.id.imageButtonEdit:
                    Bundle args2 = new Bundle();
                    args2.putInt("MODE", EditCategoryActivity.EDIT);
                    args2.putLong("EDIT_ID", getItemId((Integer) v.getTag()));
                    Cursor c = getCursor();
                    c.moveToPosition((Integer) v.getTag());
                    args2.putString("CURRENT_NAME", c.getString(c.getColumnIndexOrThrow(Db.Table2.CATEGORY_NAME)));
                    args2.putInt("CURRENT_COLOR", c.getInt(c.getColumnIndexOrThrow(Db.Table2.CATEGORY_COLOR)));
                    Intent it = new Intent(EditCategories.this, EditCategoryActivity.class);
                    it.putExtras(args2);
                    startActivity(it);
                    return;
            }
        }

    }

    public static class DeleteDialog extends DialogFragment implements OnCheckedChangeListener, DialogInterface.OnClickListener {
        private long deleteId;
        private EditCategories act;
        private App app;
        private View layout;

        @NonNull
        public Dialog onCreateDialog(Bundle savedInstance) {
            act = (EditCategories) getActivity();
            app = (App) act.getApplication();

            Bundle args = getArguments();

            LayoutInflater li = LayoutInflater.from(act);
            layout = li.inflate(R.layout.editgroupseditcategories_deldialog, null);

            deleteId = args.getLong("DELETE_ID");

            ((RadioButton) layout.findViewById(R.id.radio0)).setText(R.string.editcategories_c4);
            ((RadioButton) layout.findViewById(R.id.radio1)).setText(R.string.editcategories_c5);

            Spinner sp = (Spinner) layout.findViewById(R.id.spinner1);
            SQLiteDatabase db = DatabaseHelper.quickDb(act, DatabaseHelper.MODE_READ);
            Cursor c = db.rawQuery("SELECT "
                    + Db.Table2._ID + ","
                    + Db.Table2.CATEGORY_NAME +
                    " FROM " + Db.Table2.TABLE_NAME +
                    " WHERE " + Db.Table2._ID + " <> " + deleteId +
                    " ORDER BY " + Db.Table2.CATEGORY_NAME + " ASC", null);
            SimpleCursorAdapter adapter = new SimpleCursorAdapter(act, android.R.layout.simple_spinner_item, c, new String[]{Db.Table2.CATEGORY_NAME}, new int[]{android.R.id.text1}, 0);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sp.setAdapter(adapter);
            sp.setEnabled(false);

            ((RadioButton) layout.findViewById(R.id.radio1)).setOnCheckedChangeListener(this);
            db.close();

            return new AlertDialog.Builder(act)
                    .setView(layout)
                    .setTitle(R.string.editcategories_c6)
                    .setPositiveButton(R.string.gp_2, this)
                    .setNegativeButton(R.string.gp_3, this)
                    .create();
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            layout.findViewById(R.id.spinner1).setEnabled(isChecked);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE)
                deleteGroup();
            else
                dismiss();
        }

        private void deleteGroup() {
            SQLiteDatabase db = DatabaseHelper.quickDb(act, DatabaseHelper.MODE_WRITE);
            RadioGroup rg = (RadioGroup) layout.findViewById(R.id.radioGroup1);
            int toastString;

            int result = db.delete(Db.Table2.TABLE_NAME, Db.Table2._ID + " = " + deleteId, null);
            //Delete budget items
            db.delete(Db.Table4.TABLE_NAME, Db.Table4.ID_CATEGORY + " = " + deleteId, null);
            if (result == 1) {
                if (rg.getCheckedRadioButtonId() == R.id.radio0) {
                    //Delete expenses
                    db.delete(Db.Table1.TABLE_NAME, Db.Table1.ID_CATEGORY + " = " + deleteId, null);
                } else {
                    long newId = ((Spinner) layout.findViewById(R.id.spinner1)).getSelectedItemId();
                    //Update expenses
                    ContentValues cv = new ContentValues();
                    cv.put(Db.Table1.ID_CATEGORY, newId);
                    db.update(Db.Table1.TABLE_NAME, cv, Db.Table1.ID_CATEGORY + " = " + deleteId, null);
                }
                toastString = R.string.editcategories_c12;
                app.setFlag(1);
                app.setFlag(2);
                app.setFlag(4);
                act.renderCategories();
                this.dismiss();
            } else
                toastString = R.string.editcategories_c13;

            App.Toast(act, toastString);
            db.close();
        }
    }
}
