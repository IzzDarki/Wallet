package com.bennet.wallet;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.preference.PreferenceManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.ResultReceiver;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    static public class SmallCard {
        public int card_ID;
        public String name;
        public @ColorInt int color;

        public SmallCard(int card_ID, String name, @ColorInt int color) {
            this.card_ID = card_ID;
            this.name = name;
            this.color = color;
        }
    }

    // UI
    protected ConstraintLayout constraintLayout;

    protected MaterialToolbar toolbar;
    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected MaterialTextView versionNumberTextView;
    protected ActionBarDrawerToggle toggle;

    // lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // hooks
        constraintLayout = findViewById(R.id.home_constraint_layout);
        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.home_drawer_layout);
        navigationView = findViewById(R.id.home_nav_view);
        versionNumberTextView = findViewById(R.id.home_nav_view_version_number_text_view);

        // toolbar
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(R.drawable.menu_icon_24dp);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // navigation drawer
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_navigation_drawer, R.string.close_navigation_drawer);
        drawerLayout.addDrawerListener(toggle);

        // navigation view
        navigationView.bringToFront();
        navigationView.setNavigationItemSelectedListener(this);

        // version number text view
        versionNumberTextView.setText(String.format(getString(R.string.version_name_format), getString(R.string.version_name)));

        // init for first run
        initFirstRun();

        // initial fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.home_fragment_container, HomeCardsFragment.class, null, HomeCardsFragment.FRAGMENT_TAG)
                    .commit();
        }
    }

    @Override
    public void onPostCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onPostCreate(savedInstanceState, persistentState);
        toggle.syncState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        navigationView.setCheckedItem(R.id.nav_home);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Since onStop is probably(?) guaranteed to be called, this is a good place to clear cached card images
        clearCachedCardImages();
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_home) {
        } else if (itemId == R.id.nav_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else {
            return false;
        }
        drawerLayout.closeDrawer(navigationView);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerVisible(navigationView))
            drawerLayout.closeDrawer(navigationView);
        else
            super.onBackPressed();
    }

    // subroutines
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
        // updateCards(); // to keep existing cards // TODO I think this is not needed anymore, but i'm not sure

        Intent intent = new Intent(this, CreateExampleCardService.class);
        CreateExampleCardService.enqueueWork(this, intent, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode == RESULT_OK) {
                    HomeCardsFragment fragment = (HomeCardsFragment) getSupportFragmentManager().findFragmentByTag(HomeCardsFragment.FRAGMENT_TAG);
                    if (fragment != null)
                        fragment.updateCards();
                }
                else {
                    /*
                    if (BuildConfig.DEBUG)
                        Log.w("EditCardActivity", "Result code from CreateExampleCardService is not RESULT_OK, instead: " + resultCode);
                     */
                }
            }
        });
    }

}
