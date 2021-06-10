package com.bennet.wallet.fragments;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bennet.wallet.R;
import com.bennet.wallet.activities.cards.CardActivity;
import com.bennet.wallet.activities.cards.EditCardActivity;
import com.bennet.wallet.activities.cards.ShowCardActivity;
import com.bennet.wallet.adapters.SmallCardAdapter;
import com.bennet.wallet.preferences.CardPreferenceManager;
import com.bennet.wallet.utils.Utility;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class HomeCardsFragment extends Fragment implements SmallCardAdapter.OnItemClickListener{

    static public final int CARD_SPAN_COUNT_PORTRAIT = 2;
    static public final int CARD_SPAN_COUNT_LANDSCAPE = 4;

    // UI
    protected RecyclerView cardGridRecyclerView;
    protected FloatingActionButton plusButton;

    // variables
    protected List<SmallCard> cards = new ArrayList<>();

    // preferences
    protected Utility.PreferenceArrayInt cardIDs;

    // class for small card properties (preview of the real card)
    static public class SmallCard {
        public int cardID;
        public String name;
        public @ColorInt
        int color;

        public SmallCard(int cardID, String name, @ColorInt int color) {
            this.cardID = cardID;
            this.name = name;
            this.color = color;
        }
    }

    // constructor
    public HomeCardsFragment() {
        super(R.layout.fragment_home_cards);
    }

    // lifecycle
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cardGridRecyclerView = getView().findViewById(R.id.fragment_home_cards_card_grid_recycler_view);
        plusButton = getView().findViewById(R.id.fragment_home_cards_plus_button);

        // card grid recycler view
        setCardGridLayoutManager();
        SmallCardAdapter adapter = new SmallCardAdapter(requireContext(), cards);
        adapter.setOnItemClickListener(this);
        cardGridRecyclerView.setAdapter(adapter);

        plusButton.setOnClickListener(v -> createNewCard());
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCards();
    }


    // main functions
    public void showCard(int cardID) {
        Intent intent = new Intent(requireContext(), ShowCardActivity.class);
        intent.putExtra(CardActivity.EXTRA_CARD_ID, cardID);
        startActivity(intent);
    }

    public void createNewCard() {
        Intent intent = new Intent(requireContext(), EditCardActivity.class);
        intent.putExtra(EditCardActivity.EXTRA_CREATE_NEW_CARD, true);
        startActivity(intent);
    }

    public void updateCards() {
        cardIDs = CardPreferenceManager.readAllCardIDs(requireContext());
        cards.clear();
        for (int cardID : cardIDs) {
            @ColorInt int cardColor = CardPreferenceManager.readCardColor(requireContext(), cardID);
            cards.add(new SmallCard(cardID, CardPreferenceManager.readCardName(requireContext(), cardID), cardColor));
        }
        cardGridRecyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        setCardGridLayoutManager(); // update layout manager, because span count is different in landscape and portrait
        super.onConfigurationChanged(newConfig);
    }


    // helper functions
    protected void setCardGridLayoutManager() {
        boolean portrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        int spanCount = CARD_SPAN_COUNT_PORTRAIT;
        if (!portrait)
            spanCount = CARD_SPAN_COUNT_LANDSCAPE;

        SmallCardAdapter.setCardWidth(getCardWidth(spanCount)); // update card width, because it changes when spanCount changes
        cardGridRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), spanCount));
    }

    protected double getCardWidth(int spanCount) {
        return ((getCalculatedLayoutWidth() / spanCount) - (2 * getResources().getDimension(R.dimen.small_card_item_margin)));
    }

    protected float getCalculatedLayoutWidth() {
        return getResources().getDisplayMetrics().widthPixels - 2 * getResources().getDimension(R.dimen.default_padding);
    }

    // Recycler view callback
    @Override
    public void onItemClick(View view, int position) {
        showCard(cards.get(position).cardID);
    }
}