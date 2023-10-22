package com.izzdarki.wallet.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.izzdarki.wallet.logic.updates.createUpdateView
import com.izzdarki.wallet.logic.updates.getUpdateToVersion10Log
import com.izzdarki.wallet.utils.Utility.setPaddingBottom
import izzdarki.wallet.R
import izzdarki.wallet.databinding.FragmentUpdateLogBinding

class UpdateLogFragment : Fragment() {

    private lateinit var binding: FragmentUpdateLogBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUpdateLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // update logs in descending order
        val updateLogs = listOf(
            getUpdateToVersion10Log(requireContext()), // version 2.2.0
        )

        for (updateLog in updateLogs) {
            val updateView = requireContext().createUpdateView(updateLog)
            updateView.setPaddingBottom(resources.getDimension(R.dimen.default_padding).toInt() * 2)
            binding.linearLayout.addView(updateView)
        }
    }

}
