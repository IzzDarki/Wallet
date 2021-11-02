package com.bennet.wallet.utils

import androidx.core.content.ContextCompat
import com.bennet.wallet.R
import com.bennet.wallet.utils.Utility.PreferenceArray
import com.bennet.wallet.utils.Utility.PreferenceArrayString
import com.bennet.wallet.utils.Utility.toPair
import com.google.android.material.card.MaterialCardView

/**
 * Utility, that is very specific to this app
 */
object AppUtility {

    // region Common
    fun makeCardViewSelected(cardView: MaterialCardView) {
        val context = cardView.context
        val strokeColor = ContextCompat.getColor(context, R.color.card_view_outline_color_black_or_white)
        val cardSelectedColor =
            if (Utility.isUsingNightModeResources(context))
                Utility.getDarkerColor(cardView.cardBackgroundColor.defaultColor)
            else
                Utility.getLighterColor(cardView.cardBackgroundColor.defaultColor)


        cardView.setCardBackgroundColor(cardSelectedColor)
        cardView.strokeWidth = 12

        // check if stroke color and card color are similar
        if (!Utility.areColorsSimilar(strokeColor, cardSelectedColor))
            cardView.strokeColor = strokeColor
        else {
            // if they are similar, use normal outline color instead
            // this should never happen, because in dark mode, the card color gets darker, so that white cards should be
            // dark enough to be different than the white outline color
            // and in light mode the other way round
            // still keep this here for some weird scenarios or future changes
            cardView.strokeColor = ContextCompat.getColor(context, R.color.card_view_outline_color)
        }
    }
    // endregion

    // region Preferences
    object CustomSortingWithGrouping {
        private const val KEY_VALUE_SEPARATOR = "~!;<>|"
        private const val MAP_SEPARATOR = "#öä%*"
        private const val LIST_SEPARATOR = "µS+=:"

        fun toString(customSorting: Map<String, List<String>>): String {
            val stringBuilder = StringBuilder("")
            for ((index, entry) in customSorting.iterator().withIndex()) {
                stringBuilder.append(entry.key).append(KEY_VALUE_SEPARATOR)
                for ((index2, value) in entry.value.withIndex()) {
                    stringBuilder.append(value)
                    if (index2 != entry.value.size - 1)
                        stringBuilder.append(LIST_SEPARATOR)
                }
                if (index != customSorting.size - 1)
                    stringBuilder.append(MAP_SEPARATOR)
            }
            return stringBuilder.toString()
        }

        fun fromString(string: String): Map<String, List<String>> {
            if (string.isEmpty())
                return mapOf()

            return string.split(MAP_SEPARATOR)
                .map { it.split(KEY_VALUE_SEPARATOR).toPair() }
                .map { Pair(it.first, it.second.split(LIST_SEPARATOR)) }
                .toMap()
        }

        fun isStringOk(string: String): Boolean {
            return KEY_VALUE_SEPARATOR !in string && MAP_SEPARATOR !in string && KEY_VALUE_SEPARATOR !in string
        }
    }

}