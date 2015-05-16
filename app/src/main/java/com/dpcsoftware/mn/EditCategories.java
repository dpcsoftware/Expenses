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
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;


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
		
		Bundle args = getIntent().getExtras();
		if(args != null && args.getBoolean("ADD_CATEGORY",false)) {
			comingFromAddEx = true;
			Bundle params = new Bundle();
			params.putInt("MODE", AddEditDialog.ADD);
			AddEditDialog addDg = new AddEditDialog();
			addDg.setArguments(params);
			addDg.show(getSupportFragmentManager(), null);
		}
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
				params.putInt("MODE", AddEditDialog.ADD);
				AddEditDialog addDg = new AddEditDialog();
				addDg.setArguments(params);
				addDg.show(getSupportFragmentManager(), null);
    			break;
    	}
    	return true;
	}
	
	private void renderCategories() {
		SQLiteDatabase db = DatabaseHelper.quickDb(this, 0);
		Cursor c = db.rawQuery("SELECT "
				+ Db.Table2._ID + ","
				+ Db.Table2.CATEGORY_NAME + ","
				+ Db.Table2.CATEGORY_COLOR +
				" FROM " + Db.Table2.TABLE_NAME +
				" ORDER BY " + Db.Table2.CATEGORY_NAME + " ASC",null);
		if(adapter == null) {
			adapter = new CategoriesAdapter(this,c);
			lv.setAdapter(adapter);
		}
		else {
			adapter.swapCursor(c);
			adapter.notifyDataSetChanged();
		}
		db.close();
	}

	private class CategoriesAdapter extends CursorAdapter implements OnClickListener {
    	private LayoutInflater mInflater;
    	
	    public CategoriesAdapter(Context context, Cursor c) {
	        super(context, c, 0);
	        mInflater=LayoutInflater.from(context);
	    }
	    
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mInflater.inflate(R.layout.editcategories_listitem,parent,false); 
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
    		switch(v.getId()) {
    			case R.id.imageButtonDelete:
    				if(getCursor().getCount() == 1) {
    					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(EditCategories.this);
    					dialogBuilder.setTitle(R.string.editcategories_c2);
    					dialogBuilder.setMessage(R.string.editcategories_c3);
    					dialogBuilder.create().show();					
    				}
    				else {
    					Bundle args = new Bundle();
    					args.putLong("DELETE_ID", getItemId((Integer) v.getTag()));
    					DeleteDialog delDg = new DeleteDialog();
						delDg.setArguments(args);
    					delDg.show(getSupportFragmentManager(), null);
    				}
    				break;
    			case R.id.imageButtonEdit:
    				Bundle args2 = new Bundle();
    				args2.putLong("EDIT_ID", getItemId((Integer) v.getTag()));
    				Cursor c = getCursor();
    				c.moveToPosition((Integer) v.getTag());
    				args2.putString("CURRENT_NAME", c.getString(c.getColumnIndex(Db.Table2.CATEGORY_NAME)));
    				args2.putInt("CURRENT_COLOR", c.getInt(c.getColumnIndex(Db.Table2.CATEGORY_COLOR)));
					args2.putInt("MODE", AddEditDialog.EDIT);
    				AddEditDialog edtDg = new AddEditDialog();
					edtDg.setArguments(args2);
    				edtDg.show(getSupportFragmentManager(), null);
    				break;
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
					" ORDER BY " + Db.Table2.CATEGORY_NAME + " ASC",null);
			SimpleCursorAdapter adapter = new SimpleCursorAdapter(act, android.R.layout.simple_spinner_item, c, new String[] {Db.Table2.CATEGORY_NAME}, new int[] {android.R.id.text1}, 0);
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
			if(which == DialogInterface.BUTTON_POSITIVE)
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
			if(result == 1) {
				if(rg.getCheckedRadioButtonId() == R.id.radio0) {
                    //Delete expenses
					db.delete(Db.Table1.TABLE_NAME, Db.Table1.ID_CATEGORY + " = " + deleteId, null);
				}
				else {
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
			}
			else
				toastString = R.string.editcategories_c13;
			
			App.Toast(act, toastString);
			db.close();
		}
	}
	
	public static class AddEditDialog extends DialogFragment implements DialogInterface.OnClickListener {
		public static final int ADD = 0, EDIT = 1;
		private long editId;
		private int mode, selectedColor;
		private int[] colors = {R.color.c0,R.color.c1,
	    		R.color.c2,R.color.c3,
	    		R.color.c4,R.color.c5,
	    		R.color.c6,R.color.c7,
	    		R.color.c8,R.color.c9,
	    		R.color.c10,R.color.c11,
	    		R.color.c12,R.color.c13,
	    		R.color.c14,R.color.c15};
		private LinearLayout colorList;
		private View.OnClickListener selectColorListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ImageView target = (ImageView) layout.findViewById(R.id.imageView1);
				target.getDrawable().clearColorFilter();
				selectedColor = getResources().getColor(colors[colorList.indexOfChild(v)]);
				target.getDrawable().setColorFilter(selectedColor, App.colorFilterMode);
				target.invalidate();
			}
		};
		private EditCategories act;
		private App app;
		private View layout;

		@NonNull
		public Dialog onCreateDialog(Bundle savedInstance) {
			act = (EditCategories) getActivity();
			app = (App) act.getApplication();

			Bundle args = getArguments();
			mode = args.getInt("MODE", ADD);

			int titleResource;

			LayoutInflater li = LayoutInflater.from(act);
			layout = li.inflate(R.layout.editcategories_editdialog, null);
			
			colorList = (LinearLayout) layout.findViewById(R.id.colorList);
			int i;
			for(i = 0;i < 16;i++) {
				ImageButton item = new ImageButton(act);
				item.setImageResource(R.drawable.square_shape);
				item.setPadding(20,20,20,20);
				item.getDrawable().setColorFilter(getResources().getColor(colors[i]), App.colorFilterMode);
				item.setOnClickListener(selectColorListener);
				colorList.addView(item);
			}
			
			if(mode == ADD) {
				titleResource = R.string.editcategories_c7;
				colorList.getChildAt(0).performClick();
			}
			else {
				titleResource = R.string.editcategories_c8;
				editId = args.getLong("EDIT_ID");
				selectedColor = args.getInt("CURRENT_COLOR");
				ImageView target = (ImageView) layout.findViewById(R.id.imageView1);
				target.getDrawable().setColorFilter(selectedColor, App.colorFilterMode);
				((EditText) layout.findViewById(R.id.editText1)).setText(args.getString("CURRENT_NAME"));
			}

			app.showKeyboard(layout.findViewById(R.id.editText1));

			return new AlertDialog.Builder(act)
					.setView(layout)
					.setTitle(titleResource)
					.setPositiveButton(R.string.gp_2, this)
					.setNegativeButton(R.string.gp_3, this)
					.create();
		}

		@Override
		public void onClick(DialogInterface dialog, int which) {
			if(which == DialogInterface.BUTTON_POSITIVE) {
				if (((EditText) layout.findViewById(R.id.editText1)).getText().toString().equals("")) {
					App.Toast(act, R.string.editcategories_c14);
					return;
				}
				saveCategory();
			}
			else
				dismiss();

			if(act.comingFromAddEx)
				dismiss();
		}

		private void saveCategory() {
			SQLiteDatabase db = DatabaseHelper.quickDb(act, 1);
			ContentValues cv = new ContentValues();
			cv.put(Db.Table2.CATEGORY_NAME, ((EditText) layout.findViewById(R.id.editText1)).getText().toString());
			cv.put(Db.Table2.CATEGORY_COLOR, selectedColor);
			long result;
			if(mode == EDIT)
				result = db.update(Db.Table2.TABLE_NAME, cv, Db.Table2._ID + " = " + editId, null);
			else
				result = db.insert(Db.Table2.TABLE_NAME, null, cv);
			int toastText;
			if((mode == EDIT && result == 1) | (mode == ADD && result != -1)) {
				if(mode == EDIT)
					toastText = R.string.editcategories_c9;
				else
					toastText = R.string.editcategories_c10;
				app.setFlag(2);
				if(act.comingFromAddEx) {
					app.addExUpdateCategoryId = result;
					app.addExUpdateCategories = true;
				}
				act.renderCategories();
				this.dismiss();
			}
			else
				toastText = R.string.editcategories_c11;
			App.Toast(act, toastText);
			db.close();
		}
	}
	
}
