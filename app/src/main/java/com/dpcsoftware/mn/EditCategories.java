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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarActivity;
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
import android.widget.Toast;


public class EditCategories extends ActionBarActivity {
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
			AddEditDialog addDg = new AddEditDialog(this,null,AddEditDialog.ADD);
			addDg.show();
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
    			AddEditDialog addDg = new AddEditDialog(this,null,AddEditDialog.ADD);
    			addDg.show();
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
    					DeleteDialog delDg = new DeleteDialog(EditCategories.this,args);
    					delDg.show();
    				}
    				break;
    			case R.id.imageButtonEdit:
    				Bundle args2 = new Bundle();
    				args2.putLong("EDIT_ID", getItemId((Integer) v.getTag()));
    				Cursor c = getCursor();
    				c.moveToPosition((Integer) v.getTag());
    				args2.putString("CURRENT_NAME", c.getString(c.getColumnIndex(Db.Table2.CATEGORY_NAME)));
    				args2.putInt("CURRENT_COLOR", c.getInt(c.getColumnIndex(Db.Table2.CATEGORY_COLOR)));
    				AddEditDialog edtDg = new AddEditDialog(EditCategories.this,args2,AddEditDialog.EDIT);
    				edtDg.show();
    				break;
    		}
    	}
    	
    }
	private class DeleteDialog extends Dialog implements OnCheckedChangeListener, View.OnClickListener {
		private long deleteId;
		
		public DeleteDialog(Context ctx, Bundle args) {
			super(ctx);
			
			setContentView(R.layout.editgroupseditcategories_deldialog);
			deleteId = args.getLong("DELETE_ID");
			
			((RadioButton) findViewById(R.id.radio0)).setText(R.string.editcategories_c4);
			((RadioButton) findViewById(R.id.radio1)).setText(R.string.editcategories_c5);
			
			Spinner sp = (Spinner) findViewById(R.id.spinner1);
			SQLiteDatabase db = DatabaseHelper.quickDb(EditCategories.this, DatabaseHelper.MODE_READ);
			Cursor c = db.rawQuery("SELECT "
					+ Db.Table2._ID + ","
					+ Db.Table2.CATEGORY_NAME +
					" FROM " + Db.Table2.TABLE_NAME +
					" WHERE " + Db.Table2._ID + " <> " + deleteId +
					" ORDER BY " + Db.Table2.CATEGORY_NAME + " ASC",null);
			SimpleCursorAdapter adapter = new SimpleCursorAdapter(EditCategories.this, android.R.layout.simple_spinner_item, c, new String[] {Db.Table2.CATEGORY_NAME}, new int[] {android.R.id.text1}, 0);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			sp.setAdapter(adapter);
			sp.setEnabled(false);
			
			setTitle(R.string.editcategories_c6);
			
			((RadioButton) findViewById(R.id.radio1)).setOnCheckedChangeListener(this);
			findViewById(R.id.button1).setOnClickListener(this);
			findViewById(R.id.button2).setOnClickListener(this);
			db.close();
		}
		
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			findViewById(R.id.spinner1).setEnabled(isChecked);
		}
		
		@Override
		public void onClick(View v) {
			switch(v.getId()) {
				case R.id.button1:
					this.dismiss();
					break;
				case R.id.button2:
					deleteGroup();
					break;
			}
			
		}
		
		private void deleteGroup() {
			SQLiteDatabase db = DatabaseHelper.quickDb(EditCategories.this, 1);
			RadioGroup rg = (RadioGroup) findViewById(R.id.radioGroup1);
			int toastString;
			
			int result = db.delete(Db.Table2.TABLE_NAME, Db.Table2._ID + " = " + deleteId, null);
			if(result == 1) {
				if(rg.getCheckedRadioButtonId() == R.id.radio0)
					db.delete(Db.Table1.TABLE_NAME,Db.Table1.ID_CATEGORY + " = " + deleteId, null);
				else {
					ContentValues cv = new ContentValues();
					cv.put(Db.Table1.ID_CATEGORY, ((Spinner) findViewById(R.id.spinner1)).getSelectedItemId());
					db.update(Db.Table1.TABLE_NAME,cv, Db.Table1.ID_CATEGORY + " = " + deleteId,null);
				}
				toastString = R.string.editcategories_c12;
				app.setFlag(1);
				app.setFlag(2);
				renderCategories();
				this.dismiss();
			}
			else
				toastString = R.string.editcategories_c13;
			
			Toast ts = Toast.makeText(EditCategories.this,toastString,Toast.LENGTH_SHORT);
			ts.show();
			db.close();
		}
	}
	
	private class AddEditDialog extends Dialog implements View.OnClickListener {
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
				ImageView target = (ImageView) findViewById(R.id.imageView1);
				target.getDrawable().clearColorFilter();
				selectedColor = getResources().getColor(colors[colorList.indexOfChild(v)]);
				target.getDrawable().setColorFilter(selectedColor, App.colorFilterMode);
				target.invalidate();
			}
		};
		
		public AddEditDialog(Context ctx, Bundle args, int md) {
			super(ctx);
			setContentView(R.layout.editcategories_editdialog);			
			mode = md;
			
			colorList = (LinearLayout) findViewById(R.id.colorList);
			int i;
			for(i = 0;i < 16;i++) {
				ImageButton item = new ImageButton(EditCategories.this);
				item.setImageResource(R.drawable.square_shape);
				item.setPadding(20,20,20,20);
				item.getDrawable().setColorFilter(getResources().getColor(colors[i]), App.colorFilterMode);
				item.setOnClickListener(selectColorListener);
				colorList.addView(item);
			}
			
			if(mode == ADD) {
				setTitle(R.string.editcategories_c7);
				colorList.getChildAt(0).performClick();
			}
			else {
				setTitle(R.string.editcategories_c8);
				editId = args.getLong("EDIT_ID");
				selectedColor = args.getInt("CURRENT_COLOR");
				ImageView target = (ImageView) findViewById(R.id.imageView1);
				target.getDrawable().setColorFilter(selectedColor, App.colorFilterMode);
				((EditText) findViewById(R.id.editText1)).setText(args.getString("CURRENT_NAME"));
			}
			
			findViewById(R.id.button1).setOnClickListener(this);
			findViewById(R.id.button2).setOnClickListener(this);
			
			app.showKeyboard(findViewById(R.id.editText1));
		}
				
		@Override			
		public void onClick(View v) {
			switch(v.getId()) {
				case R.id.button2:
					if(((EditText) findViewById(R.id.editText1)).getText().toString().equals("")) {
						App.Toast(EditCategories.this, R.string.editcategories_c14);
						return;
					}
					saveGroupName();
					break;
				case R.id.button1:
					this.dismiss();
					break;
			}
			
			if(comingFromAddEx)
				EditCategories.this.finish();
		}
		
		private void saveGroupName() {
			SQLiteDatabase db = DatabaseHelper.quickDb(EditCategories.this, 1);
			ContentValues cv = new ContentValues();
			cv.put(Db.Table2.CATEGORY_NAME, ((EditText) findViewById(R.id.editText1)).getText().toString());
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
				if(comingFromAddEx) {
					app.addExUpdateCategoryId = result;
					app.addExUpdateCategories = true;
				}
				renderCategories();
				this.dismiss();
			}
			else
				toastText = R.string.editcategories_c11;
			App.Toast(EditCategories.this, toastText);
			db.close();
		}
	}
	
}
