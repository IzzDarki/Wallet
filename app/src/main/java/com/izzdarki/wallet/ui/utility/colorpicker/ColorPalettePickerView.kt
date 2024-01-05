package com.izzdarki.wallet.ui.utility.colorpicker

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ListView
import com.izzdarki.wallet.ui.adapters.ColorSquareAdapter
import izzdarki.wallet.databinding.ViewColorPalettePickerBinding

data class NamedColorPalette(
    val name: String,
    val colors: Lazy<List<Int>>
)

class ColorPalettePickerView : FrameLayout {

    private var binding: ViewColorPalettePickerBinding = ViewColorPalettePickerBinding.inflate(LayoutInflater.from(context), this, true)
    private lateinit var colorPalettes: List<NamedColorPalette>
    private var currentPaletteIndex: Int = 0
    private lateinit var colorSquaresAdapter: ColorSquareAdapter

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    constructor(
        context: Context,
        colorPalettes: List<NamedColorPalette>,
        title: String,
        initialIndex: Int = 0,
        onColorPicked: (color: Int, paletteIndex: Int, indexInPalette: Int) -> Unit = { _, _, _ -> }
    ) : super(context) {
        this.colorPalettes = colorPalettes
        currentPaletteIndex = initialIndex

        // Title
        binding.colorPaletteTitle.text = title

        // Spinner
        binding.colorPaletteSpinner.adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_dropdown_item,
            colorPalettes.map { it.name },
        )
        binding.colorPaletteSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                changeColorPalette(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }
        binding.colorPaletteSpinner.setSelection(currentPaletteIndex, false)

        // Color palette
        colorSquaresAdapter = ColorSquareAdapter(colorPalettes[currentPaletteIndex].colors.value) { color, indexInPalette ->
            onColorPicked(color, currentPaletteIndex, indexInPalette)
        }
        binding.colorListRecyclerView.adapter = colorSquaresAdapter
    }

    var colorPaletteIndex: Int
        get() = currentPaletteIndex
        set(value) {
            changeColorPalette(value)
        }

    private fun changeColorPalette(index: Int) {
        if (index < 0 || index >= colorPalettes.size)
            return

        currentPaletteIndex = index
        colorSquaresAdapter.colorList = colorPalettes[index].colors.value
        colorSquaresAdapter.notifyDataSetChanged()
    }

}