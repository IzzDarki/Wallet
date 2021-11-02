package com.bennet.wallet.fragments

import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bennet.wallet.R
import com.bennet.wallet.activities.passwords.EditPasswordActivity
import com.bennet.wallet.adapters.ExpandingListAdapter
import com.bennet.wallet.adapters.PasswordAdapter
import com.bennet.wallet.preferences.AppPreferenceManager
import com.bennet.wallet.preferences.AppPreferenceManager.SortingType
import com.bennet.wallet.preferences.PasswordPreferenceManager
import com.bennet.wallet.utils.*
import com.bennet.wallet.utils.Utility.attachDragAndDropToRecyclerView
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
    private lateinit var recyclerViewContainer: FrameLayout

    // variables
    private val passwords: MutableList<CardOrPasswordPreviewData> = mutableListOf()
    private val labelsList: MutableList<ExpandingListAdapter.GroupInfo> = mutableListOf()
    private var selectionTracker: SelectionTracker<Long>? = null
    private var init = false
    private val isGroupByLabels get() = AppPreferenceManager.isPasswordsGroupByLabels(requireContext())

    // lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // this fragment uses the action bar
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // hooks
        plusButton = view.findViewById(R.id.fragment_home_passwords_plus_button)
        recyclerViewContainer = view.findViewById(R.id.fragment_home_passwords_recycler_view_container)

        // plus button
        plusButton.setOnClickListener {
            createNewPassword()
        }

        updateRecyclerViewGrouping()
    }

    override fun onPause() {
        super.onPause()
        selectionTracker?.clearSelection() // When fragment is no longer visible clear selection
    }

    override fun onResume() {
        super.onResume()

        if (init) {
            updatePasswords()
            if (isGroupByLabels)
                updateLabelsList()

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

    private fun enableGrouping() {
        updatePasswords()
        updateLabelsList()

        selectionTracker = null
        createRecyclerView()

        recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = ExpandingListAdapter<PasswordAdapter>(
            groups = labelsList,
            childLayoutManagerFactory = this::getPasswordGridLayoutManager
        ) { holder, pos ->

            val passwordsWithLabel = if (pos != labelsList.size - 1) {
                holder.groupHeader.setTypeface(null, Typeface.BOLD)

                passwords.filter {
                    PasswordPreferenceManager.readLabels(requireContext(), it.ID)
                        .contains(labelsList[pos].name)
                }
            } else {
                holder.groupHeader.setTypeface(null, Typeface.BOLD_ITALIC)
                passwords
            }

            val adapter = PasswordAdapter(passwordsWithLabel)
            holder.contentsRecyclerView.adapter = adapter

            // selection tracker
            val selectionTracker = SelectionTracker.Builder(
                "$SELECTION_ID-$pos",
                holder.contentsRecyclerView,
                CardOrPasswordStableIDKeyProvider(passwordsWithLabel),
                MultiSelectItemDetailsLookup(holder.contentsRecyclerView),
                StorageStrategy.createLongStorage()
            ).withSelectionPredicate(
                SelectionPredicates.createSelectAnything()
            ).build()
            adapter.selectionTracker = selectionTracker

            // drag and drop
            attachDragAndDropToRecyclerView(
                holder.contentsRecyclerView,
                passwordsWithLabel
            ) {
                AppPreferenceManager.setPasswordsSortingType(requireContext(), SortingType.CustomSorting) // change sorting type to custom
                AppPreferenceManager.setPasswordsSortReverse(requireContext(), false) // the current sorting is not reverse (even if it was reverse before moving an item)
                PasswordPreferenceManager.writeCustomSortingNoGrouping(
                    requireContext(),
                    Utility.PreferenceArrayInt(passwordsWithLabel.map { it.ID }.iterator())
                ) // save the custom sorting to preferences
                selectionTracker.clearSelection()
            }

            selectionTracker.addObserver(object : SelectionTracker.SelectionObserver<Long>() {
                override fun onSelectionChanged() {
                    if (selectionTracker !== this@HomePasswordsFragment.selectionTracker) {
                        this@HomePasswordsFragment.selectionTracker?.clearSelection()
                        this@HomePasswordsFragment.selectionTracker = selectionTracker
                    }
                    activity?.invalidateOptionsMenu() // reload action bar menu
                }
            })
        }
    }

    private fun disableGrouping() {
        updatePasswords()

        createRecyclerView()
        recyclerView.layoutManager = getPasswordGridLayoutManager()

        val adapter = PasswordAdapter(passwords)
        recyclerView.adapter = adapter

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
        adapter.selectionTracker = selectionTracker!!

        // drag and drop
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
            selectionTracker?.clearSelection()
        }

        selectionTracker?.addObserver(object : SelectionTracker.SelectionObserver<Long>() {
            override fun onSelectionChanged() {
                activity?.invalidateOptionsMenu() // reload action bar menu
            }
        })
    }


    // action bar
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        when {
            selectionTracker != null && selectionTracker!!.selection.size() == 1 -> inflater.inflate(R.menu.home_action_bar_with_one_item_selected_menu, menu)
            selectionTracker != null && selectionTracker!!.selection.size() > 1 -> inflater.inflate(R.menu.home_action_bar_with_multiple_items_selected_menu, menu)
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
                updateRecyclerViewGrouping()
            }
            R.id.home_action_bar_edit_selected_item -> {
                editPassword(passwordID = selectionTracker!!.selection.first().toInt())
            }
            R.id.home_action_bar_delete_selected_item -> {
                selectionTracker?.let { selectionTracker ->
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
        if (isGroupByLabels) {
            for (index in 0..passwords.size) {
                val viewHolder = recyclerView.findViewHolderForAdapterPosition(index) as ExpandingListAdapter<*>.ViewHolder
                viewHolder.contentsRecyclerView.layoutManager = getPasswordGridLayoutManager()
            }
        }
        else {
            recyclerView.layoutManager = getPasswordGridLayoutManager()
        }
        super.onConfigurationChanged(newConfig)
    }


    // helper
    private fun updatePasswords() {
        passwords.clear()
        passwords.addAll(PasswordPreferenceManager.readAll(requireContext()))
        sortPasswordsArray(AppPreferenceManager.getPasswordsSortingType(requireContext())) // sort the list according to saved sorting type
    }

    private fun updateLabelsList() {
        labelsList.clear()
        labelsList.addAll(
            PasswordPreferenceManager.collectAllLabels(requireContext()).toMutableList()
                .map { ExpandingListAdapter.GroupInfo(it) }
        )
        labelsList.add(ExpandingListAdapter.GroupInfo(
            getString(R.string.all_passwords),
            false
        ))
    }

    private fun updateRecyclerViewGrouping() {
        Log.d("asdf", "Enable/disable grouping")
        // Enable/disable grouping
        if (isGroupByLabels)
            enableGrouping()
        else
            disableGrouping()
    }

    private fun createRecyclerView() {
        recyclerView = RecyclerView(requireContext())
        recyclerView.layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.MATCH_PARENT)
        recyclerView.clipToPadding = false
        recyclerViewContainer.removeAllViews()
        recyclerViewContainer.addView(recyclerView)
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
        if (!isGroupByLabels)
            recyclerView.adapter?.notifyItemRemoved(indexRemoved)
        else {
            updateLabelsList()
            recyclerView.adapter?.notifyDataSetChanged()
        }
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
            SortingType.ByCreationDate -> passwords.sortBy { PasswordPreferenceManager.readCreationDate(requireContext(), it.ID) }
            SortingType.ByAlterationDate -> passwords.sortBy { PasswordPreferenceManager.readAlterationDate(requireContext(), it.ID) }
        }

        if (AppPreferenceManager.isPasswordsSortReverse(requireContext()))
            passwords.reverse()
    }

}
