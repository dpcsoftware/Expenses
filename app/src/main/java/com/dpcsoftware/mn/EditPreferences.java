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

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class EditPreferences extends PreferenceActivity {
    private CheckBoxPreference autoBackupPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);

        autoBackupPref = (CheckBoxPreference) findPreference("BACKUP_AUTO");
        autoBackupPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object obj) {
                if ((Boolean) obj) {
                    // Check storage permission
                    int permission = ContextCompat.checkSelfPermission(EditPreferences.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    if (permission == PackageManager.PERMISSION_GRANTED) {
                        return true;
                    } else {
                        ActivityCompat.requestPermissions(EditPreferences.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                        return false;
                    }
                } else {
                    return true;
                }
            }
        });

        setTitle(R.string.editpreferences_c1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            int i;
            for (i = 0; i < permissions.length; ++i) {
                if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        autoBackupPref.setChecked(true);
                    }
                }
            }
        }
    }
}
