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

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class ExportDataChoiceDialog extends DialogFragment implements View.OnClickListener {
    private OnChosenListener listener = null;

    public enum Choice {
        SHARE,
        SAVE
    }
    public interface OnChosenListener {
        void onChosen(Choice ch);
    }

    public void setOnChosenListener(OnChosenListener listener) {
        this.listener = listener;
    }

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstance) {
        LayoutInflater li = requireActivity().getLayoutInflater();
        View layout = li.inflate(R.layout.exportdata_choicedialog, null);

        layout.findViewById(R.id.buttonShare).setOnClickListener(this);
        layout.findViewById(R.id.buttonSave).setOnClickListener(this);
        layout.findViewById(R.id.buttonCancel).setOnClickListener(this);

        return new AlertDialog.Builder(requireContext())
                .setView(layout)
                .setTitle(R.string.exportdata_choicedialog_c1)
                .create();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.buttonShare) {
            if (listener != null) {
                listener.onChosen(Choice.SHARE);
            }
            dismiss();
        }
        else if (id == R.id.buttonSave) {
            if (listener != null) {
                listener.onChosen(Choice.SAVE);
            }
            dismiss();
        }
        else if (id == R.id.buttonCancel){
            dismiss();
        }
    }
}
