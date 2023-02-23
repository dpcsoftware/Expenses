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
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceFragmentCompat;

public class EditPreferencesFragment extends PreferenceFragmentCompat {
    private CheckBoxPreference autoBackupPref, budgetAlertsPref;
    private ActivityResultLauncher<String> permissionStorageLauncher, permissionNotificationsLauncher;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.prefs, rootKey);

        if (App.isAndroidQOrAbove()) {
            budgetAlertsPref = findPreference("budgetAlert");

            permissionNotificationsLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            budgetAlertsPref.setChecked(true);
                        }
                    }
            );

            budgetAlertsPref.setOnPreferenceChangeListener((preference, obj) -> {
                if ((Boolean) obj) {
                    // Check notifications permission
                    int permission = ContextCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.POST_NOTIFICATIONS);
                    if (permission == PackageManager.PERMISSION_GRANTED) {
                        return true;
                    } else {
                        permissionNotificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                        return false;
                    }
                } else {
                    return true;
                }
            });
        }
        else {
            autoBackupPref = findPreference("BACKUP_AUTO");

            permissionStorageLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            autoBackupPref.setChecked(true);
                        }
                    }
            );

            autoBackupPref.setOnPreferenceChangeListener((preference, obj) -> {
                if ((Boolean) obj) {
                    // Check storage permission
                    int permission = ContextCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    if (permission == PackageManager.PERMISSION_GRANTED) {
                        return true;
                    } else {
                        permissionStorageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        return false;
                    }
                } else {
                    return true;
                }
            });
        }
    }
}