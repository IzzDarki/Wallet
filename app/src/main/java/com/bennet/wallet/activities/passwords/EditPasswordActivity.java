package com.bennet.wallet.activities.passwords;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bennet.wallet.R;
import com.bennet.wallet.adapters.EditPasswordPropertyListItemAdapter;
import com.bennet.wallet.preferences.PasswordPreferenceManager;
import com.bennet.wallet.utils.Utility;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.microedition.khronos.egl.EGLDisplay;

public class EditPasswordActivity extends AppCompatActivity {

    // intent extras
    static public final String EXTRA_PASSWORD_ID = "extra_password_id"; // int
    static public final String EXTRA_CREATE_NEW_PASSWORD = "edit_password.create_new_password"; // boolean

    // UI
    protected TextInputLayout nameInputLayout;
    protected TextInputEditText nameInputEditText;
    protected TextInputLayout passwordInputLayout;
    protected TextInputEditText passwordInputEditText;
    protected RecyclerView propertiesRecyclerView;
    protected LinearLayout createNewPasswordPropertyButton;

    // password properties
    protected int ID;
    protected String passwordName;
    protected String passwordValue;
    protected List<EditPasswordProperty> passwordProperties = new ArrayList<>();

    // variables
    protected boolean hasBeenModified = false;
    protected boolean isNewPasswordIntent;

    public class EditPasswordProperty {
        public int propertyID; // 0 is invalid
        public String name;
        public String value;
        public boolean hidden;

        public EditPasswordProperty(String name, String value, boolean hidden) {
            propertyID = generateNewPropertyID();
            this.name = name;
            this.value = value;
            this.hidden = hidden;
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
            for (EditPasswordProperty property : passwordProperties) {
                if (propertyID == property.propertyID)
                    return false;
            }
            return true;
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

        // init
        isNewPasswordIntent = getIntent().getBooleanExtra(EXTRA_CREATE_NEW_PASSWORD, false);
        if (isNewPasswordIntent)
            initNewPassword();
        else
            initFromPreferences();

        // toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.edit_password);

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

    // main functions
    protected void initFromPreferences() {
        ID = getIntent().getIntExtra(EXTRA_PASSWORD_ID, -1);
        if (ID == -1)
            throw new IllegalStateException("PasswordActivity: missing intent extra: ID");

        passwordName = PasswordPreferenceManager.readPasswordName(this, ID);
        if (passwordName == null)
            throw new IllegalStateException("PasswordActivity: missing preference: password name");

        passwordValue = PasswordPreferenceManager.readPasswordPassword(this, ID);

        List<Integer> passwordPropertyIDs = PasswordPreferenceManager.readPasswordPropertyIds(this, ID);
        for (Integer propertyID : passwordPropertyIDs) {
            String propertyName = PasswordPreferenceManager.readPasswordPropertyName(this, ID, propertyID);
            String propertyValue = PasswordPreferenceManager.readPasswordPropertyValue(this, ID, propertyID);
            boolean propertyHidden = PasswordPreferenceManager.readPasswordPropertyHidden(this, ID, propertyID);
            passwordProperties.add(new EditPasswordActivity.EditPasswordProperty(propertyName, propertyValue, propertyHidden));
        }
    }

    protected void initNewPassword() {
        ID = generateNewPasswordID();
        passwordName = getString(R.string.new_password_name);
        passwordValue = ""; // init as empty

        // init default properties
        passwordProperties.add(new EditPasswordProperty(getString(R.string.username), "", false)); // password username
        passwordProperties.add(new EditPasswordProperty(getString(R.string.email_address), "", false)); // email address
        passwordProperties.add(new EditPasswordProperty("Test1", "Hi", true));
        passwordInputEditText.setImeOptions(EditorInfo.IME_ACTION_NEXT);
    }

    protected void addNewProperty() {
        passwordProperties.add(new EditPasswordProperty(getString(R.string.new_password_property_name), "", false));
        propertiesRecyclerView.getAdapter().notifyItemInserted(passwordProperties.size() - 1);
        propertiesRecyclerView.getAdapter().notifyItemRangeChanged(passwordProperties.size() - 1, 1);
        Utility.setImeOptionsAndRestart(passwordInputEditText, EditorInfo.IME_ACTION_NEXT);
    }

    // helpers
    protected void onPropertyRemoved() {
        // clear focus from all elements because otherwise the onFocusChangeListeners will get called with wrong or invalid getAdapterPosition()
        for (int pos = 0; pos < passwordProperties.size(); pos++) {
            EditPasswordPropertyListItemAdapter.ViewHolder holder = (EditPasswordPropertyListItemAdapter.ViewHolder) propertiesRecyclerView.findViewHolderForAdapterPosition(pos);
            holder.clearFocus();
        }
        if (passwordProperties.size() == 0)
            Utility.setImeOptionsAndRestart(passwordInputEditText, EditorInfo.IME_ACTION_DONE);
    }

    /**
     * Checks if there are errors in the name input.
     * Updates {@link #hasBeenModified}
     * @return true if there are no errors in the users input, false otherwise
     */
    protected boolean checkNameInput() {
        passwordName = passwordName.trim();
        if (nameInputLayout.getError() != null)
            return false;
        if (!passwordName.equals(PasswordPreferenceManager.readPasswordName(this, ID)))
            hasBeenModified = true;

        return true;
    }

    protected void checkPasswordValueInput() {
        if (!passwordValue.equals(PasswordPreferenceManager.readPasswordPassword(this, ID)))
            hasBeenModified = true;
    }

    protected void checkPropertyHiddenInput(int propertyID, int position, boolean newHidden) {
        if (newHidden != PasswordPreferenceManager.readPasswordPropertyHidden(this, ID, propertyID))
            hasBeenModified = true;
    }

    protected void checkPropertyNameInput(int propertyID, int position, String newName) {
        if (!newName.equals(PasswordPreferenceManager.readPasswordPropertyName(this, ID, propertyID)))
            hasBeenModified = true;
    }

    protected void checkPropertyValueInput(int propertyID, int position, String newValue) {
        if (!newValue.equals(PasswordPreferenceManager.readPasswordPropertyValue(this, ID, propertyID)))
            hasBeenModified = true;
    }

    protected int generateNewPasswordID() {
        List<Integer> passwordIDs = PasswordPreferenceManager.readAllPasswordIDs(this); // this is fine because there can't be an unsaved card with an unsaved ID at this moment
        Utility.IDGenerator generator = new Utility.IDGenerator(passwordIDs);
        return generator.generateID();
    }

}