package com.izzdarki.wallet.ui.cards

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Space
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import izzdarki.wallet.R
import com.izzdarki.wallet.ui.adapters.ShowPropertyAdapter
import com.izzdarki.wallet.preferences.AppPreferenceManager
import com.izzdarki.wallet.preferences.CardPreferenceManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.divider.MaterialDivider
import com.google.android.material.textview.MaterialTextView
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import java.util.*

class ShowCardActivity : CardActivity() {

    // region UI
    private lateinit var cardCodeImageView: ImageView
    private lateinit var cardCodePlainTextView: MaterialTextView
    private lateinit var cardPropertiesRecyclerView: RecyclerView
    private lateinit var dividerCardProperties: MaterialDivider
    private lateinit var dividerCardImages: MaterialDivider
    private lateinit var extraSpaceCardImages: Space
    private lateinit var labelsChipGroup: ChipGroup
    private lateinit var labelsDivider: MaterialDivider
    // endregion


    // region variables
    @ColorInt private var codeForegroundColor = 0
    @ColorInt private var codeBackgroundColor = 0
    // endregion


    // region lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_card)

        // hooks
        cardViewLayout = findViewById(R.id.show_card_card_view_layout)
        scrollView = findViewById(R.id.show_card_scroll_view)
        linearLayout = findViewById(R.id.show_card_linear_layout)
        cardCodeImageView = findViewById(R.id.show_card_code_image_view)
        cardCodePlainTextView = findViewById(R.id.show_card_code_plain_text)
        cardPropertiesRecyclerView = findViewById(R.id.show_card_property_list_recycler_view)
        dividerCardProperties = findViewById(R.id.show_card_divider_card_properties)
        dividerCardImages = findViewById(R.id.show_card_divider_card_images)
        extraSpaceCardImages = findViewById(R.id.show_card_extra_space_card_images)
        labelsChipGroup = findViewById(R.id.labels_chip_group)
        labelsDivider = findViewById(R.id.labels_divider)

        // init
        initFromPreferences()

        // toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // variables
        codeForegroundColor = resources.getColor(R.color.barcode_foreground_color)
        codeBackgroundColor = resources.getColor(R.color.barcode_background_color)

        // card properties recyclerview
        cardPropertiesRecyclerView.layoutManager = LinearLayoutManager(this)
        cardPropertiesRecyclerView.adapter = createShowPropertyAdapter()
        show()

        // password labels
        if (labels.isEmpty()) {
            labelsDivider.visibility = View.GONE
            labelsChipGroup.visibility = View.GONE
        } else {
            labelsDivider.visibility = View.VISIBLE
            labelsChipGroup.visibility = View.VISIBLE
            addLabelsToChipGroup()
        }

        // card view
        createCardView()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        scrollView.scrollY = 0 // refreshes scroll position
        cardView.removeFrontImage() // hides old image, new image will be loaded later
        cardView.removeBackImage() // hides old image, new image will be loaded later
        initFromPreferences()

        // hide/show labels chip group
        if (labels.isEmpty()) {
            labelsDivider.visibility = View.GONE
            labelsChipGroup.visibility = View.GONE
        } else {
            labelsDivider.visibility = View.VISIBLE
            labelsChipGroup.visibility = View.VISIBLE
            labelsChipGroup.removeAllViews()
            addLabelsToChipGroup()
        }

        // update
        cardPropertiesRecyclerView.adapter = createShowPropertyAdapter() // because cardProperties has been reassigned, a new adapter, that holds the new cardProperties is needed
        show()
        updateFrontImage()
        updateBackImage()
    }
    // endregion


    // region action bar
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.show_activity_action_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.show_card_or_password_action_bar_edit) {
            val editIntent = Intent(this, EditCardActivity::class.java)
            editIntent.putExtra(EXTRA_CARD_ID, ID)
            startActivity(editIntent)
            return true
        } else if (itemId == R.id.show_card_or_password_action_bar_delete) {
            val builder =
                AlertDialog.Builder(this)
            builder.setTitle(R.string.delete_card)
            builder.setMessage(R.string.delete_card_dialog_message)
            builder.setCancelable(true)
            builder.setPositiveButton(R.string.delete) { dialog, _ ->
                finish() // ALWAYS FINISH BEFORE STARTING OTHER ACTIVITY
                CardPreferenceManager.removeComplete(this, ID)
                dialog.dismiss()
            }
            builder.setNegativeButton(
                android.R.string.cancel
            ) { dialog: DialogInterface, _: Int -> dialog.cancel() }
            builder.show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    // endregion

    private fun show() {
        // hide scrollbar (TODO feature was removed, because it didn't do anything)
        //hideScrollbar()

        // card properties
        supportActionBar!!.title = cardName
        // code
        if (cardCode != "") {
            if (!cardCodeTypeText)
                cardCodePlainTextView.visibility = View.GONE
            else {
                cardCodePlainTextView.visibility = View.VISIBLE
                cardCodePlainTextView.text = cardCode
            }

            // lambda because there are 3 different exceptions to catch
            val catchExceptionFunc = { e: Exception ->
                val detailedErrorMessage =
                    if (AppPreferenceManager.isDetailedErrors(this))
                        System.getProperty("line.separator")!! + e.localizedMessage
                    else
                        ""
                Toast.makeText(
                    this,
                    String.format(
                        getString(R.string.show_card_visual_code_cannot_be_displayed),
                        codeTypeIntToString(this, cardCodeType)
                    ) + detailedErrorMessage,
                    Toast.LENGTH_LONG
                ).show()
                cardCodeImageView.visibility = View.GONE
            }

            cardCodeImageView.visibility = View.VISIBLE
            val codeBitmap: Bitmap
            try {
                codeBitmap = if (codeIs1D(cardCodeType)) createCode1D() else createCodeDefault()
            } catch (e: IllegalArgumentException) {
                catchExceptionFunc(e)
                return
            } catch (e: WriterException) {
                catchExceptionFunc(e)
                return
            } catch (e: ArrayIndexOutOfBoundsException) {
                catchExceptionFunc(e)
                return
            }
            cardCodeImageView.setImageBitmap(codeBitmap)
        } else {
            cardCodePlainTextView.visibility = View.GONE
            cardCodeImageView.visibility = View.GONE
        }

        // properties
        cardPropertiesRecyclerView.adapter?.notifyDataSetChanged() // reload
        if (cardProperties.size > 0 && cardCode != "")
            dividerCardProperties.visibility = View.VISIBLE
        else
            dividerCardProperties.visibility = View.GONE

        // images
        if ((currentFrontImage != null || currentBackImage != null) && (cardProperties.size > 0 || cardCode != "")) {
            dividerCardImages.visibility = View.VISIBLE
            if (cardProperties.size > 0)
                extraSpaceCardImages.visibility = View.GONE
            else
                extraSpaceCardImages.visibility = View.VISIBLE
        }
        else
            dividerCardImages.visibility = View.GONE
    }

    private fun addLabelsToChipGroup() {
        for (label in labels) {
            val chip = Chip(this)
            chip.text = label
            labelsChipGroup.addView(chip)
        }
    }

    private fun createShowPropertyAdapter(): ShowPropertyAdapter {
        return ShowPropertyAdapter(cardProperties) {
            // hide all secret values in adapter each time the user presses any of the visibility buttons
            for (position in cardProperties.indices) {
                val holder = cardPropertiesRecyclerView.findViewHolderForAdapterPosition(position) as? ShowPropertyAdapter.ViewHolder
                holder?.setValueHidden(cardProperties[position].secret)
            }
        }
    }

    @Throws(IllegalArgumentException::class, WriterException::class)
    private fun createCodeDefault(): Bitmap {
        val widthAndHeight = getBarcodeSize(cardCodeType)
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            cardCode,
            cardCodeIntToBarcodeFormat(cardCodeType),
            widthAndHeight.width,
            widthAndHeight.height
        )
        val bitMatrixWidth = bitMatrix.width
        val bitMatrixHeight = bitMatrix.height
        val pixels = IntArray(bitMatrixWidth * bitMatrixHeight)
        for (y in 0 until bitMatrixHeight) {
            val offset = y * bitMatrixWidth
            for (x in 0 until bitMatrixWidth) pixels[offset + x] =
                if (bitMatrix[x, y]) codeForegroundColor else codeBackgroundColor
        }
        val bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, bitMatrixWidth, 0, 0, bitMatrixWidth, bitMatrixHeight)
        return bitmap
    }

    @Throws(IllegalArgumentException::class, WriterException::class)
    private fun createCode1D(): Bitmap {
        val widthAndHeight = getBarcodeSize(cardCodeType)
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            cardCode,
            cardCodeIntToBarcodeFormat(cardCodeType),
            widthAndHeight.width,
            widthAndHeight.height,
            null
        )
        val bitMatrixWidth = bitMatrix.width
        val bitMatrixHeight = bitMatrix.height
        val bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.ARGB_8888)
        for (i in 0 until bitMatrixWidth) {
            val column = IntArray(bitMatrixHeight)
            Arrays.fill(column, if (bitMatrix[i, 0]) codeForegroundColor else codeBackgroundColor)
            bitmap.setPixels(column, 0, 1, i, 0, 1, bitMatrixHeight)
        }
        return bitmap
    }

    private class WidthAndHeight {
        var width = 0
        var height = 0

        enum class StdFormats {
            Square, Horizontal
        }

        constructor(width: Float, height: Float) {
            this.width = width.toInt()
            this.height = height.toInt()
        }

        constructor(layoutWidth: Float, format: StdFormats) {
            when (format) {
                StdFormats.Square -> {
                    width = (layoutWidth / 2).toInt()
                    height = width
                }
                StdFormats.Horizontal -> {
                    width = layoutWidth.toInt()
                    height = width / 3
                }
            }
        }
    }

    private fun getBarcodeSize(cardCodeType: Int): WidthAndHeight {
        return when (cardCodeType) {
            CardPreferenceManager.CARD_CODE_TYPE_AZTEC,
            CardPreferenceManager.CARD_CODE_TYPE_QR,
            CardPreferenceManager.CARD_CODE_TYPE_DATA_MATRIX
                -> WidthAndHeight(calculatedLayoutWidth, WidthAndHeight.StdFormats.Square)

            CardPreferenceManager.CARD_CODE_TYPE_PDF_417,
            CardPreferenceManager.CARD_CODE_TYPE_CODABAR,
            CardPreferenceManager.CARD_CODE_TYPE_CODE_39,
            CardPreferenceManager.CARD_CODE_TYPE_CODE_93,
            CardPreferenceManager.CARD_CODE_TYPE_CODE_128,
            CardPreferenceManager.CARD_CODE_TYPE_EAN_8,
            CardPreferenceManager.CARD_CODE_TYPE_EAN_13,
            CardPreferenceManager.CARD_CODE_TYPE_ITF,
            CardPreferenceManager.CARD_CODE_TYPE_UPC_A,
            CardPreferenceManager.CARD_CODE_TYPE_UPC_E
                -> WidthAndHeight(calculatedLayoutWidth, WidthAndHeight.StdFormats.Horizontal)

            else -> throw IllegalStateException("Unexpected value: $cardCodeType")
        }
    }
}