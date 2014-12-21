package com.dpcsoftware.mn;

import java.util.Calendar;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class Goals extends ActionBarActivity {
	private App app;
	private ListView lv;
	private GoalsAdapter adapter;
	private Runnable menuCallback = new Runnable() {
		public void run() {
			renderList();
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (App) getApplication();
		
		setContentView(R.layout.goals);
		lv = (ListView) findViewById(R.id.listView1);
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	app.new SpinnerMenu(this,menuCallback);
    	
    	return true;
    }
    
    private void renderList() {
    	SQLiteDatabase db = DatabaseHelper.quickDb(this, DatabaseHelper.MODE_READ);
		Cursor c = db.rawQuery("SELECT "
				+ Db.Table4._ID + ","
				+ Db.Table4.TYPE + ","
				+ Db.Table4.AMOUNT + ","
				+ Db.Table4.ID_CATEGORY +			
				" FROM " + Db.Table4.TABLE_NAME +
				" WHERE " + Db.Table4.ID_GROUP + " = " + app.activeGroupId +
				" ORDER BY " + Db.Table4.TYPE + " ASC", null);
		if(adapter == null) {
			adapter = new GoalsAdapter(this, c);
			lv.setAdapter(adapter);
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
    		Cursor c;  
    		int type = cursor.getInt(1);
    		if(type == Db.Table4.TYPE_TOTAL) {
    			c = db.rawQuery("SELECT "
    					+ "SUM(" + Db.Table1.AMOUNT + ")" +
    					" FROM " + Db.Table1.TABLE_NAME +
    					" WHERE " + Db.Table1.ID_GROUP + " = " + app.activeGroupId, null);
    			((TextView) view.findViewById(R.id.textView2)).setText("Total");
    		}
    		else if(type == Db.Table4.TYPE_TOTAL_BY_MONTH){
    			c = db.rawQuery("SELECT "
    					+ "SUM(" + Db.Table1.AMOUNT + ")" +
    					" FROM " + Db.Table1.TABLE_NAME +
    					" WHERE " + Db.Table1.ID_GROUP + " = " + app.activeGroupId +
    					" AND strftime('%Y-%m'," + Db.Table1.DATE + ") = '" + app.dateToDb("yyyy-MM", Calendar.getInstance().getTime()) + "'", null);
    			((TextView) view.findViewById(R.id.textView2)).setText("Total Mensal");    			
    		}
    		else if(type == Db.Table4.TYPE_CAT) {
    			c = db.rawQuery("SELECT "
    					+ "SUM(" + Db.Table1.T_AMOUNT + "), "
    					+ Db.Table2.CATEGORY_NAME +
    					" FROM " + Db.Table1.TABLE_NAME + "," + Db.Table2.TABLE_NAME +
    					" WHERE " + Db.Table1.T_ID_GROUP + " = " + app.activeGroupId +
    					" AND " + Db.Table1.T_ID_CATEGORY + " = " + cursor.getInt(3), null);
    			c.moveToFirst();
    			((TextView) view.findViewById(R.id.textView2)).setText(c.getString(1));
    		}
    		else {
    			c = db.rawQuery("SELECT "
    					+ "SUM(" + Db.Table1.T_AMOUNT + "), "
    					+ Db.Table2.CATEGORY_NAME + 
    					" FROM " + Db.Table1.TABLE_NAME + "," + Db.Table2.TABLE_NAME +
    					" WHERE " + Db.Table1.T_ID_GROUP + " = " + app.activeGroupId +
    					" AND " + Db.Table1.T_ID_CATEGORY + " = " + cursor.getInt(3) +
    					" AND strftime('%Y-%m'," + Db.Table1.T_DATE + ") = '" + app.dateToDb("yyyy-MM", Calendar.getInstance().getTime()) + "'", null);
    			c.moveToFirst();
    			((TextView) view.findViewById(R.id.textView2)).setText(c.getString(1) + " - Mensal");
    		}
    		
    		c.moveToFirst();
    		float rate = c.getFloat(0)/cursor.getFloat(2);
    		((TextView) view.findViewById(R.id.textView1)).setText(String.format("%.1f", rate*100) + "%");
    		ProgressBar pb = (ProgressBar) view.findViewById(R.id.progressBar1);
    		pb.setProgress(Math.round(rate*pb.getMax()));
    		((TextView) view.findViewById(R.id.textView3)).setText(app.printMoney(c.getFloat(0)) + " / " + app.printMoney(cursor.getFloat(2)));
    		
    		
    		/*ImageButton btEdit = (ImageButton) view.findViewById(R.id.imageButtonEdit);
    		btEdit.setOnClickListener(this);
    		btEdit.setTag(Integer.valueOf(cursor.getPosition()));
    		ImageButton btDelete = (ImageButton) view.findViewById(R.id.imageButtonDelete);
    		btDelete.setOnClickListener(this);
    		btDelete.setTag(Integer.valueOf(cursor.getPosition()));*/
    	}
    }
}