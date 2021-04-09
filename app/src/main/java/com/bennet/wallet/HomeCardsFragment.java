package com.bennet.wallet;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class HomeCardsFragment extends Fragment implements SmallCardAdapter.OnItemClickListener{

    static public String FRAGMENT_TAG = "home_cards_fragment";

    // UI
    protected RecyclerView cardGridRecyclerView;
    protected FloatingActionButton plusButton;

    // variables
    protected List<HomeActivity.SmallCard> cards = new ArrayList<>();

    // preferences
    protected Utility.PreferenceArrayInt cardIDs;

    // constructor
    public HomeCardsFragment() {
        super(R.layout.fragment_home_cards);
    }

    // lifecycle
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cardGridRecyclerView = getView().findViewById(R.id.fragment_home_card_grid_recycler_view);
        plusButton = getView().findViewById(R.id.fragment_home_plus_button);

        // card grid recycler view
        cardGridRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        SmallCardAdapter adapter = new SmallCardAdapter(requireContext(), cards, getCardWidth());
        adapter.setOnItemClickListener(this);
        cardGridRecyclerView.setAdapter(adapter);

        plusButton.setOnClickListener(v -> createNewCard());
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCards();
    }

    // Recycler view callback
    @Override
    public void onItemClick(View view, int position) {
        showCard(cards.get(position).card_ID);
    }


    // main functions
    protected void showCard(int cardID) {
        Intent intent = new Intent(requireContext(), ShowCardActivity.class);
        intent.putExtra(CardActivity.EXTRA_CARD_ID, cardID);
        startActivity(intent);
    }

    protected void createNewCard() {
        Intent intent = new Intent(requireContext(), EditCardActivity.class);
        intent.putExtra(EditCardActivity.EXTRA_CREATE_NEW_CARD, true);
        startActivity(intent);
    }

    protected void updateCards() {
        cardIDs = CardPreferenceManager.readAllCardIDs(requireContext());
        cards.clear();
        for (int cardID : cardIDs)
            cards.add(new HomeActivity.SmallCard(cardID, CardPreferenceManager.readCardName(requireContext(), cardID), CardPreferenceManager.readCardColor(requireContext(), cardID)));
        cardGridRecyclerView.getAdapter().notifyDataSetChanged();
    }

    // helper functions
    protected double getCardWidth() {
        return ((getCalculatedLayoutWidth() / 2) - (2 * getResources().getDimension(R.dimen.small_card_item_margin)));
    }

    protected float getCalculatedLayoutWidth() {
        return getResources().getDisplayMetrics().widthPixels - 2 * getResources().getDimension(R.dimen.card_padding);
    }
}