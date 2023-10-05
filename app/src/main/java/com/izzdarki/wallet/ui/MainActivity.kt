package com.izzdarki.wallet.ui

import android.content.Intent
import android.os.Bundle
import androidx.core.view.GravityCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.izzdarki.wallet.logic.AuthenticatedAppCompatActivity
import com.izzdarki.wallet.services.ClearDirectoryService
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

    }

    override fun onStop() {
        super.onStop()

        // Since onStop is probably(?) guaranteed to be called, this is a good place to clear cached card images
        // Clear cached card images
        val intent = Intent(this, ClearDirectoryService::class.java)
        intent.putExtra(
            ClearDirectoryService.EXTRA_DIRECTORY_NAME,
            cacheDir.toString() + "/" + getString(R.string.cards_images_folder_name)
        )
        ClearDirectoryService.enqueueWork(this, intent)
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

}