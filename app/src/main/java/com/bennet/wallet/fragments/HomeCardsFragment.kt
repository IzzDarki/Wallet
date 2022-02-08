package com.bennet.wallet.fragments

import android.content.DialogInterface
import com.bennet.wallet.R
import com.bennet.wallet.adapters.CardAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.bennet.wallet.utils.Utility.PreferenceArrayInt
import android.os.Bundle
import android.content.Intent
import android.content.res.Configuration
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import com.bennet.wallet.activities.cards.EditCardActivity
import com.bennet.wallet.preferences.CardPreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import com.bennet.wallet.activities.cards.CardActivity
import com.bennet.wallet.preferences.AppPreferenceManager
import com.bennet.wallet.preferences.AppPreferenceManager.SortingType
import com.bennet.wallet.utils.CardOrPasswordPreviewData
import com.bennet.wallet.utils.CardOrPasswordStableIDKeyProvider
import com.bennet.wallet.utils.MultiSelectItemDetailsLookup
import com.bennet.wallet.utils.Utility.attachDragAndDropToRecyclerView

class HomeCardsFragment
    : Fragment(R.layout.fragment_home_cards) {

    companion object {
        const val CARD_SPAN_COUNT_PORTRAIT = 2
        const val CARD_SPAN_COUNT_LANDSCAPE = 4
        const val SELECTION_ID = "cards_selection" // identifies the selection
    }

    // UI
    private lateinit var cardGridRecyclerView: RecyclerView
    private lateinit var plusButton: FloatingActionButton

    // variables
    private var cards: MutableList<CardOrPasswordPreviewData> = mutableListOf()
    private lateinit var selectionTracker: SelectionTracker<Long>
    private var init = false

    // preferences
    private lateinit var cardIDs: PreferenceArrayInt


    // lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // this fragment uses the action bar
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // hooks
        cardGridRecyclerView = view.findViewById(R.id.fragment_home_cards_card_grid_recycler_view)
        plusButton = view.findViewById(R.id.fragment_home_cards_plus_button)

        updateCards()

        // card grid recycler view
        cardGridRecyclerView.layoutManager = getCardGridLayoutManager()
        val adapter = CardAdapter(cards)
        cardGridRecyclerView.adapter = adapter
        plusButton.setOnClickListener {
            createNewCard()
        }

        // drag and drop
        attachDragAndDropToRecyclerView(
            cardGridRecyclerView,
            cards
        ) {
            AppPreferenceManager.setCardsSortingType(requireContext(), SortingType.CustomSorting) // change sorting type to custom
            AppPreferenceManager.setCardsSortReverse(requireContext(), false) // the current sorting is not reverse (even if it was reverse before moving an item)
            CardPreferenceManager.writeCustomSortingNoGrouping(
                requireContext(),
                PreferenceArrayInt(cards.map { it.ID }.iterator())
            ) // save the custom sorting to preferences

            selectionTracker.clearSelection()
        }

        // selection tracker
        selectionTracker = SelectionTracker.Builder(
            SELECTION_ID,
            cardGridRecyclerView,
            CardOrPasswordStableIDKeyProvider(cards),
            MultiSelectItemDetailsLookup(cardGridRecyclerView),
            StorageStrategy.createLongStorage()
        ).withSelectionPredicate(
            SelectionPredicates.createSelectAnything()
        ).build()
        adapter.selectionTracker = selectionTracker

        selectionTracker.addObserver(object : SelectionTracker.SelectionObserver<Long>() {
            override fun onSelectionChanged() {
                activity?.invalidateOptionsMenu() // reload action bar menu
            }
        })
    }

    override fun onPause() {
        super.onPause()
        selectionTracker.clearSelection() // When fragment is no longer visible clear selection
    }
    
    override fun onResume() {
        super.onResume()
        if (init) {
            // This code does not run if this is the first time that onResume is being called
            updateCardsAndNotifyAdapter()
        }
        else
            init = true
    }


    // main functions
    private fun createNewCard() {
        val intent = Intent(requireContext(), EditCardActivity::class.java)
        intent.putExtra(EditCardActivity.EXTRA_CREATE_NEW_CARD, true)
        startActivity(intent)
    }
    
    /**
     * Sorts the cards list, notifies the adapter and saves the new sorting type to preferences
     */
    private fun sortCards(sortingType: SortingType) {
        // Doesn't care about grouping by label at the moment

        AppPreferenceManager.setCardsSortingType(requireContext(), sortingType) // updates sorting type in preferences
        sortCardsArray(sortingType)
        cardGridRecyclerView.adapter?.notifyDataSetChanged()
    }

    fun updateCardsAndNotifyAdapter() {
        updateCards()
        cardGridRecyclerView.adapter?.notifyDataSetChanged()
    }

    private fun updateCards() {
        cardIDs = CardPreferenceManager.readAllIDs(requireContext())
        cards.clear()
        for (cardID in cardIDs) {
            cards.add(
                CardOrPasswordPreviewData(
                    cardID,
                    CardPreferenceManager.readName(requireContext(), cardID),
                    CardPreferenceManager.readColor(requireContext(), cardID)
                )
            )
        }
        sortCardsArray(AppPreferenceManager.getCardsSortingType(requireContext())) // sort the list according to saved sorting type
    }

    private fun editCard(ID: Int) {
        val intent = Intent(requireContext(), EditCardActivity::class.java)
        intent.putExtra(CardActivity.EXTRA_CARD_ID, ID)
        startActivity(intent)
    }

    private fun deleteSingleCard(ID: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_card)
            .setMessage(R.string.delete_card_dialog_message)
            .setCancelable(true)
            .setPositiveButton(R.string.delete) { dialog, _ ->
                deleteCardDirectly(ID)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog: DialogInterface, _: Int ->
                dialog.cancel()
            }
            .show()
    }

    private fun deleteMultipleCards(IDs: List<Int>) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_x_cards).format(IDs.size))
            .setMessage(R.string.delete_x_cards_dialog_message)
            .setCancelable(true)
            .setPositiveButton(R.string.delete) { dialog, _ ->
                for (ID in IDs)
                    deleteCardDirectly(ID)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog: DialogInterface, _: Int ->
                dialog.cancel()
            }
            .show()
    }
    

    // action bar
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear() // Needed because HomePasswordsFragment also accesses this menu
        when {
            selectionTracker.selection.size() == 1 -> inflater.inflate(R.menu.home_action_bar_with_one_item_selected_menu, menu)
            selectionTracker.selection.size() > 1 -> inflater.inflate(R.menu.home_action_bar_with_multiple_items_selected_menu, menu)
            else -> inflater.inflate(R.menu.home_action_bar_menu, menu)
        }

        // Set checkboxes
        when (AppPreferenceManager.getCardsSortingType(requireContext())) {
            SortingType.ByName -> menu.findItem(R.id.home_action_bar_sort_by_name)?.isChecked = true
            SortingType.CustomSorting -> menu.findItem(R.id.home_action_bar_sort_custom_order)?.isChecked = true
            SortingType.ByCreationDate -> menu.findItem(R.id.home_action_bar_sort_by_creation_date)?.isChecked = true
            SortingType.ByAlterationDate -> menu.findItem(R.id.home_action_bar_sort_by_alteration_date)?.isChecked = true
        }
        menu.findItem(R.id.home_action_bar_sort_reverse)?.isChecked = AppPreferenceManager.isCardsSortReverse(requireContext())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home_action_bar_sort_by_name -> {
                item.isChecked = true
                sortCards(SortingType.ByName)
            }
            R.id.home_action_bar_sort_custom_order -> {
                item.isChecked = true
                sortCards(SortingType.CustomSorting)
            }
            R.id.home_action_bar_sort_by_creation_date -> {
                item.isChecked = true
                sortCards(SortingType.ByCreationDate)
            }
            R.id.home_action_bar_sort_by_alteration_date -> {
                item.isChecked = true
                sortCards(SortingType.ByAlterationDate)
            }
            R.id.home_action_bar_sort_reverse -> {
                item.isChecked = !item.isChecked // toggle checkbox
                AppPreferenceManager.setCardsSortReverse(requireContext(), item.isChecked)
                sortCards(AppPreferenceManager.getCardsSortingType(requireContext())) // sort again
            }
            R.id.home_action_bar_edit_selected_item -> {
                editCard(ID = selectionTracker.selection.first().toInt())
            }
            R.id.home_action_bar_delete_selected_item -> {
                if (selectionTracker.selection.size() == 1)
                    deleteSingleCard(selectionTracker.selection.first().toInt())
                else
                    deleteMultipleCards(selectionTracker.selection.map { it.toInt() })
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    // TODO onBackPressed: https://stackoverflow.com/questions/5448653/how-to-implement-onbackpressed-in-fragments


    // screen orientation change
    override fun onConfigurationChanged(newConfig: Configuration) {
        // update layout manager, because span count is different in landscape and portrait
        cardGridRecyclerView.layoutManager = getCardGridLayoutManager()
        super.onConfigurationChanged(newConfig)
    }


    // helper
    private fun getCardGridLayoutManager(): GridLayoutManager  {
        val portrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        var spanCount = CARD_SPAN_COUNT_PORTRAIT
        if (!portrait) spanCount = CARD_SPAN_COUNT_LANDSCAPE
        CardAdapter.cardWidth = calcCardWidth(spanCount) // update card width, because it changes when spanCount changes
        return GridLayoutManager(requireContext(), spanCount)
    }

    private fun deleteCardDirectly(ID: Int) {
        CardPreferenceManager.removeComplete(requireContext(), ID)

        val indexRemoved = cards.indexOfFirst { it.ID == ID }
        cards.removeAt(indexRemoved)
        cardGridRecyclerView.adapter?.notifyItemRemoved(indexRemoved)
    }

    /**
     * Helper for [sortCards] and used in [updateCards]
     */
    private fun sortCardsArray(sortingType: SortingType) {
        when (sortingType) {
            SortingType.ByName -> cards.sortBy { it.name }
            SortingType.CustomSorting -> {
                val savedCustomSortingIDs = CardPreferenceManager.readCustomSortingNoGrouping(requireContext())
                cards.sortBy { savedCustomSortingIDs.indexOf(it.ID) }
            }
            SortingType.ByCreationDate -> cards.sortBy { CardPreferenceManager.readCreationDate(requireContext(), it.ID) }
            SortingType.ByAlterationDate -> cards.sortBy { CardPreferenceManager.readAlterationDate(requireContext(), it.ID) }
        }

        if (AppPreferenceManager.isCardsSortReverse(requireContext()))
            cards.reverse()
    }

    private fun calcCardWidth(spanCount: Int): Double {
        return (calculatedLayoutWidth / spanCount - 2 * resources.getDimension(R.dimen.small_card_item_margin)).toDouble()
    }

    private val calculatedLayoutWidth: Float
        get() = resources.displayMetrics.widthPixels - 2 * resources.getDimension(R.dimen.default_padding)

}
