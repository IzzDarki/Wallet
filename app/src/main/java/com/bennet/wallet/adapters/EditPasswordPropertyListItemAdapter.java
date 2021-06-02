package com.bennet.wallet.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;

import com.bennet.wallet.activities.passwords.EditPasswordActivity;
import com.bennet.wallet.utils.Utility;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.bennet.wallet.R;

import java.util.List;

public class EditPasswordPropertyListItemAdapter extends RecyclerView.Adapter<EditPasswordPropertyListItemAdapter.ViewHolder> {
    
    // password properties
    protected List<EditPasswordActivity.EditPasswordProperty> passwordProperties;

    protected Utility.VoidCallback onPropertyRemovedListener;
    private Pair<Integer, Integer> cursorToReset = new Pair<>(-1, -1); // first: position in adapter (-1 = none), second: cursor position

    public class ViewHolder extends RecyclerView.ViewHolder {
        protected TextInputLayout textInputLayout;
        protected TextInputEditText textInputEditText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            textInputLayout = itemView.findViewById(R.id.edit_password_property_text_input_layout);
            textInputEditText = itemView.findViewById(R.id.edit_password_property_text_input_edit_text);

            textInputLayout.setStartIconOnClickListener(this::onStartIconClick);
            textInputLayout.setEndIconOnClickListener(this::onEndIconClick); // gets called only for endIconMode="custom"

            textInputEditText.setOnFocusChangeListener((view, hasFocus) -> {
                if (getAdapterPosition() == -1)
                    return;

                if (!hasFocus) {
                    readValue();
                    if (passwordProperties.get(getAdapterPosition()).hidden)
                        textInputEditText.setInputType(Utility.inputTypeTextHiddenPassword);
                }
            });
        }

        protected void readValue() {
            String newValue = textInputEditText.getText().toString().trim();
            passwordProperties.get(getAdapterPosition()).value = newValue;
        }

        protected void onStartIconClick(View view) {
            readValue(); // Could get lost otherwise

            AlertDialog dialog;
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

            View dialogView = View.inflate(getContext(), R.layout.edit_property_input_dialog, null);
            TextInputEditText editText = dialogView.findViewById(R.id.edit_property_input_dialog_name_input_edit_text);
            SwitchMaterial visibilitySwitch = dialogView.findViewById(R.id.edit_property_input_dialog_visibility_switch);

            editText.setText(passwordProperties.get(getAdapterPosition()).name);
            editText.requestFocus();
            visibilitySwitch.setChecked(passwordProperties.get(getAdapterPosition()).hidden);

            builder.setView(dialogView)
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok, (dialog1, which) -> {
                        // read name input
                        String newPropertyName = editText.getText().toString().trim();
                        passwordProperties.get(getAdapterPosition()).name = newPropertyName;
                        textInputLayout.setHint(newPropertyName);

                        // read visibility input
                        passwordProperties.get(getAdapterPosition()).hidden = visibilitySwitch.isChecked();

                        cursorToReset = new Pair<>(getAdapterPosition(), textInputEditText.getSelectionStart()); // onBindViewHolder uses this to reset the cursor
                        notifyItemChanged(getAdapterPosition());
                    })
                    .setNegativeButton(android.R.string.cancel, (dialog1, which) -> dialog1.cancel())
                    .setOnDismissListener(dialog1 -> Utility.hideKeyboard(editText));
            dialog = builder.create();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE); // Without this keyboard closes when alert dialog opens
            dialog.show();
        }

        protected void onEndIconClick(View view) {
            if (passwordProperties.get(getAdapterPosition()).hidden) {
                View popupView = LayoutInflater.from(view.getContext()).inflate(R.layout.edit_password_property_list_item_end_icon_popup_window_layout, null);
                ImageButton visibilityButton = popupView.findViewById(R.id.edit_password_property_list_item_end_icon_popup_window_visibility_button);
                ImageButton deleteButton = popupView.findViewById(R.id.edit_password_property_list_item_end_icon_popup_window_delete_button);
                setVisibilityImageButton(visibilityButton);

                Utility.hideKeyboard(itemView); // hide keyboard, because otherwise it closes and reopens when popup window appears (idk why)
                textInputEditText.requestFocus();
                textInputLayout.setEndIconDrawable(R.drawable.expand_less_icon_30dp);
                PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
                popupWindow.showAsDropDown(view);

                visibilityButton.setOnClickListener((v) -> {
                    toggleTextVisibility();
                    setVisibilityImageButton(visibilityButton);
                    popupWindow.dismiss();
                });
                deleteButton.setOnClickListener((v) -> {
                    deleteProperty();
                    popupWindow.dismiss();
                });
                popupWindow.setOnDismissListener(() -> textInputLayout.setEndIconDrawable(R.drawable.expand_more_icon_30dp));
            }
            else
                deleteProperty();
        }

        protected void deleteProperty() {
            int position = getAdapterPosition();
            onPropertyRemovedListener.callback();
            passwordProperties.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, passwordProperties.size() - position);
        }

        // helpers
        protected void setVisibilityImageButton(ImageButton visibilityButton) {
            if (isTextHidden())
                visibilityButton.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.visibility_icon_30dp));
            else
                visibilityButton.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.visibility_off_icon_30dp));
        }

        protected void toggleTextVisibility() {
            int cursor = textInputEditText.getSelectionStart();
            if (isTextHidden())
                textInputEditText.setInputType(Utility.inputTypeTextVisiblePassword);
            else
                textInputEditText.setInputType(Utility.inputTypeTextHiddenPassword);
            textInputEditText.setSelection(cursor); // Otherwise cursor will be reset to start
        }

        public void clearFocus() {
            textInputEditText.clearFocus();
        }

        protected boolean isTextHidden() {
            return textInputEditText.getInputType() == Utility.inputTypeTextHiddenPassword;
        }

        public Context getContext() {
            return itemView.getContext();
        }

        public void setImeOptions(int imeOptions) {
            Utility.setImeOptionsAndRestart(textInputEditText, imeOptions);
        }
    }

    // constructor
    public EditPasswordPropertyListItemAdapter(List<EditPasswordActivity.EditPasswordProperty> passwordProperties, Utility.VoidCallback onPropertyRemovedListener) {
        this.passwordProperties = passwordProperties;
        this.onPropertyRemovedListener = onPropertyRemovedListener;
    }

    // override functions
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.edit_password_property_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EditPasswordPropertyListItemAdapter.ViewHolder holder, int position) {
        /*  These things need to be updated:
            - input type (normal text, password text)
            - end icon (delete, expand)
         */

        // text hidden mode, end icon expand (dropdown) for both visibility and delete icons
        if (passwordProperties.get(position).hidden) {
            holder.textInputEditText.setInputType(Utility.inputTypeTextHiddenPassword);
            holder.textInputLayout.setEndIconDrawable(R.drawable.expand_more_icon_30dp);
        }
        // text visible mode, only delete end icon
        else {
            holder.textInputEditText.setInputType(Utility.inputTypeTextNormal);
            holder.textInputLayout.setEndIconDrawable(R.drawable.delete_icon_30dp);
        }

        holder.textInputLayout.setHint(passwordProperties.get(position).name);
        holder.textInputEditText.setText(passwordProperties.get(position).value);

        if (position == cursorToReset.first) {
            holder.textInputEditText.setSelection(cursorToReset.second);
            cursorToReset = new Pair<>(-1, -1); // reset to default state
        }

        if (position == passwordProperties.size() - 1)
            holder.setImeOptions(EditorInfo.IME_ACTION_DONE);
        else
            holder.setImeOptions(EditorInfo.IME_ACTION_NEXT);
    }

    @Override
    public int getItemCount() {
        return passwordProperties.size();
    }

}
