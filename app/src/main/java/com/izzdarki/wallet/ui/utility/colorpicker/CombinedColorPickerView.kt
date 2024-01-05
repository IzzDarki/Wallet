package com.izzdarki.wallet.ui.utility.colorpicker

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import com.github.antonpopoff.colorwheel.gradientseekbar.setBlackToColor
import com.izzdarki.wallet.utils.Utility
import izzdarki.wallet.R
import izzdarki.wallet.databinding.ViewCombinedColorPickerBinding
import kotlin.math.absoluteValue
import kotlin.math.pow

class CombinedColorPickerView : FrameLayout {

    var onColorChange: (color: Int) -> Unit = { }
    private var binding: ViewCombinedColorPickerBinding = ViewCombinedColorPickerBinding.inflate(LayoutInflater.from(context), this, true)
    private var currentColor: Int = Color.BLACK


    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    constructor(
        context: Context,
        initialColor: Int,
        colorPalettes: List<NamedColorPalette>, // can be used to for different schemes, but also for different sortings of the same colors
        initialPaletteIndex: Int = 0,
        showColorText: Boolean = true,
        onColorChange: (color: Int) -> Unit = { }
    ) : super(context) {
        this.onColorChange = onColorChange

        // Current color box
        if (showColorText) {
            binding.currentColorBox.colorTextView.visibility = View.VISIBLE // By default gone
            binding.currentColorBox.colorTextView.setOnEditorActionListener { textView, imeCode, _ ->
                if (imeCode == EditorInfo.IME_ACTION_DONE) {
                    val color = readColorString(textView.text.toString())?.withAlpha(255)
                    if (color != null) {
                        setCurrentColor(color)
                        this.onColorChange(color)
                    } else {
                        setCurrentColor(currentColor) // Reset to previous color
                    }
                    // Hide keyboard
                    textView.clearFocus()
                    Utility.hideKeyboard(textView)
                    true
                } else
                    false
            }
        }

        // Color wheel and brightness slider
        setCurrentColor(initialColor)
        binding.colorWheel.colorChangeListener = { color ->
            // Brightness slider
            binding.brightnessSlider.setBlackToColor(binding.colorWheel.rgb)
            val actualColor = binding.brightnessSlider.argb

            internalSetCurrentColor(actualColor)

            // Callback
            this.onColorChange(color)
        }
        binding.brightnessSlider.colorChangeListener = { _: Float, argb: Int ->
            internalSetCurrentColor(argb)

            // No need to update color wheel (because it didn't change)

            // Callback
            this.onColorChange(argb)
        }

        // Color palettes
        val colorPalettePickerView = ColorPalettePickerView(
            context = context,
            title = context.getString(R.string.color_palette),
            colorPalettes = colorPalettes,
            initialIndex = initialPaletteIndex,
            onColorPicked = { rgb, _, _ ->
                setCurrentColor(rgb) // No need to disable listeners because color palette picker is not notified about color changes

                // Callback
                this.onColorChange(rgb)
            }
        )
        binding.root.addView(colorPalettePickerView) // Add to the linear layout
    }

    fun getCurrentColor(): Int = currentColor

    fun setCurrentColor(rgb: Int) {
        internalSetCurrentColor(rgb)

        // Color wheel
        binding.colorWheel.rgb = rgb

        // Brightness slider
        binding.brightnessSlider.setBlackToColor(binding.colorWheel.rgb)
        binding.brightnessSlider.offset = calculateOffset(rgb)
    }

    private fun internalSetCurrentColor(rgb: Int) {
        // Update internal color
        currentColor = rgb

        // Current color box
        binding.currentColorBox.colorBox.setCardBackgroundColor(rgb)
        binding.currentColorBox.colorTextView.setText("#${Integer.toHexString(rgb).substring(2).uppercase()}")
        binding.currentColorBox.colorTextView.setTextColor(
            if (Utility.isColorDark(rgb))
                context.getColor(R.color.on_dark_text_color)
            else
                context.getColor(R.color.on_light_text_color)
        )
    }

    /**
     * Try to calculate the offset of the given color that can be used to set the brightness slider.
     * Due to rounding errors (float to int) the offset might not reproduce the exact color.
     * Reverse engineered from https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/animation/ArgbEvaluator.java.
     * which is used by the [com.github.antonpopoff.colorwheel.gradientseekbar.GradientSeekBar] to calculate the color.
     */
    private fun calculateOffset(color: Int): Float {
        val endColor = binding.brightnessSlider.endColor
        return listOf(
            Pair(Color.red(endColor), Color.red(color)),
            Pair(Color.green(endColor), Color.green(color)),
            Pair(Color.blue(endColor), Color.blue(color))
        )
            .filter { it.second != 0 } // Cannot calculate offset if start and end colors are the same in given channel
            .map { (end, actual) ->
                Pair(
                    end.sRGBToLinearRGB(),
                    actual.sRGBToLinearRGB()
                )
            }
            // Now find the channel with the biggest difference between start and end colors (better than taking average in the end)
            .maxByOrNull { (endLinear, _) -> (endLinear - 0f).absoluteValue }
            ?.let { (endLinear, actualLinear) ->
                actualLinear / endLinear
            } ?: 0f // If start and end colors are the same in all channels, return 0 (any value would be correct)
    }

    private fun readColorString(colorString: String): Int? {
        return try {
            Color.parseColor(colorString)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun Int.sRGBToLinearRGB(): Float = (this.toFloat() / 255f).pow(2.2f)

    private fun Int.withAlpha(alpha: Int): Int = Color.argb(alpha, Color.red(this), Color.green(this), Color.blue(this))

}


/* More general offset calculator:

    private fun calculateOffset2(startColor: Int, endColor: Int, color: Int): Float {
        return listOf(
            Triple(Color.red(startColor), Color.red(endColor), Color.red(color)),
            Triple(Color.green(startColor), Color.green(endColor), Color.green(color)),
            Triple(Color.blue(startColor), Color.blue(endColor), Color.blue(color))
        )
            .filter { it.first != it.second } // Cannot calculate offset if start and end colors are the same in given channel
            .map { (start, end, actual) ->
                Triple(
                    start.sRGBToLinearRGB(),
                    end.sRGBToLinearRGB(),
                    actual.sRGBToLinearRGB()
                )
            }
            // Now find the channel with the biggest difference between start and end colors (better than taking average in the end)
            .maxByOrNull { (startLinear, endLinear, _) -> (endLinear - startLinear).absoluteValue }
            ?.let { (startLinear, endLinear, actualLinear) ->
                (actualLinear - startLinear) / (endLinear - startLinear)
            } ?: 0f // If start and end colors are the same in all channels, return 0 (any value would be correct)
    }

   Interpolation:

    fun interpolate(fraction: Float, c1: Int, c2: Int): Int {
        val factor1 = (1f - fraction)
        val factor2 = fraction
        return Color.argb(
            (Color.alpha(c1) * factor1 + Color.alpha(c2) * factor2).roundToInt(),
            (((Color.red(c1).toFloat() / 255f).pow(2.2f)   * factor1 + (Color.red(c2).toFloat() / 255f).pow(2.2f)   * factor2).pow(1f / 2.2f) * 255f).roundToInt(),
            (((Color.green(c1).toFloat() / 255f).pow(2.2f) * factor1 + (Color.green(c2).toFloat() / 255f).pow(2.2f) * factor2).pow(1f / 2.2f) * 255f).roundToInt(),
            (((Color.blue(c1).toFloat() / 255f).pow(2.2f)  * factor1 + (Color.blue(c2).toFloat() / 255f).pow(2.2f)  * factor2).pow(1f / 2.2f) * 255f).roundToInt()
        )
    }
 */