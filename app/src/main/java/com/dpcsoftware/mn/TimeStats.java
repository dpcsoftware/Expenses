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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Calendar;


public class TimeStats extends AppCompatActivity {
    private enum Period {
        YEAR,
        MONTH
    }

    private App app;
    private SharedPreferences prefs;
    private TimeChart chart;
    private Calendar referenceDate;
    private Period mPeriod;
    private boolean cumulative, showBudget;
    private Runnable menuCallback = new Runnable() {
        public void run() {
            renderGraph();
        }
    };
    private View.OnClickListener changePageListener = new View.OnClickListener() {
        public void onClick(View v) {
            int n = 0;
            switch (v.getId()) {
                case R.id.imageButton1:
                    n = -1;
                    break;
                case R.id.imageButton2:
                    n = 1;
            }

            if (mPeriod == Period.YEAR)
                referenceDate.add(Calendar.YEAR, n);
            else
                referenceDate.add(Calendar.MONTH, n);

            renderGraph();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (App) getApplication();

        setContentView(R.layout.timestats);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getInt("TIMESTATS_PERIOD", 0) == 0) {
            mPeriod = Period.YEAR;
            ((RadioButton) findViewById(R.id.radio1)).setChecked(true);
        } else {
            mPeriod = Period.MONTH;
            ((RadioButton) findViewById(R.id.radio2)).setChecked(true);
        }
        cumulative = prefs.getBoolean("TIMESTATS_CUMULATIVE", false);
        ((CheckBox) findViewById(R.id.checkBoxCumulative)).setChecked(cumulative);
        showBudget = prefs.getBoolean("TIMESTATS_SHOWBUDGET", false);
        ((CheckBox) findViewById(R.id.checkBoxBudget)).setChecked(showBudget);

        ((RadioGroup) findViewById(R.id.radioGroup1)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                SharedPreferences.Editor pEditor = prefs.edit();
                if (checkedId == R.id.radio1) {
                    mPeriod = Period.YEAR;
                    pEditor.putInt("TIMESTATS_PERIOD", 0);
                } else {
                    mPeriod = Period.MONTH;
                    pEditor.putInt("TIMESTATS_PERIOD", 1);
                }
                pEditor.apply();
                renderGraph();
            }
        });

        ((CheckBox) findViewById(R.id.checkBoxCumulative)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                cumulative = isChecked;
                SharedPreferences.Editor pEditor = prefs.edit();
                pEditor.putBoolean("TIMESTATS_CUMULATIVE", cumulative);
                pEditor.apply();
                renderGraph();
            }
        });

        ((CheckBox) findViewById(R.id.checkBoxBudget)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showBudget = isChecked;
                SharedPreferences.Editor pEditor = prefs.edit();
                pEditor.putBoolean("TIMESTATS_SHOWBUDGET", showBudget);
                pEditor.apply();
                renderGraph();
            }
        });

        findViewById(R.id.imageButton1).setOnClickListener(changePageListener);
        findViewById(R.id.imageButton2).setOnClickListener(changePageListener);

        referenceDate = Calendar.getInstance();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        app.new SpinnerMenu(this, menuCallback);

        return true;
    }

    public void renderGraph() {
        SQLiteDatabase db = DatabaseHelper.quickDb(this, DatabaseHelper.MODE_READ);

        //Query the database
        int numberOfItems;
        Cursor c;
        if (mPeriod == Period.YEAR) {
            numberOfItems = 12;
            c = db.rawQuery("SELECT SUM(" + Db.Table1.AMOUNT + ")," +
                    " strftime('%Y-%m'," + Db.Table1.DATE + ") AS timeUnit" +
                    " FROM " + Db.Table1.TABLE_NAME +
                    " WHERE " +
                    Db.Table1.ID_GROUP + " = " + app.activeGroupId +
                    " AND timeUnit >= '" + App.dateToDb("yyyy-01", referenceDate.getTime()) + "'" +
                    " AND timeUnit <= '" + App.dateToDb("yyyy-12", referenceDate.getTime()) + "'" +
                    " GROUP BY timeUnit", null);
        } else {
            numberOfItems = referenceDate.getActualMaximum(Calendar.DAY_OF_MONTH);
            c = db.rawQuery("SELECT SUM(" + Db.Table1.AMOUNT + ")," +
                    " strftime('%Y-%m-%d'," + Db.Table1.DATE + ") AS timeUnit" +
                    " FROM " + Db.Table1.TABLE_NAME +
                    " WHERE " +
                    Db.Table1.ID_GROUP + " = " + app.activeGroupId +
                    " AND timeUnit >= '" + App.dateToDb("yyyy-MM-01", referenceDate.getTime()) + "'" +
                    " AND timeUnit <= '" + App.dateToDb("yyyy-MM-31", referenceDate.getTime()) + "'" +
                    " GROUP BY timeUnit", null);
        }

        //Fill chart values
        float[] values = new float[numberOfItems];
        Arrays.fill(values, 0);
        String timeUnit;
        if (c.getCount() > 0) {
            c.moveToFirst();
            while (!c.isAfterLast()) {
                timeUnit = c.getString(1);
                values[Integer.valueOf(timeUnit.substring(timeUnit.length() - 2)) - 1] = c.getFloat(0);
                c.moveToNext();
            }
        }

        //Generate chart labels
        Calendar iDate = Calendar.getInstance();
        iDate.setTime(referenceDate.getTime());
        String[] labels = new String[values.length];
        int i = 0;
        if (mPeriod == Period.YEAR) {
            iDate.set(Calendar.MONTH, 0);
            while (i < numberOfItems) {
                labels[i] = App.dateToUser("MMMMM", iDate.getTime());
                i++;
                iDate.add(Calendar.MONTH, 1);
            }
        } else {
            iDate.set(Calendar.DAY_OF_MONTH, 1);
            while (i < numberOfItems) {
                labels[i] = App.dateToUser("d", iDate.getTime());
                i++;
                iDate.add(Calendar.DAY_OF_MONTH, 1);
            }
        }

        float budgetTotal = -1;
        findViewById(R.id.checkBoxBudget).setVisibility(View.GONE);
        Cursor c2 = db.rawQuery("SELECT SUM(" + Db.Table4.T_AMOUNT + ")," + Db.Table3.T_GROUP_TYPE +
                " FROM " + Db.Table4.TABLE_NAME +
                " LEFT JOIN " + Db.Table3.TABLE_NAME + " ON " + Db.Table4.T_ID_GROUP + " = " + Db.Table3.T_ID +
                " WHERE " + Db.Table4.T_ID_GROUP + " = " + app.activeGroupId +
                " GROUP BY " + Db.Table4.T_ID_GROUP, null);
        if (c2.getCount() > 0) {
            c2.moveToFirst();
            if (c2.getInt(1) == Db.Table3.TYPE_MONTH) {
                if ((mPeriod == Period.MONTH && cumulative) || (mPeriod == Period.YEAR && !cumulative)) {
                    if (showBudget) {
                        budgetTotal = c2.getFloat(0);
                    }
                    findViewById(R.id.checkBoxBudget).setVisibility(View.VISIBLE);
                }
            }
        }
        c2.close();
        db.close();

        FrameLayout graphLayout = ((FrameLayout) findViewById(R.id.FrameLayoutTimeChart));
        if (chart == null) {
            chart = new TimeChart(this, values, labels, budgetTotal, cumulative);
            graphLayout.addView(chart);
        } else
            chart.setNewData(values, labels, budgetTotal, cumulative);

        if (mPeriod == Period.YEAR)
            ((TextView) findViewById(R.id.textView2)).setText(App.dateToUser("yyyy", referenceDate.getTime()));
        else {
            ((TextView) findViewById(R.id.textView2)).setText(App.dateToUser("MMMM / yyyy", referenceDate.getTime()));
        }
    }

    private class TimeChart extends View {
        private Paint paintModel, paintAxe, paintScale, paintPoints, paintUnderPoints, paintLinePoints, paintTextV, paintTextH, paintTargetVal;
        private float left, right, top, bottom;
        private float values[], targetValue, points[], targetPointY, max, min;
        private Path pathUnder;
        private String labels[];
        private float numberOfIntervals, gridStep;
        private int gridStepMoney, skipLabelNum;
        private float horizontalStep;
        private boolean cumulative;

        //private GestureDetector touchDetector;

        public TimeChart(Context ct, float[] val, String[] lbs, float targetVal, boolean cumul) {
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

            paintTargetVal = new Paint(paintModel);
            paintTargetVal.setColor(getResources().getColor(R.color.lightGreen));
            paintTargetVal.setStrokeWidth(6);

            setPadding(10, 10, 10, 10);

            values = val;
            labels = lbs;
            cumulative = cumul;
            targetValue = targetVal;
            //touchDetector = new GestureDetector(ct, new TouchListener());
        }

        public void setNewData(float[] val, String[] lbs, float targetVal, boolean cumul) {
            values = val;
            labels = lbs;
            cumulative = cumul;
            targetValue = targetVal;

            calculateMaxMin();
            calculatePoints();
            invalidate();
        }

        private void calculateMaxMin() {
            int i;

            if (cumulative) {
                max = 0;
                for (i = 0; i < values.length; ++i) {
                    max += values[i];
                }
                min = values[0];
            } else {
                max = values[0];
                for (i = 1; i < values.length; i++) {
                    if (values[i] > max)
                        max = values[i];
                }
                min = values[0];
                for (i = 1; i < values.length; i++) {
                    if (values[i] < min)
                        min = values[i];
                }
            }

            max = Math.max(max, targetValue);
            max = (float) Math.ceil(max);
            min = (float) Math.floor(min);

            if (max == min)
                max += 10;

            gridStepMoney = 1;
            numberOfIntervals = (bottom - top) / 40;
            while (gridStepMoney * numberOfIntervals < (max - min)) {
                gridStepMoney *= 10;
            }

            max = (float) Math.ceil(max / gridStepMoney) * gridStepMoney;
            min = (float) Math.floor(min / gridStepMoney) * gridStepMoney;

            numberOfIntervals = Math.round((max - min) / gridStepMoney);
            gridStep = (bottom - top) / numberOfIntervals;

            left = getPaddingLeft() + paintTextV.measureText(String.valueOf(max)) + 5;

            skipLabelNum = (int) Math.ceil((double) labels.length * paintTextH.measureText("31") / (right - left));
        }

        private void calculatePoints() {
            points = new float[values.length * 2];
            pathUnder = new Path();
            pathUnder.moveTo(left, bottom);

            int i;
            horizontalStep = (right - left) / (values.length - 1);
            float currentValue = 0;
            for (i = 0; i < values.length; i++) {
                if (cumulative) {
                    currentValue += values[i];
                } else {
                    currentValue = values[i];
                }
                float yPoint = top + ((max - currentValue) / (max - min)) * (bottom - top);
                points[2 * i] = left + horizontalStep * i;
                points[2 * i + 1] = yPoint;
                pathUnder.lineTo(points[2 * i], yPoint);
            }
            pathUnder.lineTo(right, bottom);
            pathUnder.lineTo(left, bottom);

            targetPointY = top + ((max - targetValue) / (max - min)) * (bottom - top);
        }

        @Override
        public void onSizeChanged(int w, int h, int oldw, int oldh) {
            top = getPaddingTop() + 20;
            bottom = h - getPaddingBottom() - 30;
            right = w - getPaddingRight() - 20;

            calculateMaxMin();
            calculatePoints();
        }

        @Override
        public void onDraw(Canvas canvas) {

            //Scale and Vertical Labels
            float j = top;
            int jm = (int) max;
            float leftPadding = getPaddingLeft();
            while (j < bottom) {
                canvas.drawLine(left, j, right, j, paintScale);
                canvas.drawText(String.valueOf(jm), leftPadding, j + 7, paintTextV);
                j += gridStep;
                jm -= gridStepMoney;
            }
            canvas.drawText(String.valueOf((int) min), leftPadding, bottom + 7, paintTextV);

            //Axes
            canvas.drawLine(left, bottom, left, top, paintAxe);
            canvas.drawLine(left, bottom, right, bottom, paintAxe);

            //Points and Horizontal Labels
            int i, end;
            float xText, yText;
            end = values.length - 1;
            canvas.drawPath(pathUnder, paintUnderPoints);
            for (i = 0; i <= end; i++) {
                if (i != end)
                    canvas.drawLine(points[2 * i], points[2 * i + 1], points[2 * i + 2], points[2 * i + 3], paintLinePoints);
                xText = left + i * horizontalStep;
                yText = bottom + 25;
                if (i % skipLabelNum == 0)
                    canvas.drawText(labels[i], xText, yText, paintTextH);
            }
            canvas.drawPoints(points, paintPoints);

            if (targetValue > 0) {
                canvas.drawLine(left, targetPointY, right, targetPointY, paintTargetVal);
            }
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
