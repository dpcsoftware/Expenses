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

import java.util.Calendar;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

public class Goals extends ActionBarActivity implements OnClickListener {
	private App app;
	private Runnable menuCallback = new Runnable() {
		public void run() {
			updateLists();
		}
	};
	private Resources res;
	private ListPageAdapter pAdapter;
	private VPager pager;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (App) getApplication();
		res = getResources();
		
		setContentView(R.layout.goals);
		pager = (VPager) findViewById(R.id.pager);
		
		pAdapter = new ListPageAdapter(getSupportFragmentManager());
		pager.setAdapter(pAdapter);
		pager.setImgTab((ImageView) findViewById(R.id.tab_indicator));
		
		((TextView) findViewById(R.id.textView1)).setOnClickListener(this);
		((TextView) findViewById(R.id.textView2)).setOnClickListener(this);
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {    	
    	getMenuInflater().inflate(R.menu.goals, menu);
    	
    	app.new SpinnerMenu(this,menuCallback);
    	
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
			case R.id.item1:
				AddEditDialog addDg = new AddEditDialog(this,null,AddEditDialog.ADD);
    			addDg.show();
				break;
    	}

    	return true;
    }
    
    public void onClick(View v) {
    	if(v.getId() == R.id.textView1)
    		pager.setCurrentItem(0);
    	else
    		pager.setCurrentItem(1);
    }
    
    private void updateLists() {
    	pAdapter.itemAt(0).renderList();
		pAdapter.itemAt(1).renderList();
		
		if(pAdapter.itemAt(0).getListAdapter().getCount() == 0 && pAdapter.itemAt(1).getListAdapter().getCount() != 0)
			pager.setCurrentItem(1);
    }
    
    private class ListPage extends ListFragment implements OnClickListener {
    	private int n;
    	private Calendar date;
    	
    	@Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            
            Bundle args = getArguments();
            n = args.getInt("POS",0);
            
            if(n == 0)
            	date = Calendar.getInstance();
        }
    	
    	@Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    		View v;
    		if(n == 0) {
    			v = inflater.inflate(R.layout.goals_page1, container, false);
    			((ImageButton) v.findViewById(R.id.imageButton1)).setOnClickListener(this);
    			((ImageButton) v.findViewById(R.id.imageButton2)).setOnClickListener(this);
    			((TextView) v.findViewById(R.id.dateView)).setText(app.dateToUser("MMMM / y", date.getTime()));
    		}
    		else
    			v = inflater.inflate(R.layout.goals_page2, container, false);
    		
            ((TextView) v.findViewById(R.id.textView1)).setText(R.string.goals_c1);
            View ev = v.findViewById(R.id.empty);
            ((ListView) v.findViewById(android.R.id.list)).setEmptyView(ev);
            return v;
        }
    	
    	public void onClick(View v) {
    		if(v.getId() == R.id.imageButton1)
    			date.add(Calendar.MONTH, -1);
    		else
    			date.add(Calendar.MONTH, 1);
    		
    		((TextView) findViewById(R.id.dateView)).setText(app.dateToUser("MMMM / y", date.getTime()));
    		renderList();
    	}
    	
    	@Override
    	public void onListItemClick(ListView l, View v, int position, long id) {
    		Bundle args = new Bundle();
			args.putLong("EDIT_ID", id);
			AddEditDialog edtDg = new AddEditDialog(Goals.this,args,AddEditDialog.EDIT);
			edtDg.show();
    	}
    	
        public void renderList() {
        	String queryOption;
        	GoalsAdapter adapter = (GoalsAdapter) getListAdapter();
        	if(n == 0)
        		queryOption = Db.Table4.T_TYPE + " = " + Db.Table4.TYPE_TOTAL_BY_MONTH + " OR " + Db.Table4.T_TYPE + " = " + Db.Table4.TYPE_CAT_BY_MONTH;
        	else
        		queryOption = Db.Table4.T_TYPE + " = " + Db.Table4.TYPE_TOTAL + " OR " + Db.Table4.T_TYPE + " = " + Db.Table4.TYPE_CAT;
        	
        	SQLiteDatabase db = DatabaseHelper.quickDb(Goals.this, DatabaseHelper.MODE_READ);
    		Cursor c = db.rawQuery("SELECT "
    				+ Db.Table4.T_ID + ","
    				+ Db.Table4.T_TYPE + ","
    				+ Db.Table4.T_AMOUNT + ","
    				+ Db.Table4.T_ID_CATEGORY +
    				" FROM " + Db.Table4.TABLE_NAME +
    				" WHERE " + Db.Table4.T_ID_GROUP + " = " + app.activeGroupId +
    				" AND (" + queryOption + ")" +
    				" ORDER BY " + Db.Table4.T_TYPE + " ASC", null);
    		if(adapter == null) {
    			adapter = new GoalsAdapter(Goals.this, c);
    			setListAdapter(adapter);
    		}
    		else {
    			adapter.swapCursor(c);
    			adapter.notifyDataSetChanged();
    		}
    		db.close();
        }
        
        private class GoalsAdapter extends CursorAdapter {
        	private LayoutInflater mInflater;
        	
    	    public GoalsAdapter(Context context, Cursor c) {
    	        super(context, c, false);
    	        
    	        mInflater = LayoutInflater.from(context);
    	    }
    	    
    	    public View newView(Context context, Cursor cursor, ViewGroup parent) {
                return mInflater.inflate(R.layout.goals_listitem,parent,false); 
            }
        	
        	public void bindView(View view, Context context, Cursor cursor) {
        		SQLiteDatabase db = DatabaseHelper.quickDb(Goals.this, DatabaseHelper.MODE_READ);
        		Cursor c, c2;  
        		int type = cursor.getInt(1);
        		if(type == Db.Table4.TYPE_TOTAL) {
        			c = db.rawQuery("SELECT "
        					+ "SUM(" + Db.Table1.AMOUNT + ")" +
        					" FROM " + Db.Table1.TABLE_NAME +
        					" WHERE " + Db.Table1.ID_GROUP + " = " + app.activeGroupId, null);
        			((TextView) view.findViewById(R.id.textView2)).setText(R.string.gp_10);
        		}
        		else if(type == Db.Table4.TYPE_TOTAL_BY_MONTH){
        			c = db.rawQuery("SELECT "
        					+ "SUM(" + Db.Table1.AMOUNT + ")" +
        					" FROM " + Db.Table1.TABLE_NAME +
        					" WHERE " + Db.Table1.ID_GROUP + " = " + app.activeGroupId +
        					" AND strftime('%Y-%m'," + Db.Table1.DATE + ") = '" + app.dateToDb("yyyy-MM", date.getTime()) + "'", null);
        			((TextView) view.findViewById(R.id.textView2)).setText(R.string.goals_c2);    			
        		}
        		else if(type == Db.Table4.TYPE_CAT) {
        			c = db.rawQuery("SELECT "
        					+ "SUM(" + Db.Table1.AMOUNT + ")" +
        					" FROM " + Db.Table1.TABLE_NAME +
        					" WHERE " + Db.Table1.ID_GROUP + " = " + app.activeGroupId +
        					" AND " + Db.Table1.ID_CATEGORY + " = " + cursor.getInt(3), null);
        			c2 = db.rawQuery("SELECT "
        					+ Db.Table2.CATEGORY_NAME +
        					" FROM " + Db.Table2.TABLE_NAME +
        					" WHERE " + Db.Table2.T_ID + " = " + cursor.getInt(3), null);
        			c2.moveToFirst();
        			((TextView) view.findViewById(R.id.textView2)).setText(c2.getString(0));
        		}
        		else {
        			c = db.rawQuery("SELECT "
        					+ "SUM(" + Db.Table1.AMOUNT + ")" +
        					" FROM " + Db.Table1.TABLE_NAME +
        					" WHERE " + Db.Table1.ID_GROUP + " = " + app.activeGroupId +
        					" AND " + Db.Table1.ID_CATEGORY + " = " + cursor.getInt(3) +
        					" AND strftime('%Y-%m'," + Db.Table1.DATE + ") = '" + app.dateToDb("yyyy-MM", date.getTime()) + "'", null);
        			c2 = db.rawQuery("SELECT "
        					+ Db.Table2.CATEGORY_NAME +
        					" FROM " + Db.Table2.TABLE_NAME +
        					" WHERE " + Db.Table2.T_ID + " = " + cursor.getInt(3), null);
        			c2.moveToFirst();
        			((TextView) view.findViewById(R.id.textView2)).setText(c2.getString(0));
        		}
        		
        		c.moveToFirst();
        		float rate = c.getFloat(0)/cursor.getFloat(2);
        		((TextView) view.findViewById(R.id.textView1)).setText(String.format("%.1f", rate*100) + "%");
        		ProgressBar pb = (ProgressBar) view.findViewById(R.id.progressBar1);
        		pb.setProgress(Math.round(rate*pb.getMax()));
        		GradientDrawable gd = (GradientDrawable) ((ScaleDrawable) ((LayerDrawable) pb.getProgressDrawable()).findDrawableByLayerId(android.R.id.progress)).getDrawable();
        		if(pb.getProgress() == pb.getMax())
        			gd.setColor(res.getColor(R.color.red));
        		else
        			gd.setColor(res.getColor(R.color.green));
        		((TextView) view.findViewById(R.id.textView3)).setText(app.printMoney(c.getFloat(0)) + " / " + app.printMoney(cursor.getFloat(2)));
        	
        		db.close();
        	}
        }
    }
    
	private class ListPageAdapter extends FragmentPagerAdapter {
		public ListPage[] pages;
		
	    public ListPageAdapter(FragmentManager fm) {
	        super(fm);
	        pages = new ListPage[2];
	    }
	
	    @Override
	    public int getCount() {
	        return 2;
	    }
	    
	    @Override
	    public ListPage getItem(int position) {
	    	ListPage lp = new ListPage();
	    	Bundle args = new Bundle();
	    	args.putInt("POS", position);
	    	lp.setArguments(args);
	    	pages[position] = lp;
	    	return lp;
	    }
	    
	    public ListPage itemAt(int position) {
	    	return pages[position];
	    }
	}
	
	private class AddEditDialog extends Dialog implements View.OnClickListener, OnCheckedChangeListener {
		public static final int ADD = 0, EDIT = 1;
		private long editId;
		private int mode;
		
		public AddEditDialog(Context ctx, Bundle args, int md) {
			super(ctx);
			setContentView(R.layout.goals_editdialog);			
			mode = md;
			
			RadioGroup rg1 = (RadioGroup) findViewById(R.id.radioGroup1);
			RadioGroup rg2 = (RadioGroup) findViewById(R.id.radioGroup2);
			EditText edt = (EditText) findViewById(R.id.editText1);
			
			((Button) findViewById(R.id.button1)).setOnClickListener(this);
			((Button) findViewById(R.id.button2)).setOnClickListener(this);
			
			edt.setOnFocusChangeListener(new OnFocusChangeListener() {				
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (hasFocus) {
			            AddEditDialog.this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
			        }					
				}
			});
			
			((RadioButton) findViewById(R.id.radio3)).setOnCheckedChangeListener(this);
			
			Spinner sp = (Spinner) findViewById(R.id.spinner1);
			SQLiteDatabase db = DatabaseHelper.quickDb(Goals.this, DatabaseHelper.MODE_READ);
			Cursor c = db.rawQuery("SELECT "
					+ Db.Table2._ID + ","
					+ Db.Table2.CATEGORY_NAME +
					" FROM " + Db.Table2.TABLE_NAME +
					" ORDER BY " + Db.Table2.CATEGORY_NAME + " ASC",null);
			SimpleCursorAdapter adapter = new SimpleCursorAdapter(Goals.this, android.R.layout.simple_spinner_item, c, new String[] {Db.Table2.CATEGORY_NAME}, new int[] {android.R.id.text1}, 0);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			sp.setAdapter(adapter);
			
			if(mode == ADD) {
				setTitle(R.string.goals_c3);
				sp.setEnabled(false);
				
				if(pager.getCurrentItem() == 1)
					rg1.check(R.id.radio1);
			}
			else {
				setTitle(R.string.goals_c4);
				editId = args.getLong("EDIT_ID");
				
				Cursor c2 = db.rawQuery("SELECT "
						+ Db.Table4.AMOUNT + ","
						+ Db.Table4.ID_CATEGORY + ","
						+ Db.Table4.TYPE +
						" FROM " + Db.Table4.TABLE_NAME + 
						" WHERE " + Db.Table4._ID + " = " + editId, null);
				c2.moveToFirst();
				
				((EditText) findViewById(R.id.editText1)).setText(c2.getString(0));
				
				int type = c2.getInt(2);
				if(type == Db.Table4.TYPE_CAT_BY_MONTH || type == Db.Table4.TYPE_TOTAL_BY_MONTH)
					rg1.check(R.id.radio0);
				else
					rg1.check(R.id.radio1);
				
				if(type == Db.Table4.TYPE_CAT_BY_MONTH || type == Db.Table4.TYPE_CAT) {
					rg2.check(R.id.radio3);
					sp.setEnabled(true);
					int catId = c2.getInt(1);
					c.moveToFirst();
		        	int i;
		        	for(i = 0;i < c.getCount();i++) {
		        		if(c.getLong(c.getColumnIndex(Db.Table2._ID)) == catId)
		        			break;
		        		c.moveToNext();
		        	}
		        	sp.setSelection(i);
				}
				else {
					rg2.check(R.id.radio2);
					sp.setEnabled(false);
				}
			}
			
			db.close();
		}
		
		public void onClick(View v) {
			if(v.getId() == R.id.button1)
				this.dismiss();
			else
				saveGoal();				
		}
		
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				((Spinner) findViewById(R.id.spinner1)).setEnabled(isChecked);
		}
		
		private void saveGoal() {
			float amount;
			EditText edt = (EditText) findViewById(R.id.editText1);
			RadioGroup rg1 = (RadioGroup) findViewById(R.id.radioGroup1);
			RadioGroup rg2 = (RadioGroup) findViewById(R.id.radioGroup2);
			int type;
			Spinner sp = (Spinner) findViewById(R.id.spinner1);
			
			try {
	    		amount = Float.parseFloat(edt.getText().toString());
	    		if(amount == 0)
	    			throw new Exception();
	    	}
	    	catch (Exception e) {
	    		App.Toast(Goals.this, R.string.goals_c5);
	    		return;
	    	}
						
			if(rg1.getCheckedRadioButtonId() == R.id.radio0) {
				if(rg2.getCheckedRadioButtonId() == R.id.radio2)
					type = Db.Table4.TYPE_TOTAL_BY_MONTH;
				else
					type = Db.Table4.TYPE_CAT_BY_MONTH;	
			}
			else {
				if(rg2.getCheckedRadioButtonId() == R.id.radio2)
					type = Db.Table4.TYPE_TOTAL;
				else
					type = Db.Table4.TYPE_CAT;	
			}
			
			SQLiteDatabase db = DatabaseHelper.quickDb(Goals.this, DatabaseHelper.MODE_WRITE);
			ContentValues cv = new ContentValues();
			cv.put(Db.Table4.AMOUNT, amount);
			cv.put(Db.Table4.TYPE, type);
			if(type == Db.Table4.TYPE_CAT || type == Db.Table4.TYPE_CAT_BY_MONTH)
				cv.put(Db.Table4.ID_CATEGORY, sp.getSelectedItemId());
			cv.put(Db.Table4.ID_GROUP, app.activeGroupId);
			long result;
			if(mode == EDIT)
				result = db.update(Db.Table4.TABLE_NAME, cv, Db.Table4._ID + " = " + editId, null);
			else
				result = db.insert(Db.Table4.TABLE_NAME, null, cv);
			int toastText;
			if((mode == EDIT && result == 1) | (mode == ADD && result != -1)) {
				if(mode == EDIT)
					toastText = R.string.goals_c6;
				else
					toastText = R.string.goals_c7;
				updateLists();
				this.dismiss();
			}
			else
				toastText = R.string.gp_11;
			App.Toast(Goals.this, toastText);
			db.close();
		}
	}
}