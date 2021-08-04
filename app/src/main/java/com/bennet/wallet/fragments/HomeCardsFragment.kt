package com.bennet.wallet.fragments

import com.bennet.wallet.R
import com.bennet.wallet.adapters.CardAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.bennet.wallet.utils.Utility.PreferenceArrayInt
import android.os.Bundle
import android.content.Intent
import android.content.res.Configuration
import android.view.View
import androidx.fragment.app.Fragment
import com.bennet.wallet.activities.cards.EditCardActivity
import com.bennet.wallet.preferences.CardPreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import com.bennet.wallet.utils.CardOrPasswordPreviewData

class HomeCardsFragment : Fragment(R.layout.fragment_home_cards) {

    companion object {
        const val CARD_SPAN_COUNT_PORTRAIT = 2
        const val CARD_SPAN_COUNT_LANDSCAPE = 4
    }

    // UI
    private lateinit var cardGridRecyclerView: RecyclerView
    private lateinit var plusButton: FloatingActionButton

    // variables
    private var cards: MutableList<CardOrPasswordPreviewData> = mutableListOf()

    // preferences
    private lateinit var cardIDs: PreferenceArrayInt


    // lifecycle
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cardGridRecyclerView = view.findViewById(R.id.fragment_home_cards_card_grid_recycler_view)
        plusButton = view.findViewById(R.id.fragment_home_cards_plus_button)

        // card grid recycler view
        setCardGridLayoutManager()
        cardGridRecyclerView.adapter = CardAdapter(cards)
        plusButton.setOnClickListener {
            createNewCard()
        }
    }

    override fun onResume() {
        super.onResume()
        updateCards()
    }


    // main functions
    private fun createNewCard() {
        val intent = Intent(requireContext(), EditCardActivity::class.java)
        intent.putExtra(EditCardActivity.EXTRA_CREATE_NEW_CARD, true)
        startActivity(intent)
    }

    fun updateCards() {
        cardIDs = CardPreferenceManager.readAllCardIDs(requireContext())
        cards.clear()
        for (cardID in cardIDs) {
            cards.add(
                CardOrPasswordPreviewData(
                    cardID,
                    CardPreferenceManager.readCardName(requireContext(), cardID),
                    CardPreferenceManager.readCardColor(requireContext(), cardID)
                )
            )
        }
        cardGridRecyclerView.adapter?.notifyDataSetChanged()
    }


    // screen orientation change
    override fun onConfigurationChanged(newConfig: Configuration) {
        setCardGridLayoutManager() // update layout manager, because span count is different in landscape and portrait
        super.onConfigurationChanged(newConfig)
    }


    // helper
    private fun setCardGridLayoutManager() {
        val portrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        var spanCount = CARD_SPAN_COUNT_PORTRAIT
        if (!portrait) spanCount = CARD_SPAN_COUNT_LANDSCAPE
        CardAdapter.cardWidth = calcCardWidth(spanCount) // update card width, because it changes when spanCount changes
        cardGridRecyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)
    }

    private fun calcCardWidth(spanCount: Int): Double {
        return (calculatedLayoutWidth / spanCount - 2 * resources.getDimension(R.dimen.small_card_item_margin)).toDouble()
    }

    private val calculatedLayoutWidth: Float
        get() = resources.displayMetrics.widthPixels - 2 * resources.getDimension(R.dimen.default_padding)

}