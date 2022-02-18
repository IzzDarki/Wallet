package com.izzdarki.wallet.components

import android.app.Activity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.core.view.allViews
import izzdarki.wallet.R
import com.izzdarki.wallet.utils.Utility
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * Using this component enables an activity to create an UI for editing labels
 *
 * You need to do the following things
 * - override [Activity.dispatchTouchEvent] (see [EditLabelsComponent.dispatchTouchEvent] for further information)
 * - create an instance of this class in [Activity.onCreate]
 * - eventually call [displayLabels] to easily create and display initial labels
 */
class EditLabelsComponent(
    activity: Activity,
    @IdRes labelsChipGroupId: Int,
    @IdRes labelsAddChipId: Int,
    private var mainLayout: ViewGroup
) {
    private var labelsChipGroup: ChipGroup = activity.findViewById(labelsChipGroupId)
    private var labelsAddChip: Chip = activity.findViewById(labelsAddChipId)

    private val context get() = labelsAddChip.context

    init {
        // labels add chip
        labelsAddChip.setOnClickListener {
            addEditTextToLabels(context.getString(R.string.new_label))
        }
    }

    /**
     * Displays all labels
     * @param labels List of labels to display
     */
    fun displayLabels(labels: List<String>) {
        for (label in labels) {
            addChipToLabels(label)
        }
    }

    /**
     * Get all labels by reading text from the views in labelsChipGroup
     */
    fun readAllLabels(): Sequence<String> {
        return labelsChipGroup.allViews
            .filter { view -> view is Chip && view !== labelsAddChip as View }
            .map {
                val chip = it as Chip
                chip.text.toString()
            }
            .sortedByDescending { it }
    }

    /**
     * For being able to finish editing chips when the user clicks elsewhere,
     * activities must override [Activity.dispatchTouchEvent] and call this method from there.
     * It finished editing in certain situations
     *
     * @return
     *  `false`, when touch event should be processed as usual => your overridden `dispatchTouchEvent` needs to call `super.dispatchTouchEvent` and return its value
     *  `true`, when the touch event should be consumed => your overridden `dispatchTouchEvent` also needs to return `true`,
     *
     *  You can use this code:
        ```kotlin
        override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
            // Every touch event goes through this function
            if (yourEditLabelsComponent.dispatchTouchEvent(ev))
            return true
            else
            return super.dispatchTouchEvent(ev)
        }
        ```
     */
    fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        // This function finishes editing of a label in certain situations

        if (ev?.actionMasked == MotionEvent.ACTION_DOWN) {

            // Check if touch event hits the edited label => don't finish editing it (the user wants to interact with the edited label)
            val editText = getEditTextFromChipGroup()
                ?: return false // if there is no EditText, touch events can be dispatched as usual

            if (!Utility.isViewHitByTouchEvent(editText, ev)) {
                getEditTextHitByTouchEvent(ev)?.requestFocus() // request focus to EditText if the touch event hits any EditText (before the focus gets cleared by finishEditingChip)
                finishEditingChip(editText)
            }

            // Check if touch event hits one of the chips => consume the touch event
            for (view in labelsChipGroup.allViews) {
                if (view is Chip && Utility.isViewHitByTouchEvent(view, ev)) {
                    return true // consume the touch event (finishing editing while also triggering other chip related UI is too much for a single touch)
                }
            }
        }
        return false // dispatch touch events as usual
    }

    private fun addChipToLabels(text: String, index: Int = 1) {
        val chip = Chip(context)
        chip.text = text
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener {
            labelsChipGroup.removeView(chip)
        }
        chip.setOnLongClickListener {
            startEditingChip(chip)
            return@setOnLongClickListener true // consumed long click
        }
        labelsChipGroup.addView(chip, index)
    }

    private fun addEditTextToLabels(text: String, index: Int = 1) {
        val editText = AutoCompleteTextView(context)
        editText.isSingleLine = true
        editText.setText(text)
        editText.setSelectAllOnFocus(true)
        editText.imeOptions = EditorInfo.IME_ACTION_NEXT
        editText.setOnEditorActionListener { _, _, _ ->
            // when action (done) triggered, finish editing
            finishEditingChip(editText)
            return@setOnEditorActionListener true // consumed the action
        }

        labelsChipGroup.addView(editText, index)
        editText.requestFocus()
        Utility.showKeyboard(editText)
    }

    private fun startEditingChip(chip: Chip) {
        val index = labelsChipGroup.indexOfChild(chip)
        labelsChipGroup.removeView(chip)
        addEditTextToLabels(chip.text.toString(), index)
    }

    private fun finishEditingChip(editText: AutoCompleteTextView) {
        // clear focus and remove editText
        editText.clearFocus()
        val index = labelsChipGroup.indexOfChild(editText)
        labelsChipGroup.removeView(editText)

        editText.setText(editText.text.toString().trim())

        val newLabel = editText.text.toString()
        if (newLabel == "") {
            Toast.makeText(context, R.string.error_label_cant_be_empty, Toast.LENGTH_SHORT).show()
            return
        }
        if (newLabel.contains(Utility.PreferenceArray.DEFAULT_SEPARATOR)) {
            val errorMessage = String.format(
                context.getString(R.string.error_label_cant_contain_x),
                Utility.PreferenceArray.DEFAULT_SEPARATOR
            )
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            return
        }
        if (newLabel in readAllLabels()) {
            Toast.makeText(context, R.string.error_label_already_added, Toast.LENGTH_SHORT).show()
            return
        }

        // label is ok => add it as chip
        addChipToLabels(editText.text.toString(), index)
    }

    private fun getEditTextFromChipGroup(): AutoCompleteTextView? {
        return labelsChipGroup.allViews.firstOrNull { it is AutoCompleteTextView } as? AutoCompleteTextView
    }

    private fun getEditTextHitByTouchEvent(ev: MotionEvent): EditText? {
        return mainLayout.allViews.firstOrNull {
            it is EditText && Utility.isViewHitByTouchEvent(it, ev)
        } as? EditText
    }
}