/*
 *   Copyright 2023 Daniel Pereira Coelho
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

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ExportData extends AppCompatActivity implements View.OnClickListener {
    private SharedPreferences prefs;
    private Resources r;
    private App app;
    private Calendar sprFrom, sprTo;
    private RadioGroup radioGroupFormat;
    private ActivityResultLauncher<String> restoreLauncher;
    private ActivityResultLauncher<String> permissionLauncherBackup;
    private ActivityResultLauncher<String> permissionLauncherExport;
    private BackupManager backupManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        r = getResources();
        app = (App) getApplication();
        backupManager = new BackupManager(this);

        setContentView(R.layout.exportdata);

        sprFrom = Calendar.getInstance();
        sprTo = Calendar.getInstance();

        radioGroupFormat = findViewById(R.id.radioGroupFormat);

        findViewById(R.id.button1).setOnClickListener(this);
        findViewById(R.id.button2).setOnClickListener(this);
        findViewById(R.id.buttonRestore).setOnClickListener(this);
        findViewById(R.id.imageButton1).setOnClickListener(this);
        findViewById(R.id.imageButton2).setOnClickListener(this);
        findViewById(R.id.imageButton3).setOnClickListener(this);
        findViewById(R.id.imageButton4).setOnClickListener(this);

        // Get first expense date
        SQLiteDatabase db = DatabaseHelper.quickDb(this, DatabaseHelper.MODE_READ);
        Cursor c = db.rawQuery("SELECT " +
                Db.Table1.DATE +
                " FROM " + Db.Table1.TABLE_NAME +
                " ORDER BY " + Db.Table1.DATE + " ASC LIMIT 1", null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            String[] date = c.getString(c.getColumnIndexOrThrow(Db.Table1.DATE)).split("-");
            sprFrom.set(Integer.parseInt(date[0]), Integer.parseInt(date[1]) - 1, Integer.parseInt(date[2]));
        }
        c.close();
        db.close();

        updateDateText(1);
        updateDateText(2);

        findViewById(R.id.dateView1).setOnClickListener(v -> {
            DatePickerDialog.OnDateSetListener dListener = (view, year, monthOfYear, dayOfMonth) -> {
                sprFrom.set(year, monthOfYear, dayOfMonth);
                updateDateText(1);
            };
            DatePickerDialog dialog = new DatePickerDialog(ExportData.this, dListener, sprFrom.get(Calendar.YEAR), sprFrom.get(Calendar.MONTH), sprFrom.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });

        findViewById(R.id.dateView2).setOnClickListener(v -> {
            DatePickerDialog.OnDateSetListener dListener = (view, year, monthOfYear, dayOfMonth) -> {
                sprTo.set(year, monthOfYear, dayOfMonth);
                updateDateText(2);
            };
            DatePickerDialog dialog = new DatePickerDialog(ExportData.this, dListener, sprTo.get(Calendar.YEAR), sprTo.get(Calendar.MONTH), sprTo.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });

        // Create launcher to pick backup file to restore
        restoreLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) {
                        return;
                    }

                    AlertDialog.Builder dgBuilder = new AlertDialog.Builder(ExportData.this);
                    dgBuilder.setMessage(R.string.exportdata_c3);
                    dgBuilder.setTitle(R.string.exportdata_c4);
                    dgBuilder.setPositiveButton(R.string.exportdata_c5, (dialog, which) -> {
                        try {
                            InputStream stream = getContentResolver().openInputStream(uri);
                            restoreDb(stream);
                        }
                        catch (FileNotFoundException e) {
                            App.Toast(getApplicationContext(), R.string.exportdata_c10);
                        }
                    });
                    dgBuilder.setNegativeButton(R.string.exportdata_c6, null);
                    dgBuilder.create().show();
                }
        );

        // Create launcher to ask for storage permission (only in versions prior to Q)
        if (!App.isAndroidQOrAbove()) {
            permissionLauncherBackup = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        backupDb(ExportDataChoiceDialog.Choice.SAVE);
                    }
                }
            );

            permissionLauncherExport = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        exportFile(ExportDataChoiceDialog.Choice.SAVE);
                    }
                }
            );
        }

        App.requireNonNull(getSupportActionBar()).setTitle(R.string.exportdata_c1);
    }

    @Override
    public void onClick(View v) {
        ExportDataChoiceDialog chDg;
        int id = v.getId();
        if (id == R.id.button1) {
            chDg = new ExportDataChoiceDialog();
            chDg.setOnChosenListener(this::backupDb);
            chDg.show(getSupportFragmentManager(), null);
        }
        else if (id == R.id.button2) {
            chDg = new ExportDataChoiceDialog();
            chDg.setOnChosenListener(this::exportFile);
            chDg.show(getSupportFragmentManager(), null);
        }
        else if (id == R.id.buttonRestore) {
            restoreLauncher.launch("*/*");
        }
        else if (id == R.id.imageButton1) {
            sprFrom.add(Calendar.DAY_OF_MONTH, -1);
            updateDateText(1);
        }
        else if (id == R.id.imageButton2) {
            sprFrom.add(Calendar.DAY_OF_MONTH, 1);
            updateDateText(1);
        }
        else if (id == R.id.imageButton3) {
            sprTo.add(Calendar.DAY_OF_MONTH, -1);
            updateDateText(2);
        }
        else if (id == R.id.imageButton4) {
            sprTo.add(Calendar.DAY_OF_MONTH, 1);
            updateDateText(2);
        }
    }

    private boolean checkStoragePermission() {
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return permission == PackageManager.PERMISSION_GRANTED;
    }

    private void updateDateText(int widget) {
        if (widget == 1)
            ((TextView) findViewById(R.id.dateView1)).setText(App.dateToUser(null, sprFrom.getTime()));
        else
            ((TextView) findViewById(R.id.dateView2)).setText(App.dateToUser(null, sprTo.getTime()));
    }

    private void backupDb(ExportDataChoiceDialog.Choice choice) {
        if (choice == ExportDataChoiceDialog.Choice.SHARE) { // Share file to another app (e-mail, file sync, etc)
            backupManager.backupToShare();
        }
        else if (choice == ExportDataChoiceDialog.Choice.SAVE) { // save file in Downloads folder
            boolean success = false;
            try {
                success = backupManager.backupToStorage();
            }
            catch (BackupManager.NoStoragePermissionException e) {
                permissionLauncherBackup.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }

            if (success) {
                App.Toast(this, R.string.exportdata_c8);
            }
            else {
                App.Toast(this, R.string.exportdata_c7);
            }
        }
    }

    private void restoreDb(InputStream input) {
        boolean success = backupManager.restoreBackup(input);
        if (success) {
            App.Toast(this, R.string.exportdata_c9);
            app.setFlag(1);
            app.setFlag(2);
            app.setFlag(3);
            app.setFlag(4);
            app.setFlag(5);
        }
        else {
            App.Toast(this, R.string.exportdata_c10);
        }
    }

    private void exportFile(ExportDataChoiceDialog.Choice choice) {
        // Check date interval
        if (sprTo.before(sprFrom)) {
            App.Toast(this, R.string.exportdata_c19);
            return;
        }

        // Check format
        boolean isODS = radioGroupFormat.getCheckedRadioButtonId() == R.id.radioODS;

        // Define exported file name
        String destName = r.getString(R.string.app_name) + "_" + App.dateToUser("yyyy-MM-dd_HH-mm", new Date());
        destName += isODS ? ".ods" : ".csv";

        try {
            if (choice == ExportDataChoiceDialog.Choice.SHARE) { // share file directly with other app
                // Generate spreadsheet
                String destPath = getFilesDir() + "/exported_file";
                FileOutputStream outStream = new FileOutputStream(destPath);

                if (isODS) {
                    generateODS(outStream);
                } else {
                    generateCSV(outStream);
                }

                // Create intent to share file
                Intent intentApp = new Intent(android.content.Intent.ACTION_SEND);
                File f = new File(destPath);
                intentApp.setType(isODS ? "application/vnd.oasis.opendocument.spreadsheet" : "text/csv");
                intentApp.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                Uri uri = FileProvider.getUriForFile(getApplicationContext(), "com.dpcsoftware.fileprovider", f, destName);
                intentApp.putExtra(Intent.EXTRA_STREAM, uri);
                intentApp.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intentApp, r.getString(R.string.exportdata_c12)));
            } else { // save file in Downloads folder
                if (App.isAndroidQOrAbove()) {
                    // Use MediaStore Downloads
                    ContentResolver contentResolver = getContentResolver();
                    Uri baseUri = MediaStore.Downloads.getContentUri("external");

                    // Get file URI
                    Uri uri;
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, destName);
                    contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
                    uri = contentResolver.insert(baseUri, contentValues);

                    // Generate file
                    OutputStream outStream = contentResolver.openOutputStream(uri);
                    if (isODS) {
                        generateODS(outStream);
                    }
                    else {
                        generateCSV(outStream);
                    }
                }
                else {
                    // Generate file in downloads folder directly
                    if (!checkStoragePermission()) {
                        permissionLauncherExport.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        return;
                    }

                    if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                        App.Toast(this, R.string.exportdata_c2);
                        return;
                    }

                    File destDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    if (!destDir.exists()) {
                        boolean tryDir = destDir.mkdirs();
                        if (!tryDir)
                            throw new IOException();
                    }

                    String destPath = destDir + "/" + destName;
                    OutputStream outStream = new FileOutputStream(destPath);
                    if (isODS) {
                        generateODS(outStream);
                    }
                    else {
                        generateCSV(outStream);
                    }
                }

                App.Toast(this, R.string.exportdata_c22);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            App.Toast(this, R.string.exportdata_c18);
        }
    }

    private void generateODS(OutputStream output) {
        try {
            ZipOutputStream ods = new ZipOutputStream(new BufferedOutputStream(output));

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
            String currencySymbol = prefs.getString("currencySymbol", r.getString(R.string.standard_currency));
            int nFractionDigits = Currency.getInstance(Locale.getDefault()).getDefaultFractionDigits();
            if (prefs.getBoolean("cSymbolBefore", r.getBoolean(R.bool.standard_currency_pos))) {
                writeString(ods, "<number:currency-symbol>" + currencySymbol + "</number:currency-symbol>"
                        + "<number:text> </number:text>"
                        + "<number:number number:decimal-places=\"" + nFractionDigits + "\" number:min-integer-digits=\"1\" number:grouping=\"true\"/>");
            } else {
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

            for (iTab = 0; iTab < c.getCount(); iTab++) {
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
                        Db.Table1.T_DATE + "," +
                        Db.Table2.T_CATEGORY_NAME + "," +
                        Db.Table1.T_AMOUNT + "," +
                        Db.Table1.T_DETAILS +
                        " FROM " + Db.Table1.TABLE_NAME + "," + Db.Table2.TABLE_NAME +
                        " WHERE " + Db.Table1.T_ID_CATEGORY + " = " + Db.Table2.T_ID +
                        " AND " + Db.Table1.T_ID_GROUP + " = " + c.getInt(0) +
                        " AND " + Db.Table1.T_DATE + " >= '" + App.dateToDb("yyyy-MM-dd", sprFrom.getTime()) + "'" +
                        " AND " + Db.Table1.T_DATE + " <= '" + App.dateToDb("yyyy-MM-dd", sprTo.getTime()) + "'" +
                        " ORDER BY " + Db.Table1.TABLE_NAME + "." + Db.Table1.DATE + " ASC", null);
                cData.moveToFirst();

                int iExp;
                for (iExp = 0; iExp < cData.getCount(); iExp++) {
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
            output.flush();
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
            App.Toast(this, R.string.exportdata_c18);
        }
    }

    private void writeString(OutputStream o, String sText) throws IOException {
        byte[] bText = sText.getBytes();
        o.write(bText, 0, bText.length);
    }

    private void generateCSV(OutputStream output) {
        try {
            // Write header
            csvWriteLine(output, new String[]{
                    r.getString(R.string.exportdata_c13),
                    r.getString(R.string.exportdata_c21),
                    r.getString(R.string.exportdata_c14),
                    r.getString(R.string.exportdata_c15),
                    r.getString(R.string.exportdata_c16)
            });

            // Query data
            SQLiteDatabase db = DatabaseHelper.quickDb(this, 0);
            Cursor cData = db.rawQuery(
                "SELECT " +
                    Db.Table1.T_DATE + "," +
                    Db.Table3.T_GROUP_NAME + "," +
                    Db.Table2.T_CATEGORY_NAME + "," +
                    Db.Table1.T_AMOUNT + "," +
                    Db.Table1.T_DETAILS +
                    " FROM " + Db.Table1.TABLE_NAME +
                    " LEFT JOIN " + Db.Table2.TABLE_NAME + " ON " + Db.Table1.T_ID_CATEGORY + " = " + Db.Table2.T_ID +
                    " LEFT JOIN " + Db.Table3.TABLE_NAME + " ON " + Db.Table1.T_ID_GROUP + " = " + Db.Table3.T_ID +
                    " WHERE " + Db.Table1.T_DATE + " >= ? " +
                    " AND " + Db.Table1.T_DATE + " <= ? " +
                    " ORDER BY " + Db.Table1.T_DATE + " ASC",
                new String[]{
                    App.dateToDb("yyyy-MM-dd", sprFrom.getTime()),
                    App.dateToDb("yyyy-MM-dd", sprTo.getTime())
                });

            // Write data to file
            cData.moveToFirst();
            for (int i = 0; i < cData.getCount(); ++i) {
                csvWriteLine(output, new String[] {
                        cData.getString(0),
                        cData.getString(1),
                        cData.getString(2),
                        app.printMoney(cData.getFloat(3), false),
                        cData.getString(4)
                });
                cData.moveToNext();
            }
            cData.close();
            db.close();

            output.flush();
            output.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            App.Toast(this, R.string.exportdata_c18);
        }
    }

    private void csvWriteLine(OutputStream output, String[] values) throws IOException {
        int count = 0;
        for (String val : values) {
            val = val.replace("\"", "\"\""); // escape double-quotes
            writeString(output, "\"" + val + "\"");
            ++count;
            if (count != values.length) {
                writeString(output,",");
            }
        }
        writeString(output,"\r\n");
    }
}