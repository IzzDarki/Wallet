package com.izzdarki.wallet.ui.settings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import androidx.navigation.Navigation
import com.izzdarki.wallet.logic.authentication.disableAuthentication
import com.izzdarki.wallet.logic.authentication.setNewAppPassword
import com.izzdarki.wallet.logic.authentication.updateAuthenticationTime
import izzdarki.wallet.R
import izzdarki.wallet.databinding.FragmentSetupAuthenticationBinding


class SetupAppPasswordFragment : Fragment() {

    private lateinit var binding: FragmentSetupAuthenticationBinding
    private val navController get() = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetupAuthenticationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set explanation text
        val explanationText = getString(R.string.setup_app_password_message) +
                " " + getString(R.string.forget_app_password_text)
        binding.explanationText.text = explanationText

        // New password input, remove error on text input
        binding.newPasswordLayout.errorIconDrawable = null // Prevent error icon from overlapping with password visibility toggle
        binding.newPasswordInput.doOnTextChanged { _, _, _, _ ->
            binding.newPasswordLayout.error = null
        }

        // New password repeat input, remove error on text input
        binding.newPasswordRepeatLayout.errorIconDrawable = null // Prevent error icon from overlapping with password visibility toggle
        binding.newPasswordRepeatInput.doOnTextChanged { _, _, _, _ ->
            binding.newPasswordRepeatLayout.error = null
        }


        // Set password button
        binding.setPasswordButton.setOnClickListener {
            checkInputAndStartDialog()
        }
    }

    private fun checkInputAndStartDialog() {
        // Abort if new password is empty
        val newPasswordEntered = binding.newPasswordInput.text.toString()
        if (newPasswordEntered.isEmpty()) {
            binding.newPasswordLayout.error = getString(R.string.app_password_cant_be_empty)
            return
        }

        // Abort if passwords don't match
        val newPasswordRepeatEntered = binding.newPasswordRepeatInput.text.toString()
        if (newPasswordEntered != newPasswordRepeatEntered) {
            binding.newPasswordRepeatLayout.error = getString(R.string.app_passwords_dont_match)
            return
        }

        // Trigger set password alert dialog
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.set_app_password)
            .setMessage(getString(R.string.set_app_password_message) + " " + getString(R.string.forget_app_password_text))
            .setPositiveButton(R.string.set_app_password) { dialog, _ ->
                // Set new password
                val success = setNewAppPassword(requireContext(), newPasswordEntered)
                if (success) {
                    // Show success toast
                    Toast.makeText(requireContext(), R.string.app_password_set_successfully, Toast.LENGTH_LONG).show()
                    // Update authentication time, so that the user is not asked for the password immediately after setting it
                    updateAuthenticationTime(requireContext())
                    navController.popBackStack() // Close fragment
                }
                else {
                    Toast.makeText(requireContext(), R.string.app_password_set_unknown_error, Toast.LENGTH_LONG).show() // Show error toast
                    disableAuthentication(requireContext()) // To be sure that no password is set
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()

    }
}
