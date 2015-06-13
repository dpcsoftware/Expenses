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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.Calendar;


public class TimeStats extends AppCompatActivity {
	private App app;
	private String[] labels;
	private TimeChart chart;
	private int numberOfItems;
	private Calendar referenceDate;
	private boolean periodIsYear = true;
	private Runnable menuCallback = new Runnable() {
		public void run() {
			renderGraph();
		}
	};
	private View.OnClickListener changePageListener = new View.OnClickListener() {
		public void onClick(View v) {
			int n = 0;
			switch(v.getId()) {
			case R.id.imageButton1:
				n = -1;
				break;
			case R.id.imageButton2:
				n = 1;
			}
			
			if(periodIsYear)
				referenceDate.add(Calendar.YEAR,n);
			else
				referenceDate.add(Calendar.DAY_OF_MONTH,n*7);
			
			renderGraph();
		}
	};
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (App) getApplication();
		
		setContentView(R.layout.timestats);
		
		((RadioGroup) findViewById(R.id.radioGroup1)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			public void onCheckedChanged (RadioGroup group, int checkedId) {
				periodIsYear = (checkedId == R.id.radio1);
				renderGraph();
			}
		});
		
		findViewById(R.id.imageButton1).setOnClickListener(changePageListener);
		findViewById(R.id.imageButton2).setOnClickListener(changePageListener);
		
		referenceDate = Calendar.getInstance();
	}
		
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	app.new SpinnerMenu(this,menuCallback);
    	
    	return true;
    }
    
	public void renderGraph() {
		
		if(!periodIsYear) {
			int x = 0;
			int firstDay = referenceDate.getFirstDayOfWeek();
			int today = referenceDate.get(Calendar.DAY_OF_WEEK);
			int diff = Math.abs(today - firstDay);
			if(today != firstDay) {	
				if(today > firstDay)
					x = -diff;
				else if(today < firstDay)
					x = -7 + diff;
			}
			referenceDate.add(Calendar.DAY_OF_MONTH, x);
		}
			
		String queryModifier, datePattern, datePatternDb;
		if(periodIsYear) {
			numberOfItems = 12;
			queryModifier = "";
			datePatternDb = "yyyy-MM";
			datePattern = "MMMMM";
		}
		else {
			numberOfItems = 7;
			queryModifier = "-%d";
			datePattern = "EEEEE";
			datePatternDb = "yyyy-MM-dd";
		}
		
		float[] values = new float[numberOfItems];
		labels = new String[values.length];
		int i = 0;
		Calendar iDate = Calendar.getInstance();
		iDate.setTime(referenceDate.getTime());
		if(periodIsYear)
			iDate.set(Calendar.MONTH, 0);
		SQLiteDatabase db = DatabaseHelper.quickDb(this, 0);
		while(i < numberOfItems) {	
	    	Cursor c = db.rawQuery("SELECT SUM(" + Db.Table1.AMOUNT + ")," +
	    			" strftime('%Y-%m" + queryModifier + "'," + Db.Table1.DATE + ") AS timeUnit" +
	    			" FROM " + Db.Table1.TABLE_NAME +
	    			" WHERE " +
	    			Db.Table1.ID_GROUP + " = " + app.activeGroupId +
	    			" AND timeUnit = '" + App.dateToDb(datePatternDb, iDate.getTime()) + "'" +
	    			" GROUP BY timeUnit",null);
	    	c.moveToFirst();
	    	
	    	float val;
	    	if(c.getCount() > 0)
	    		val = c.getFloat(0);
	    	else
	    		val = 0;
	    	values[i] = val;
	    	labels[i] = App.dateToUser(datePattern, iDate.getTime()).substring(0, 1).toUpperCase();
	    	i++;
	    	if(periodIsYear)
	    		iDate.add(Calendar.MONTH,1);
	    	else
	    		iDate.add(Calendar.DAY_OF_MONTH,1);

            c.close();
		}
		db.close();
		
		FrameLayout graphLayout = ((FrameLayout) findViewById(R.id.FrameLayoutTimeChart));
		if(chart == null) {
			chart = new TimeChart(this, values, labels);
			graphLayout.addView(chart);
		}
		else
			chart.setNewData(values, labels);
		
		if(periodIsYear)
			((TextView) findViewById(R.id.textView2)).setText(App.dateToUser("yyyy", referenceDate.getTime()));
		else {
			iDate.add(Calendar.DAY_OF_MONTH,-1);
			((TextView) findViewById(R.id.textView2)).setText(App.dateToUser(null, referenceDate.getTime()) + " - " + App.dateToUser(null, iDate.getTime()));
		}
	}
	
	private class TimeChart extends View {
		private Paint paintModel, paintAxe, paintScale, paintPoints, paintUnderPoints, paintLinePoints, paintTextV, paintTextH;
		private float left, right, top, bottom;
		private float values[], points[], max, min;
		private Path pathUnder;
		private String labels[];
		private float numberOfIntervals, gridStep;
		private int gridStepMoney;
		private float horizontalStep;
		
		//private GestureDetector touchDetector;
		
		public TimeChart(Context ct, float[] val, String[] lbs) {
			super(ct);
			
			paintModel = new Paint(Paint.ANTI_ALIAS_FLAG);
			paintModel.setStyle(Paint.Style.FILL);
			
			paintAxe = new Paint(paintModel);
			paintAxe.setColor(Color.BLACK);
			paintAxe.setStrokeWidth(3);
			
			paintScale = new Paint(paintModel);
			paintScale.setColor(Color.GRAY);
			paintScale.setStrokeWidth(2);
			
			paintUnderPoints = new Paint(paintModel);
			paintUnderPoints.setColor(Color.RED);
			paintUnderPoints.setAlpha(120);
			
			paintLinePoints = new Paint(Paint.ANTI_ALIAS_FLAG);
			paintLinePoints.setStyle(Paint.Style.STROKE);
			paintLinePoints.setColor(Color.RED);
			paintLinePoints.setStrokeWidth(4);
			
			paintPoints = new Paint(paintModel);
			paintPoints.setColor(Color.RED);
			paintPoints.setStrokeWidth(10);
			paintPoints.setStrokeCap(Paint.Cap.ROUND);
			
			paintTextV = new Paint(paintModel);
			paintTextV.setColor(Color.BLACK);
			paintTextV.setTextSize(20);
			paintTextV.setTextAlign(Paint.Align.LEFT);
			
			paintTextH = new Paint(paintTextV);
			paintTextH.setTextAlign(Paint.Align.CENTER);
			
			setPadding(10,10,10,10);
			
			values = val;
			labels = lbs;
			//touchDetector = new GestureDetector(ct, new TouchListener());
		}
		
		public void setNewData(float[] val, String[] lbs) {
			values = val;
			labels = lbs;
			
			calculateMaxMin();
			calculatePoints();
			invalidate();
		}
		
		private void calculateMaxMin() {
			int i;
			max = values[0];
			for(i = 1;i < values.length;i++) {
				if(values[i] > max)
					max = values[i];
			}
			min = values[0];
			for(i = 1;i < values.length;i++) {
				if(values[i] < min)
					min = values[i];
			}
		
			max = (float) Math.ceil(max);
			min = (float) Math.floor(min);
			
			if(max == min)
				max +=10;
						
			gridStepMoney = 1;
			numberOfIntervals = (bottom - top)/40;
			while(gridStepMoney*numberOfIntervals < (max - min)) {
				gridStepMoney *= 10;				
			}
			
			max = (float) Math.ceil(max/gridStepMoney)*gridStepMoney;
			min = (float) Math.floor(min/gridStepMoney)*gridStepMoney;
			
			numberOfIntervals = Math.round((max - min)/gridStepMoney);
			gridStep = (bottom - top)/numberOfIntervals;
			
			left = getPaddingLeft() + String.valueOf((int)max).length()*15;
		}
		
		private void calculatePoints() {
			points = new float[values.length*2];
			pathUnder = new Path();
			pathUnder.moveTo(left,bottom);
			
			int i;
			horizontalStep = (right - left)/(values.length-1);
			for(i = 0;i < values.length;i++) {
				float yPoint = top + ((max - values[i])/(max - min))*(bottom - top);
				points[2*i] = left + horizontalStep*i;
				points[2*i+1] = yPoint;
				pathUnder.lineTo(points[2*i], yPoint);
			}
			pathUnder.lineTo(right, bottom);
			pathUnder.lineTo(left, bottom);
		}
		
		@Override
		public void onSizeChanged(int w, int h, int oldw, int oldh) {
			top = getPaddingTop() + 20;
			bottom = h - getPaddingBottom() - 30;
			
			calculateMaxMin();
						
			right = w - getPaddingRight() - 20;
			
			calculatePoints();			
		}
		
		@Override
		public void onDraw(Canvas canvas) {
			
			//Scale and Vertical Labels
			float j = top;
			int jm = (int)max;
			float leftPadding = getPaddingLeft();
			while(j < bottom) {
				canvas.drawLine(left, j, right, j, paintScale);
				canvas.drawText(String.valueOf(jm), leftPadding, j+7, paintTextV);
				j += gridStep;
				jm -= gridStepMoney;
			}			
			canvas.drawText(String.valueOf((int)min), leftPadding, bottom+7, paintTextV);
			
			//Axes
			canvas.drawLine(left, bottom, left, top, paintAxe);
			canvas.drawLine(left, bottom, right, bottom, paintAxe);
						
			//Points and Horizontal Labels
			int i, end;
			float xText, yText;
			end = values.length - 1;
			canvas.drawPath(pathUnder, paintUnderPoints);
			for(i = 0;i <= end;i++) {
				if(i != end)
					canvas.drawLine(points[2*i], points[2*i+1], points[2*i+2], points[2*i+3], paintLinePoints);
				xText = left + i*horizontalStep;
				yText = bottom + 25;
				canvas.drawText(labels[i], xText, yText, paintTextH);
			}
			canvas.drawPoints(points, paintPoints);

		}
		
		/*@Override
		public boolean onTouchEvent(@NonNull MotionEvent event) {
		   boolean result = touchDetector.onTouchEvent(event);
		   if (!result) {
		       if (event.getAction() == MotionEvent.ACTION_UP) {
		    	   //int index = (int)Math.round((event.getX()-left)/(right-left));
		    	   //App.Toast(context, app.printMoney(values[index]));
		       }
		       else if(event.getAction() == MotionEvent.ACTION_MOVE) {

		       }
		   }
		   return result;
		}
		
		private class TouchListener extends GestureDetector.SimpleOnGestureListener {
			@Override
			public boolean onDown(MotionEvent e) {
				return true;
			}
		}*/
	}
}
