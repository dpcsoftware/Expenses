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



import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;


public class EditGroups extends SherlockActivity {
	private App app;
	private ListView lv;
	private GroupsAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (App) getApplication();
		lv = new ListView(EditGroups.this);
				
		renderGroups();
		
		getSupportActionBar().setTitle(R.string.editgroups_c1);
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.groups, menu);
		
		return true;
	}
	
    @Override
    public View findViewById(int id) {
    	return lv.findViewById(id);
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
	
	private void renderGroups() {
		SQLiteDatabase db = DatabaseHelper.quickDb(this, 1);
		Cursor c = db.rawQuery("SELECT "
				+ Db.Table3._ID + ","
				+ Db.Table3.COLUMN_NGRUPO +
				" FROM " + Db.Table3.TABLE_NAME +
				" ORDER BY " + Db.Table3.COLUMN_NGRUPO + " ASC", null);
		if(adapter == null) {
			adapter = new GroupsAdapter(this, c);
			lv.setAdapter(adapter);
			setContentView(lv);
		}
		else {
			adapter.swapCursor(c);
			adapter.notifyDataSetChanged();
		}
		db.close();
	}
	
	private class GroupsAdapter extends CursorAdapter implements OnClickListener {
    	private LayoutInflater mInflater;
    	
	    public GroupsAdapter(Context context, Cursor c) {
	        super(context, c, 0);
	        mInflater=LayoutInflater.from(context);
	    }
	    
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mInflater.inflate(R.layout.editgroups_listitem,parent,false); 
        }
    	
    	public void bindView(View view, Context context, Cursor cursor) {
    		((TextView) view.findViewById(R.id.textViewGroup)).setText(cursor.getString(1));
    		view.findViewById(R.id.imageButtonEdit).setOnClickListener(this);
    		view.findViewById(R.id.imageButtonDelete).setOnClickListener(this);
    	}
    	
		@Override
		public void onClick(View v) {
			switch(v.getId()) {
			case R.id.imageButtonDelete:
				if(getCursor().getCount() == 1) {
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(EditGroups.this);
					dialogBuilder.setTitle(R.string.editgroups_c2);
					dialogBuilder.setMessage(R.string.editgroups_c3);
					dialogBuilder.create().show();					
				}
				else {
					Bundle args = new Bundle();
					args.putLong("DELETE_ID", getItemId(lv.indexOfChild((View) v.getParent())));
					DeleteDialog delDg = new DeleteDialog(EditGroups.this,args);
					delDg.show();
				}
				break;
			case R.id.imageButtonEdit:
				Bundle args2 = new Bundle();
				args2.putLong("EDIT_ID", getItemId(lv.indexOfChild((View) v.getParent())));
				Cursor c = getCursor();
				c.moveToPosition(lv.indexOfChild((View) v.getParent()));
				args2.putString("CURRENT_NAME", c.getString(c.getColumnIndex(Db.Table3.COLUMN_NGRUPO)));
				AddEditDialog edtDg = new AddEditDialog(EditGroups.this,args2,AddEditDialog.EDIT);
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
			
			Spinner sp = (Spinner) findViewById(R.id.spinner1);
			SQLiteDatabase db = DatabaseHelper.quickDb(EditGroups.this, 0);
			Cursor c = db.rawQuery("SELECT "
					+ Db.Table3._ID + ","
					+ Db.Table3.COLUMN_NGRUPO +
					" FROM " + Db.Table3.TABLE_NAME +
					" WHERE " + Db.Table3._ID + " <> " + deleteId +
					" ORDER BY " + Db.Table3.COLUMN_NGRUPO + " ASC",null);
			SimpleCursorAdapter adapter = new SimpleCursorAdapter(EditGroups.this, android.R.layout.simple_spinner_item, c, new String[] {Db.Table3.COLUMN_NGRUPO}, new int[] {android.R.id.text1}, 0);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			sp.setAdapter(adapter);
			sp.setEnabled(false);
			
			setTitle(R.string.editgroups_c4);
			
			((RadioButton) findViewById(R.id.radio1)).setOnCheckedChangeListener(this);
			((Button) findViewById(R.id.button1)).setOnClickListener(this);
			((Button) findViewById(R.id.button2)).setOnClickListener(this);
			db.close();
		}
		
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			((Spinner) findViewById(R.id.spinner1)).setEnabled(isChecked);
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
			SQLiteDatabase db = DatabaseHelper.quickDb(EditGroups.this, 1);
			RadioGroup rg = (RadioGroup) findViewById(R.id.radioGroup1);
			int toastString;
			
			int result = db.delete(Db.Table3.TABLE_NAME, new String(Db.Table3._ID + " = " + deleteId), null);
			if(result == 1) {
				if(rg.getCheckedRadioButtonId() == R.id.radio0)
					db.delete(Db.Table1.TABLE_NAME,Db.Table1.COLUMN_IDGRUPO + " = " + deleteId, null);
				else {
					ContentValues cv = new ContentValues();
					cv.put(Db.Table1.COLUMN_IDGRUPO, ((Spinner) findViewById(R.id.spinner1)).getSelectedItemId());
					db.update(Db.Table1.TABLE_NAME,cv,new String(Db.Table1.COLUMN_IDGRUPO + " = " + deleteId),null);
				}
				toastString = R.string.editgroups_c5;
				app.setFlag(1);
				app.setFlag(3);
				renderGroups();
				this.dismiss();
			}
			else
				toastString = R.string.editgroups_c6;
			
			Toast ts = Toast.makeText(EditGroups.this,toastString,Toast.LENGTH_SHORT);
			ts.show();
			db.close();
		}
	}
	
	private class AddEditDialog extends Dialog implements View.OnClickListener {
		public static final int ADD = 0, EDIT = 1;
		private long editId;
		private int mode;
		
		public AddEditDialog(Context ctx, Bundle args, int md) {
			super(ctx);
			setContentView(R.layout.editgroups_editdialog);			
			mode = md;
			
			if(mode == ADD) {
				setTitle(R.string.editgroups_c7);
			}
			else {
				setTitle(R.string.editgroups_c8);
				editId = args.getLong("EDIT_ID");
				((EditText) findViewById(R.id.editText1)).setText(args.getString("CURRENT_NAME"));
			}
			
			((Button) findViewById(R.id.button1)).setOnClickListener(this);
			((Button) findViewById(R.id.button2)).setOnClickListener(this);
			
			((EditText) findViewById(R.id.editText1)).setOnFocusChangeListener(new OnFocusChangeListener() {				
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (hasFocus) {
			            AddEditDialog.this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
			        }					
				}
			});
		}
		
		@Override			
		public void onClick(View v) {
			switch(v.getId()) {
				case R.id.button2:
					if(((EditText) findViewById(R.id.editText1)).getText().toString().equals("")) {
						App.Toast(EditGroups.this, R.string.editgroups_c12);
						return;
					}
					saveGroupName();
					break;
				case R.id.button1:
					this.dismiss();
					break;
			}
		}
		
		private void saveGroupName() {
			SQLiteDatabase db = DatabaseHelper.quickDb(EditGroups.this, 1);
			ContentValues cv = new ContentValues();
			cv.put(Db.Table3.COLUMN_NGRUPO, ((EditText) findViewById(R.id.editText1)).getText().toString());
			long result;
			if(mode == EDIT)
				result = db.update(Db.Table3.TABLE_NAME, cv, Db.Table3._ID + " = " + editId, null);
			else
				result = db.insert(Db.Table3.TABLE_NAME, null, cv);
			int toastText;
			if((mode == EDIT && result == 1) | (mode == ADD && result != -1)) {
				if(mode == EDIT)
					toastText = R.string.editgroups_c9;
				else
					toastText = R.string.editgroups_c10;
				App app = (App) getApplication();
				app.setFlag(3);
				renderGroups();
				this.dismiss();
			}
			else
				toastText = R.string.editgroups_c11;
			Toast ts = Toast.makeText(EditGroups.this, toastText, Toast.LENGTH_SHORT);
			ts.show();
			db.close();
		}
	}
}
