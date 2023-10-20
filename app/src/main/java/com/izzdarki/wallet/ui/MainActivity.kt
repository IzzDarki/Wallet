package com.izzdarki.wallet.ui

import android.os.Bundle
import androidx.core.view.GravityCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.izzdarki.wallet.logic.AuthenticatedAppCompatActivity
import com.izzdarki.wallet.logic.updates.showUpdateAlert
import com.izzdarki.wallet.services.ClearDirectoryService
import izzdarki.wallet.BuildConfig
import izzdarki.wallet.R
import izzdarki.wallet.databinding.ActivityMainBinding

class MainActivity : AuthenticatedAppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(findViewById(R.id.toolbar))

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations
        // (The Up button will not be displayed when on these destinations)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
            ),
            binding.drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)

        showUpdateAlerts() // Show update alerts if new version has been installed
    }

    override fun onStop() {
        super.onStop()

        // Since onStop is probably(?) guaranteed to be called, this is a good place to clear cached card images
        // Clear cached card images
        ClearDirectoryService.enqueueWork(this, cacheDir.toString() + "/" + getString(R.string.cards_images_folder_name))
    }

    override fun onBackPressed() {
        // Close drawer if visible
        if (binding.drawerLayout.isDrawerVisible(GravityCompat.START))
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        else
            super.onBackPressed()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    /**
     * Shows update alerts if new version has been installed
     * This cannot be part of `Application.runUpdateCode` because it's not possible to show an AlertDialog from there
     */
    private fun showUpdateAlerts() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val lastVersionNumber = sharedPreferences.getInt(LAST_VERSION_UPDATE_DIALOG_SHOWN, -1) // -1 means never shown any update dialog

        // Update to 2.2.0-alpha.0 (version code 10)
        if (lastVersionNumber < 10)
            showUpdateAlert(getString(R.string.version_name_2_2_0), listOf(
                Pair(getString(R.string.new_feature_authentication), getString(R.string.new_feature_authentication_text)),
                Pair(getString(R.string.cards_and_passwords_in_one_place), getString(R.string.cards_and_passwords_in_one_place_text)),
                // TODO more
            ))

        // Write new version code for future update dialog
        sharedPreferences.edit().putInt(LAST_VERSION_UPDATE_DIALOG_SHOWN, BuildConfig.VERSION_CODE).apply()
    }

    companion object {
        const val LAST_VERSION_UPDATE_DIALOG_SHOWN = "last_version_dialog_shown"
    }

}