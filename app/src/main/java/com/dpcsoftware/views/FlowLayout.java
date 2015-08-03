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
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public class FlowLayout extends ViewGroup {
    private ArrayList<Integer> rowPos;
    private ArrayList<Integer> childrenLeft;

    public FlowLayout(Context context) {
        super(context);
        init();
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        rowPos = new ArrayList<Integer>();
        childrenLeft = new ArrayList<Integer>();
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();

        int childLeft = 0;
        int childTop = 0;
        int rowHeight = 0;

        int myWidth = Math.max(MeasureSpec.getSize(widthMeasureSpec), getSuggestedMinimumWidth());

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
                MarginLayoutParams mlp = (MarginLayoutParams) child.getLayoutParams();

                int chHeight = child.getMeasuredHeight() + mlp.topMargin + mlp.bottomMargin;
                int chWidth = child.getMeasuredWidth() + mlp.leftMargin + mlp.rightMargin;

                if(childLeft + chWidth > myWidth) { //put child in next row
                    childLeft = 0;
                    childTop += rowHeight;
                    rowHeight = 0;
                }
                childLeft += chWidth;
                rowHeight = Math.max(rowHeight, chHeight);
            }
        }
        setMeasuredDimension(myWidth, childTop + rowHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int count = getChildCount();

        int childLeft = 0;
        int childTop = 0;
        int rowHeight = 0;

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                MarginLayoutParams mlp = (MarginLayoutParams) child.getLayoutParams();

                int chHeight = child.getMeasuredHeight() + mlp.topMargin + mlp.bottomMargin;
                int chWidth = child.getMeasuredWidth() + mlp.leftMargin + mlp.rightMargin;

                if(childLeft + chWidth > right) { //put child in next row
                    childLeft = 0;
                    childTop += rowHeight;
                    rowHeight = 0;
                }

                child.layout(
                        childLeft,
                        childTop,
                        childLeft + child.getMeasuredWidth(),
                        childTop + child.getMeasuredHeight());

                childLeft += chWidth;
                rowHeight = Math.max(rowHeight, chHeight);
            }
        }
    }

}
