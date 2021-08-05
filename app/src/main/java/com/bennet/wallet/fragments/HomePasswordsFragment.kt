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
import com.bennet.wallet.preferences.PasswordPreferenceManager
import com.bennet.wallet.utils.CardOrPasswordPreviewData
import com.bennet.wallet.utils.ItemProperty
import com.bennet.wallet.utils.MultiSelectItemDetailsLookup
import com.bennet.wallet.utils.StableIDKeyProvider
import com.bennet.wallet.utils.Utility.downUntil
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.jetbrains.annotations.TestOnly
import java.util.*
import kotlin.math.max
import kotlin.math.min


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
                activity?.invalidateOptionsMenu()
                checkIfViewHoldersAreCorrect()
                var str = ""
                str += ""
                for (passwordID in passwords.map { it.ID }) {
                    if (selectionTracker.isSelected(passwordID.toLong()))
                        str += "${passwords.find { it.ID == passwordID }?.name}, "
                }
                //Log.d("asdf", str)
            }
        })

        // plus button
        plusButton.setOnClickListener {
            createNewPassword()
            checkIfViewHoldersAreCorrect() // TODO remove check
        }
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
        passwordsRecyclerView.adapter?.notifyDataSetChanged()
    }

    private fun editPassword(passwordID: Int) {
        val intent = Intent(context, EditPasswordActivity::class.java)
        intent.putExtra(EditPasswordActivity.EXTRA_PASSWORD_ID, passwordID)
        startActivity(intent)
    }

    private fun deletePassword(passwordID: Int) {
        AlertDialog.Builder(ContextThemeWrapper(context, R.style.RoundedCornersDialog))
            .setTitle(R.string.delete_password)
            .setMessage(R.string.delete_password_dialog_message)
            .setCancelable(true)
            .setPositiveButton(R.string.delete) { dialog, _ ->
                PasswordPreferenceManager.removePassword(context, passwordID)

                val indexRemoved = passwords.indexOfFirst { it.ID == passwordID }
                passwords.removeAt(indexRemoved)
                passwordsRecyclerView.adapter?.notifyItemRemoved(indexRemoved)

                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog: DialogInterface, _: Int ->
                dialog.cancel()
            }
            .show()
    }


    // action bar
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (selectionTracker.isSelectionMode)
            inflater.inflate(R.menu.home_action_bar_with_item_selected_menu, menu)
        else
            inflater.inflate(R.menu.home_action_bar_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home_action_bar_sort -> {
                // TODO
            }
            R.id.home_action_bar_edit_selected_item -> {} // TODO editPassword(...id...)
            R.id.home_action_bar_delete_selected_item -> {} // TODO deletePassword(...multiple ids...)
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }


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
    private val SelectionTracker<Long>.isSelectionMode: Boolean get() {
        // checks if at least one item is selected
        return passwords.map { password -> password.ID }
            .map { passwordID -> selectionTracker.isSelected(passwordID.toLong()) }
            .contains(true)
    }

    @Deprecated("just for test")
    @TestOnly
    private fun checkIfViewHoldersAreCorrect() {
        for (i in passwords.indices) {
            val viewHolderName = (passwordsRecyclerView.findViewHolderForAdapterPosition(i) as? PasswordAdapter.ViewHolder)?.textView?.text
            if (passwords[i].name != viewHolderName)
                    Log.e("asdf", "Wrong view holder at pos $i: real = ${passwords[i].name} fail = $viewHolderName")
        }
    }

}
