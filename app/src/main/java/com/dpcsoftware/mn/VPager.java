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
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.widget.ImageView;

public class VPager extends ViewPager {
	private ImageView tabInd;
	private int indWidth;
	
	public VPager(Context context) {
		super(context);
	}
	
	public VPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void setImgTab(ImageView iv) {
		tabInd = iv;
		indWidth = tabInd.getDrawable().getIntrinsicWidth();
	}
	
	@Override
	protected void onPageScrolled(int position, float offset, int offsetPixels) {
		super.onPageScrolled(position, offset, offsetPixels);
		if(tabInd != null) {
			tabInd.setPadding(offsetPixels*indWidth/tabInd.getWidth()+position*indWidth, 0, 0, 0);
			tabInd.invalidate();
		}
	}
	
	
 }