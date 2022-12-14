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

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.dpcsoftware.views.HSVColorPicker;

public class EditCategoryActivity extends AppCompatActivity {
    public static final int ADD = 0, EDIT = 1;
    private int mode, selectedColor;
    private long editId;
    private int[] colors = {R.color.c0, R.color.c1,
            R.color.c2, R.color.c3,
            R.color.c4, R.color.c5,
            R.color.c6, R.color.c7,
            R.color.c8, R.color.c9,
            R.color.c10, R.color.c11,
            R.color.c12, R.color.c13,
            R.color.c14, R.color.c15};
    private ImageView target;
    private ViewGroup colorList;
    private HSVColorPicker picker;
    private App app;
    private boolean comingFromAddEx = false;

    private View.OnClickListener selectColorListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            target.getDrawable().clearColorFilter();
            selectedColor = getResources().getColor(colors[colorList.indexOfChild(v)]);
            target.getDrawable().setColorFilter(selectedColor, App.colorFilterMode);
            target.invalidate();
            picker.setColor(selectedColor);
        }
    };

    private HSVColorPicker.OnColorChangeListener hsvColorListener = new HSVColorPicker.OnColorChangeListener() {
        @Override
        public void onColorChange(int color) {
            target.getDrawable().clearColorFilter();
            target.getDrawable().setColorFilter(color, App.colorFilterMode);
            target.invalidate();
            selectedColor = color;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (App) getApplication();

        setContentView(R.layout.editcategoryactivity);
        picker = ((HSVColorPicker) findViewById(R.id.hsvColorPicker));
        picker.setOnColorChangeListener(hsvColorListener);
        target = (ImageView) findViewById(R.id.imageView1);

        colorList = (ViewGroup) findViewById(R.id.colorList);
        int i, margin = app.dpToPx(4);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ViewGroup.MarginLayoutParams mParams = new ViewGroup.MarginLayoutParams(lp);
        mParams.setMargins(margin, margin, margin, margin);
        for (i = 0; i < 16; i++) {
            ImageButton item = new ImageButton(this, null, R.style.MN_Widget_ImageButton);
            item.setImageResource(R.drawable.square_shape_big);
            item.getDrawable().setColorFilter(getResources().getColor(colors[i]), App.colorFilterMode);
            item.setLayoutParams(mParams);
            item.setOnClickListener(selectColorListener);
            colorList.addView(item);
        }

        EditText text = (EditText) findViewById(R.id.editText1);
        text.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (((EditText) findViewById(R.id.editText1)).getText().toString().equals(""))
                        App.Toast(EditCategoryActivity.this, R.string.editcategoryactivity_c5);
                    else
                        saveCategory();
                    return true;
                }
                return false;
            }
        });

        Bundle args = getIntent().getExtras();
        mode = args.getInt("MODE", ADD);
        comingFromAddEx = args.getBoolean("FROM_ADDEX", false);

        int titleResource;
        if (mode == ADD) {
            titleResource = R.string.editcategoryactivity_c2;
            colorList.getChildAt(0).performClick();
        } else {
            titleResource = R.string.editcategoryactivity_c1;
            editId = args.getLong("EDIT_ID");
            selectedColor = args.getInt("CURRENT_COLOR");
            target.getDrawable().setColorFilter(selectedColor, App.colorFilterMode);
            picker.setColor(selectedColor);
            text.setText(args.getString("CURRENT_NAME"));
        }

        getSupportActionBar().setTitle(titleResource);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.editcategoryactivity, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item1:
                if (((EditText) findViewById(R.id.editText1)).getText().toString().equals(""))
                    App.Toast(this, R.string.editcategoryactivity_c5);
                else
                    saveCategory();
                break;
        }
        return true;
    }

    private void saveCategory() {
        SQLiteDatabase db = DatabaseHelper.quickDb(this, DatabaseHelper.MODE_WRITE);
        ContentValues cv = new ContentValues();
        cv.put(Db.Table2.CATEGORY_NAME, ((EditText) findViewById(R.id.editText1)).getText().toString());
        cv.put(Db.Table2.CATEGORY_COLOR, selectedColor);
        long result;
        if (mode == EDIT)
            result = db.update(Db.Table2.TABLE_NAME, cv, Db.Table2._ID + " = " + editId, null);
        else
            result = db.insert(Db.Table2.TABLE_NAME, null, cv);
        int toastText;
        if ((mode == EDIT && result == 1) || (mode == ADD && result != -1)) {
            if (mode == EDIT)
                toastText = R.string.editcategoryactivity_c3;
            else
                toastText = R.string.editcategoryactivity_c4;
            app.setFlag(2);
            if (comingFromAddEx) {
                app.addExUpdateCategoryId = result;
                app.addExUpdateCategories = true;
            } else
                app.editCategoriesUpdateList = true;
            finish();
        } else
            toastText = R.string.gp_11;
        App.Toast(this, toastText);
        db.close();
    }
}
