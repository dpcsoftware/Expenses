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

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CursorAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.dpcsoftware.mn.App.SpinnerMenu;

import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.Locale;
import java.util.Stack;

public class AddEx extends AppCompatActivity {
    private App app;
    private boolean editMode;
    private long editModeId;
    private Spinner cSpinner;
    private CategoryAdapter cAdapter;
    private Calendar expDate;
    private OnClickListener upDownDateListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.imageButton3)
                expDate.add(Calendar.DAY_OF_MONTH, -1);
            else
                expDate.add(Calendar.DAY_OF_MONTH, 1);

            ((TextView) findViewById(R.id.dateView)).setText(App.dateToUser("E", expDate.getTime()) + ", " + App.dateToUser(null, expDate.getTime()));
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addex);
        app = (App) getApplication();

        Bundle options = getIntent().getExtras();
        editMode = options != null && options.getBoolean("EDIT_MODE", false);

        expDate = Calendar.getInstance();

        cSpinner = ((Spinner) findViewById(R.id.spinner1));
        loadCategoryList();

        findViewById(R.id.imageButton1).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Bundle args = new Bundle();
                String value = ((EditText) findViewById(R.id.editText1)).getText().toString();
                args.putString("NUMBER", value);
                CalculatorDialog calcDialog = new CalculatorDialog();
                calcDialog.setArguments(args);
                calcDialog.show(getSupportFragmentManager(), null);
            }
        });

        findViewById(R.id.imageButton2).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent openAct = new Intent(AddEx.this, EditCategoryActivity.class);
                Bundle args = new Bundle();
                args.putBoolean("FROM_ADDEX", true);
                openAct.putExtras(args);
                startActivity(openAct);
            }
        });

        findViewById(R.id.dateView).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog.OnDateSetListener dListener = new DatePickerDialog.OnDateSetListener() {
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        expDate.set(year, monthOfYear, dayOfMonth);
                        ((TextView) findViewById(R.id.dateView)).setText(App.dateToUser("E", expDate.getTime()) + ", " + App.dateToUser(null, expDate.getTime()));
                    }
                };
                DatePickerDialog dialog = new DatePickerDialog(AddEx.this, dListener, expDate.get(Calendar.YEAR), expDate.get(Calendar.MONTH), expDate.get(Calendar.DAY_OF_MONTH));
                dialog.show();
            }
        });

        findViewById(R.id.imageButton3).setOnClickListener(upDownDateListener);
        findViewById(R.id.imageButton4).setOnClickListener(upDownDateListener);

        EditText edtValue = ((EditText) findViewById(R.id.editText1));
        edtValue.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    saveExpense();
                    return true;
                }
                return false;
            }
        });

        SQLiteDatabase db = DatabaseHelper.quickDb(this, 0);
        if (editMode) {
            editModeId = options.getLong("EM_ID");
            Cursor c2 = db.query(Db.Table1.TABLE_NAME, new String[]{Db.Table1.AMOUNT, Db.Table1.DATE, Db.Table1.DETAILS, Db.Table1.ID_CATEGORY}, Db.Table1._ID + " = " + editModeId, null, null, null, null);
            c2.moveToFirst();
            edtValue.setText(c2.getString(c2.getColumnIndexOrThrow(Db.Table1.AMOUNT)));
            edtValue.setSelection(edtValue.length());
            String[] date = c2.getString(c2.getColumnIndexOrThrow(Db.Table1.DATE)).split("-");
            expDate.set(Integer.parseInt(date[0]), Integer.parseInt(date[1]) - 1, Integer.parseInt(date[2]));
            cSpinner.setSelection(cAdapter.getPositionById(c2.getLong(c2.getColumnIndexOrThrow(Db.Table1.ID_CATEGORY))));
            ((EditText) findViewById(R.id.editText2)).setText(c2.getString(c2.getColumnIndexOrThrow(Db.Table1.DETAILS)));
        }
        db.close();

        ((TextView) findViewById(R.id.dateView)).setText(App.dateToUser("E", expDate.getTime()) + ", " + App.dateToUser(null, expDate.getTime()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.addex, menu);

        SpinnerMenu sm = app.new SpinnerMenu(this, null);

        Bundle options = getIntent().getExtras();
        long newGroupId;
        if (options != null) {
            newGroupId = options.getLong("SET_GROUP_ID", -1);
            if (newGroupId >= 0 && newGroupId != app.activeGroupId)
                sm.setSelectedById(newGroupId);
        }

        return true;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (app.addExUpdateCategories)
            loadCategoryList();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item1:
                saveExpense();
                break;
        }

        return true;
    }

    public void saveExpense() {
        String date = App.dateToDb("yyyy-MM-dd", expDate.getTime());

        EditText edtValue = ((EditText) findViewById(R.id.editText1));
        float valor;

        try {
            valor = Float.parseFloat(edtValue.getText().toString());
            if (valor == 0)
                throw new Exception();
        } catch (Exception e) {
            App.Toast(this, R.string.addex_c1);
            return;
        }

        SQLiteDatabase db = DatabaseHelper.quickDb(this, DatabaseHelper.MODE_WRITE);

        ContentValues values = new ContentValues();
        values.put(Db.Table1.AMOUNT, ((float) Math.round(valor * 100)) / 100);
        values.put(Db.Table1.DATE, date);
        values.put(Db.Table1.DETAILS, ((EditText) findViewById(R.id.editText2)).getText().toString());
        values.put(Db.Table1.ID_GROUP, app.activeGroupId);
        values.put(Db.Table1.ID_CATEGORY, cSpinner.getSelectedItemId());

        long result;
        if (editMode)
            result = db.update(Db.Table1.TABLE_NAME, values, Db.Table1._ID + " = " + editModeId, null);
        else
            result = db.insert(Db.Table1.TABLE_NAME, null, values);

        if ((editMode && result != 0) || (!editMode && result != -1)) {
            App.Toast(this, R.string.addex_c2);
            app.setFlag(1);
            finish();
        } else {
            App.Toast(this, R.string.addex_c3);
        }
        db.close();
    }

    private void loadCategoryList() {
        SQLiteDatabase db = DatabaseHelper.quickDb(this, 0);

        Cursor c = db.rawQuery(
                "SELECT " +
                        Db.Table2.TABLE_NAME + "." + Db.Table2._ID + "," +
                        Db.Table2.TABLE_NAME + "." + Db.Table2.CATEGORY_NAME + "," +
                        Db.Table2.TABLE_NAME + "." + Db.Table2.CATEGORY_COLOR + "," +
                        "count(" + Db.Table1.TABLE_NAME + "." + Db.Table1.ID_CATEGORY + ") AS frequency" +
                        " FROM " + Db.Table2.TABLE_NAME +
                        " LEFT JOIN (SELECT " + Db.Table1.ID_CATEGORY + " FROM " + Db.Table1.TABLE_NAME + " ORDER BY " + Db.Table1.DATE + " DESC LIMIT 0,100) AS " + Db.Table1.TABLE_NAME +
                        " ON " + Db.Table1.TABLE_NAME + "." + Db.Table1.ID_CATEGORY + " = " + Db.Table2.TABLE_NAME + "." + Db.Table2._ID +
                        " GROUP BY " + Db.Table2.TABLE_NAME + "." + Db.Table2._ID +
                        " ORDER BY frequency DESC", null
        );

        if (cAdapter == null) {
            cAdapter = new CategoryAdapter(this, c);
            cSpinner.setAdapter(cAdapter);
        } else {
            cAdapter.changeCursor(c);
        }

        db.close();

        if (app.addExUpdateCategories) {
            cSpinner.setSelection(cAdapter.getPositionById(app.addExUpdateCategoryId));
            app.addExUpdateCategories = false;
        }
    }

    private class CategoryAdapter extends CursorAdapter {
        private LayoutInflater mInflater;

        public CategoryAdapter(Context context, Cursor c) {
            super(context, c, false);
            mInflater = LayoutInflater.from(context);
        }

        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mInflater.inflate(R.layout.addex_category, parent, false);
        }

        public View newDropDownView(Context context, Cursor cursor, ViewGroup parent) {
            return mInflater.inflate(R.layout.addex_category_dd, parent, false);
        }

        public void bindView(View view, Context context, Cursor cursor) {
            TextView itemText = (TextView) view.findViewById(R.id.textView1);
            ImageView itemSquare = (ImageView) view.findViewById(R.id.imageView1);
            itemText.setText(cursor.getString(cursor.getColumnIndexOrThrow(Db.Table2.CATEGORY_NAME)));
            itemSquare.getDrawable().setColorFilter(cursor.getInt(cursor.getColumnIndexOrThrow(Db.Table2.CATEGORY_COLOR)), App.colorFilterMode);
        }

        public int getPositionById(long id) {
            Cursor cursor = getCursor();
            cursor.moveToFirst();
            int i;
            for (i = 0; i < cursor.getCount(); i++) {
                if (cursor.getLong(cursor.getColumnIndexOrThrow(Db.Table2._ID)) == id)
                    break;
                cursor.moveToNext();
            }
            return i;
        }
    }

    public static class CalculatorDialog extends DialogFragment implements View.OnClickListener, DialogInterface.OnClickListener {
        private EditText expression;
        private boolean calcError = false;
        private Bundle params;
        private AddEx act;
        private App app;

        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            params = getArguments();
            act = (AddEx) getActivity();
            app = (App) act.getApplication();

            LayoutInflater li = LayoutInflater.from(getActivity());
            View v = li.inflate(R.layout.addex_calculator, null);

            expression = (EditText) v.findViewById(R.id.editText1);

            String value = params.getString("NUMBER");
            if (value != null) {
                expression.setText(value);
                expression.setSelection(expression.length());
            }

            v.findViewById(R.id.button1).setOnClickListener(this);
            v.findViewById(R.id.button2).setOnClickListener(this);
            v.findViewById(R.id.button3).setOnClickListener(this);
            v.findViewById(R.id.button4).setOnClickListener(this);
            v.findViewById(R.id.button5).setOnClickListener(this);

            Dialog dg = new AlertDialog.Builder(act)
                    .setView(v)
                    .setTitle(R.string.addex_c5)
                    .setPositiveButton(R.string.gp_2, this)
                    .setNegativeButton(R.string.gp_3, null)
                    .create();

            Window dgW = dg.getWindow();
            dgW.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM|WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            dgW.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

            expression.requestFocus();

            return dg;
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button1:
                    expression.append("+");
                    break;
                case R.id.button2:
                    expression.append("-");
                    break;
                case R.id.button3:
                    expression.append("*");
                    break;
                case R.id.button4:
                    expression.append("/");
                    break;
                case R.id.button5:
                    calcError = false;
                    float result = calc(expression.getText().toString());
                    if (!calcError) {
                        expression.setText(String.format(Locale.US, "%.2f", result));
                        expression.setSelection(expression.length());
                    }
                    break;
            }

        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                calcError = false;
                float result2 = calc(expression.getText().toString());
                if (!calcError) {
                    ((EditText) act.findViewById(R.id.editText1)).setText(String.format(Locale.US, "%.2f", result2));
                    this.dismiss();
                }
            }
        }

        private float calc(String s) {
            try {
                //Tokenização e Validação
                int nOperators = 0, flagPar = 0, nNum = 0, i = 0;
                ArrayDeque<String> tokens = new ArrayDeque<String>();

                while (i < s.length()) {
                    char c = s.charAt(i);
                    if (isOther(c)) {
                        int j = i + 1;
                        while (j < s.length() && isOther(s.charAt(j)))
                            j++;
                        String num = s.substring(i, j).trim();
                        if (!num.equals("")) {
                            tokens.add(num);
                            nNum++;
                        }
                        i = j;
                    } else {
                        if (isOperator(c))
                            nOperators++;
                        else if (c == '(')
                            flagPar++;
                        else if (c == ')')
                            flagPar--;

                        if (flagPar < 0)
                            throw new Exception();

                        tokens.add(String.valueOf(c));
                        i++;
                    }
                }

                if (flagPar != 0)
                    throw new Exception();

                if (tokens.getFirst().charAt(0) == '-') {
                    nOperators--;
                    tokens.addFirst("0");
                }

                if (nNum != (nOperators + 1))
                    throw new Exception();


                //Conversão para notação pós-fixa
                Stack<String> st = new Stack<String>();
                ArrayDeque<String> postFixed = new ArrayDeque<String>();

                while (!tokens.isEmpty()) {
                    String token = tokens.getFirst();
                    char c = token.charAt(0);

                    if (isOperator(c)) {
                        while (!st.isEmpty() && priority(st.peek().charAt(0)) >= priority(c))
                            postFixed.add(st.pop());
                        st.push(token);
                    } else if (c == '(') {
                        st.push(token);
                    } else if (c == ')') {
                        while (st.peek().charAt(0) != '(')
                            postFixed.add(st.pop());
                        st.pop();
                    } else {
                        postFixed.add(token);
                    }

                    tokens.removeFirst();
                }

                while (!st.isEmpty()) {
                    postFixed.add(st.pop());
                }

                App.Log(postFixed.toString());

                //Cálculo da expressão
                Stack<Float> stcalc = new Stack<Float>();
                while (!postFixed.isEmpty()) {
                    char c = postFixed.getFirst().charAt(0);

                    if (isOperator(c)) {
                        float op1, op2;
                        op1 = stcalc.pop();
                        op2 = stcalc.pop();

                        if (c == '+')
                            stcalc.push(op2 + op1);
                        else if (c == '-')
                            stcalc.push(op2 - op1);
                        else if (c == '*')
                            stcalc.push(op2 * op1);
                        else if (c == '/')
                            stcalc.push(op2 / op1);

                        postFixed.removeFirst();
                    } else
                        stcalc.push(Float.parseFloat(postFixed.removeFirst()));
                }

                return stcalc.pop();
            } catch (Exception e) {
                App.Toast(act, R.string.addex_c4);
                calcError = true;
                return 0;
            }
        }

        private boolean isOperator(char c) {
            return (c == '+' || c == '-' || c == '*' || c == '/');
        }

        private int priority(char c) {
            if (c == '+' || c == '-')
                return 1;
            else if (c == '*' || c == '/')
                return 2;
            else
                return 0;
        }

        private boolean isOther(char c) {
            return (!isOperator(c) && c != '(' && c != ')');
        }
    }
}
