package com.izzdarki.wallet.ui.credentials

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.izzdarki.wallet.logic.passwordgeneration.generatePassword
import izzdarki.wallet.R
import izzdarki.wallet.databinding.DialogPasswordGenerationBinding
import izzdarki.wallet.databinding.FilterChipBinding
import kotlin.math.roundToInt

data class CharacterSet(
    val name: String,
    val characters: String,
)

class PasswordGenerationDialog(
    context: Context,
    private val characterSets: List<CharacterSet>,
    private val onPasswordGenerated: (String) -> Unit,
) : AlertDialog(context) {

    private lateinit var binding: DialogPasswordGenerationBinding

    init {
        setTitle(R.string.generate_a_random_password)
        setView(inflatePasswordGenerationView(context))
        setButton(BUTTON_POSITIVE, context.getString(R.string.generate)) { _, _ ->
            val password = generatePassword(
                characterSets = binding.chipGroup.children
                    .filterIsInstance<Chip>()
                    .withIndex()
                    .filter { (_, chip) -> chip.isChecked }
                    .map { (index, _) -> characterSets[index].characters }
                    .toList(),
                length = binding.sliderPasswordLength.value.toInt(),
                oneFromEach = binding.chipOneFromEachSet.isChecked,
                noAmbiguousCharacters = binding.chipNoAmbiguousCharacters.isChecked,
            )
            onPasswordGenerated(password)
        }
        setButton(BUTTON_NEGATIVE, context.getString(R.string.cancel)) { _, _ -> }
    }

    private fun inflatePasswordGenerationView(context: Context): View {
        val inflater = LayoutInflater.from(context)
        binding = DialogPasswordGenerationBinding.inflate(LayoutInflater.from(context))

        binding.sliderPlus.setOnClickListener {
            binding.sliderPasswordLength.valueTo = (binding.sliderPasswordLength.valueTo * 1.5f).roundToInt().toFloat()
        }
        for (characterSet in characterSets) {
            val filterChipBinding = FilterChipBinding.inflate(inflater, binding.chipGroup, false).apply {
                chip.text = characterSet.name
                chip.isChecked = true
                chip.gravity = Gravity.CENTER
                chip.setOnLongClickListener {
                    Toast.makeText(context, "${characterSet.name}: ${characterSet.characters}", Toast.LENGTH_SHORT).show()
                    true
                }
            }
            binding.chipGroup.addView(
                filterChipBinding.chip,
                binding.chipGroup.childCount,
                ChipGroup.LayoutParams(ChipGroup.LayoutParams.WRAP_CONTENT, ChipGroup.LayoutParams.WRAP_CONTENT))
        }
        return binding.root
    }

}
