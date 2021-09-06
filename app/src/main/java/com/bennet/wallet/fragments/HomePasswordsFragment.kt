package com.bennet.wallet.fragments

import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bennet.wallet.R
import com.bennet.wallet.activities.passwords.EditPasswordActivity
import com.bennet.wallet.adapters.PasswordAdapter
import com.bennet.wallet.preferences.AppPreferenceManager
import com.bennet.wallet.preferences.AppPreferenceManager.SortingType
import com.bennet.wallet.preferences.AppPreferenceManager.isAppFunctionPasswords
import com.bennet.wallet.preferences.PasswordPreferenceManager
import com.bennet.wallet.utils.CardOrPasswordPreviewData
import com.bennet.wallet.utils.MultiSelectItemDetailsLookup
import com.bennet.wallet.utils.StableIDKeyProvider
import com.bennet.wallet.utils.Utility
import com.bennet.wallet.utils.Utility.downUntil
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*


class HomePasswordsFragment()
    : Fragment(R.layout.fragment_home_passwords) {

    companion object {
        const val PASSWORD_SPAN_COUNT_PORTRAIT = HomeCardsFragment.CARD_SPAN_COUNT_PORTRAIT
        const val PASSWORD_SPAN_COUNT_LANDSCAPE  = HomeCardsFragment.CARD_SPAN_COUNT_LANDSCAPE
        const val SELECTION_ID = "passwords_selection" // identifies the selection
    }

    // UI
    private lateinit var passwordsRecyclerView: RecyclerView
    private lateinit var plusButton: FloatingActionButton

    // variables
    private var passwords: MutableList<CardOrPasswordPreviewData> = mutableListOf()
    private lateinit var selectionTracker: SelectionTracker<Long>


    // lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true) // this fragment uses the action bar
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // hooks
        passwordsRecyclerView = view.findViewById(R.id.fragment_home_passwords_passwords_recycler_view)
        plusButton = view.findViewById(R.id.fragment_home_passwords_plus_button)

        // recycler view
        setPasswordGridLayoutManager()
        val adapter = PasswordAdapter(passwords)
        passwordsRecyclerView.adapter = adapter

        ItemTouchHelper(
            object: ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.START or ItemTouchHelper.END,
                0
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val fromPos = viewHolder.adapterPosition
                    val toPos = target.adapterPosition

                    // move item in the list, by swapping with neighbour until the destination position is reached (Moves all the items between one up or down)
                    if (fromPos < toPos) {
                        for (i in fromPos until toPos)
                            Collections.swap(passwords, i, i + 1)
                    }
                    else {
                        for (i in fromPos downUntil toPos)
                            Collections.swap(passwords, i, i - 1)
                    }
                    recyclerView.adapter?.notifyItemMoved(fromPos, toPos) // calling notifyItemMoved is enough

                    AppPreferenceManager.setPasswordsSortingType(requireContext(), SortingType.CustomSorting) // change sorting type to custom
                    AppPreferenceManager.setPasswordsSortReverse(requireContext(), false) // the current sorting is not reverse (even if it was reverse before moving an item)
                    PasswordPreferenceManager.writePasswordsCustomSortingNoGrouping(
                        requireContext(),
                        Utility.PreferenceArrayInt(passwords.map { it.ID }.iterator())
                    ) // save the custom sorting to preferences

                    return false
                }
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            }
        ).attachToRecyclerView(passwordsRecyclerView)

        // selection tracker
        selectionTracker = SelectionTracker.Builder(
            SELECTION_ID,
            passwordsRecyclerView,
            StableIDKeyProvider(passwords),
            MultiSelectItemDetailsLookup(passwordsRecyclerView),
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

        // plus button
        plusButton.setOnClickListener {
            createNewPassword()
        }
    }

    override fun onPause() {
        super.onPause()
        selectionTracker.clearSelection() // When fragment is no longer visible clear selection
    }

    override fun onResume() {
        super.onResume()
        updatePasswords()
    }


    // main functions
    private fun createNewPassword() {
        val intent = Intent(requireContext(), EditPasswordActivity::class.java)
        intent.putExtra(EditPasswordActivity.EXTRA_CREATE_NEW_PASSWORD, true)
        startActivity(intent)
    }

    private fun updatePasswords() {
        val passwordIDs = PasswordPreferenceManager.readAllPasswordIDs(requireContext())
        passwords.clear()
        for (passwordID in passwordIDs) {
            passwords.add(
                CardOrPasswordPreviewData(
                    passwordID,
                    PasswordPreferenceManager.readPasswordName(requireContext(), passwordID),
                    PasswordPreferenceManager.readPasswordColor(requireContext(), passwordID)
                )
            )
        }
        sortPasswordsArray(AppPreferenceManager.getPasswordsSortingType(requireContext())) // sort the list according to saved sorting type
        passwordsRecyclerView.adapter?.notifyDataSetChanged()
    }

    /**
     * Sorts the passwords list, notifies the adapter and saves the new sorting type to preferences
     */
    private fun sortPasswords(sortingType: SortingType) {
        // Doesn't care about grouping by label at the moment

        AppPreferenceManager.setPasswordsSortingType(requireContext(), sortingType) // updates sorting type in preferences
        sortPasswordsArray(sortingType)
        passwordsRecyclerView.adapter?.notifyDataSetChanged()
    }

    private fun editPassword(passwordID: Int) {
        val intent = Intent(context, EditPasswordActivity::class.java)
        intent.putExtra(EditPasswordActivity.EXTRA_PASSWORD_ID, passwordID)
        startActivity(intent)
    }

    private fun deleteSinglePassword(passwordID: Int) {
        AlertDialog.Builder(ContextThemeWrapper(context, R.style.RoundedCornersDialog))
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
        AlertDialog.Builder(ContextThemeWrapper(context, R.style.RoundedCornersDialog))
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
        when {
            isSingleSelected -> inflater.inflate(R.menu.home_action_bar_with_one_item_selected_menu, menu)
            isMultipleSelected -> inflater.inflate(R.menu.home_action_bar_with_multiple_items_selected_menu, menu)
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
        menu.findItem(R.id.home_action_bar_group_by_label)?.isChecked = AppPreferenceManager.isPasswordsGroupByLabels(requireContext())
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
            R.id.home_action_bar_group_by_label -> {
                item.isChecked = !item.isChecked // toggle checkbox
                AppPreferenceManager.setPasswordsGroupByLabels(requireContext(), item.isChecked)
            }
            R.id.home_action_bar_edit_selected_item -> {
                editPassword(passwordID = selectionTracker.selection.first().toInt())
            }
            R.id.home_action_bar_delete_selected_item -> {
                if (selectionTracker.selection.size() == 1)
                    deleteSinglePassword(selectionTracker.selection.first().toInt())
                else
                    deleteMultiplePasswords(selectionTracker.selection.map { it.toInt() })
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    // TODO onBackPressed: https://stackoverflow.com/questions/5448653/how-to-implement-onbackpressed-in-fragments


    // screen orientation change
    override fun onConfigurationChanged(newConfig: Configuration) {
        setPasswordGridLayoutManager()
        super.onConfigurationChanged(newConfig)
    }

    private fun setPasswordGridLayoutManager() {
        val portrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

        val spanCount = if (portrait) PASSWORD_SPAN_COUNT_PORTRAIT else PASSWORD_SPAN_COUNT_LANDSCAPE
        passwordsRecyclerView.layoutManager = StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL)
    }


    // helper
    private val isSingleSelected: Boolean get() = selectionTracker.selection.size() == 1
    private val isMultipleSelected: Boolean get() = selectionTracker.selection.size() > 1

    private fun deletePasswordDirectly(passwordID: Int) {
        PasswordPreferenceManager.removePassword(context, passwordID)

        val indexRemoved = passwords.indexOfFirst { it.ID == passwordID }
        passwords.removeAt(indexRemoved)
        passwordsRecyclerView.adapter?.notifyItemRemoved(indexRemoved)
    }

    /**
     * Helper for [sortPasswords] and used in [updatePasswords]
     */
    private fun sortPasswordsArray(sortingType: SortingType) {
        when (sortingType) {
            SortingType.ByName -> passwords.sortBy { it.name }
            SortingType.CustomSorting -> {
                val savedCustomSortingIDs = PasswordPreferenceManager.readPasswordsCustomSortingNoGrouping(requireContext())
                passwords.sortBy { savedCustomSortingIDs.indexOf(it.ID) }
            }
            SortingType.ByCreationDate -> passwords.sortBy { PasswordPreferenceManager.readPasswordCreationDate(context, it.ID) }
            SortingType.ByAlterationDate -> passwords.sortBy { PasswordPreferenceManager.readPasswordAlterationDate(context, it.ID) }
        }

        if (AppPreferenceManager.isPasswordsSortReverse(requireContext()))
            passwords.reverse()
    }

}
