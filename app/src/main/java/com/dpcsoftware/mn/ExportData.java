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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class ExportData extends ActionBarActivity implements View.OnClickListener, FileFilter {
	private static final int REQUEST_STD_FOLDER = 1;
	
	private SharedPreferences prefs;
	private Resources r;
	private App app;
	private String stdAppFolder;
	private ListView lv;
	private View header;
	private Comparator<? super File> filecomparator = new Comparator<File>(){
    	public int compare(File file1, File file2) {
    		long t1 = file1.lastModified();
    		long t2 = file2.lastModified();
    		
    		if(t1 > t2)
    			return -1;
    		else if(t1 < t2)
    			return 1;
    		else
    			return 0;
    	}	    	
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {    
	    super.onCreate(savedInstanceState);
	    
	    prefs = PreferenceManager.getDefaultSharedPreferences(this);
	    r = getResources();
	    app = (App) getApplication();
	    
	    stdAppFolder = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + r.getString(R.string.app_name);
	    
	    setContentView(R.layout.exportdata_list);
	    
	    header = getLayoutInflater().inflate(R.layout.exportdata, null);
	    lv = (ListView) findViewById(R.id.listView1);
	    lv.addHeaderView(header);
	    
        header.findViewById(R.id.button1).setOnClickListener(this);
        header.findViewById(R.id.button2).setOnClickListener(this);
        Button bt3 = ((Button) header.findViewById(R.id.button3));
        bt3.setOnClickListener(this);
        String path = prefs.getString("STD_FOLDER", stdAppFolder);
        bt3.setText(path.substring(path.lastIndexOf("/")+1));
        
	    getSupportActionBar().setTitle(R.string.exportdata_c1);
	    
	    renderList();
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button1:
			backupDb();
			break;
		case R.id.button2:
			exportODS();
			break;
		case R.id.button3:
			Intent intentFolder = new Intent(this, FolderPicker.class);
			intentFolder.putExtra("START_FOLDER",prefs.getString("STD_FOLDER", ""));
			startActivityForResult(intentFolder, REQUEST_STD_FOLDER);
			break;
		}
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
	    if(resultCode != -1)
	    	return;
	    
	    String path = data.getExtras().getString("PATH");
		
		switch (requestCode) {
	    case REQUEST_STD_FOLDER:	    	
	    	SharedPreferences.Editor pEdit = prefs.edit();
	    	pEdit.putString("STD_FOLDER", path);
	    	pEdit.apply();
	    	((Button) header.findViewById(R.id.button3)).setText(path.substring(path.lastIndexOf("/")+1));
	    	renderList();
	    	break;
    	}
	}
	
	private void backupDb() {
		try {
	    	if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
	    		App.Toast(this, R.string.exportdata_c2);
	    		return;
	    	}
	    	
	    	File destDir = new File(prefs.getString("STD_FOLDER",stdAppFolder));
	    	if(!destDir.exists()) {
                boolean tryDir = destDir.mkdirs();
                if (!tryDir)
                    throw new IOException();
            }
	    		    		
			String destName;
	    	if(prefs.getBoolean("BACKUP_OVERRIDE_OLD", false))
	    		destName = r.getString(R.string.app_name) + ".backup";
	    	else
	    		destName = r.getString(R.string.app_name) + "_" + App.dateToUser("yyyy-MM-dd_HH-mm", new Date()) + ".backup";
	    		
	    	
			boolean tryCopy = App.copyFile(getDatabasePath(DatabaseHelper.DATABASE_NAME).getAbsolutePath(), destDir.getAbsolutePath() + "/" + destName);

			if (tryCopy) {
				SharedPreferences.Editor pEdit = prefs.edit();
				pEdit.putLong("BACKUP_TIME", (new Date().getTime()));
				pEdit.apply();
				App.Toast(this, R.string.exportdata_c8);
				renderList();
			}
			else {
				App.Toast(this, R.string.exportdata_c7);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			App.Toast(this, R.string.exportdata_c7);
		}
	}
	
	private void restoreDb(File source) {
		try {
	    	if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
	    		App.Toast(this, R.string.exportdata_c2);
	    		return;
	    	}
	    	
	    	boolean tryCopy = App.copyFile(source.getAbsolutePath(),getDatabasePath(DatabaseHelper.DATABASE_NAME).getAbsolutePath());
	    	
	    	if(tryCopy) {
				App.Toast(this, R.string.exportdata_c9);
				app.setFlag(1);
				app.setFlag(2);
				app.setFlag(3);
                app.setFlag(4);
	    	}
	    	else
	    		App.Toast(this, R.string.exportdata_c10);
	    		
		}
		catch (Exception e) {
			e.printStackTrace();
			App.Toast(this, R.string.exportdata_c10);
		}
	}
	
	private void renderList() {
		if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
    		App.Toast(this, R.string.exportdata_c2);
    		return;
    	}
		
		List<File> fList;
		
		File dir = new File(prefs.getString("STD_FOLDER", stdAppFolder));
        if(!dir.exists()) {
            boolean tryDir = dir.mkdirs();
            if(!tryDir) {
                App.Toast(this, R.string.exportdata_c2);
                return;
            }
        }

        fList = Arrays.asList(dir.listFiles(this));
        Collections.sort(fList, filecomparator);

        FileListAdapter adapter = new FileListAdapter(fList);
        lv.setAdapter(adapter);
	}
	
	//FileFilter
	public boolean accept(File f) {
		String fName = f.getName();
		return (!f.isDirectory() && (fName.endsWith(".backup") || fName.endsWith(".ods")));
	}
	
	private class FileListAdapter extends ArrayAdapter<File> implements OnClickListener {
		private int clickedIndex;
		
		public FileListAdapter(List<File> fItems) {
			super(getApplicationContext(),R.layout.exportdata_listitem,fItems);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			File item = getItem(position);
			View v;
			if(convertView == null) {
				v = getLayoutInflater().inflate(R.layout.exportdata_listitem, parent, false);
				v.findViewById(R.id.imageButton1).setOnClickListener(this);
				v.findViewById(R.id.imageButton2).setOnClickListener(this);
				v.findViewById(R.id.imageButton3).setOnClickListener(this);
				v.findViewById(R.id.imageButton4).setOnClickListener(this);
			}
			else
				v = convertView;
			
			v.findViewById(R.id.imageButton1).setTag(position);
			v.findViewById(R.id.imageButton2).setTag(position);
			v.findViewById(R.id.imageButton3).setTag(position);
			v.findViewById(R.id.imageButton4).setTag(position);
			
			if(item.getName().endsWith(".backup"))
				v.findViewById(R.id.imageButton3).setVisibility(View.VISIBLE);
			else
				v.findViewById(R.id.imageButton3).setVisibility(View.GONE);
			
			if(item.getName().endsWith(".ods"))
				v.findViewById(R.id.imageButton4).setVisibility(View.VISIBLE);
			else
				v.findViewById(R.id.imageButton4).setVisibility(View.GONE);
			
			((TextView) v.findViewById(R.id.textView1)).setText(item.getName());
			
			return v;
		}
		
		@Override
		public void onClick(View v) {
			clickedIndex = (Integer) v.getTag();
			switch(v.getId()) {
			case R.id.imageButton1:
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(ExportData.this);
				dialogBuilder.setMessage(R.string.exportdata_c11);
				dialogBuilder.setPositiveButton(R.string.exportdata_c5, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						getItem(clickedIndex).delete();
						renderList();
					}
				});
				dialogBuilder.setNegativeButton(R.string.exportdata_c6, null);
				dialogBuilder.create().show();
				break;
			case R.id.imageButton2:
				Intent intentApp = new Intent(android.content.Intent.ACTION_SEND);
				File f = getItem(clickedIndex);
				if(f.getName().endsWith(".backup"))
					intentApp.setType("application/x-sqlite3");	
				else if(f.getName().endsWith(".ods"))
					intentApp.setType("application/vnd.oasis.opendocument.spreadsheet");						
				intentApp.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				Uri uri = Uri.fromFile(f);
				intentApp.putExtra(Intent.EXTRA_STREAM, uri);
			    startActivity(Intent.createChooser(intentApp, r.getString(R.string.exportdata_c12)));
				break;
			case R.id.imageButton3:
				AlertDialog.Builder dgBuilder = new AlertDialog.Builder(ExportData.this);
				dgBuilder.setMessage(R.string.exportdata_c3);
				dgBuilder.setTitle(R.string.exportdata_c4);
				dgBuilder.setPositiveButton(R.string.exportdata_c5, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						restoreDb(getItem(clickedIndex));
					}
				});
				dgBuilder.setNegativeButton(R.string.exportdata_c6, null);
				dgBuilder.create().show();
				break;
			case R.id.imageButton4:
				Intent intentApp2 = new Intent(android.content.Intent.ACTION_VIEW);
				intentApp2.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				Uri uri2 = Uri.fromFile(getItem(clickedIndex));
				intentApp2.setDataAndType(uri2, "application/vnd.oasis.opendocument.spreadsheet");
			    startActivity(Intent.createChooser(intentApp2, r.getString(R.string.exportdata_c12)));
			}
		}
	}
	
	private void exportODS() {
		try {
			if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
	    		App.Toast(this, R.string.exportdata_c2);
	    		return;
	    	}
	    	
	    	File destDir = new File(prefs.getString("STD_FOLDER",stdAppFolder));
	    	if(!destDir.exists()) {
                boolean tryDir = destDir.mkdirs();
                if (!tryDir)
                    throw new IOException();
            }
	    	
	    	String destName = r.getString(R.string.exportdata_c17) + "_" + r.getString(R.string.app_name) + "_" + App.dateToUser("yyyy-MM-dd_HH-mm", new Date()) + ".ods";
	    		    	
	    	FileOutputStream destFile = new FileOutputStream(destDir.getAbsolutePath() + "/" + destName); 
	    	ZipOutputStream ods = new ZipOutputStream(new BufferedOutputStream(destFile));
	    	
	    	//mimetype
	    	ods.putNextEntry(new ZipEntry("mimetype"));
	    	writeString(ods, "application/vnd.oasis.opendocument.spreadsheet");
	    	ods.closeEntry();
	    	
	    	//manifest
	    	ods.putNextEntry(new ZipEntry("META-INF/manifest.xml"));
	    	writeString(ods, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
	    			+ "<manifest:manifest xmlns:manifest=\"urn:oasis:names:tc:opendocument:xmlns:manifest:1.0\" manifest:version=\"1.2\">"
	    			+ "<manifest:file-entry manifest:full-path=\"/\" manifest:media-type=\"application/vnd.oasis.opendocument.spreadsheet\" manifest:version=\"1.2\" />"
	    			+ "<manifest:file-entry manifest:full-path=\"content.xml\" manifest:media-type=\"text/xml\"/>"
	    			+ "<manifest:file-entry manifest:full-path=\"styles.xml\" manifest:media-type=\"text/xml\"/>"
	    			+ "<manifest:file-entry manifest:full-path=\"meta.xml\" manifest:media-type=\"text/xml\"/>"
	    			+ "</manifest:manifest>");
	    	ods.closeEntry();
	    	
	    	//meta
	    	ods.putNextEntry(new ZipEntry("meta.xml"));
	    	writeString(ods, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
	    			+ "<office:document-meta xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" "
	    			+ "xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
	    			+ "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" "
	    			+ "xmlns:meta=\"urn:oasis:names:tc:opendocument:xmlns:meta:1.0\" "
	    			+ "xmlns:ooo=\"http://openoffice.org/2004/office\" "
	    			+ "xmlns:grddl=\"http://www.w3.org/2003/g/data-view#\" office:version=\"1.2\">"
	    			+ "</office:document-meta>");
	    	ods.closeEntry();
	    	
	    	//styles
	    	ods.putNextEntry(new ZipEntry("styles.xml"));
	    	writeString(ods, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
	    			+ "<office:document-styles xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" "
	    			+ "xmlns:style=\"urn:oasis:names:tc:opendocument:xmlns:style:1.0\" "
	    			+ "xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\" "
	    			+ "xmlns:table=\"urn:oasis:names:tc:opendocument:xmlns:table:1.0\" "
	    			+ "xmlns:draw=\"urn:oasis:names:tc:opendocument:xmlns:drawing:1.0\" "
	    			+ "xmlns:fo=\"urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0\" "
	    			+ "xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
	    			+ "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" "
	    			+ "xmlns:meta=\"urn:oasis:names:tc:opendocument:xmlns:meta:1.0\" "
	    			+ "xmlns:number=\"urn:oasis:names:tc:opendocument:xmlns:datastyle:1.0\" "
	    			+ "xmlns:presentation=\"urn:oasis:names:tc:opendocument:xmlns:presentation:1.0\" "
	    			+ "xmlns:svg=\"urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0\" "
	    			+ "xmlns:chart=\"urn:oasis:names:tc:opendocument:xmlns:chart:1.0\" "
	    			+ "xmlns:dr3d=\"urn:oasis:names:tc:opendocument:xmlns:dr3d:1.0\" "
	    			+ "xmlns:math=\"http://www.w3.org/1998/Math/MathML\" "
	    			+ "xmlns:form=\"urn:oasis:names:tc:opendocument:xmlns:form:1.0\" "
	    			+ "xmlns:script=\"urn:oasis:names:tc:opendocument:xmlns:script:1.0\" "
	    			+ "xmlns:ooo=\"http://openoffice.org/2004/office\" "
	    			+ "xmlns:ooow=\"http://openoffice.org/2004/writer\" "
	    			+ "xmlns:oooc=\"http://openoffice.org/2004/calc\" "
	    			+ "xmlns:dom=\"http://www.w3.org/2001/xml-events\" "
	    			+ "xmlns:rpt=\"http://openoffice.org/2005/report\" "
	    			+ "xmlns:of=\"urn:oasis:names:tc:opendocument:xmlns:of:1.2\" "
	    			+ "xmlns:xhtml=\"http://www.w3.org/1999/xhtml\" "
	    			+ "xmlns:grddl=\"http://www.w3.org/2003/g/data-view#\" "
	    			+ "xmlns:tableooo=\"http://openoffice.org/2009/table\" "
	    			+ "xmlns:drawooo=\"http://openoffice.org/2010/draw\" "
	    			+ "xmlns:calcext=\"urn:org:documentfoundation:names:experimental:calc:xmlns:calcext:1.0\" "
	    			+ "xmlns:css3t=\"http://www.w3.org/TR/css3-text/\" office:version=\"1.2\">");
	    	writeString(ods, "<office:styles>"
	    			+ "<number:date-style style:name=\"nsDate\" number:automatic-order=\"true\">"
	    			+ "<number:day number:style=\"long\"/>"
	    			+ "<number:text>/</number:text>"
	    			+ "<number:month number:style=\"long\"/>"
	    			+ "<number:text>/</number:text>"
	    			+ "<number:year/>"
	    			+ "</number:date-style>");
	    	writeString(ods, "<number:currency-style style:name=\"nsCurrency\" style:volatile=\"true\">");
	    	String currencySymbol = prefs.getString("currencySymbol",r.getString(R.string.standard_currency));
	    	int nFractionDigits = Currency.getInstance(Locale.getDefault()).getDefaultFractionDigits();
	    	if(prefs.getBoolean("cSymbolBefore",r.getBoolean(R.bool.standard_currency_pos))) {
				writeString(ods, "<number:currency-symbol>" + currencySymbol + "</number:currency-symbol>"
						+ "<number:text> </number:text>"
						+ "<number:number number:decimal-places=\"" + nFractionDigits + "\" number:min-integer-digits=\"1\" number:grouping=\"true\"/>");
	    	}
			else {
				writeString(ods, "<number:number number:decimal-places=\"" + nFractionDigits + "\" number:min-integer-digits=\"1\" number:grouping=\"true\"/>"
						+ "<number:text> </number:text>"
						+ "<number:currency-symbol>" + currencySymbol + "</number:currency-symbol>");
			}
	    	
	    	
	    	writeString(ods, "</number:currency-style>");
	    	writeString(ods, "<style:style style:name=\"Default\" style:family=\"table-cell\">"
	    			+ "<style:text-properties style:font-name-asian=\"Droid Sans Fallback\" style:font-family-asian=\"&apos;Droid Sans Fallback&apos;\" style:font-family-generic-asian=\"system\" style:font-pitch-asian=\"variable\" style:font-name-complex=\"FreeSans\" style:font-family-complex=\"FreeSans\" style:font-family-generic-complex=\"system\" style:font-pitch-complex=\"variable\"/>"
	    			+ "</style:style>"
	    			+ "<style:style style:name=\"Normal\" style:family=\"table-cell\" style:parent-style-name=\"Default\">"
	    			+ "<style:table-cell-properties fo:border=\"0.06pt solid #000000\"/>"
	    			+ "</style:style>"
	    			+ "<style:style style:name=\"Header\" style:family=\"table-cell\" style:parent-style-name=\"Normal\">"
	    			+ "<style:table-cell-properties fo:background-color=\"#008000\" style:diagonal-bl-tr=\"none\" style:diagonal-tl-br=\"none\" style:text-align-source=\"fix\" style:repeat-content=\"false\" />"
	    			+ "<style:paragraph-properties fo:text-align=\"center\" />"
	    			+ "<style:text-properties fo:color=\"#ffffff\" fo:font-weight=\"bold\"/>"
	    			+ "</style:style>"
	    			+ "<style:style style:name=\"Date\" style:family=\"table-cell\" style:parent-style-name=\"Normal\" style:data-style-name=\"nsDate\"/>"
	    			+ "<style:style style:name=\"Money\" style:family=\"table-cell\" style:parent-style-name=\"Normal\" style:data-style-name=\"nsCurrency\"/>"
	    			+ "</office:styles>"
	    			+ "</office:document-styles>");
	    	ods.closeEntry();
	    	
	    	//content
	    	ods.putNextEntry(new ZipEntry("content.xml"));
	    	writeString(ods, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
	    			+ "<office:document-content xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" "
	    			+ "xmlns:style=\"urn:oasis:names:tc:opendocument:xmlns:style:1.0\" "
	    			+ "xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\" "
	    			+ "xmlns:table=\"urn:oasis:names:tc:opendocument:xmlns:table:1.0\" "
	    			+ "xmlns:draw=\"urn:oasis:names:tc:opendocument:xmlns:drawing:1.0\" "
	    			+ "xmlns:fo=\"urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0\" "
	    			+ "xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
	    			+ "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" "
	    			+ "xmlns:meta=\"urn:oasis:names:tc:opendocument:xmlns:meta:1.0\" "
	    			+ "xmlns:number=\"urn:oasis:names:tc:opendocument:xmlns:datastyle:1.0\" "
	    			+ "xmlns:presentation=\"urn:oasis:names:tc:opendocument:xmlns:presentation:1.0\" "
	    			+ "xmlns:svg=\"urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0\" "
	    			+ "xmlns:chart=\"urn:oasis:names:tc:opendocument:xmlns:chart:1.0\" "
	    			+ "xmlns:dr3d=\"urn:oasis:names:tc:opendocument:xmlns:dr3d:1.0\" "
	    			+ "xmlns:math=\"http://www.w3.org/1998/Math/MathML\" "
	    			+ "xmlns:form=\"urn:oasis:names:tc:opendocument:xmlns:form:1.0\" "
	    			+ "xmlns:script=\"urn:oasis:names:tc:opendocument:xmlns:script:1.0\" "
	    			+ "xmlns:ooo=\"http://openoffice.org/2004/office\" "
	    			+ "xmlns:ooow=\"http://openoffice.org/2004/writer\" "
	    			+ "xmlns:oooc=\"http://openoffice.org/2004/calc\" "
	    			+ "xmlns:dom=\"http://www.w3.org/2001/xml-events\" "
	    			+ "xmlns:xforms=\"http://www.w3.org/2002/xforms\" "
	    			+ "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
	    			+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
	    			+ "xmlns:rpt=\"http://openoffice.org/2005/report\" "
	    			+ "xmlns:of=\"urn:oasis:names:tc:opendocument:xmlns:of:1.2\" "
	    			+ "xmlns:xhtml=\"http://www.w3.org/1999/xhtml\" "
	    			+ "xmlns:grddl=\"http://www.w3.org/2003/g/data-view#\" "
	    			+ "xmlns:tableooo=\"http://openoffice.org/2009/table\" "
	    			+ "xmlns:drawooo=\"http://openoffice.org/2010/draw\" "
	    			+ "xmlns:calcext=\"urn:org:documentfoundation:names:experimental:calc:xmlns:calcext:1.0\""
	    			+ " xmlns:field=\"urn:openoffice:names:experimental:ooo-ms-interop:xmlns:field:1.0\" "
	    			+ "xmlns:formx=\"urn:openoffice:names:experimental:ooxml-odf-interop:xmlns:form:1.0\" "
	    			+ "xmlns:css3t=\"http://www.w3.org/TR/css3-text/\" office:version=\"1.2\">");
	    	writeString(ods, "<office:automatic-styles>"
	    			+ "<style:style style:name=\"co1\" style:family=\"table-column\">"
	    			+ "<style:table-column-properties fo:break-before=\"auto\" style:column-width=\"2.258cm\"/>"
	    			+ "</style:style>"
	    			+ "<style:style style:name=\"ro1\" style:family=\"table-row\">"
	    			+ "<style:table-row-properties style:row-height=\"0.427cm\" fo:break-before=\"auto\" style:use-optimal-row-height=\"true\"/>"
	    			+ "</style:style>"
	    			+ "<style:style style:name=\"ro2\" style:family=\"table-row\">"
	    			+ "<style:table-row-properties style:row-height=\"0.452cm\" fo:break-before=\"auto\" style:use-optimal-row-height=\"true\"/>"
	    			+ "</style:style>"
	    			+ "<style:style style:name=\"ta1\" style:family=\"table\" style:master-page-name=\"Default\">"
	    			+ "<style:table-properties table:display=\"true\" style:writing-mode=\"lr-tb\"/>"
	    			+ "</style:style></office:automatic-styles>");
	    	writeString(ods, "<office:body>"
	    			+ "<office:spreadsheet>");
	    	
	    	  	
	    	int iTab;
	    	SQLiteDatabase db = DatabaseHelper.quickDb(this, 0);
	    	
	    	Cursor c = db.rawQuery("SELECT " +
	    			Db.Table3._ID + "," +
	    			Db.Table3.GROUP_NAME +
	    			" FROM " +
	    			Db.Table3.TABLE_NAME, null);
	    	c.moveToFirst();
	    	
	    	for(iTab = 0;iTab < c.getCount();iTab++) {
	    		writeString(ods, "<table:table table:name=\"" + c.getString(1) + "\" table:style-name=\"ta1\">"
	    				+ "<table:table-column table:number-columns-repeated=\"5\" table:style-name=\"co1\"/>"
	    				+ "<table:table-row><table:table-cell table:number-columns-repeated=\"5\" table:style-name=\"ro1\"/></table:table-row>");
	    		writeString(ods, "<table:table-row table:style-name=\"ro1\"><table:table-cell/>");
	    		writeString(ods, "<table:table-cell table:style-name=\"Header\" office:value-type=\"string\"><text:p>" + r.getString(R.string.exportdata_c13) + "</text:p></table:table-cell>");
	    		writeString(ods, "<table:table-cell table:style-name=\"Header\" office:value-type=\"string\"><text:p>" + r.getString(R.string.exportdata_c14) + "</text:p></table:table-cell>");
	    		writeString(ods, "<table:table-cell table:style-name=\"Header\" office:value-type=\"string\"><text:p>" + r.getString(R.string.exportdata_c15) + "</text:p></table:table-cell>");
	    		writeString(ods, "<table:table-cell table:style-name=\"Header\" office:value-type=\"string\"><text:p>" + r.getString(R.string.exportdata_c16) + "</text:p></table:table-cell>");
	    		writeString(ods, "</table:table-row>");
	    		
	    		Cursor cData = db.rawQuery("SELECT " +
	    				Db.Table1.TABLE_NAME + "." + Db.Table1.DATE + "," +
	    				Db.Table2.TABLE_NAME + "." + Db.Table2.CATEGORY_NAME + "," +
	    				Db.Table1.TABLE_NAME + "." + Db.Table1.AMOUNT + "," +
	    				Db.Table1.TABLE_NAME + "." + Db.Table1.DETAILS + 
	    				" FROM " + Db.Table1.TABLE_NAME + "," + Db.Table2.TABLE_NAME +
	    				" WHERE " +
	    				Db.Table1.TABLE_NAME + "." + Db.Table1.ID_CATEGORY + " = " + Db.Table2.TABLE_NAME + "." + Db.Table2._ID +
	    				" AND " + Db.Table1.TABLE_NAME + "." + Db.Table1.ID_GROUP + " = " + c.getInt(0) +
	    				" ORDER BY " + Db.Table1.TABLE_NAME + "." + Db.Table1.DATE + " ASC", null);
	    		cData.moveToFirst();
	    		
	    		int iExp;
	    		for(iExp = 0;iExp < cData.getCount();iExp++) {
	    			writeString(ods, "<table:table-row table:style-name=\"ro1\"><table:table-cell/>");
	    			writeString(ods, "<table:table-cell table:style-name=\"Date\" office:value-type=\"date\" office:date-value=\"" + cData.getString(0) + "\"><text:p>" + App.dateToUser(null, cData.getString(0)) + "</text:p></table:table-cell>");
	    			writeString(ods, "<table:table-cell table:style-name=\"Normal\" office:value-type=\"string\"><text:p>" + cData.getString(1) + "</text:p></table:table-cell>");
	    			writeString(ods, "<table:table-cell table:style-name=\"Money\" office:value-type=\"currency\" office:value=\"" + String.format(Locale.US, "%.2f", cData.getFloat(2)) + "\"><text:p>" + app.printMoney(cData.getFloat(2)) + "</text:p></table:table-cell>");
	    			writeString(ods, "<table:table-cell table:style-name=\"Normal\" office:value-type=\"string\"><text:p>" + cData.getString(3) + "</text:p></table:table-cell>");
	    			writeString(ods, "</table:table-row>");
	    			cData.moveToNext();
	    		}

                cData.close();
	    		
	    		writeString(ods, "</table:table>");
	    		
	    		c.moveToNext();
	    	}
            c.close();
	    	
	    	writeString(ods, "<table:named-expressions/></office:spreadsheet></office:body></office:document-content>");
	    	ods.closeEntry();
	    	
	    	ods.close();
	    	
	    	renderList();
		}
		catch (Exception e) {
			e.printStackTrace();
			App.Toast(this, R.string.exportdata_c18);
		}
	}
	
	private boolean writeString(ZipOutputStream o, String sText) {
		try {
			byte[] bText = sText.getBytes();
			o.write(bText, 0, bText.length);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}