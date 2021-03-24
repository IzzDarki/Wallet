package com.bennet.wallet;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;


/*
 * TODO list
 *
 * TODO Optional list (future releases)
 *  - Alert dialogs verschönern (Eigene Dialog-Klasse mit RecyclerView)
 *  - selectable items in RecyclerView in HomeActivity (to delete) ((maybe drag and drop position))
 *  - animations</p>
 *  - consider using glide library for bitmaps https://developer.android.com/topic/performance/graphics (if there's a significant performance issue)
 */

public class HomeActivity extends AppCompatActivity implements SmallCardAdapter.OnItemClickListener, NavigationView.OnNavigationItemSelectedListener {

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
    protected RecyclerView cardGridRecyclerView;
    protected FloatingActionButton plusButton;
    protected MaterialToolbar toolbar;
    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected MaterialTextView versionNumberTextView;
    protected ActionBarDrawerToggle toggle;

    // preferences
    protected Utility.PreferenceArrayInt cardIDs;

    // variables
    protected List<SmallCard> cards = new ArrayList<>();

    // lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // hooks
        constraintLayout = findViewById(R.id.home_constraint_layout);
        cardGridRecyclerView = findViewById(R.id.home_card_grid_recycler_view);
        plusButton = findViewById(R.id.home_plus_button);
        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.home_drawer_layout);
        navigationView = findViewById(R.id.home_nav_view);
        versionNumberTextView = findViewById(R.id.home_nav_view_version_number_text_view);

        // card grid recycler view
        cardGridRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        SmallCardAdapter adapter = new SmallCardAdapter(this, cards, getCardWidth());
        adapter.setOnItemClickListener(this);
        cardGridRecyclerView.setAdapter(adapter);

        plusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewCard();
            }
        });

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
        clearCachedCardImages();
        updateCards();
    }

    @Override
    public void onItemClick(View view, int position) {
        showCard(cards.get(position).card_ID);
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_home:
                break;

            case R.id.nav_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;

            default:
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


    // main functions
    protected void showCard(int cardID) {
        Intent intent = new Intent(HomeActivity.this, ShowCardActivity.class);
        intent.putExtra(CardActivity.EXTRA_CARD_ID, cardID);
        startActivity(intent);
    }

    protected void createNewCard() {
        Intent intent = new Intent(HomeActivity.this, EditCardActivity.class);
        intent.putExtra(EditCardActivity.EXTRA_CREATE_NEW_CARD, true);
        startActivity(intent);
    }


    // subroutines
    protected void clearCachedCardImages() {
        Intent intent = new Intent(this, ClearDirectoryService.class);
        intent.putExtra(ClearDirectoryService.EXTRA_DIRECTORY_NAME, getCacheDir() + "/" + getString(R.string.cards_images_folder_name));
        ClearDirectoryService.enqueueWork(this, intent);
    }

    protected void updateCards() {
        cardIDs = CardPreferenceManager.readAllCardIDs(this);
        cards.clear();
        for (int cardID : cardIDs)
            cards.add(new SmallCard(cardID, CardPreferenceManager.readCardName(this, cardID), CardPreferenceManager.readCardColor(this, cardID)));
        cardGridRecyclerView.getAdapter().notifyDataSetChanged();
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
        updateCards(); // to keep existing cards

        Intent intent = new Intent(this, CreateExampleCardService.class);
        CreateExampleCardService.enqueueWork(this, intent, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode == RESULT_OK)
                    updateCards();
                else {
                    /*
                    if (BuildConfig.DEBUG)
                        Log.w("EditCardActivity", "Result code from CreateExampleCardService is not RESULT_OK, instead: " + resultCode);
                     */
                }
            }
        });
    }


    // helper functions
    protected double getCardWidth() {
        return ((getCalculatedLayoutWidth() / 2) - (2 * getResources().getDimension(R.dimen.small_card_item_margin)));
    }

    protected float getCalculatedLayoutWidth() {
        return getResources().getDisplayMetrics().widthPixels - 2 * getResources().getDimension(R.dimen.card_padding);
    }

}
