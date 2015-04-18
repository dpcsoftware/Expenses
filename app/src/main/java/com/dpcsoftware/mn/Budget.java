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
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Calendar;

public class Budget extends ActionBarActivity implements OnClickListener {
	private App app;
	private Runnable menuCallback = new Runnable() {
		public void run() {
			updateLists();
		}
	};
	private ListPageAdapter pAdapter;
	private VPager pager;
    private boolean firstLoad;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (App) getApplication();
        firstLoad = true;
		
		setContentView(R.layout.budget);
		pager = (VPager) findViewById(R.id.pager);
		
		pAdapter = new ListPageAdapter(getSupportFragmentManager());
		pager.setAdapter(pAdapter);
		pager.setImgTab((ImageView) findViewById(R.id.tab_indicator));
		
		findViewById(R.id.textView1).setOnClickListener(this);
		findViewById(R.id.textView2).setOnClickListener(this);
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {    	
    	getMenuInflater().inflate(R.menu.budget, menu);
    	
    	app.new SpinnerMenu(this,menuCallback);
    	
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
			case R.id.item1:
                pAdapter.itemAt(pager.getCurrentItem()).showAddDialog();
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

        if(firstLoad) {
            int ind = 0;
            Bundle args = getIntent().getExtras();
            if(args != null) {
                ind = args.getInt("SET_TAB_INDEX", 0);
                App.Log("index : " + ind);
            }
            pager.setCurrentItem(ind);
            firstLoad = false;
        }
        else {
            if (pAdapter.itemAt(0).getListAdapter().getCount() == 0 && pAdapter.itemAt(1).getListAdapter().getCount() != 0)
                pager.setCurrentItem(1);
            else if (pAdapter.itemAt(1).getListAdapter().getCount() == 0 && pAdapter.itemAt(0).getListAdapter().getCount() != 0)
                pager.setCurrentItem(0);
        }
    }

    public static class ListPage extends ListFragment implements OnClickListener {
    	private int type;
    	private Calendar date;
        private Resources res;
        private App app;
        private View layout;
        private View header;
        private ActionBarActivity act;
    	
    	@Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            res = getResources();
            act = (ActionBarActivity) getActivity();
            app = (App) act.getApplication();
            
            Bundle args = getArguments();
            type = args.getInt("TYPE",Db.Table4.TYPE_BY_MONTH);
            
            if(type == Db.Table4.TYPE_BY_MONTH)
            	date = Calendar.getInstance();
        }
    	
    	@Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    		if(type == Db.Table4.TYPE_BY_MONTH) {
    			layout = inflater.inflate(R.layout.budget_page1, container, false);
    			layout.findViewById(R.id.imageButton1).setOnClickListener(this);
    			layout.findViewById(R.id.imageButton2).setOnClickListener(this);
    			((TextView) layout.findViewById(R.id.dateView)).setText(App.dateToUser("MMMM / y", date.getTime()));
    		}
    		else
    			layout = inflater.inflate(R.layout.budget_page2, container, false);

            ((TextView) layout.findViewById(R.id.textView1)).setText(R.string.budget_c1);
            View ev = layout.findViewById(R.id.empty);
            ((ListView) layout.findViewById(android.R.id.list)).setEmptyView(ev);

            ListView lv = (ListView) layout.findViewById(android.R.id.list);
            header = LayoutInflater.from(act).inflate(R.layout.budget_listitem, null);
            ((TextView) header.findViewById(R.id.textViewLabel)).setText(R.string.gp_10);
            header.setBackgroundColor(getResources().getColor(R.color.yellow));
            header.setOnClickListener(null);
            lv.addHeaderView(header);

            return layout;
        }
    	
    	public void onClick(View v) {
    		if(v.getId() == R.id.imageButton1)
    			date.add(Calendar.MONTH, -1);
    		else
    			date.add(Calendar.MONTH, 1);
    		
    		((TextView) layout.findViewById(R.id.dateView)).setText(App.dateToUser("MMMM / y", date.getTime()));
    		renderList();
    	}
    	
    	@Override
    	public void onListItemClick(ListView l, View v, int position, long id) {
    		Bundle args = new Bundle();
			args.putLong("EDIT_ID", id);
			BudgetDialog edtDg = new BudgetDialog(act,args, BudgetDialog.EDIT);
			edtDg.show();
    	}
    	
        public void renderList() {
        	BudgetAdapter adapter = (BudgetAdapter) getListAdapter();

            SQLiteDatabase db = DatabaseHelper.quickDb(getActivity(), DatabaseHelper.MODE_READ);

            String queryModifier = "";
            if(type == Db.Table4.TYPE_BY_MONTH)
                queryModifier = " AND strftime('%Y-%m'," + Db.Table1.T_DATE + ") = '" + App.dateToDb("yyyy-MM", date.getTime()) + "'";

            Cursor c = db.rawQuery("SELECT "
                    + Db.Table4.T_ID + ","
                    + Db.Table4.T_TYPE + ","
                    + Db.Table4.T_AMOUNT + ","
                    + Db.Table4.T_ID_CATEGORY + ","
                    + Db.Table2.T_CATEGORY_NAME + ","
                    + "SUM(" + Db.Table1.T_AMOUNT + ")" +
                    " FROM " + Db.Table4.TABLE_NAME +
                    " INNER JOIN " + Db.Table2.TABLE_NAME + " ON " + Db.Table4.T_ID_CATEGORY + " = " + Db.Table2.T_ID +
                    " LEFT OUTER JOIN " + Db.Table1.TABLE_NAME + " ON " + Db.Table4.T_ID_CATEGORY + " = " + Db.Table1.T_ID_CATEGORY +
                    " AND " + Db.Table4.T_ID_GROUP + " = " + Db.Table1.T_ID_GROUP +
                    queryModifier +
                    " WHERE " + Db.Table4.T_ID_GROUP + " = " + app.activeGroupId +
                    " AND " + Db.Table4.T_TYPE + " = " + type +
                    " GROUP BY " + Db.Table4.T_ID +
                    " ORDER BY " + Db.Table4.T_ALERT + " DESC, " + Db.Table2.T_CATEGORY_NAME + " ASC", null);
    		if(adapter == null) {
    			adapter = new BudgetAdapter(act, c);
    			setListAdapter(adapter);
    		}
    		else {
    			adapter.swapCursor(c);
    			adapter.notifyDataSetChanged();
    		}

            float totalSpent = 0;
            float totalBudget = 0;

            c.moveToFirst();
            while(!c.isAfterLast()) {
                totalSpent += c.getFloat(5);
                totalBudget += c.getFloat(2);
                c.moveToNext();
            }

            float rate = totalSpent/totalBudget;

            ((TextView) header.findViewById(R.id.textViewPercentage)).setText(String.format("%.1f", (totalSpent/totalBudget)*100) + "%");
            ProgressBar pb = (ProgressBar) header.findViewById(R.id.progressBar1);
            pb.setProgress(Math.round(rate*pb.getMax()));
            GradientDrawable gd = (GradientDrawable) ((ScaleDrawable) ((LayerDrawable) pb.getProgressDrawable()).findDrawableByLayerId(android.R.id.progress)).getDrawable();
            if(pb.getProgress() == pb.getMax())
                gd.setColor(res.getColor(R.color.red));
            else
                gd.setColor(res.getColor(R.color.green));
            ((TextView) header.findViewById(R.id.textViewValues)).setText(app.printMoney(totalSpent) + " / " + app.printMoney(totalBudget));

            db.close();
        }

        public void showAddDialog() {
            BudgetDialog addDg = new BudgetDialog(act,null, BudgetDialog.ADD);
            if(!addDg.failed)
                addDg.show();
        }

        private class BudgetAdapter extends CursorAdapter {
        	private LayoutInflater mInflater;
        	
    	    public BudgetAdapter(Context context, Cursor c) {
    	        super(context, c, false);
    	        
    	        mInflater = LayoutInflater.from(context);
    	    }
    	    
    	    public View newView(Context context, Cursor cursor, ViewGroup parent) {
                return mInflater.inflate(R.layout.budget_listitem,parent,false);
            }
        	
        	public void bindView(View view, Context context, Cursor cursor) {
                ((TextView) view.findViewById(R.id.textViewLabel)).setText(cursor.getString(4));
        		
                float spent = cursor.getFloat(5);
                float budget = cursor.getFloat(2);
        		float rate = spent/budget;

        		((TextView) view.findViewById(R.id.textViewPercentage)).setText(String.format("%.1f", rate * 100) + "%");
        		ProgressBar pb = (ProgressBar) view.findViewById(R.id.progressBar1);
        		pb.setProgress((int)Math.floor(rate*pb.getMax()));
        		GradientDrawable gd = (GradientDrawable) ((ScaleDrawable) ((LayerDrawable) pb.getProgressDrawable()).findDrawableByLayerId(android.R.id.progress)).getDrawable();
        		if(pb.getProgress() == pb.getMax())
        			gd.setColor(res.getColor(R.color.red));
        		else
        			gd.setColor(res.getColor(R.color.green));
        		((TextView) view.findViewById(R.id.textViewValues)).setText(app.printMoney(spent) + " / " + app.printMoney(budget));
        	}
        }

        private class BudgetDialog extends Dialog implements View.OnClickListener {
            public static final int ADD = 0, EDIT = 1;
            private long editId;
            private int mode;
            private boolean failed = false;
            private Bundle params;
            private EditText edtValue;

            public BudgetDialog(Context ctx, Bundle args, int md) {
                super(ctx);
                params = args;
                mode = md;
            }

            @Override
            public void onCreate(Bundle savedInstance) {
                setContentView(R.layout.budget_dialog);

                edtValue = (EditText) findViewById(R.id.editText1);

                findViewById(R.id.button1).setOnClickListener(this);
                findViewById(R.id.button2).setOnClickListener(this);
                findViewById(R.id.button3).setOnClickListener(this);

                Spinner sp = (Spinner) findViewById(R.id.spinner1);
                SQLiteDatabase db = DatabaseHelper.quickDb(act, DatabaseHelper.MODE_READ);

                if(mode == ADD) {
                    Cursor c = db.rawQuery("SELECT "
                            + Db.Table2.T_ID + ","
                            + Db.Table2.T_CATEGORY_NAME +
                            " FROM " + Db.Table2.TABLE_NAME +
                            " WHERE NOT EXISTS(" +
                            "SELECT * FROM " + Db.Table4.TABLE_NAME +
                            " WHERE " + Db.Table4.T_ID_CATEGORY + " = " + Db.Table2.T_ID +
                            " AND " + Db.Table4.T_ID_GROUP + " = " + app.activeGroupId +
                            " AND " + Db.Table4.T_TYPE + " = " + type + ")" +
                            " ORDER BY " + Db.Table2.T_CATEGORY_NAME + " ASC",null);

                    if(c.getCount() == 0) {
                        App.Toast(act, "All categories are already in the budget");
                        failed = true;
                        c.close();
                    }
                    else {
                        SimpleCursorAdapter adapter = new SimpleCursorAdapter(act, android.R.layout.simple_spinner_item, c, new String[]{Db.Table2.CATEGORY_NAME}, new int[]{android.R.id.text1}, 0);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        sp.setAdapter(adapter);

                        setTitle(R.string.budget_c3);
                        findViewById(R.id.button3).setVisibility(View.GONE);

                        app.showKeyboard(edtValue);
                    }
                }
                else {
                    setTitle(R.string.budget_c4);
                    editId = params.getLong("EDIT_ID");

                    Cursor c2 = db.rawQuery("SELECT "
                            + Db.Table4.AMOUNT +
                            " FROM " + Db.Table4.TABLE_NAME +
                            " WHERE " + Db.Table4._ID + " = " + editId, null);
                    c2.moveToFirst();

                    edtValue.setText(c2.getString(0));
                    app.showKeyboard(edtValue);
                    c2.close();
                    sp.setVisibility(View.GONE);
                    findViewById(R.id.textView1).setVisibility(View.GONE);
                }

                db.close();
            }

            public void onClick(View v) {
                int id = v.getId();
                if(id == R.id.button1)
                    this.dismiss();
                else if(id == R.id.button2)
                    saveItem();
                else
                    deleteItem();
            }

            private void saveItem() {
                float amount;
                Spinner sp = (Spinner) findViewById(R.id.spinner1);

                try {
                    amount = Float.parseFloat(edtValue.getText().toString());
                    if(amount == 0)
                        throw new Exception();
                }
                catch (Exception e) {
                    App.Toast(act, R.string.budget_c5);
                    return;
                }

                SQLiteDatabase db = DatabaseHelper.quickDb(act, DatabaseHelper.MODE_WRITE);
                ContentValues cv = new ContentValues();
                cv.put(Db.Table4.AMOUNT, amount);
                cv.put(Db.Table4.TYPE, type);
                if(mode == ADD) {
                    cv.put(Db.Table4.ID_CATEGORY, sp.getSelectedItemId());
                    cv.put(Db.Table4.ALERT, 0);
                }
                cv.put(Db.Table4.ID_GROUP, app.activeGroupId);
                long result;
                if(mode == EDIT)
                    result = db.update(Db.Table4.TABLE_NAME, cv, Db.Table4._ID + " = " + editId, null);
                else
                    result = db.insert(Db.Table4.TABLE_NAME, null, cv);
                int toastText;
                if((mode == EDIT && result == 1) | (mode == ADD && result != -1)) {
                    if(mode == EDIT)
                        toastText = R.string.budget_c6;
                    else
                        toastText = R.string.budget_c7;
                    renderList();
                    this.dismiss();
                }
                else
                    toastText = R.string.gp_11;
                App.Toast(act, toastText);
                db.close();
            }

            private void deleteItem() {
                SQLiteDatabase db = DatabaseHelper.quickDb(act, DatabaseHelper.MODE_WRITE);
                String toastString;
                int result = db.delete(Db.Table4.TABLE_NAME, Db.Table4._ID + " = " + editId, null);

                if(result == 1) {
                    toastString = "certo";
                    app.setFlag(4);
                    renderList();
                    this.dismiss();
                }
                else
                    toastString = "errado";

                App.Toast(act, toastString);
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
	    	args.putInt("TYPE", position == 0 ? Db.Table4.TYPE_BY_MONTH : Db.Table4.TYPE_NO_TIME);
	    	lp.setArguments(args);
	    	pages[position] = lp;
	    	return lp;
	    }
	    
	    public ListPage itemAt(int position) {
	    	return pages[position];
	    }
	}
}