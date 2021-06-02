package com.bennet.wallet.utils;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.bennet.wallet.preferences.CardPreferenceManager;
import com.bennet.wallet.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.bennet.wallet.utils.Utility.PreferenceArrayInt;

public class CardPropertyView extends FrameLayout {

    // callbacks
    public interface OnDeletePropertyListener {
        /**
         * Gets called before card property gets deleted on call to {@link #save()}
         * @param view the instance that got deleted
         */
        void onDeleteProperty(CardPropertyView view);
    }

    public interface OnSetPropertyDeletedListener {
        /**
         * Gets called when user deletes a property by pressing the delete icon <br>
         * Note, that the property has not yet been deleted. That happens when {@link #save()} gets called. Use {@link OnDeletePropertyListener} to listen for actually deleting property in preferences.
         * @param view the instance that was set to be deleted
         * @param isDeleted true if set to deleted, false otherwise
         */
        void onSetPropertyDeleted(CardPropertyView view, boolean isDeleted);
    }

    // variables
    protected int propertyID;
    protected int card_ID;
    protected String name;
    protected String value;
    protected boolean isDeleted = false;
    protected OnDeletePropertyListener onDeletePropertyListener;
    protected OnSetPropertyDeletedListener onSetPropertyDeletedListener;

    // UI
    public TextInputLayout inputLayout;
    public TextInputEditText inputEditText;


    // construct
    public CardPropertyView(@NonNull Context context, final PreferenceArrayInt cardPropertyUsedIDs, int card_ID, @Nullable OnDeletePropertyListener onDeletePropertyListener, @Nullable OnSetPropertyDeletedListener onSetPropertyDeletedListener) {
        this(context, card_ID, (new Utility.IDGenerator(cardPropertyUsedIDs)).generateID(), context.getString(R.string.new_card_property_name), "", onDeletePropertyListener, onSetPropertyDeletedListener);
    }

    private CardPropertyView(@NonNull Context context, int card_ID, final int propertyID, String name, final String value, @Nullable OnDeletePropertyListener onDeletePropertyListener, @Nullable OnSetPropertyDeletedListener onSetPropertyDeletedListener) {
        super(context);

        inflate(getContext(), R.layout.card_property_edit_view, this);
        inputLayout = findViewById(R.id.card_property_edit_view_text_input_layout);
        inputEditText = findViewById(R.id.card_property_edit_view_text_input_layout_edit_text);

        this.card_ID = card_ID;
        this.propertyID = propertyID;
        this.name = name;
        this.value = value;
        this.onDeletePropertyListener = onDeletePropertyListener;
        this.onSetPropertyDeletedListener = onSetPropertyDeletedListener;

        inputLayout.setEndIconOnClickListener(v -> setPropertyDeleted());
        inputLayout.setStartIconOnClickListener(v -> editPropertyName());

        inputEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus)
                readValue();
        });

        inputLayout.setHint(name);
        inputEditText.setText(value);

        setImeDone();
    }


    // functions
    /**
     * updates preferences if property has been changed or removes preferences if property has been deleted
     */
    public void save() {
        if (!isDeleted) {
            readValue();

            CardPreferenceManager.writeCardPropertyName(getContext(), card_ID, propertyID, name);
            CardPreferenceManager.writeCardPropertyValue(getContext(), card_ID, propertyID, value);
        }
        else {
            if (onDeletePropertyListener != null)
                onDeletePropertyListener.onDeleteProperty(this);

            // remove preferences
            CardPreferenceManager.removeCardProperty(getContext(), card_ID, propertyID);
        }
    }


    /**
     * Sets property to a state, where it will remove all preferences in the next call to {@link #save()}
     */
    protected void setPropertyDeleted() {
        isDeleted = true;
        if (onSetPropertyDeletedListener != null)
            onSetPropertyDeletedListener.onSetPropertyDeleted(this, true);
    }

    public void editPropertyName() {
        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getContext(), R.style.RoundedCornersDialog));

        final View dialogView = View.inflate(getContext(), R.layout.text_input_dialog, null);
        final TextInputEditText textInput = dialogView.findViewById(R.id.text_input_dialog_edit_text);

        textInput.setText(name);
        dialogView.requestFocus();

        builder.setView(dialogView)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, (dialog1, which) -> {
                    name = textInput.getText().toString().trim();
                    inputLayout.setHint(name);
                    Utility.hideKeyboard(CardPropertyView.this);
                });
        dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE); // Without this keyboard closes when alert dialog opens
        dialog.show();
    }


    // static functions
    static public CardPropertyView loadFromPreferences(Context context, int card_ID, int propertyID, @Nullable OnDeletePropertyListener onDeletePropertyListener, @Nullable OnSetPropertyDeletedListener onSetPropertyDeletedListener) {
        String name = CardPreferenceManager.readCardPropertyName(context, card_ID, propertyID);
        String value = CardPreferenceManager.readCardPropertyValue(context, card_ID, propertyID);

        if (name == null)
            throw new IllegalStateException("CardPropertyView: tried to load property from preferences, but name was not there");
        if (value == null)
            throw new IllegalStateException("CardPropertyView: tried to load property from preferences, but value was not there");

        return new CardPropertyView(context, card_ID, propertyID, name, value, onDeletePropertyListener, onSetPropertyDeletedListener);
    }


    // helpers
    protected void readValue() {
        value = inputEditText.getText().toString().trim();
        inputEditText.setText(value);
    }


    // getters and setters
    public int getPropertyID() {
        return propertyID;
    }

    public String getPropertyName() {
        return name;
    }

    public void setPropertyName(String name) {
        this.name = name;
    }

    public String getPropertyValue() {
        return value;
    }

    public void setPropertyValue(String propertyValue) {
        this.value = propertyValue;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    /**
     * Sets ime options of text field
     * @param imeOptions values are defined in {@link android.view.inputmethod.EditorInfo}
     */
    public void setImeOptions(int imeOptions) {
        Utility.setImeOptionsAndRestart(inputEditText, imeOptions);
    }

    public void setImeNext() {
        setImeOptions(EditorInfo.IME_ACTION_NEXT);
    }

    public void setImeDone() {
        setImeOptions(EditorInfo.IME_ACTION_DONE);
    }

    public int getImeOptions() {
        return inputEditText.getImeOptions();
    }

}
