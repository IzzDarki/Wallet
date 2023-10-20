package com.izzdarki.wallet.ui.home

import android.content.DialogInterface
import izzdarki.wallet.R
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.os.Bundle
import android.content.Intent
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import com.izzdarki.wallet.ui.credentials.EditCredentialActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.izzdarki.wallet.data.Credential
import com.izzdarki.wallet.data.CredentialStableIDKeyProvider
import com.izzdarki.wallet.logic.updates.removeOldPreferences
import com.izzdarki.wallet.ui.credentials.CredentialActivity
import com.izzdarki.wallet.storage.AppPreferenceManager
import com.izzdarki.wallet.storage.AppPreferenceManager.SortingType
import com.izzdarki.wallet.services.CreateExampleCredentialService
import com.izzdarki.wallet.storage.CredentialPreferenceStorage
import com.izzdarki.wallet.utils.MultiSelectItemDetailsLookup
import com.izzdarki.wallet.utils.Utility.attachDragAndDropToRecyclerView
import com.izzdarki.wallet.utils.Utility.setPaddingBottom

class HomeCredentialsFragment
    : Fragment(R.layout.fragment_home_credentials) {

    companion object {
        const val CARD_SPAN_COUNT_PORTRAIT = 2
        const val CARD_SPAN_COUNT_LANDSCAPE = 4
        const val SELECTION_ID = "cards_selection" // identifies the selection
    }

    // UI
    private lateinit var cardGridRecyclerView: RecyclerView
    private lateinit var plusButton: FloatingActionButton

    // variables
    private val cards: MutableList<Credential> = mutableListOf() // val is necessary so adapter has the same object
    private lateinit var selectionTracker: SelectionTracker<Long>
    private var wasCreatedRecently = true
    private var searchQuery: String = ""
    private lateinit var clearSelectionOnBackPressedCallback: OnBackPressedCallback


    // lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // this fragment uses the action bar

        Log.d("asdf", "onCreate in HomeCredentialsFragment")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("asdf", "onViewCreated in HomeCredentialsFragment")

        // hooks
        cardGridRecyclerView = view.findViewById(R.id.fragment_home_cards_card_grid_recycler_view)
        plusButton = view.findViewById(R.id.fragment_home_cards_plus_button)

        updateCards() // This requires reading all credentials, this is called before

        // back button
        clearSelectionOnBackPressedCallback = object : OnBackPressedCallback(enabled = false) {
            override fun handleOnBackPressed() {
                selectionTracker.clearSelection()
            }
        }
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner,
            clearSelectionOnBackPressedCallback
        )

        // Plus button (FAB)
        plusButton.setOnClickListener {
            createNewCard()
        }

        // Card grid recycler view
        cardGridRecyclerView.layoutManager = getCardGridLayoutManager()
        val adapter = CardAdapter(cards)
        cardGridRecyclerView.adapter = adapter
        cardGridRecyclerView.doOnPreDraw { // Add bottom padding
            it.setPaddingBottom(plusButton.height + 2 * plusButton.paddingBottom)
            // needed to prevent the floating action button to overlap with the recyclerview
        }

        // Drag and drop
        attachDragAndDropToRecyclerView(
            cardGridRecyclerView,
            cards
        ) {
            AppPreferenceManager.setCredentialsSortingType(requireContext(), SortingType.CustomSorting) // change sorting type to custom
            AppPreferenceManager.setCredentialsSortReverse(requireContext(), false) // the current sorting is not reverse (even if it was reverse before moving an item)
            AppPreferenceManager.setCredentialsCustomSortingOrder(
                requireContext(),
                cards.map { it.id }
            ) // save the custom sorting to preferences

            selectionTracker.clearSelection()
        }

        // Selection tracker
        selectionTracker = SelectionTracker.Builder(
            SELECTION_ID,
            cardGridRecyclerView,
            CredentialStableIDKeyProvider(cards),
            MultiSelectItemDetailsLookup(cardGridRecyclerView),
            StorageStrategy.createLongStorage()
        ).withSelectionPredicate(
            SelectionPredicates.createSelectAnything()
        ).build()
        adapter.selectionTracker = selectionTracker

        selectionTracker.addObserver(object : SelectionTracker.SelectionObserver<Long>() {
            override fun onSelectionChanged() {
                clearSelectionOnBackPressedCallback.isEnabled =
                    (selectionTracker.selection.size() > 0)
                activity?.invalidateOptionsMenu() // reload action bar menu
            }
        })

        initFirstRun()

        wasCreatedRecently = true
    }

    override fun onPause() {
        super.onPause()
        selectionTracker.clearSelection() // When fragment is no longer visible clear selection
        clearSelectionOnBackPressedCallback.isEnabled = false
    }
    
    override fun onResume() {
        super.onResume()
        if (wasCreatedRecently)
            wasCreatedRecently = false
        else { // This code does not run if onResume is called directly after onViewCreated
            updateCardsAndNotifyAdapter()
        }
        clearSelectionOnBackPressedCallback.isEnabled = (selectionTracker.selection.size() > 0)
    }


    // main functions
    private fun createNewCard() {
        val intent = Intent(requireContext(), EditCredentialActivity::class.java)
        intent.putExtra(EditCredentialActivity.EXTRA_CREATE_NEW_CREDENTIAL, true)
        startActivity(intent)
    }
    
    /**
     * Sorts the cards list, notifies the adapter and saves the new sorting type to preferences
     */
    private fun sortCards(sortingType: SortingType) {
        AppPreferenceManager.setCredentialsSortingType(requireContext(), sortingType) // updates sorting type in preferences
        sortCardsArray(sortingType)
        cardGridRecyclerView.adapter?.notifyDataSetChanged()
    }

    fun updateCardsAndNotifyAdapter() {
        if (updateCards())
            cardGridRecyclerView.adapter?.notifyDataSetChanged()
    }

    private fun updateCards(): Boolean {
        val oldCards = cards.toList()
        cards.clear()
        cards.addAll(CredentialPreferenceStorage.readAllCredentials(requireContext()).toMutableList())
        sortCardsArray(AppPreferenceManager.getCredentialsSortingType(requireContext())) // sort the list according to saved sorting type
        if (searchQuery != "") {
            cards.retainAll {
                it.name.contains(searchQuery, ignoreCase = true)
                        || it.labels.any { label -> label.contains(searchQuery, ignoreCase = true) }
            }
        }
        return cards != oldCards
    }

    private fun editCard(id: Int) {
        val intent = Intent(requireContext(), EditCredentialActivity::class.java)
        intent.putExtra(CredentialActivity.EXTRA_CREDENTIAL_ID, id)
        startActivity(intent)
    }

    private fun deleteSingleCard(id: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_entry)
            .setMessage(R.string.delete_entry_dialog_message)
            .setCancelable(true)
            .setPositiveButton(R.string.delete) { dialog, _ ->
                deleteCardDirectly(id)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog: DialogInterface, _: Int ->
                dialog.cancel()
            }
            .show()
    }

    private fun deleteMultipleCards(ids: List<Int>) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_x_entries).format(ids.size))
            .setMessage(R.string.delete_x_entries_dialog_message)
            .setCancelable(true)
            .setPositiveButton(R.string.delete) { dialog, _ ->
                for (id in ids)
                    deleteCardDirectly(id)
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
        when { // Inflate the correct menu
            selectionTracker.selection.size() == 1 -> inflater.inflate(R.menu.home_action_bar_with_one_item_selected_menu, menu)
            selectionTracker.selection.size() > 1 -> inflater.inflate(R.menu.home_action_bar_with_multiple_items_selected_menu, menu)
            else -> inflater.inflate(R.menu.home_action_bar_menu, menu)
        }

        // SearchView
        val searchItem = menu.findItem(R.id.home_action_bar_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = getString(R.string.home_search_hint)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchQuery = query ?: ""
                updateCardsAndNotifyAdapter() // updates card (also filters according to searchQuery)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return this.onQueryTextSubmit(newText) // submit on every change
            }
        })

        // Set checkboxes
        when (AppPreferenceManager.getCredentialsSortingType(requireContext())) {
            SortingType.ByName -> menu.findItem(R.id.home_action_bar_sort_by_name)?.isChecked = true
            SortingType.CustomSorting -> menu.findItem(R.id.home_action_bar_sort_custom_order)?.isChecked = true
            SortingType.ByCreationDate -> menu.findItem(R.id.home_action_bar_sort_by_creation_date)?.isChecked = true
            SortingType.ByAlterationDate -> menu.findItem(R.id.home_action_bar_sort_by_alteration_date)?.isChecked = true
        }
        menu.findItem(R.id.home_action_bar_sort_reverse)?.isChecked = AppPreferenceManager.isCredentialsSortReverse(requireContext())
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
                AppPreferenceManager.setCredentialsSortReverse(requireContext(), item.isChecked)
                sortCards(AppPreferenceManager.getCredentialsSortingType(requireContext())) // sort again
            }
            R.id.home_action_bar_edit_selected_item -> {
                editCard(id = selectionTracker.selection.first().toInt())
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

    private fun deleteCardDirectly(id: Int) {
        val index = cards.indexOfFirst { it.id == id }
        if (index == -1) // card not found
            return

        CredentialActivity.deleteCredentialWithImages(requireContext(), cards[index])

        selectionTracker.deselect(id.toLong())
        cards.removeAt(index)
        cardGridRecyclerView.adapter?.notifyItemRemoved(index)

    }

    /**
     * Helper for [sortCards] and used in [updateCards]
     */
    private fun sortCardsArray(sortingType: SortingType) {
        when (sortingType) {
            SortingType.ByName -> cards.sortBy { it.name }
            SortingType.CustomSorting -> {
                val savedCustomSortingIDs = AppPreferenceManager.getCredentialsCustomSortingOrder(requireContext())
                cards.sortBy { savedCustomSortingIDs.indexOf(it.id) }
            }
            SortingType.ByCreationDate -> cards.sortBy { it.creationDate }
            SortingType.ByAlterationDate -> cards.sortBy { it.alterationDate }
        }

        if (AppPreferenceManager.isCredentialsSortReverse(requireContext()))
            cards.reverse()
    }

    private fun calcCardWidth(spanCount: Int): Double {
        return (calculatedLayoutWidth / spanCount - 2 * resources.getDimension(R.dimen.small_card_item_margin)).toDouble()
    }

    private val calculatedLayoutWidth: Float
        get() = resources.displayMetrics.widthPixels - 2 * resources.getDimension(R.dimen.default_padding)


    /** Will execute the code only once (first time HomeCardsFragment gets created after installation or data removal) */
    private fun initFirstRun() {
        val firstRunKey = "home_cards_fragment_first_run_done"
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        if (!sharedPreferences.getBoolean(firstRunKey, false)) {
            createExampleCard()
            // set first run preference to false
            sharedPreferences.edit().putBoolean(firstRunKey, true).apply()
        }
    }

    private fun createExampleCard() {
        val intent = Intent(requireContext(), CreateExampleCredentialService::class.java)
        val resultReceiver = object : ResultReceiver(Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                updateCardsAndNotifyAdapter()
            }
        }
        CreateExampleCredentialService.enqueueWork(requireContext(), intent, resultReceiver)
    }
}
