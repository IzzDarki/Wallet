package com.izzdarki.wallet.ui.utility.colorpicker

import android.graphics.Color

object PredefinedColorPalettes {

    fun generateHSVColorPalette(hueStep: Int, saturation: Float, value: Float): List<Int> {
        return (0 until 360 step hueStep)
            .map { Color.HSVToColor(255, floatArrayOf(it.toFloat(), saturation, value)) }
    }

}
