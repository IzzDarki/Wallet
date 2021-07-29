package com.bennet.wallet.activities.passwords

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bennet.wallet.R
import com.bennet.wallet.adapters.ShowPropertyAdapter
import com.bennet.wallet.preferences.PasswordPreferenceManager
import com.bennet.wallet.utils.ItemProperty
import com.google.android.material.appbar.MaterialToolbar

class ShowPasswordActivity : AppCompatActivity() {

    // region intent extras
    companion object {
        const val EXTRA_PASSWORD_ID =
            "show_password.extra_password_id" // int (EditPasswordActivity matches this)
    } // endregion


    // region UI
    private lateinit var passwordPropertiesView: RecyclerView
    // endregion


    // region password properties
    private var ID = 0
    private lateinit var passwordName: String
    private lateinit var passwordValue: String
    private var passwordProperties: MutableList<ItemProperty> = mutableListOf()
    // endregion


    // region lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_password)

        // hooks
        passwordPropertiesView = findViewById(R.id.show_password_recycler_view)

        // init
        initFromPreferences()

        // toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        setActionBarName()

        // password properties recycler view
        passwordPropertiesView.layoutManager = LinearLayoutManager(this)
        passwordPropertiesView.adapter = ShowPropertyAdapter(passwordProperties) {
            // hide all secret values in adapter each time the user presses any of the visibility buttons
            hideAllSecretValuesInAdapter()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // re-init and notify adapter
        initFromPreferences()
        passwordPropertiesView.adapter!!.notifyDataSetChanged()

        // toolbar
        setActionBarName()

        // reset scroll
        passwordPropertiesView.scrollToPosition(0)
    }

    override fun onPause() {
        super.onPause()
        hideAllSecretValuesInAdapter()
    }
    // endregion


    // region main functions
    private fun editPassword() {
        val intent = Intent(this, EditPasswordActivity::class.java)
        intent.putExtra(EditPasswordActivity.EXTRA_PASSWORD_ID, ID)
        startActivity(intent)
    }

    private fun initFromPreferences() {
        passwordProperties.clear()

        ID = intent.getIntExtra(EXTRA_PASSWORD_ID, -1)
        check(ID != -1) { "ShowPasswordActivity: missing intent extra: ID" }

        passwordName = PasswordPreferenceManager.readPasswordName(this, ID)
        passwordValue = PasswordPreferenceManager.readPasswordValue(this, ID)

        if (passwordValue != "") {
            // Add password value to password properties (will also be part of recycler view)
            passwordProperties.add(
                ItemProperty(
                    propertyID = ItemProperty.INVALID_ID,
                    name = getString(R.string.password),
                    value = passwordValue,
                    secret = true
                )
            )
        }

        val passwordPropertyIDs = PasswordPreferenceManager.readPasswordPropertyIds(this, ID)
        for (propertyID in passwordPropertyIDs) {
            val propertyName = PasswordPreferenceManager.readPasswordPropertyName(this, ID, propertyID)
            val propertyValue = PasswordPreferenceManager.readPasswordPropertyValue(this, ID, propertyID)
            val propertySecret = PasswordPreferenceManager.readPasswordPropertySecret(this, ID, propertyID)
            passwordProperties.add(
                ItemProperty(ItemProperty.INVALID_ID, propertyName, propertyValue, propertySecret)
            )
        }
    }
    // endregion


    // region action bar and menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.show_card_or_password_action_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.show_card_or_password_action_bar_edit -> {
                editPassword()
                return true
            }

            R.id.show_card_or_password_action_bar_delete -> {
                AlertDialog.Builder(ContextThemeWrapper(this, R.style.RoundedCornersDialog))
                    .setTitle(R.string.delete_password)
                    .setMessage(R.string.delete_password_dialog_message)
                    .setCancelable(true)
                    .setPositiveButton(R.string.delete) { dialog, _ ->
                        PasswordPreferenceManager.removePassword(this, ID)
                        finish()
                        dialog.dismiss()
                    }
                    .setNegativeButton(android.R.string.cancel) { dialog: DialogInterface, _: Int ->
                        dialog.cancel()
                    }
                    .show()
                return true
            }

            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    // endregion


    // region helpers
    private fun hideAllSecretValuesInAdapter() {
        for (position in passwordProperties.indices) {
            val holder = passwordPropertiesView.findViewHolderForAdapterPosition(position) as? ShowPropertyAdapter.ViewHolder
            holder?.setValueHidden(passwordProperties[position].secret)
        }
    }

    private fun setActionBarName() {
        supportActionBar!!.title = passwordName
    }
    // endregion
}