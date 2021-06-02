package com.bennet.wallet.activities.passwords;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;

import com.bennet.wallet.R;
import com.bennet.wallet.adapters.ShowPasswordPropertyListItemAdapter;
import com.bennet.wallet.preferences.PasswordPreferenceManager;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class ShowPasswordActivity extends AppCompatActivity {

    // intent extras
    static public final String EXTRA_PASSWORD_ID = "show_password.extra_password_id"; // int

    // UI
    protected RecyclerView passwordPropertiesView;

    // password properties
    protected int ID;
    protected String passwordName;
    protected String passwordValue;
    protected List<ShowPasswordProperty> passwordProperties = new ArrayList<>();

    // class to store all data of password properties
    static public class ShowPasswordProperty {
        public String name;
        public String value;
        public boolean hidden;

        public ShowPasswordProperty(String name, String value, boolean hidden) {
            this.name = name;
            this.value = value;
            this.hidden = hidden;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_password);

        // toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // hooks
        passwordPropertiesView = findViewById(R.id.show_password_recycler_view);

        // password properties recycler view
        passwordPropertiesView.setLayoutManager(new LinearLayoutManager(this));
        passwordPropertiesView.setAdapter(new ShowPasswordPropertyListItemAdapter(passwordProperties));

        // TODO display password value

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // (re)init and notify adapter
        initFromPreferences();
        passwordPropertiesView.getAdapter().notifyDataSetChanged();

        // reset scroll
        passwordPropertiesView.scrollToPosition(0);

        // toolbar
        getSupportActionBar().setTitle(passwordName);
    }

    // handling action bar menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.show_card_or_password_action_bar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.show_card_or_password_action_bar_edit) {
            editPassword();
            return true;
        }
        else if (item.getItemId() == R.id.show_card_or_password_action_bar_delete) {
            // TODO AlertDialog
            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.RoundedCornersDialog));
            builder.setTitle(R.string.delete_password);
            builder.setMessage(R.string.delete_password_dialog_message);
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.delete, (dialog, which) -> {
                finish(); // ALWAYS FINISH BEFORE STARTING OTHER ACTIVITY
                deletePassword();
                dialog.dismiss();
            });
            builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
            builder.show();
            return true;
        }
        else
            return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish(); // TODO check this
        return true;
    }

    // main functions
    protected void editPassword() {
        // TODO implement edit password
    }

    protected void deletePassword() {
        // TODO implement delete password
    }

    protected void initFromPreferences() {
        passwordProperties.clear();

        ID = getIntent().getIntExtra(EXTRA_PASSWORD_ID, -1);
        if (ID == -1)
            throw new IllegalStateException("ShowPasswordActivity: missing intent extra: ID");

        passwordName = PasswordPreferenceManager.readPasswordName(this, ID);
        if (passwordName == null)
            throw new IllegalStateException("PasswordActivity: missing preference: password name");

        passwordValue = PasswordPreferenceManager.readPasswordPassword(this, ID);

        List<Integer> passwordPropertyIDs = PasswordPreferenceManager.readPasswordPropertyIds(this, ID);
        for (Integer propertyID : passwordPropertyIDs) {
            String propertyName = PasswordPreferenceManager.readPasswordPropertyName(this, ID, propertyID);
            String propertyValue = PasswordPreferenceManager.readPasswordPropertyValue(this, ID, propertyID);
            boolean propertyHidden = PasswordPreferenceManager.readPasswordPropertyHidden(this, ID, propertyID);
            passwordProperties.add(new ShowPasswordProperty(propertyName, propertyValue, propertyHidden));
        }
    }

}