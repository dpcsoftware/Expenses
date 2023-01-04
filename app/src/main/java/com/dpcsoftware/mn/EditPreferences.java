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

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class EditPreferences extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.editpreferences);
        setTitle(R.string.editpreferences_c1);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.scrollView, new EditPreferencesFragment())
                .commit();
    }
}
