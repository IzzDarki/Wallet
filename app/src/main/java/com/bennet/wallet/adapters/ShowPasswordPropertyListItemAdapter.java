package com.bennet.wallet.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bennet.wallet.R;
import com.bennet.wallet.activities.passwords.ShowPasswordActivity;
import com.bennet.wallet.preferences.AppPreferenceManager;
import com.bennet.wallet.utils.Utility;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;

public class ShowPasswordPropertyListItemAdapter extends RecyclerView.Adapter<ShowPasswordPropertyListItemAdapter.ViewHolder> {

    protected List<ShowPasswordActivity.ShowPasswordProperty> passwordProperties;
    protected Utility.VoidCallback onTextVisibilityChanged;

    public class ViewHolder extends RecyclerView.ViewHolder {
        protected View divider;
        protected MaterialTextView nameView;
        protected MaterialTextView valueView;
        protected AppCompatImageButton visibilityToggleButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            divider = itemView.findViewById(R.id.show_password_properties_list_item_divider);
            nameView = itemView.findViewById(R.id.show_password_properties_list_item_name_view);
            valueView = itemView.findViewById(R.id.show_password_properties_list_item_value_view);
            visibilityToggleButton = itemView.findViewById(R.id.show_password_properties_list_item_visibility_toggle);

            visibilityToggleButton.setOnClickListener(v -> toggleTextVisibility());
        }

        // helpers
        protected void toggleTextVisibility() {
            boolean isCurrentlyHidden = isTextHidden();
            onTextVisibilityChanged.callback();
            if (isCurrentlyHidden)
                setValueHidden(false);
        }

        /**
         * Sets the value view to be either hidden (password dots, not selectable) or not hidden (real text, selectable) and also sets the according visibility button drawable (visibility or visibility off)
         * @param textHidden true if the value should be displayed as hidden, false otherwise
         */
        public void setValueHidden(boolean textHidden) {
            if (textHidden) {
                valueView.setTypeface(Typeface.MONOSPACE);
                if (AppPreferenceManager.isLengthHiddenInSecretFields(getContext()))
                    valueView.setText(R.string.hidden_password_dots);
                else {
                    int valueLength = passwordProperties.get(getAdapterPosition()).value.length();
                    String hiddenValue = Utility.createStringNCopies(valueLength, "\u2022");
                    valueView.setText(hiddenValue);
                }
                valueView.setTextIsSelectable(false);
                visibilityToggleButton.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.visibility_off_icon_30dp));
            }
            else {
                setValueTypeface();
                valueView.setText(passwordProperties.get(getAdapterPosition()).value);
                valueView.setTextIsSelectable(true);
                visibilityToggleButton.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.visibility_icon_30dp));
            }
        }

        /**
         * Changes the typeface.
         * If preference {@link AppPreferenceManager#isMonospaceInSecretFields(Context)} is true, secret properties are monospace, others are 'default' (not monospace).
         */
        public void setValueTypeface() {
            if (passwordProperties.get(getAdapterPosition()).secret && AppPreferenceManager.isMonospaceInSecretFields(getContext()))
                valueView.setTypeface(Typeface.MONOSPACE);
            else
                valueView.setTypeface(Typeface.DEFAULT);
        }

        protected boolean isTextHidden() {
            return valueView.getText().toString().contains("\u2022");
        }

        protected Context getContext() {
            return itemView.getContext();
        }
    }

    public ShowPasswordPropertyListItemAdapter(List<ShowPasswordActivity.ShowPasswordProperty> passwordProperties, Utility.VoidCallback onTextVisibilityChanged) {
        this.passwordProperties = passwordProperties;
        this.onTextVisibilityChanged = onTextVisibilityChanged;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.adapter_show_password_property_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShowPasswordPropertyListItemAdapter.ViewHolder holder, int position) {
        if (position == 0)
            holder.divider.setVisibility(View.GONE);
        else
            holder.divider.setVisibility(View.VISIBLE);

        holder.nameView.setText(passwordProperties.get(position).name);

        boolean secret = passwordProperties.get(position).secret;
        holder.setValueTypeface(); // must be called before setValueHidden because setValueHidden sets typeface to monospace if the text is actually hidden
        holder.setValueHidden(secret);
        if (!secret)
            holder.visibilityToggleButton.setVisibility(View.GONE);
        else
            holder.visibilityToggleButton.setVisibility(View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return passwordProperties.size();
    }

}
