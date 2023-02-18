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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.Date;

public class ExpensesList extends AppCompatActivity implements OnItemClickListener, OnItemLongClickListener, NavigationView.OnNavigationItemSelectedListener {
    private static final int NUMBER_OF_ITEMS = 40;

    private App app;
    private App.SpinnerMenu sMenu;
    private ArrayList<Long> selectedIds;
    private boolean pickMode = false, searchMode = false;
    private ActionMode.Callback mActionModeCallback;
    private ActionMode mActionMode;
    private ListView listView;
    private ExAdapter adapterListView;
    private boolean creating = true;
    private Runnable menuCallback = new Runnable() {
        public void run() {
            numberOfItems = NUMBER_OF_ITEMS;
            renderList();
        }
    };
    private int numberOfItems = NUMBER_OF_ITEMS;
    private View footer, header;
    private long filterId;
    private Date filterDate;
    private SharedPreferences prefs;
    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawerLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.expenseslist);

        app = (App) getApplication();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        selectedIds = new ArrayList<>();

        mActionModeCallback = new RExActionModeCallback();

        listView = (ListView) findViewById(R.id.listView1);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);

        View emptyView = findViewById(R.id.empty);
        ((TextView) emptyView.findViewById(R.id.textView1)).setText(R.string.expenseslist_c2);
        listView.setEmptyView(emptyView);

        footer = LayoutInflater.from(this).inflate(R.layout.expenseslist_footer, null);
        footer.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                numberOfItems += NUMBER_OF_ITEMS;
                renderList();
            }
        });
        listView.addFooterView(footer);

        findViewById(R.id.addExpenseButton).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(ExpensesList.this, AddEx.class);
                intent.putExtra("EDIT_MODE", false);
                startActivity(intent);
            }
        });

        Bundle bd = getIntent().getExtras();
        if (bd != null) {
            filterId = bd.getLong("FILTER_ID", -1);
            if (filterId >= 0) {
                filterDate = (Date) bd.get("FILTER_DATE");
                header = LayoutInflater.from(this).inflate(R.layout.expenseslist_header, null);
                header.setOnClickListener(null);
                listView.addHeaderView(header);
            }
        } else {
            filterId = -1;
        }

        // Set menu listener
        NavigationView navigationView = (NavigationView) findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(this);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.expenseslist_c3, R.string.expenseslist_c4) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };
        drawerLayout.addDrawerListener(drawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        if (app.showChangesDialog) {
            ChangesDialog cdg = new ChangesDialog();
            cdg.show(getSupportFragmentManager(), null);
            SharedPreferences.Editor pEditor = prefs.edit();
            pEditor.putInt("APP_VERSION", app.appVersion);
            pEditor.apply();
            app.showChangesDialog = false;
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.expenseslist, menu);

        sMenu = app.new SpinnerMenu(this, menuCallback, true);

        Bundle options = getIntent().getExtras();
        long newGroupId;
        if (options != null) {
            newGroupId = options.getLong("SET_GROUP_ID", -1);
            if (newGroupId >= 0 && newGroupId != app.activeGroupId)
                sMenu.setSelectedById(newGroupId);
        }

        sMenu.getEditText().setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    renderList();
                    return true;
                }
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        EditText et = sMenu.getEditText();
        if (item.getItemId() == R.id.item1) {
            if (!searchMode) {
                sMenu.getSpinner().setVisibility(View.GONE);
                et.setVisibility(View.VISIBLE);
                et.requestFocus();
                imm.showSoftInput(et, 0);
                item.setIcon(R.drawable.x_white);
                ((TextView) listView.getEmptyView().findViewById(R.id.textView1)).setText(R.string.expenseslist_c5);
                item.setTitle(R.string.menu_expenseslist_2);
            } else {
                sMenu.getSpinner().setVisibility(View.VISIBLE);
                et.setVisibility(View.GONE);
                et.setText("");
                imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
                item.setIcon(R.drawable.search_white);
                ((TextView) listView.getEmptyView().findViewById(R.id.textView1)).setText(R.string.expenseslist_c2);
                item.setTitle(R.string.menu_expenseslist_1);
                renderList();
            }
            searchMode = !searchMode;
        }

        return true;
    }

    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_item_edit_groups) {
            Intent intent2 = new Intent(this, EditGroups.class);
            startActivity(intent2);
        }
        else if (id == R.id.menu_item_edit_categories) {
            Intent intent3 = new Intent(this, EditCategories.class);
            startActivity(intent3);
        }
        else if (id == R.id.menu_item_stats_category) {
            Intent intent4 = new Intent(this, CategoryStats.class);
            startActivity(intent4);
        }
        else if (id == R.id.menu_item_stats_time) {
            Intent intent5 = new Intent(this, TimeStats.class);
            startActivity(intent5);
        }
        else if (id == R.id.menu_item_prefs) {
            Intent intent6 = new Intent(this, EditPreferences.class);
            startActivity(intent6);
        }
        else if (id == R.id.menu_item_export_data) {
            Intent intent8 = new Intent(this, ExportData.class);
            startActivity(intent8);
        }
        else if (id == R.id.menu_item_about) {
            Intent intent9 = new Intent(this, About.class);
            startActivity(intent9);
        }
        else if (id == R.id.menu_item_budget) {
            Intent intent10 = new Intent(this, Budget.class);
            startActivity(intent10);
        }
        else if (id == R.id.menu_item_stats_group) {
            Intent intent11 = new Intent(this, GroupStats.class);
            startActivity(intent11);
        }
        drawerLayout.closeDrawers();
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mActionMode != null) mActionMode.finish();
        if (!creating) {
            if (app.mnUpdateList)
                renderList();
            if (app.mnUpdateMenu)
                sMenu.renderMenu();
            if (sMenu.getSpinner().getSelectedItemPosition() != app.activeGroupPos)
                sMenu.getSpinner().setSelection(app.activeGroupPos);
        }
        app.mnUpdateList = false;
        app.mnUpdateMenu = false;
        creating = false;

        //Verify auto backup
        if (prefs.getBoolean("BACKUP_AUTO", false)) {
            int days;
            if (prefs.getString("BACKUP_AUTO_INT", "M").equals("M"))
                days = 30;
            else
                days = 7;

            if ((new Date()).getTime() > (prefs.getLong("BACKUP_TIME", 0) + (long) days * 1000 * 60 * 60 * 24)) {
                //Backup procedure
                try {
                    BackupManager mng = new BackupManager(this);
                    mng.backupToStorage();
                } catch (BackupManager.NoStoragePermissionException e) {
                    App.Log("Auto backup is configured, but storage permission is not granted");
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mActionMode != null) mActionMode.finish();
    }

    @Override
    public void onStop() {
        super.onStop();
        SharedPreferences.Editor pEdit = prefs.edit();
        pEdit.putInt("ACTIVE_GROUP_POS", app.activeGroupPos);
        pEdit.putLong("ACTIVE_GROUP_ID", app.activeGroupId);
        pEdit.apply();
    }

    public void renderList() {
        SQLiteDatabase db = DatabaseHelper.quickDb(this, 0);
        String queryModifier = "", queryModifier2 = "", queryModifier3 = "";
        if (filterId != -1)
            queryModifier = " AND " + Db.Table1.T_ID_CATEGORY + " = " + filterId;
        if (filterDate != null)
            queryModifier2 = " AND strftime('%Y-%m'," + Db.Table1.T_DATE + ") = '" + App.dateToDb("yyyy-MM", filterDate) + "'";
        if (searchMode) {
            String[] words = sMenu.getEditText().getText().toString().split(" ");
            if (words.length > 0) {
                queryModifier3 = " AND (";
                for (String w : words) {
                    String wEscaped = DatabaseUtils.sqlEscapeString('%' + w + '%');
                    queryModifier3 += Db.Table1.T_DETAILS + " LIKE " + wEscaped + " OR ";
                    queryModifier3 += Db.Table1.T_AMOUNT + " LIKE " + wEscaped + " OR ";
                    queryModifier3 += Db.Table2.CATEGORY_NAME + " LIKE " + wEscaped + " OR ";
                }
                queryModifier3 = queryModifier3.substring(0, queryModifier3.length() - 3);
                queryModifier3 += ")";
            }
        }

        Cursor c = db.rawQuery("SELECT " +
                Db.Table2.TABLE_NAME + "." + Db.Table2.CATEGORY_NAME + "," +
                Db.Table2.TABLE_NAME + "." + Db.Table2.CATEGORY_COLOR + "," +
                Db.Table1.TABLE_NAME + "." + Db.Table1.AMOUNT + "," +
                Db.Table1.TABLE_NAME + "." + Db.Table1.DATE + "," +
                Db.Table1.TABLE_NAME + "." + Db.Table1.DETAILS + "," +
                Db.Table1.TABLE_NAME + "." + Db.Table1._ID +
                " FROM " +
                Db.Table1.TABLE_NAME + "," +
                Db.Table2.TABLE_NAME +
                " WHERE " +
                Db.Table1.TABLE_NAME + "." + Db.Table1.ID_GROUP + " = " + app.activeGroupId +
                " AND " + Db.Table1.TABLE_NAME + "." + Db.Table1.ID_CATEGORY + " = " + Db.Table2.TABLE_NAME + "." + Db.Table2._ID + queryModifier + queryModifier2 + queryModifier3 +
                " ORDER BY " +
                Db.Table1.TABLE_NAME + "." + Db.Table1.DATE + " DESC, " +
                Db.Table1.TABLE_NAME + "." + Db.Table1._ID + " DESC" +
                " LIMIT " + numberOfItems, null);

        if (c.getCount() > 0 && filterId != -1) {
            c.moveToFirst();
            String text = c.getString(0);
            if (filterDate != null)
                text = text + ", " + App.dateToUser("MMMM / yyyy", filterDate);
            ((TextView) header.findViewById(R.id.textView2)).setText(text);
        }

        if (c.getCount() < numberOfItems)
            footer.setVisibility(View.GONE);
        else
            footer.setVisibility(View.VISIBLE);

        if (adapterListView == null) {
            adapterListView = new ExAdapter(this, c);
            listView.setAdapter(adapterListView);
        } else {
            adapterListView.changeCursor(c);
            adapterListView.notifyDataSetChanged();
        }

        db.close();
    }

    private class ExAdapter extends CursorAdapter {
        private LayoutInflater mInflater;

        public ExAdapter(Context context, Cursor c) {
            super(context, c, false);
            mInflater = LayoutInflater.from(context);
        }

        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mInflater.inflate(R.layout.expenseslist_listitem, parent, false);
        }

        public void bindView(View view, Context context, Cursor cursor) {
            ((TextView) view.findViewById(R.id.textView1)).setText(cursor.getString(0));
            ((ImageView) view.findViewById(R.id.imageView1)).getDrawable().setColorFilter(cursor.getInt(1), App.colorFilterMode);
            ((TextView) view.findViewById(R.id.textView2)).setText(app.printMoney(cursor.getFloat(2)));
            ((TextView) view.findViewById(R.id.textView3)).setText(App.dateToUser(null, cursor.getString(3)));
            TextView tvObs = ((TextView) view.findViewById(R.id.textView4));
            String obs = cursor.getString(4);
            if (obs.isEmpty())
                tvObs.setVisibility(View.GONE);
            else {
                tvObs.setText(obs);
                tvObs.setVisibility(View.VISIBLE);
            }

            if (selectedIds.contains(cursor.getLong(5)))
                selectItem(view, -1);
            else
                unselectItem(view, -1);
        }
    }

    private class RExActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.expenseslist_actionmode, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.item1) {
                deleteExs(selectedIds);
                renderList();
                mode.finish();
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            pickMode = false;
            clearSelections();
            mActionMode = null;
        }
    }

    private void selectItem(View v, long id) {
        if (id >= 0)
            selectedIds.add(id);

        v.setBackgroundColor(getResources().getColor(R.color.gray));
    }

    private void unselectItem(View v, long id) {
        if (id >= 0)
            selectedIds.remove(selectedIds.indexOf(id));

        v.setBackgroundResource(R.drawable.statelist_normal);
        if (mActionMode != null && selectedIds.isEmpty())
            mActionMode.finish();
    }

    private void clearSelections() {
        int i, max, start;
        if (!selectedIds.isEmpty()) {
            max = listView.getChildCount();
            if (listView.getHeaderViewsCount() == 0)
                start = 0;
            else if (listView.getFirstVisiblePosition() == 0)
                start = 1;
            else
                start = 0;
            for (i = start; i < max; i++)
                unselectItem(listView.getChildAt(i), -1);
            selectedIds.clear();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        if (pickMode) {
            if (!selectedIds.contains(id))
                selectItem(v, id);
            else
                unselectItem(v, id);
        } else {
            Intent intent = new Intent(ExpensesList.this, AddEx.class);
            Bundle args = new Bundle();
            args.putBoolean("EDIT_MODE", true);
            args.putLong("EM_ID", id);
            intent.putExtras(args);
            startActivity(intent);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (!pickMode) {
            pickMode = true;
            selectItem(view, id);
            mActionMode = startSupportActionMode(mActionModeCallback);
        }
        return true;
    }

    private void deleteExs(ArrayList<Long> ids) {
        SQLiteDatabase db = DatabaseHelper.quickDb(this, DatabaseHelper.MODE_WRITE);
        int i;

        for (i = 0; i < ids.size(); i++)
            db.delete(Db.Table1.TABLE_NAME, Db.Table1._ID + " = " + ids.get(i), null);

        app.setFlag(1);

        db.close();
    }

    public static class ChangesDialog extends DialogFragment implements DialogInterface.OnClickListener {

        public Dialog onCreateDialog(Bundle savedInstance) {
            FragmentActivity act = getActivity();

            LayoutInflater li = act.getLayoutInflater();
            View layout = li.inflate(R.layout.expenseslist_changesdialog, null);

            return new AlertDialog.Builder(act)
                    .setView(layout)
                    .setTitle(R.string.expenseslist_c1)
                    .setPositiveButton(R.string.gp_2, this)
                    .create();
        }

        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE)
                dismiss();
        }
    }
}
