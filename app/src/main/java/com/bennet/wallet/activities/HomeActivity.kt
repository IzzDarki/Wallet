package com.bennet.wallet.activities

import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.bennet.wallet.R
import com.bennet.wallet.fragments.HomeCardsFragment
import com.bennet.wallet.fragments.HomePasswordsFragment
import com.bennet.wallet.preferences.AppPreferenceManager
import com.bennet.wallet.services.ClearDirectoryService
import com.bennet.wallet.services.CreateExampleCardService
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textview.MaterialTextView

class HomeActivity : AppCompatActivity() {
    // UI
    private lateinit var constraintLayout: ConstraintLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var sideNavigationView: NavigationView
    private lateinit var versionNumberTextView: MaterialTextView
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var fragmentViewPager: ViewPager2

    private enum class State {
        CardsAndPasswords, Cards, Passwords
    }

    // adapter for view pager
    private inner class ScreenSlidePagerAdapter(fragment: FragmentActivity)
        : FragmentStateAdapter(fragment) {

        private var context: Context = fragment

        private var state: State = (
            if (AppPreferenceManager.isAppFunctionCards(context) && AppPreferenceManager.isAppFunctionPasswords(context))
                State.CardsAndPasswords
            else if (AppPreferenceManager.isAppFunctionCards(context))
                State.Cards
            else
                State.Passwords
        )

        val isStateUpToDate: Boolean
            get() {
                if (AppPreferenceManager.isAppFunctionCards(context)
                    && AppPreferenceManager.isAppFunctionPasswords(context)
                    && state == State.CardsAndPasswords
                ) return true

                if (AppPreferenceManager.isAppFunctionCards(context)
                    && !AppPreferenceManager.isAppFunctionPasswords(context)
                    && state == State.Cards
                ) return true

                if (!AppPreferenceManager.isAppFunctionCards(context)
                    && AppPreferenceManager.isAppFunctionPasswords(context)
                    && state == State.Passwords
                ) return true

                return false
            }

        override fun createFragment(position: Int): Fragment {
            return when(state) {
                State.CardsAndPasswords -> {
                    if (position == 0)
                        HomeCardsFragment()
                    else
                        HomePasswordsFragment()
                }
                State.Cards ->
                    HomeCardsFragment()
                State.Passwords ->
                    HomePasswordsFragment()
            }
        }

        override fun getItemCount(): Int {
            return if (state == State.CardsAndPasswords) 2 else 1
        }
    }

    // lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // hooks
        constraintLayout = findViewById(R.id.home_constraint_layout)
        toolbar = findViewById(R.id.toolbar)
        drawerLayout = findViewById(R.id.home_drawer_layout)
        sideNavigationView = findViewById(R.id.home_nav_view)
        versionNumberTextView = findViewById(R.id.home_nav_view_version_number_text_view)
        bottomNavigationView = findViewById(R.id.home_bottom_navigation)
        fragmentViewPager = findViewById(R.id.home_fragment_view_pager)

        // toolbar
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar!!.setHomeAsUpIndicator(R.drawable.icon_menu_24dp)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // navigation drawer
        toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.open_navigation_drawer,
            R.string.close_navigation_drawer
        )
        drawerLayout.addDrawerListener(toggle)

        //side navigation view
        sideNavigationView.bringToFront()
        sideNavigationView.setNavigationItemSelectedListener { item: MenuItem ->
            onSideNavigationItemSelected(
                item
            )
        }

        // bottom navigation view
        bottomNavigationView.setOnNavigationItemSelectedListener { item: MenuItem ->
            onBottomNavigationItemSelected(
                item
            )
        }

        // version number text view
        versionNumberTextView.text = String.format(
            getString(R.string.version_name_format),
            getString(R.string.version_name)
        )

        // fragment view pager
        fragmentViewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == 0)
                    bottomNavigationView.selectedItemId = R.id.home_bottom_navigation_menu_cards
                else
                    bottomNavigationView.selectedItemId = R.id.home_bottom_navigation_menu_passwords
            }
        })

        // init for first run
        initFirstRun()
    }

    override fun onPostCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onPostCreate(savedInstanceState, persistentState)
        toggle.syncState()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        sideNavigationView.setCheckedItem(R.id.nav_home)

        // bottom navigation (could be changed when coming back from SettingsActivity)
        if (!AppPreferenceManager.isAppFunctionCards(this) || !AppPreferenceManager.isAppFunctionPasswords(this))
            bottomNavigationView.visibility = View.GONE
        else
            bottomNavigationView.visibility = View.VISIBLE

        // fragment view pager (reload if settings have changed)
        val currentAdapter = fragmentViewPager.adapter as? ScreenSlidePagerAdapter
        if (currentAdapter == null || !currentAdapter.isStateUpToDate)
            fragmentViewPager.adapter = ScreenSlidePagerAdapter(this)
    }

    override fun onStop() {
        super.onStop()

        // Since onStop is probably(?) guaranteed to be called, this is a good place to clear cached card images
        clearCachedCardImages()
    }

    private fun onSideNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.nav_settings) {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        } else return false
        drawerLayout.closeDrawer(sideNavigationView)
        return true
    }

    private fun onBottomNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home_bottom_navigation_menu_cards -> setCardsFragment()
            R.id.home_bottom_navigation_menu_passwords -> setPasswordsFragment()
            else -> return false
        }
        return true
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerVisible(sideNavigationView)) drawerLayout.closeDrawer(
            sideNavigationView
        ) else super.onBackPressed()
    }

    private fun clearCachedCardImages() {
        val intent = Intent(this, ClearDirectoryService::class.java)
        intent.putExtra(
            ClearDirectoryService.EXTRA_DIRECTORY_NAME,
            cacheDir.toString() + "/" + getString(R.string.cards_images_folder_name)
        )
        ClearDirectoryService.enqueueWork(this, intent)
    }

    /**
     * Will execute the code only once (first time HomeActivity gets created after installation or data removal)
     */
    private fun initFirstRun() {
        val HOME_ACTIVITY_FIRST_RUN_DONE = "com.bennet.wallet.home_activity_first_run_done"
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (!sharedPreferences.getBoolean(HOME_ACTIVITY_FIRST_RUN_DONE, false)) {
            createExampleCard()

            // set first run preference to false
            sharedPreferences.edit().putBoolean(HOME_ACTIVITY_FIRST_RUN_DONE, true).apply()
        }
    }

    private fun createExampleCard() {
        val intent = Intent(this, CreateExampleCardService::class.java)
        val resultReceiver = object : ResultReceiver(
            Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                val fragment = supportFragmentManager.findFragmentByTag("f0") // f0 is the Tag of the view at position 0 in the fragmentViewPager // this could be a cause for bugs in the future
                if (fragment is HomeCardsFragment) // also checks null
                    fragment.updateCards()
                else if (fragment == null)
                    Log.e("WalletImportant", "From: HomeActivity.createExampleCard(): Couldn't find HomeCardsFragment")
            }
        }

        CreateExampleCardService.enqueueWork(this, intent, resultReceiver)
    }

    private fun setCardsFragment() {
        fragmentViewPager.setCurrentItem(0, true)
    }

    private fun setPasswordsFragment() {
        fragmentViewPager.setCurrentItem(1, true)
    }
}