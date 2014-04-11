/*
 *   Copyright 2013, 2014 Daniel Pereira Coelho
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

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class FolderPicker extends SherlockActivity implements FileFilter {
	private Resources r;
	private File currentDir;
	private File root;
	private FolderPickerAdapter listAdapter;
	private ListView lv;
	private Comparator<? super File> filecomparator = new Comparator<File>(){
    	public int compare(File file1, File file2) {
    		if(file1.isDirectory() && file2.isFile())
    			return 1;
    		
    		if(file1.isFile() && file2.isDirectory())
    			return -1;
    			
    		return file1.getName().compareTo(file2.getName());
    	}	    	
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
    	    	
    	r = getResources();
    	
    	Bundle options = getIntent().getExtras();
    	
    	if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
    		App.Toast(this, R.string.folderpicker_c1);
    		setResult(0,new Intent());
    		this.finish();
    		return;
    	}
    	
    	root = Environment.getExternalStorageDirectory();
    	
    	if(options.containsKey("START_FOLDER")) {
	    	currentDir = new File(options.getString("START_FOLDER"));
	    	if(!currentDir.exists()) {
	    		currentDir = new File(root.getAbsolutePath() + "/" + r.getString(R.string.app_name));
	    		if(!currentDir.exists())
	    			currentDir.mkdirs();
	    	}
    	}
    	else
    		currentDir = root;
    	   	
    	setContentView(R.layout.folderpicker);
    	
    	lv = ((ListView) findViewById(R.id.listView1));
    	View header = getLayoutInflater().inflate(R.layout.folderpicker_item, null);
    	((TextView) header.findViewById(R.id.textView1)).setText(R.string.folderpicker_c5);
    	header.setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			if(currentDir.equals(root))
    				App.Toast(getApplicationContext(), R.string.folderpicker_c2);
    			else {
    				currentDir = currentDir.getParentFile();
    				renderList();
    			}    				
    		}
    	});
    	lv.addHeaderView(header);
    	
    	renderList();
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.folderpicker, menu);
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    		case R.id.item1:
    			Intent result = new Intent();
				result.putExtra("PATH", currentDir.getAbsolutePath());
				setResult(-1,result);
				App.Toast(this, R.string.folderpicker_c3);
				FolderPicker.this.finish();
    			break;
    	}
    	return true;
	}
	
	private void renderList() {
		List<File> fList;
		
		if(currentDir.equals(root))
			getSupportActionBar().setTitle(R.string.folderpicker_c4);
		else
			getSupportActionBar().setTitle(currentDir.getName());
		
		fList = Arrays.asList(currentDir.listFiles(this));
		Collections.sort(fList, filecomparator);

    	listAdapter = new FolderPickerAdapter(fList);    	
    	lv.setAdapter(listAdapter);
    	lv.setOnItemClickListener(listAdapter);
	}
	
	//FileFilter
	public boolean accept(File f) {
		if(f.getName().startsWith("."))
			return false;
		else
			return true;
	}
	
	private class FolderPickerAdapter extends ArrayAdapter<File> implements OnItemClickListener {
		
		public FolderPickerAdapter(List<File> fItems) {
			super(getApplicationContext(),R.layout.folderpicker_item,fItems);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			File item = getItem(position);
			View v;
			if(convertView == null)
				v = getLayoutInflater().inflate(R.layout.folderpicker_item, parent, false);
			else
				v = convertView;
			
			if(item.isDirectory())
				((ImageView) v.findViewById(R.id.imageView1)).setVisibility(View.VISIBLE);
			else
				((ImageView) v.findViewById(R.id.imageView1)).setVisibility(View.INVISIBLE);
			
			((TextView) v.findViewById(R.id.textView1)).setText(item.getName());
			
			return v;
		}
		
		@Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			File item = getItem(position-1);
			if(item.isDirectory()) {
				currentDir = item;
				renderList();
			}
		}
	}
}