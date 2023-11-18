package com.izzdarki.wallet.ui.credentials

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
import com.izzdarki.wallet.ui.adapters.ShowFieldAdapter
import com.izzdarki.wallet.storage.AppSettingsStorage
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.divider.MaterialDivider
import com.google.android.material.textview.MaterialTextView
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.izzdarki.wallet.data.Barcode
import java.util.*

class ShowCredentialActivity : CredentialActivity() {

    // region UI
    private lateinit var barcodeImageView: ImageView
    private lateinit var barcodePlainTextView: MaterialTextView
    private lateinit var credentialPropertiesRecyclerView: RecyclerView
    private lateinit var dividerProperties: MaterialDivider
    private lateinit var dividerImages: MaterialDivider
    private lateinit var extraSpaceImages: Space
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
        setContentView(R.layout.activity_show_credential)

        // hooks
        cardViewLayout = findViewById(R.id.show_card_card_view_layout)
        scrollView = findViewById(R.id.show_card_scroll_view)
        linearLayout = findViewById(R.id.show_card_linear_layout)
        barcodeImageView = findViewById(R.id.show_card_code_image_view)
        barcodePlainTextView = findViewById(R.id.show_card_code_plain_text)
        credentialPropertiesRecyclerView = findViewById(R.id.show_card_property_list_recycler_view)
        dividerProperties = findViewById(R.id.show_card_divider_card_properties)
        dividerImages = findViewById(R.id.show_card_divider_card_images)
        extraSpaceImages = findViewById(R.id.show_card_extra_space_card_images)
        labelsChipGroup = findViewById(R.id.labels_chip_group)
        labelsDivider = findViewById(R.id.labels_divider)

        // init
        initCredentialFromStorage()

        // toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // variables
        codeForegroundColor = getColor(R.color.barcode_foreground_color)
        codeBackgroundColor = getColor(R.color.barcode_background_color)

        // credential properties recyclerview
        credentialPropertiesRecyclerView.layoutManager = LinearLayoutManager(this)
        credentialPropertiesRecyclerView.adapter = createShowPropertyAdapter()
        show()

        // password labels
        if (credential.labels.isEmpty()) {
            labelsDivider.visibility = View.GONE
            labelsChipGroup.visibility = View.GONE
        } else {
            labelsDivider.visibility = View.VISIBLE
            labelsChipGroup.visibility = View.VISIBLE
            addLabelsToChipGroup()
        }

        // card view
        createCardView() // also asynchronously loads and displays all images
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        scrollView.scrollY = 0 // refreshes scroll position
        cardView.removeFrontImage() // hides old image, new image will be loaded later
        cardView.removeBackImage() // hides old image, new image will be loaded later
        initCredentialFromStorage()

        // hide/show labels chip group
        if (credential.labels.isEmpty()) {
            labelsDivider.visibility = View.GONE
            labelsChipGroup.visibility = View.GONE
        } else {
            labelsDivider.visibility = View.VISIBLE
            labelsChipGroup.visibility = View.VISIBLE
            labelsChipGroup.removeAllViews()
            addLabelsToChipGroup()
        }

        // update
        credentialPropertiesRecyclerView.adapter = createShowPropertyAdapter() // because credentialProperties has been reassigned, a new adapter, that holds the new credentialProperties is needed
        show()
        asynchronouslyLoadAndDisplayAllImages()
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
            val editIntent = Intent(this, EditCredentialActivity::class.java)
            editIntent.putExtra(EXTRA_CREDENTIAL_ID, credential.id)
            startActivity(editIntent)
            return true
        } else if (itemId == R.id.show_card_or_password_action_bar_delete) {
            val builder =
                AlertDialog.Builder(this)
            builder.setTitle(R.string.delete_entry)
            builder.setMessage(R.string.delete_entry_dialog_message)
            builder.setCancelable(true)
            builder.setPositiveButton(R.string.delete) { dialog, _ ->
                finish() // ALWAYS FINISH BEFORE STARTING OTHER ACTIVITY
                deleteCredentialWithImages(this, credential)
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
        // credential properties
        supportActionBar!!.title = credential.name

        // code
        if (credential.barcode != null && credential.barcode?.code != "") {
            val barcodeNotNull = credential.barcode!!
            if (!barcodeNotNull.showText)
                barcodePlainTextView.visibility = View.GONE
            else {
                barcodePlainTextView.visibility = View.VISIBLE
                barcodePlainTextView.text = barcodeNotNull.code
            }

            // function to handle exceptions
            val catchExceptionFunc = { e: Exception ->
                val detailedErrorMessage =
                    if (AppSettingsStorage.isDetailedErrors(this))
                        System.getProperty("line.separator")!! + e.localizedMessage
                    else
                        ""
                Toast.makeText(
                    this,
                    String.format(
                        getString(R.string.barcode_cannot_be_displayed_as_x),
                        codeTypeIntToString(this, barcodeNotNull.type)
                    ) + detailedErrorMessage,
                    Toast.LENGTH_LONG
                ).show()
                barcodeImageView.visibility = View.GONE
            }

            barcodeImageView.visibility = View.VISIBLE
            val codeBitmap: Bitmap
            try {
                codeBitmap = if (codeIs1D(barcodeNotNull.type)) createCode1D() else createCodeDefault()
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
            barcodeImageView.setImageBitmap(codeBitmap)
        } else {
            barcodePlainTextView.visibility = View.GONE
            barcodeImageView.visibility = View.GONE
        }

        // properties
        credentialPropertiesRecyclerView.adapter?.notifyDataSetChanged() // reload
        if (credential.fields.isNotEmpty() && credential.barcode != null && credential.barcode?.code != "")
            dividerProperties.visibility = View.VISIBLE
        else
            dividerProperties.visibility = View.GONE

        // images
        if (credential.imagePaths.isNotEmpty() && (credential.fields.isNotEmpty() || credential.barcode != null)) {
            dividerImages.visibility = View.VISIBLE
            if (credential.fields.isNotEmpty())
                extraSpaceImages.visibility = View.GONE
            else
                extraSpaceImages.visibility = View.VISIBLE
        }
        else
            dividerImages.visibility = View.GONE
    }

    private fun addLabelsToChipGroup() {
        for (label in credential.labels) {
            val chip = Chip(this)
            chip.text = label
            labelsChipGroup.addView(chip)
        }
    }

    private fun createShowPropertyAdapter(): ShowFieldAdapter {
        return ShowFieldAdapter(credential.fields) {
            // hide all secret values in adapter each time the user presses any of the visibility buttons
            for (position in credential.fields.indices) {
                val holder = credentialPropertiesRecyclerView.findViewHolderForAdapterPosition(position) as? ShowFieldAdapter.ViewHolder
                holder?.setValueHidden(credential.fields[position].secret)
            }
        }
    }

    @Throws(IllegalArgumentException::class, WriterException::class)
    private fun createCodeDefault(): Bitmap {
        val barcodeNotNull = credential.barcode!!
        val widthAndHeight = getBarcodeSize(barcodeNotNull.type)
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            barcodeNotNull.code,
            barcodeIntToBarcodeFormat(barcodeNotNull.type),
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
        val barcodeNotNull = credential.barcode!!
        val widthAndHeight = getBarcodeSize(barcodeNotNull.type)
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            barcodeNotNull.code,
            barcodeIntToBarcodeFormat(barcodeNotNull.type),
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

    private fun getBarcodeSize(barcodeType: Int): WidthAndHeight {
        return when (barcodeType) {
            Barcode.TYPE_AZTEC,
            Barcode.TYPE_QR,
            Barcode.TYPE_DATA_MATRIX
                -> WidthAndHeight(calculatedLayoutWidth, WidthAndHeight.StdFormats.Square)

            Barcode.TYPE_PDF_417,
            Barcode.TYPE_CODABAR,
            Barcode.TYPE_CODE_39,
            Barcode.TYPE_CODE_93,
            Barcode.TYPE_CODE_128,
            Barcode.TYPE_EAN_8,
            Barcode.TYPE_EAN_13,
            Barcode.TYPE_ITF,
            Barcode.TYPE_UPC_A,
            Barcode.TYPE_UPC_E
                -> WidthAndHeight(calculatedLayoutWidth, WidthAndHeight.StdFormats.Horizontal)

            else -> throw IllegalStateException("Unexpected value: $barcodeType")
        }
    }
}