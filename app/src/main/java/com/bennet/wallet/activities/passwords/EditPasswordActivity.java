package com.bennet.wallet.activities.passwords;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bennet.wallet.R;
import com.bennet.wallet.activities.HomeActivity;
import com.bennet.wallet.adapters.EditPasswordPropertyListItemAdapter;
import com.bennet.wallet.preferences.AppPreferenceManager;
import com.bennet.wallet.preferences.PasswordPreferenceManager;
import com.bennet.wallet.utils.Utility;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EditPasswordActivity extends AppCompatActivity {

    // intent extras
    static public final String EXTRA_PASSWORD_ID = ShowPasswordActivity.EXTRA_PASSWORD_ID; // int
    static public final String EXTRA_CREATE_NEW_PASSWORD = "edit_password.create_new_password"; // boolean

    // UI
    protected TextInputLayout nameInputLayout;
    protected TextInputEditText nameInputEditText;
    protected TextInputLayout passwordInputLayout;
    protected TextInputEditText passwordInputEditText;
    protected RecyclerView propertiesRecyclerView;
    protected LinearLayoutCompat createNewPasswordPropertyButton;

    // password properties
    protected int ID;
    protected String passwordName;
    protected String passwordValue;
    protected List<EditPasswordProperty> passwordProperties = new ArrayList<>();

    // variables
    protected boolean hasBeenModified = false;
    protected boolean isCreateNewPasswordIntent;

    public class EditPasswordProperty {
        public int propertyID; // 0 is invalid
        public String name;
        public String value;
        public boolean secret;

        public EditPasswordProperty() {}

        public EditPasswordProperty(String name, String value, boolean secret) {
            propertyID = generateNewPropertyID();
            this.name = name;
            this.value = value;
            this.secret = secret;
        }

        // helpers
        protected int generateNewPropertyID() {
            int newID;
            Random random = new Random();
            do {
                newID = random.nextInt();
            } while (!isNewIDValid(newID));
            return newID;
        }

        private boolean isNewIDValid(int propertyID) {
            if (propertyID == 0)
                return false;
            return !doesPropertyIDExist(propertyID);
        }
    }

    // lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_password);

        // hooks
        nameInputLayout = findViewById(R.id.edit_password_name_input_layout);
        nameInputEditText = findViewById(R.id.edit_password_name_edit_text);
        passwordInputLayout = findViewById(R.id.edit_password_password_input_layout);
        passwordInputEditText = findViewById(R.id.edit_password_password_edit_text);
        propertiesRecyclerView = findViewById(R.id.edit_password_recycler_view);
        createNewPasswordPropertyButton = findViewById(R.id.edit_password_create_new_password_property_button);

        // toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // init
        isCreateNewPasswordIntent = getIntent().getBooleanExtra(EXTRA_CREATE_NEW_PASSWORD, false);
        if (isCreateNewPasswordIntent) {
            initNewPassword();
            getSupportActionBar().setTitle(R.string.new_password);
        }
        else {
            initFromPreferences();
            getSupportActionBar().setTitle(R.string.edit_password);
        }

        // name input
        nameInputEditText.setText(passwordName);
        nameInputEditText.addTextChangedListener(new TextWatcher() { // this callback is able to set the error immediately
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String newName = s.toString();
                if (newName.equals(""))
                    nameInputLayout.setError(getString(R.string.password_name_empty_error));
                else
                    nameInputLayout.setError(null);
                passwordName = newName;
            }
        });
        nameInputEditText.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                passwordName = passwordName.trim();
                nameInputEditText.setText(passwordName);
            }
        });

        // password value input
        passwordInputEditText.setText(passwordValue);
        passwordInputEditText.setInputType(Utility.inputTypeTextHiddenPassword); // works better than xml "textPassword" (monospace + keyboard)
        passwordInputEditText.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                passwordValue = passwordInputEditText.getText().toString().trim();
                passwordInputEditText.setText(passwordValue);
                passwordInputEditText.setInputType(Utility.inputTypeTextHiddenPassword);
            }
        });

        // create new password property button
        createNewPasswordPropertyButton.setOnClickListener((v) -> addNewProperty());

        // recycler view
        propertiesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        EditPasswordPropertyListItemAdapter adapter = new EditPasswordPropertyListItemAdapter(passwordProperties, this::onPropertyRemoved);
        propertiesRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {
        requestCancel();
    }


    // main functions
    protected void initFromPreferences() {
        ID = getIntent().getIntExtra(EXTRA_PASSWORD_ID, -1);
        if (ID == -1)
            throw new IllegalStateException("PasswordActivity: missing intent extra: ID");

        passwordName = PasswordPreferenceManager.readPasswordName(this, ID);
        if (passwordName == null)
            throw new IllegalStateException("PasswordActivity: missing preference: password name");

        passwordValue = PasswordPreferenceManager.readPasswordValue(this, ID);

        List<Integer> passwordPropertyIDs = PasswordPreferenceManager.readPasswordPropertyIds(this, ID);
        for (Integer propertyID : passwordPropertyIDs) {
            EditPasswordProperty property = new EditPasswordProperty();
            property.propertyID = propertyID;
            property.name = PasswordPreferenceManager.readPasswordPropertyName(this, ID, propertyID);
            property.value = PasswordPreferenceManager.readPasswordPropertyValue(this, ID, propertyID);
            property.secret = PasswordPreferenceManager.readPasswordPropertySecret(this, ID, propertyID);
            passwordProperties.add(property);
        }
    }

    protected void initNewPassword() {
        ID = generateNewPasswordID();
        passwordName = getString(R.string.new_password);
        passwordValue = ""; // init as empty

        // init default properties
        passwordProperties.add(new EditPasswordProperty(getString(R.string.username), "", false)); // password username
        passwordProperties.add(new EditPasswordProperty(getString(R.string.email_address), "", false)); // email address
        passwordInputEditText.setImeOptions(EditorInfo.IME_ACTION_NEXT);
    }

    protected void addNewProperty() {
        passwordProperties.add(new EditPasswordProperty(getString(R.string.new_password_property_name), "", false));
        propertiesRecyclerView.getAdapter().notifyItemInserted(passwordProperties.size() - 1);
        propertiesRecyclerView.getAdapter().notifyItemRangeChanged(passwordProperties.size() - 1, 1);
        Utility.setImeOptionsAndRestart(passwordInputEditText, EditorInfo.IME_ACTION_NEXT);
    }

    protected void saveAndShowPassword() {
        List<Integer> propertyIDs = PasswordPreferenceManager.readPasswordPropertyIds(this, ID);

        // Check if the password was modified (hasBeenModified will be set true)
        readAndCheckPasswordValueInput();
        readAndCheckAllProperties();
        checkIfPropertyWasRemoved(propertyIDs);
        if (!readAndCheckNameInput()) {
            // When there are errors, saving will be aborted and Toast will be shown to user
            Toast.makeText(this, R.string.there_are_still_errors, Toast.LENGTH_SHORT).show();
            return;
        }

        writeToPreferences();

        finishAndShowPassword();
    }

    /**
     * If possible cancels the editing activity.
     * If there are errors in user input, a Toast is shown and the activity will not be canceled.
     * If the back confirm preference is true, an AlertDialog is shown and the activity will not be canceled.
     * @return true if the activity got finished, false otherwise
     */
    protected boolean requestCancel() {

        boolean isErrors = !checkAndReadAllInput(PasswordPreferenceManager.readPasswordPropertyIds(this, ID)); // Read input to figure out if password has been modified (member variable hasBeenModified)
        if (isErrors) {
            Toast.makeText(this, R.string.there_are_still_errors, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (AppPreferenceManager.isBackConfirmNewCardOrPassword(this) && isCreateNewPasswordIntent || AppPreferenceManager.isBackConfirmEditCardOrPassword(this) && !isCreateNewPasswordIntent && hasBeenModified) {
            String dialogTitle;
            String dialogMessage;
            if (isCreateNewPasswordIntent) {
                dialogTitle = getString(R.string.cancel_create_new_password_title);
                dialogMessage = getString(R.string.nothing_will_be_saved);
            }
            else {
                dialogTitle = getString(R.string.discard_changes);
                dialogMessage = getString(R.string.changes_are_not_saved);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.RoundedCornersDialog));
            builder.setTitle(dialogTitle);
            builder.setMessage(dialogMessage);
            builder.setCancelable(true);
            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                cancelDirectly();
                dialog.dismiss();
            });
            /*builder.setNeutralButton(R.string.save, (dialog, which) -> {
                saveAndShowPassword();
                dialog.dismiss();
            });*/ // The dialog is easier to understand without this extra option
            builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
            builder.show();
            return false;
        }

        cancelDirectly();
        return true;
    }

    protected void deleteAndReturnToHome() {
        PasswordPreferenceManager.removePassword(this, ID);
        finishAndReturnToHome();
    }


    // action bar menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_action_bar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.edit_action_bar_done) {
            saveAndShowPassword();
            return true;
        }
        else if (item.getItemId() == R.id.edit_action_bar_delete) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.delete_password)
                    .setMessage(R.string.delete_password_dialog_message)
                    .setCancelable(true)
                    .setPositiveButton(R.string.delete, (dialog, which) -> deleteAndReturnToHome())
                    .setNegativeButton(android.R.string.cancel,  (dialog, which) -> dialog.cancel())
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return requestCancel();
    }


    // helpers
    protected void cancelDirectly() {
        if (isCreateNewPasswordIntent) {
            setResult(RESULT_CANCELED);
            finish();
        }
        else
            finishAndShowPassword();
    }

    protected void finishAndShowPassword() {
        finish(); // finish first
        Intent intent = new Intent(this, ShowPasswordActivity.class);
        intent.putExtra(ShowPasswordActivity.EXTRA_PASSWORD_ID, ID);
        startActivity(intent);
    }

    protected void finishAndReturnToHome() {
        finish();
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // will clear back stack until instance of HomeActivity // It looks like this is needed although HomeActivity is singleTop
        startActivity(intent);
    }

    protected void writeToPreferences() {
        PasswordPreferenceManager.addToAllPasswordIDs(this, ID); // This should be fine. At this moment no other process should modify this preference list (Note that this only adds the ID if it is not yet contained in the list)

        PasswordPreferenceManager.writePasswordName(this, ID, passwordName);
        PasswordPreferenceManager.writePasswordValue(this, ID, passwordValue);

        Utility.PreferenceArrayInt currentPropertyIDs = new Utility.PreferenceArrayInt(); // Collects all current propertyIDs to write into preferences

        PasswordPreferenceManager.removePasswordProperties(this, ID);
        for (EditPasswordProperty property : passwordProperties) {
            currentPropertyIDs.add(property.propertyID);
            PasswordPreferenceManager.writePasswordPropertyName(this, ID, property.propertyID, property.name);
            PasswordPreferenceManager.writePasswordPropertyValue(this, ID, property.propertyID, property.value);
            PasswordPreferenceManager.writePasswordPropertySecret(this, ID, property.propertyID, property.secret);
        }
        PasswordPreferenceManager.writePasswordPropertyIds(this, ID, currentPropertyIDs);
    }

    protected void onPropertyRemoved() {
        // clear focus from all elements because otherwise the onFocusChangeListeners will get called with wrong or invalid getAdapterPosition()
        for (int pos = 0; pos < passwordProperties.size(); pos++) {
            EditPasswordPropertyListItemAdapter.ViewHolder holder = (EditPasswordPropertyListItemAdapter.ViewHolder) propertiesRecyclerView.findViewHolderForAdapterPosition(pos);
            if (holder != null)
                holder.clearFocus();
        }
        if (passwordProperties.size() == 0)
            Utility.setImeOptionsAndRestart(passwordInputEditText, EditorInfo.IME_ACTION_DONE);
    }
    
    /**
     * Reads all the user input and checks if the password has been modified ({@link #hasBeenModified} will be set true)
     * @param propertyIDs List of all property Ids
     * @return true if there are no errors, false otherwise
     */
    protected boolean checkAndReadAllInput(final List<Integer> propertyIDs) {
        readAndCheckPasswordValueInput();
        readAndCheckAllProperties();
        checkIfPropertyWasRemoved(propertyIDs);
        return readAndCheckNameInput();
    }
    
    /**
     * Checks if there are errors in the name input.
     * Updates {@link #hasBeenModified}
     * @return true if there are no errors in the users input, false otherwise
     */
    protected boolean readAndCheckNameInput() {
        passwordName = nameInputEditText.getText().toString().trim();
        if (nameInputLayout.getError() != null)
            return false;

        if (!passwordName.equals(PasswordPreferenceManager.readPasswordName(this, ID)))
            hasBeenModified = true;
        return true;
    }

    protected void readAndCheckPasswordValueInput() {
        passwordValue = passwordInputEditText.getText().toString().trim();
        if (!passwordValue.equals(PasswordPreferenceManager.readPasswordValue(this, ID)))
            hasBeenModified = true;
    }

    /**
     * Checks if any property was modified. Alsa checks if a new property was added.
     * Then sets {@link #hasBeenModified} to true
     */
    protected void readAndCheckAllProperties() {
        for (int position = 0; position < passwordProperties.size(); position++) {
            EditPasswordPropertyListItemAdapter.ViewHolder holder = (EditPasswordPropertyListItemAdapter.ViewHolder) propertiesRecyclerView.findViewHolderForAdapterPosition(position);
            if (holder != null)
                holder.readValue();

            EditPasswordProperty property = passwordProperties.get(position);
            if (!property.name.equals(PasswordPreferenceManager.readPasswordPropertyName(this, ID, property.propertyID)))
                hasBeenModified = true;
            if (!property.value.equals(PasswordPreferenceManager.readPasswordPropertyValue(this, ID, property.propertyID)))
                hasBeenModified = true;
            if (property.secret != PasswordPreferenceManager.readPasswordPropertySecret(this, ID, property.propertyID))
                hasBeenModified = true;
        }
        // Note: PasswordPreferenceManager.readPasswordPropertyName will return null if property.propertyID is not in preferences = if the property was added (String.equals accepts null and returns false)
    }

    /**
     *  Checks if a property got removed.
     *  Then sets {@link #hasBeenModified} to true
     * @param propertyIDs The list of all property IDs of this password currently in preferences
     */
    protected void checkIfPropertyWasRemoved(final List<Integer> propertyIDs) {
        for (Integer propertyID : propertyIDs) {
            if (!doesPropertyIDExist(propertyID)) { // if propertyID doesn't exist anymore after editing
                hasBeenModified = true;
                return;
            }
        }
    }

    protected boolean doesPropertyIDExist(int propertyID) {
        for (EditPasswordProperty property : passwordProperties) {
            if (propertyID == property.propertyID)
                return true;
        }
        return false;
    }

    protected int generateNewPasswordID() {
        List<Integer> passwordIDs = PasswordPreferenceManager.readAllPasswordIDs(this); // this is fine because there can't be an unsaved password with an unsaved ID at this moment
        Utility.IDGenerator generator = new Utility.IDGenerator(passwordIDs);
        return generator.generateID();
    }

}