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
        LayoutInflater li = getActivity().getLayoutInflater();
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
