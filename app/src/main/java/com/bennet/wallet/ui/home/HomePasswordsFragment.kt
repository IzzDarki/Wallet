package com.bennet.wallet.ui.home

import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bennet.wallet.R
import com.bennet.wallet.ui.passwords.EditPasswordActivity
import com.bennet.wallet.preferences.AppPreferenceManager
import com.bennet.wallet.preferences.AppPreferenceManager.SortingType
import com.bennet.wallet.preferences.PasswordPreferenceManager
import com.bennet.wallet.utils.*
import com.bennet.wallet.utils.Utility.attachDragAndDropToRecyclerView
import com.bennet.wallet.utils.Utility.setPaddingBottom
import com.google.android.material.floatingactionbutton.FloatingActionButton

class HomePasswordsFragment()
    : Fragment(R.layout.fragment_home_passwords) {

    companion object {
        const val PASSWORD_SPAN_COUNT_PORTRAIT = HomeCardsFragment.CARD_SPAN_COUNT_PORTRAIT
        const val PASSWORD_SPAN_COUNT_LANDSCAPE  = HomeCardsFragment.CARD_SPAN_COUNT_LANDSCAPE
        const val SELECTION_ID = "passwords_selection" // identifies the selection
    }

    // UI
    private lateinit var recyclerView: RecyclerView
    private lateinit var plusButton: FloatingActionButton

    // variables
    private val passwords: MutableList<CardOrPasswordPreviewData> = mutableListOf()
    private lateinit var selectionTracker: SelectionTracker<Long>
    private var init = false

    // lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // this fragment uses the action bar
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // hooks
        plusButton = view.findViewById(R.id.fragment_home_passwords_plus_button)
        recyclerView = view.findViewById(R.id.fragment_home_passwords_recycler_view)

        updatePasswords()

        // plus button
        plusButton.setOnClickListener {
            createNewPassword()
        }

        // Passwords recycler view
        recyclerView.layoutManager = getPasswordGridLayoutManager()
        val adapter = PasswordAdapter(passwords)
        recyclerView.adapter = adapter
        recyclerView.doOnPreDraw { // Add bottom padding
            it.setPaddingBottom(plusButton.height + 2 * plusButton.paddingBottom)
            // needed to prevent the floating action button to overlap with the recyclerview
        }

        // Drag and drop
        attachDragAndDropToRecyclerView(
            recyclerView,
            passwords
        ) {
            AppPreferenceManager.setPasswordsSortingType(requireContext(), SortingType.CustomSorting) // change sorting type to custom
            AppPreferenceManager.setPasswordsSortReverse(requireContext(), false) // the current sorting is not reverse (even if it was reverse before moving an item)
            PasswordPreferenceManager.writeCustomSortingNoGrouping(
                requireContext(),
                Utility.PreferenceArrayInt(passwords.map { it.ID }.iterator())
            ) // save the custom sorting to preferences
            selectionTracker.clearSelection()
        }

        // selection tracker
        selectionTracker = SelectionTracker.Builder(
            SELECTION_ID,
            recyclerView,
            CardOrPasswordStableIDKeyProvider(passwords),
            MultiSelectItemDetailsLookup(recyclerView),
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
            updatePasswords()
            recyclerView.adapter?.notifyDataSetChanged()
        }
        else
            init = true // if this is the first run of onResume, don't update passwords
    }


    // main functions
    private fun createNewPassword() {
        val intent = Intent(requireContext(), EditPasswordActivity::class.java)
        intent.putExtra(EditPasswordActivity.EXTRA_CREATE_NEW_PASSWORD, true)
        startActivity(intent)
    }

    /**
     * Sorts the passwords list, notifies the adapter and saves the new sorting type to preferences
     */
    private fun sortPasswords(sortingType: SortingType) {
        // Doesn't care about grouping by label at the moment

        AppPreferenceManager.setPasswordsSortingType(requireContext(), sortingType) // updates sorting type in preferences
        sortPasswordsArray(sortingType)
        recyclerView.adapter?.notifyDataSetChanged()
    }

    private fun editPassword(passwordID: Int) {
        val intent = Intent(context, EditPasswordActivity::class.java)
        intent.putExtra(EditPasswordActivity.EXTRA_PASSWORD_ID, passwordID)
        startActivity(intent)
    }

    private fun deleteSinglePassword(passwordID: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_password)
            .setMessage(R.string.delete_password_dialog_message)
            .setCancelable(true)
            .setPositiveButton(R.string.delete) { dialog, _ ->
                deletePasswordDirectly(passwordID)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog: DialogInterface, _: Int ->
                dialog.cancel()
            }
            .show()
    }

    private fun deleteMultiplePasswords(passwordIDs: List<Int>) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_x_passwords).format(passwordIDs.size))
            .setMessage(R.string.delete_x_passwords_dialog_message)
            .setCancelable(true)
            .setPositiveButton(R.string.delete) { dialog, _ ->
                for (passwordID in passwordIDs)
                    deletePasswordDirectly(passwordID)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog: DialogInterface, _: Int ->
                dialog.cancel()
            }
            .show()
    }


    // action bar
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear() // Needed because HomeCardsFragment also accesses this menu
        when {
            selectionTracker.selection.size() == 1 -> inflater.inflate(R.menu.home_action_bar_with_one_item_selected_menu, menu)
            selectionTracker.selection.size() > 1 -> inflater.inflate(R.menu.home_action_bar_with_multiple_items_selected_menu, menu)
            else -> inflater.inflate(R.menu.home_action_bar_menu, menu)
        }

        // Set checkboxes
        when (AppPreferenceManager.getPasswordsSortingType(requireContext())) {
            SortingType.ByName -> menu.findItem(R.id.home_action_bar_sort_by_name)?.isChecked = true
            SortingType.CustomSorting -> menu.findItem(R.id.home_action_bar_sort_custom_order)?.isChecked = true
            SortingType.ByCreationDate -> menu.findItem(R.id.home_action_bar_sort_by_creation_date)?.isChecked = true
            SortingType.ByAlterationDate -> menu.findItem(R.id.home_action_bar_sort_by_alteration_date)?.isChecked = true
        }
        menu.findItem(R.id.home_action_bar_sort_reverse)?.isChecked = AppPreferenceManager.isPasswordsSortReverse(requireContext())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home_action_bar_sort_by_name -> {
                item.isChecked = true
                sortPasswords(SortingType.ByName)
            }
            R.id.home_action_bar_sort_custom_order -> {
                item.isChecked = true
                sortPasswords(SortingType.CustomSorting)
            }
            R.id.home_action_bar_sort_by_creation_date -> {
                item.isChecked = true
                sortPasswords(SortingType.ByCreationDate)
            }
            R.id.home_action_bar_sort_by_alteration_date -> {
                item.isChecked = true
                sortPasswords(SortingType.ByAlterationDate)
            }
            R.id.home_action_bar_sort_reverse -> {
                item.isChecked = !item.isChecked // toggle checkbox
                AppPreferenceManager.setPasswordsSortReverse(requireContext(), item.isChecked)
                sortPasswords(AppPreferenceManager.getPasswordsSortingType(requireContext())) // sort again
            }
            R.id.home_action_bar_edit_selected_item -> {
                editPassword(passwordID = selectionTracker.selection.first().toInt())
            }
            R.id.home_action_bar_delete_selected_item -> {
                selectionTracker.let { selectionTracker ->
                    if (selectionTracker.selection.size() == 1)
                        deleteSinglePassword(selectionTracker.selection.first().toInt())
                    else
                        deleteMultiplePasswords(selectionTracker.selection.map { it.toInt() })
                }
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    // TODO onBackPressed: https://stackoverflow.com/questions/5448653/how-to-implement-onbackpressed-in-fragments


    // screen orientation change
    override fun onConfigurationChanged(newConfig: Configuration) {
        recyclerView.layoutManager = getPasswordGridLayoutManager()
        super.onConfigurationChanged(newConfig)
    }


    // helper
    private fun updatePasswords() {
        passwords.clear()
        passwords.addAll(PasswordPreferenceManager.readAll(requireContext()))
        sortPasswordsArray(AppPreferenceManager.getPasswordsSortingType(requireContext())) // sort the list according to saved sorting type
    }

    private fun getPasswordGridLayoutManager(): StaggeredGridLayoutManager {
        val portrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val spanCount = if (portrait) PASSWORD_SPAN_COUNT_PORTRAIT else PASSWORD_SPAN_COUNT_LANDSCAPE

        return StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL)
    }

    private fun deletePasswordDirectly(passwordID: Int) {
        PasswordPreferenceManager.removeComplete(requireContext(), passwordID)

        val indexRemoved = passwords.indexOfFirst { it.ID == passwordID }
        passwords.removeAt(indexRemoved)
        recyclerView.adapter?.notifyItemRemoved(indexRemoved)
    }

    /**
     * Helper for [sortPasswords] and used in [onResume]
     */
    private fun sortPasswordsArray(sortingType: SortingType) {
        when (sortingType) {
            SortingType.ByName -> passwords.sortBy { it.name }
            SortingType.CustomSorting -> {
                val savedCustomSortingIDs = PasswordPreferenceManager.readCustomSortingNoGrouping(requireContext())
                passwords.sortBy { savedCustomSortingIDs.indexOf(it.ID) }
            }
            SortingType.ByCreationDate -> passwords.sortBy {
                val x = PasswordPreferenceManager.readCreationDate(requireContext(), it.ID)
                x
            }
            SortingType.ByAlterationDate -> passwords.sortBy {
                val x = PasswordPreferenceManager.readAlterationDate(requireContext(), it.ID)
                x
            }
        }

        if (AppPreferenceManager.isPasswordsSortReverse(requireContext()))
            passwords.reverse()
    }

}
