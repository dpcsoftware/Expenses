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

package com.dpcsoftware.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.dpcsoftware.mn.App;

public class HSVColorPicker extends LinearLayout {
    private float hue, sat, val;
    private BarView hv, sv, vv;
    private Context ctx;
    private float screenDensity;

    public HSVColorPicker(Context context) {
        super(context);
        ctx = context;
        init();
    }

    public HSVColorPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        ctx = context;
        init();
    }

    public int getColor() {
        return Color.HSVToColor(new float[] {hue, sat, val});
    }

    public void setColor(int color) {
        float hsv[] = new float[3];
        Color.colorToHSV(color, hsv);
        hue = hsv[0];
        sat = hsv[1];
        val = hsv[2];
        drawBars();
    }

    public interface OnColorChangeListener {
        void onColorChange(int newColor);
    }

    private OnColorChangeListener mColorChangeListener = null;

    public void setOnColorChangeListener(OnColorChangeListener l) {
        mColorChangeListener = l;
    }

    private void init() {
        screenDensity = ctx.getResources().getDisplayMetrics().density;
        setOrientation(LinearLayout.VERTICAL);
        hue = 200;
        sat = 0.9f;
        val = 0.9f;

        hv = new BarView(ctx, BarView.HUE);
        addView(hv);
        sv = new BarView(ctx, BarView.SAT);
        addView(sv);
        vv = new BarView(ctx, BarView.VAL);
        addView(vv);
    }

    private void drawBars() {
        hv.invalidate();
        sv.invalidate();
        vv.invalidate();
        if(mColorChangeListener != null)
            mColorChangeListener.onColorChange(getColor());
    }

    private class BarView extends View {
        private float left, right, top, bottom, height, range;
        private float cursorWidth, cursorBorderWidth, cursorPos;
        private float lastX = 0, i;
        private Paint p, pCursorBorder;

        public static final int HUE = 1;
        public static final int SAT = 2;
        public static final int VAL = 3;

        private int type;

        public BarView(Context context, int t) {
            super(context);
            type = t;
            init();
        }

        private int dpToPx(float dp) {
            return Math.round(screenDensity*dp);
        }

        private void init() {
            setLayoutParams(new LinearLayoutCompat.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(35)));
            int pad = dpToPx(5);
            setPadding(pad, pad, pad, pad);

            cursorWidth = dpToPx(8);
            cursorBorderWidth = dpToPx(1);

            p = new Paint(Paint.ANTI_ALIAS_FLAG);
            pCursorBorder = new Paint(p);
            p.setStyle(Paint.Style.FILL);
            p.setStrokeWidth(4);
            pCursorBorder.setStyle(Paint.Style.STROKE);
            pCursorBorder.setColor(Color.BLACK);
            pCursorBorder.setStrokeWidth(cursorBorderWidth);
            pCursorBorder.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        protected void onSizeChanged (int w, int h, int oldw, int oldh) {
            left = getPaddingLeft();
            right = w - getPaddingRight();
            range = right - left;
            top = getPaddingTop();
            bottom = h - getPaddingBottom();
            height = h;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
             switch(event.getAction()) {
                 case MotionEvent.ACTION_MOVE:
                 case MotionEvent.ACTION_DOWN:
                     float x = event.getX();
                     if(x >= left && x <= right && Math.abs(x - lastX) > 5) {
                         cursorPos = event.getX();
                         if (type == HUE)
                             hue = (cursorPos - left) / range * 360;
                         else if (type == SAT)
                             sat = (cursorPos - left) / range;
                         else
                             val = (cursorPos - left) / range;
                         lastX = x;
                         drawBars();
                     }
                     return true;
                 default:
                     return super.onTouchEvent(event);
            }
        }

        @Override
        protected void onDraw (Canvas canvas) {
            if(type == HUE) {
                for (i = left; i < right; i += 3) {
                    p.setColor(Color.HSVToColor(new float[]{(i - left) / range * 360, 0.9f, 0.9f}));
                    canvas.drawLine(i, top, i, bottom, p);
                }
                cursorPos = hue/360.0f*range + left;
            }
            else if(type == SAT) {
                for (i = left; i < right; i += 1) {
                    p.setColor(Color.HSVToColor(new float[]{hue, (i - left) / range, val}));
                    canvas.drawLine(i, top, i, bottom, p);
                }
                cursorPos = sat*range + left;
            }
            else {
                for (i = left; i < right; i += 1) {
                    p.setColor(Color.HSVToColor(new float[]{hue, sat, (i - left) / range}));
                    canvas.drawLine(i, top, i, bottom, p);
                }
                cursorPos = val*range + left;
            }

            p.setColor(Color.WHITE);
            canvas.drawRect(cursorPos - cursorWidth / 2, 0, cursorPos + cursorWidth / 2, height, p);
            canvas.drawRect(cursorPos - cursorWidth / 2, cursorBorderWidth / 2, cursorPos + cursorWidth / 2, height - cursorBorderWidth / 2, pCursorBorder);
        }
    }
}
