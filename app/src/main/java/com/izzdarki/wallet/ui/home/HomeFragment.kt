package com.izzdarki.wallet.ui.home

import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import izzdarki.wallet.R
import com.izzdarki.wallet.preferences.AppPreferenceManager
import com.izzdarki.wallet.preferences.CardPreferenceManager
import com.izzdarki.wallet.preferences.PasswordPreferenceManager
import com.izzdarki.wallet.services.CreateExampleCardService
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    // UI
    private lateinit var constraintLayout: ConstraintLayout
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var fragmentViewPager: ViewPager2

    private enum class State {
        CardsAndPasswords, Cards, Passwords
    }

    // adapter for view pager
    private inner class ScreenSlidePagerAdapter(fragment: Fragment)
        : FragmentStateAdapter(fragment) {

        private var context: Context = fragment.requireContext()

        private var state = getNewState(context)

        fun isStateUpToDate(context: Context) = state == getNewState(context)

        private fun getNewState(context: Context): State {
            return if (AppPreferenceManager.isAppFunctionCards(context) && AppPreferenceManager.isAppFunctionPasswords(context))
                State.CardsAndPasswords
            else if (AppPreferenceManager.isAppFunctionCards(context))
                State.Cards
            else
                State.Passwords
        }

        fun reload(context: Context) {
            state = getNewState(context)
            notifyDataSetChanged()
        }

        override fun createFragment(position: Int): Fragment {
            return when(state) {
                State.CardsAndPasswords -> {
                    if (position == 0)
                        HomeCardsFragment()
                    else
                        HomePasswordsFragment()
                }
                State.Cards ->
                    HomeCardsFragment()
                State.Passwords ->
                    HomePasswordsFragment()
            }
        }

        override fun getItemCount() = if (state == State.CardsAndPasswords) 2 else 1
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // hooks
        constraintLayout = view.findViewById(R.id.home_constraint_layout)
        bottomNavigationView = view.findViewById(R.id.home_bottom_navigation)
        fragmentViewPager = view.findViewById(R.id.home_fragment_view_pager)

        // bottom navigation view
        bottomNavigationView.setOnNavigationItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.home_bottom_navigation_menu_cards -> setCardsFragment()
                R.id.home_bottom_navigation_menu_passwords -> setPasswordsFragment()
                else -> return@setOnNavigationItemSelectedListener false
            }
            true
        }

        // fragment view pager
        fragmentViewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == 0)
                    bottomNavigationView.selectedItemId = R.id.home_bottom_navigation_menu_cards
                else
                    bottomNavigationView.selectedItemId = R.id.home_bottom_navigation_menu_passwords
            }
        })
        fragmentViewPager.adapter = ScreenSlidePagerAdapter(this)

        // bottom navigation
        if (!AppPreferenceManager.isAppFunctionCards(requireContext())
            || !AppPreferenceManager.isAppFunctionPasswords(requireContext())
        )
            bottomNavigationView.visibility = View.GONE
        else
            bottomNavigationView.visibility = View.VISIBLE

        // preload encrypted preferences for better performance
        if (AppPreferenceManager.isAppFunctionCards(requireContext()))
            lifecycleScope.launch {
                CardPreferenceManager.getPreferences(requireContext())
            }
        if (AppPreferenceManager.isAppFunctionPasswords(requireContext()))
            lifecycleScope.launch {
                PasswordPreferenceManager.getPreferences(requireContext())
            }
    }

    override fun onResume() {
        super.onResume()

        val adapter = fragmentViewPager.adapter as ScreenSlidePagerAdapter
        if (!adapter.isStateUpToDate(requireContext())) {
            Toast.makeText(
                requireContext(),
                R.string.changing_app_functions_requires_restart,
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private fun setCardsFragment() {
        fragmentViewPager.setCurrentItem(0, true)
    }

    private fun setPasswordsFragment() {
        fragmentViewPager.setCurrentItem(1, true)
    }

}