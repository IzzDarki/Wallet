package com.izzdarki.wallet.ui.settings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.navigation.Navigation
import com.izzdarki.wallet.logic.disableAuthentication
import com.izzdarki.wallet.logic.isFingerprintEnabled
import com.izzdarki.wallet.logic.isPasswordCorrect
import com.izzdarki.wallet.storage.authenticationStorage
import izzdarki.wallet.R
import izzdarki.wallet.databinding.FragmentDisableAuthenticationBinding


class DisableAppPasswordFragment : Fragment() {

    lateinit var binding: FragmentDisableAuthenticationBinding
    private val navController get() = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDisableAuthenticationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Warning text
        if (isFingerprintEnabled(requireContext()))
            binding.warningText.visibility = View.GONE // Only show if fingerprint is also disabled

        // App password input
        // Prevent error icon from overlapping with password visibility toggle
        binding.appPasswordLayout.errorIconDrawable = null

        // Remove error on text input
        binding.appPasswordInput.doOnTextChanged { _, _, _, _ ->
            binding.appPasswordLayout.error = null
        }

        // Disable authentication button
        binding.disableAuthenticationButton.setOnClickListener {
            val enteredPassword = binding.appPasswordInput.text.toString()
            val encodedPassword = authenticationStorage.readEncodedAppPassword(requireContext())!!

            // Abort if app password is incorrect
            if (!isPasswordCorrect(encodedPassword, enteredPassword)) {
                binding.appPasswordLayout.error = getString(R.string.app_password_is_incorrect)
                return@setOnClickListener
            }

            val success = disableAuthentication(requireContext())
            if (success) {
                Toast.makeText(requireContext(), R.string.app_password_disabled_successfully, Toast.LENGTH_SHORT).show() // Show success toast
                navController.popBackStack() // Close fragment

            } else
                Toast.makeText(requireContext(), R.string.app_password_disabled_unknown_error, Toast.LENGTH_SHORT).show() // Show error toast
        }
    }

}
