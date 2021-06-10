package com.bennet.wallet.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.bennet.wallet.preferences.AppPreferenceManager;
import com.bennet.wallet.services.ClearDirectoryService;
import com.bennet.wallet.services.CreateExampleCardService;
import com.bennet.wallet.fragments.HomeCardsFragment;
import com.bennet.wallet.fragments.HomePasswordsFragment;
import com.bennet.wallet.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textview.MaterialTextView;

public class HomeActivity extends AppCompatActivity {

    static protected String CARDS_FRAGMENT_TAG = "cards_fragment";
    static protected String PASSWORDS_FRAGMENT_TAG = "passwords_fragment";

    // UI
    protected ConstraintLayout constraintLayout;

    protected MaterialToolbar toolbar;
    protected DrawerLayout drawerLayout;
    protected NavigationView sideNavigationView;
    protected MaterialTextView versionNumberTextView;
    protected ActionBarDrawerToggle toggle;
    protected BottomNavigationView bottomNavigationView;
    protected ViewPager2 fragmentViewPager;

    // adapter for view pager
    static public class ScreenSlidePagerAdapter extends FragmentStateAdapter {

        protected Context context;

        enum State {
            cardsAndPasswords,
            cards,
            passwords
        }
        protected State state;

        public ScreenSlidePagerAdapter(@NonNull FragmentActivity fragment) {
            super(fragment);
            context = fragment;
            if (AppPreferenceManager.isAppFunctionCards(context) && AppPreferenceManager.isAppFunctionPasswords(context))
                state = State.cardsAndPasswords;
            else if (AppPreferenceManager.isAppFunctionCards(context))
                state = State.cards;
            else
                state = State.passwords;
        }

        public boolean isStateUpToDate() {
            if (AppPreferenceManager.isAppFunctionCards(context) && AppPreferenceManager.isAppFunctionPasswords(context) && state == State.cardsAndPasswords)
                return true;
            if (AppPreferenceManager.isAppFunctionCards(context) && !AppPreferenceManager.isAppFunctionPasswords(context) && state == State.cards)
                return true;
            if (!AppPreferenceManager.isAppFunctionCards(context) && AppPreferenceManager.isAppFunctionPasswords(context) && state == State.passwords)
                return true;
            return false;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (state == State.cardsAndPasswords) {
                if (position == 0)
                    return new HomeCardsFragment();
                else
                    return new HomePasswordsFragment();
            }
            else if (state == State.cards)
                return new HomeCardsFragment();
            else
                return new HomePasswordsFragment();
        }

        @Override
        public int getItemCount() {
            if (state == State.cardsAndPasswords)
                return 2;
            else
                return 1;
        }
    }

    // lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // hooks
        constraintLayout = findViewById(R.id.home_constraint_layout);
        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.home_drawer_layout);
        sideNavigationView = findViewById(R.id.home_nav_view);
        versionNumberTextView = findViewById(R.id.home_nav_view_version_number_text_view);
        bottomNavigationView = findViewById(R.id.home_bottom_navigation);
        fragmentViewPager = findViewById(R.id.home_fragment_view_pager);

        // toolbar
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(R.drawable.menu_icon_24dp);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // navigation drawer
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_navigation_drawer, R.string.close_navigation_drawer);
        drawerLayout.addDrawerListener(toggle);

        //side navigation view
        sideNavigationView.bringToFront();
        sideNavigationView.setNavigationItemSelectedListener(this::onSideNavigationItemSelected);

        // bottom navigation view
        bottomNavigationView.setOnNavigationItemSelectedListener(this::onBottomNavigationItemSelected);

        // version number text view
        versionNumberTextView.setText(String.format(getString(R.string.version_name_format), getString(R.string.version_name)));

        // fragment view pager
        fragmentViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == 0)
                    bottomNavigationView.setSelectedItemId(R.id.home_bottom_navigation_menu_cards);
                else
                    bottomNavigationView.setSelectedItemId(R.id.home_bottom_navigation_menu_passwords);
            }
        });

        // init for first run
        initFirstRun();

    }

    @Override
    public void onPostCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onPostCreate(savedInstanceState, persistentState);
        toggle.syncState();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

    }

    @Override
    protected void onResume() {
        super.onResume();
        sideNavigationView.setCheckedItem(R.id.nav_home);

        // bottom navigation (could be changed when coming back from SettingsActivity)
        if (!AppPreferenceManager.isAppFunctionCards(this) || !AppPreferenceManager.isAppFunctionPasswords(this))
            bottomNavigationView.setVisibility(View.GONE);
        else
            bottomNavigationView.setVisibility(View.VISIBLE);

        // fragment view pager (reload if settings have changed)
        ScreenSlidePagerAdapter currentAdapter = (ScreenSlidePagerAdapter)fragmentViewPager.getAdapter();
        if (currentAdapter == null || !currentAdapter.isStateUpToDate())
            fragmentViewPager.setAdapter(new ScreenSlidePagerAdapter(this));
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Since onStop is probably(?) guaranteed to be called, this is a good place to clear cached card images
        clearCachedCardImages();
    }

    protected boolean onSideNavigationItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.nav_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else
            return false;

        drawerLayout.closeDrawer(sideNavigationView);
        return true;
    }

    protected boolean onBottomNavigationItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.home_bottom_navigation_menu_cards)
            setCardsFragment();
        else if (item.getItemId() == R.id.home_bottom_navigation_menu_passwords)
            setPasswordsFragment();
        else
            return false;

        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerVisible(sideNavigationView))
            drawerLayout.closeDrawer(sideNavigationView);
        else
            super.onBackPressed();
    }

    protected void clearCachedCardImages() {
        Intent intent = new Intent(this, ClearDirectoryService.class);
        intent.putExtra(ClearDirectoryService.EXTRA_DIRECTORY_NAME, getCacheDir() + "/" + getString(R.string.cards_images_folder_name));
        ClearDirectoryService.enqueueWork(this, intent);
    }

    /**
     * Will execute the code only once (first time HomeActivity gets created after installation or data removal)
     */
    private void initFirstRun() {
        final String HOME_ACTIVITY_FIRST_RUN_DONE = "com.bennet.wallet.home_activity_first_run_done";
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!sharedPreferences.getBoolean(HOME_ACTIVITY_FIRST_RUN_DONE, false)) {

            createExampleCard();

            // set first run preference to false
            sharedPreferences.edit().putBoolean(HOME_ACTIVITY_FIRST_RUN_DONE, true).apply();
        }
    }

    protected void createExampleCard() {
        Intent intent = new Intent(this, CreateExampleCardService.class);
        CreateExampleCardService.enqueueWork(this, intent, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode == RESULT_OK) {
                    Fragment fragment = getSupportFragmentManager().findFragmentByTag("f0"); // f0 is the Tag of the view at position 0 in the fragmentViewPager // this could be a cause for bugs in the future
                    if (fragment instanceof HomeCardsFragment) // also checks null
                        ((HomeCardsFragment) fragment).updateCards();
                    else if (fragment == null)
                        Log.e("WalletImportant", "From: HomeActivity.createExampleCard(): Couldn't find HomeCardsFragment");
                }
                /*
                else {
                    if (BuildConfig.DEBUG)
                        Log.w("EditCardActivity", "Result code from CreateExampleCardService is not RESULT_OK, instead: " + resultCode);
                }
                 */
            }
        });
    }

    protected void setCardsFragment() {
        fragmentViewPager.setCurrentItem(0, true);
    }

    protected void setPasswordsFragment() {
        fragmentViewPager.setCurrentItem(1, true);
    }
}
