package com.dpcsoftware.mn;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.preference.PreferenceManager;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

public class BackupManager {
    private Context context;
    private SharedPreferences prefs;

    public static class NoStoragePermissionException extends Exception {
    }

    public BackupManager(Context ctx) {
        context = ctx;
        prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    public void backupToShare() {
        String srcPath = getDatabasePath();
        String destName = getBackupFileName();

        // First copy file to a folder that can be exposed in an intent
        String destPath = context.getFilesDir() + "/backup";
        App.copyFile(srcPath, destPath);

        // Create intent to share file
        Intent intentApp = new Intent(android.content.Intent.ACTION_SEND);
        File f = new File(destPath);
        intentApp.setType("application/x-sqlite3");
        intentApp.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        Uri uri = FileProvider.getUriForFile(context, "com.dpcsoftware.fileprovider", f, destName);
        intentApp.putExtra(Intent.EXTRA_STREAM, uri);
        intentApp.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intentApp,
                context.getResources().getString(R.string.exportdata_c12)));
    }

    public boolean backupToStorage() throws NoStoragePermissionException {
        boolean success = false;
        String destName = getBackupFileName();
        String srcPath = getDatabasePath();

        if (App.isAndroidQOrAbove()) {
            // Use MediaStore Downloads
            ContentResolver contentResolver = context.getContentResolver();
            Uri baseUri = MediaStore.Downloads.getContentUri("external");

            // If we have to override old backups, look for an existing file in MediaStore
            boolean updateFile = false;
            int fileID = 0;
            if (prefs.getBoolean("BACKUP_OVERRIDE_OLD", false)) {
                String[] columns = new String[] {MediaStore.MediaColumns._ID};
                String where = MediaStore.MediaColumns.DISPLAY_NAME + " = ?";
                String[] args = new String[] {destName};
                Cursor c = contentResolver.query(
                        baseUri,
                        columns,
                        where,
                        args,
                        null);
                if (c.getCount() > 0) {
                    c.moveToFirst();
                    updateFile = true;
                    fileID = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID));
                }
                c.close();
            }

            // Get file URI
            Uri uri;
            if (updateFile) {
                uri = ContentUris.withAppendedId(baseUri, fileID);
            }
            else {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, destName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/x-sqlite3");

                uri = contentResolver.insert(baseUri, contentValues);
            }

            // Copy file content
            try {
                OutputStream outStream = contentResolver.openOutputStream(uri);
                FileInputStream inStream = new FileInputStream(srcPath);
                success = App.copyStream(inStream, outStream);
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
                return false;
            }
        }
        else {
            // Copy file to downloads folder directly
            if (!checkStoragePermission()) {
                throw new NoStoragePermissionException();
            }

            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                return false;
            }

            File destDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!destDir.exists()) {
                boolean tryDir = destDir.mkdirs();
                if (!tryDir)
                    return false;
            }

            success = App.copyFile(srcPath, destDir.getAbsolutePath() + "/" + destName);
        }

        if (success) {
            SharedPreferences.Editor pEdit = prefs.edit();
            pEdit.putLong("BACKUP_TIME", (new Date().getTime()));
            pEdit.apply();
            return true;
        }
        else {
            return false;
        }
    }

    public boolean restoreBackup(InputStream input) {
        // Copy stream to tmp file
        String tmpPath = context.getFilesDir() + "/restore_backup";
        try {
            OutputStream output = new FileOutputStream(tmpPath);
            if (!App.copyStream(input, output)) {
                return false;
            }
        }
        catch (IOException e) {
            return false;
        }

        // Open file to check database content and upgrade if necessary
        SQLiteDatabase new_db = SQLiteDatabase.openDatabase(tmpPath, null, SQLiteDatabase.OPEN_READWRITE);
        try {
            Cursor c = new_db.rawQuery("SELECT " + Db.Table5.VALUE +
                    " FROM " + Db.Table5.TABLE_NAME +
                    " WHERE " + Db.Table5.TAG + " = 'db_version'", null);
            if (c.getCount() > 0) {
                c.moveToFirst();
                DatabaseHelper.upgradeDb(new_db, c.getInt(0), DatabaseHelper.DATABASE_VERSION);
            }
            c.close();
        } catch (Exception e) {
            DatabaseHelper.upgradeDb(new_db, 1, DatabaseHelper.DATABASE_VERSION);
        }
        new_db.close();

        // Overwrite current database file
        boolean result = App.copyFile(tmpPath, getDatabasePath());
        File tmpFile = new File(tmpPath);
        tmpFile.delete();

        return result;
    }

    private String getDatabasePath() {
        return context.getDatabasePath(DatabaseHelper.DATABASE_NAME).getAbsolutePath();
    }

    private String getBackupFileName() {
        Resources r = context.getResources();
        if (prefs.getBoolean("BACKUP_OVERRIDE_OLD", false))
            return r.getString(R.string.app_name) + ".backup";
        else
            return r.getString(R.string.app_name) + "_" + App.dateToUser("yyyy-MM-dd_HH-mm", new Date()) + ".backup";
    }

    private boolean checkStoragePermission() {
        int permission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return permission == PackageManager.PERMISSION_GRANTED;
    }
}
