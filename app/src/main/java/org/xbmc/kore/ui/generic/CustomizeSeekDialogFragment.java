/*
 * Copyright 2015 Synced Synapse. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xbmc.kore.ui.generic;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.databinding.DialogCustomizeSeekButtonBinding;
import org.xbmc.kore.jsonrpc.type.PlayerType;

import java.util.ArrayList;

/**
 * Dialog that allows the user to send text
 */
public class CustomizeSeekDialogFragment extends DialogFragment {
    private static final String TITLE_KEY = "TITLE";

    // The listener activity we will call when the user finishes the selection
    private CustomizeSeekDialogListener mListener;
    private DialogCustomizeSeekButtonBinding binding;

    /**
     * Create a new instance of the dialog, providing arguments.
     *
     * @param title Title of the dialog
     * @return New dialog
     */
    public static CustomizeSeekDialogFragment newInstance(String title) {
        CustomizeSeekDialogFragment dialog = new CustomizeSeekDialogFragment();

        Bundle args = new Bundle();
        args.putString(TITLE_KEY, title);
        dialog.setArguments(args);
        return dialog;
    }

    /**
     * Override the attach to the activity to guarantee that the activity implements required interface
     *
     * @param context Context activity that implements listener interface
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (CustomizeSeekDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement CustomizeSeekDialogListener");
        }
    }

    /**
     * Create the dialog
     *
     * @param savedInstanceState Saved state
     * @return Created dialog
     */
    @NonNull
    @Override
    @SuppressWarnings("InflateParams")
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int currentTime = PreferenceManager.getDefaultSharedPreferences(requireContext()).getInt(
                Settings.KEY_PREF_CUSTOM_SEEK_TIME, Settings.DEFAULT_PREF_CUSTOM_SEEK_TIME);
        int currentKind = PreferenceManager.getDefaultSharedPreferences(requireContext()).getInt(
                Settings.KEY_PREF_CUSTOM_SEEK_KIND, Settings.DEFAULT_PREF_CUSTOM_SEEK_KIND);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());

        assert getArguments() != null;
        final String title = getArguments().getString(TITLE_KEY, getString(R.string.customize_seek_button_title));
        binding = DialogCustomizeSeekButtonBinding.inflate(requireActivity().getLayoutInflater(), null, false);

        builder.setTitle(title)
                .setView(binding.getRoot())
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    customizationFinished();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> mListener.onCustomizeSeekCancel());

        final Dialog dialog = builder.create();
        binding.timeInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
        });
        binding.timeInput.requestFocus();
        binding.timeInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    onCustomizeSeekFinished();
                }  // handles enter key on external keyboard, issue #99
                else if (actionId == EditorInfo.IME_ACTION_UNSPECIFIED &&
                        (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    onCustomizeSeekFinished();
                }
                dialog.dismiss();
                return false;
            }

            private void onCustomizeSeekFinished() {
                customizationFinished();
            }
        });
        binding.timeInput.setText(String.valueOf(currentTime));

        ArrayList<String> kindsOfSeek = new ArrayList<>();
        kindsOfSeek.add(getString(R.string.kind_of_seek_jump_by));
        kindsOfSeek.add(getString(R.string.kind_of_seek_jump_to));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, kindsOfSeek);
        binding.kindOfSeek.setAdapter(adapter);
        binding.kindOfSeek.setText(getString(currentKind == 0 ? R.string.kind_of_seek_jump_by : R.string.kind_of_seek_jump_to), false);

        return dialog;
    }

    private void customizationFinished() {
        if (binding.timeInput.getText() != null) {
            PlayerType.KindOfSeek kindOfSeek = getString(R.string.kind_of_seek_jump_by).equals(binding.kindOfSeek.getText().toString())
                    ? PlayerType.KindOfSeek.JUMP_BY : PlayerType.KindOfSeek.JUMP_TO;
            mListener.onCustomizeSeekFinished(binding.timeInput.getText().toString(), kindOfSeek);
        }
    }

    /**
     * Interface to pass events back to the calling activity
     * The calling activity must implement this interface
     */
    public interface CustomizeSeekDialogListener {
        void onCustomizeSeekFinished(String time, PlayerType.KindOfSeek kindOfSeek);

        void onCustomizeSeekCancel();
    }
}
