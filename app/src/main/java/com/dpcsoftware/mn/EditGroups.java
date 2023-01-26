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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
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


public class EditGroups extends AppCompatActivity {
    private ListView lv;
    private GroupsAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listview);
        lv = (ListView) findViewById(R.id.listView1);

        renderGroups();

        getSupportActionBar().setTitle(R.string.editgroups_c1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.groups, menu);

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.item1) {
            Bundle args = new Bundle();
            args.putInt("MODE", AddEditDialog.ADD);
            AddEditDialog addDg = new AddEditDialog();
            addDg.setArguments(args);
            addDg.show(getSupportFragmentManager(), null);
        }
        return true;
    }

    private void renderGroups() {
        SQLiteDatabase db = DatabaseHelper.quickDb(this, DatabaseHelper.MODE_READ);
        Cursor c = db.rawQuery("SELECT "
                + Db.Table3._ID + ","
                + Db.Table3.GROUP_NAME +
                " FROM " + Db.Table3.TABLE_NAME +
                " ORDER BY " + Db.Table3.GROUP_NAME + " ASC", null);
        if (adapter == null) {
            adapter = new GroupsAdapter(this, c);
            lv.setAdapter(adapter);
            setContentView(lv);
        } else {
            adapter.swapCursor(c);
            adapter.notifyDataSetChanged();
        }
        db.close();
    }

    private class GroupsAdapter extends CursorAdapter implements OnClickListener {
        private LayoutInflater mInflater;

        public GroupsAdapter(Context context, Cursor c) {
            super(context, c, 0);
            mInflater = LayoutInflater.from(context);
        }

        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mInflater.inflate(R.layout.editgroups_listitem, parent, false);
        }

        public void bindView(View view, Context context, Cursor cursor) {
            ((TextView) view.findViewById(R.id.textViewGroup)).setText(cursor.getString(1));
            ImageButton btEdit = (ImageButton) view.findViewById(R.id.imageButtonEdit);
            btEdit.setOnClickListener(this);
            btEdit.setTag(cursor.getPosition());
            ImageButton btDelete = (ImageButton) view.findViewById(R.id.imageButtonDelete);
            btDelete.setOnClickListener(this);
            btDelete.setTag(cursor.getPosition());
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.imageButtonDelete) {
                if (getCursor().getCount() == 1) {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(EditGroups.this);
                    dialogBuilder.setTitle(R.string.editgroups_c2);
                    dialogBuilder.setMessage(R.string.editgroups_c3);
                    dialogBuilder.create().show();
                } else {
                    Bundle args = new Bundle();
                    args.putLong("DELETE_ID", getItemId((Integer) v.getTag()));
                    DeleteDialog delDg = new DeleteDialog();
                    delDg.setArguments(args);
                    delDg.show(getSupportFragmentManager(), null);
                }
            }
            else if (id == R.id.imageButtonEdit) {
                Bundle args2 = new Bundle();
                args2.putLong("EDIT_ID", getItemId((Integer) v.getTag()));
                Cursor c = getCursor();
                c.moveToPosition((Integer) v.getTag());
                args2.putString("CURRENT_NAME", c.getString(c.getColumnIndexOrThrow(Db.Table3.GROUP_NAME)));
                args2.putInt("MODE", AddEditDialog.EDIT);
                AddEditDialog edtDg = new AddEditDialog();
                edtDg.setArguments(args2);
                edtDg.show(getSupportFragmentManager(), null);
            }
        }

    }

    public static class DeleteDialog extends DialogFragment implements OnCheckedChangeListener, DialogInterface.OnClickListener {
        private long deleteId;
        private EditGroups act;
        private App app;
        private View layout;

        @NonNull
        public Dialog onCreateDialog(Bundle savedInstance) {
            act = (EditGroups) getActivity();
            app = (App) act.getApplication();

            Bundle args = getArguments();

            LayoutInflater li = act.getLayoutInflater();
            layout = li.inflate(R.layout.editgroupseditcategories_deldialog, null);

            deleteId = args.getLong("DELETE_ID");

            Spinner sp = (Spinner) layout.findViewById(R.id.spinner1);
            SQLiteDatabase db = DatabaseHelper.quickDb(act, DatabaseHelper.MODE_READ);
            Cursor c = db.rawQuery("SELECT "
                    + Db.Table3._ID + ","
                    + Db.Table3.GROUP_NAME +
                    " FROM " + Db.Table3.TABLE_NAME +
                    " WHERE " + Db.Table3._ID + " <> " + deleteId +
                    " ORDER BY " + Db.Table3.GROUP_NAME + " ASC", null);
            SimpleCursorAdapter adapter = new SimpleCursorAdapter(act, android.R.layout.simple_spinner_item, c, new String[]{Db.Table3.GROUP_NAME}, new int[]{android.R.id.text1}, 0);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sp.setAdapter(adapter);
            sp.setEnabled(false);

            ((RadioButton) layout.findViewById(R.id.radio1)).setOnCheckedChangeListener(this);
            db.close();

            return new AlertDialog.Builder(act)
                    .setView(layout)
                    .setTitle(R.string.editgroups_c4)
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

            int result = db.delete(Db.Table3.TABLE_NAME, Db.Table3._ID + " = " + deleteId, null);
            if (result == 1) {
                if (rg.getCheckedRadioButtonId() == R.id.radio0) {
                    //Delete expenses
                    db.delete(Db.Table1.TABLE_NAME, Db.Table1.ID_GROUP + " = " + deleteId, null);
                    //Delete budget items
                    db.delete(Db.Table4.TABLE_NAME, Db.Table4.ID_GROUP + " = " + deleteId, null);
                } else {
                    long newId = ((Spinner) layout.findViewById(R.id.spinner1)).getSelectedItemId();
                    //Update expenses
                    ContentValues cv = new ContentValues();
                    cv.put(Db.Table1.ID_GROUP, newId);
                    db.update(Db.Table1.TABLE_NAME, cv, Db.Table1.ID_GROUP + " = " + deleteId, null);

                    //Update budget items
                    cv = new ContentValues();
                    cv.put(Db.Table4.ID_GROUP, newId);
                    db.update(Db.Table4.TABLE_NAME, cv, Db.Table4.ID_GROUP + " = " + deleteId, null);
                }
                toastString = R.string.editgroups_c5;
                app.setFlag(1);
                app.setFlag(3);
                app.setFlag(4);
                act.renderGroups();
                this.dismiss();
            } else
                toastString = R.string.editgroups_c6;

            App.Toast(act, toastString);
            db.close();
        }
    }

    public static class AddEditDialog extends DialogFragment implements DialogInterface.OnClickListener {
        public static final int ADD = 0, EDIT = 1;
        private long editId;
        private int mode;
        private EditGroups act;
        private App app;
        private View layout;

        @NonNull
        public Dialog onCreateDialog(Bundle savedInstance) {
            act = (EditGroups) getActivity();
            app = (App) act.getApplication();

            Bundle args = getArguments();
            mode = args.getInt("MODE", ADD);

            int titleResource;

            LayoutInflater li = act.getLayoutInflater();
            layout = li.inflate(R.layout.editgroups_editdialog, null);

            if (mode == ADD) {
                titleResource = R.string.editgroups_c7;
            } else {
                titleResource = R.string.editgroups_c8;
                editId = args.getLong("EDIT_ID");
                ((EditText) layout.findViewById(R.id.editText1)).setText(args.getString("CURRENT_NAME"));
            }

            Dialog dg = new AlertDialog.Builder(act)
                    .setView(layout)
                    .setTitle(titleResource)
                    .setPositiveButton(R.string.gp_2, this)
                    .setNegativeButton(R.string.gp_3, this)
                    .create();

            Window dgW = dg.getWindow();
            dgW.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM|WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            dgW.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

            layout.findViewById(R.id.editText1).requestFocus();

            return dg;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                if (((EditText) layout.findViewById(R.id.editText1)).getText().toString().equals("")) {
                    App.Toast(act, R.string.editgroups_c12);
                    return;
                }
                saveGroupName();
            } else
                dismiss();
        }

        private void saveGroupName() {
            SQLiteDatabase db = DatabaseHelper.quickDb(act, DatabaseHelper.MODE_WRITE);
            ContentValues cv = new ContentValues();
            cv.put(Db.Table3.GROUP_NAME, ((EditText) layout.findViewById(R.id.editText1)).getText().toString());
            long result;
            if (mode == EDIT)
                result = db.update(Db.Table3.TABLE_NAME, cv, Db.Table3._ID + " = " + editId, null);
            else
                result = db.insert(Db.Table3.TABLE_NAME, null, cv);
            int toastText;
            if ((mode == EDIT && result == 1) | (mode == ADD && result != -1)) {
                if (mode == EDIT)
                    toastText = R.string.editgroups_c9;
                else
                    toastText = R.string.editgroups_c10;
                app.setFlag(3);
                act.renderGroups();
                dismiss();
            } else
                toastText = R.string.editgroups_c11;

            App.Toast(act, toastText);
            db.close();
        }
    }
}
