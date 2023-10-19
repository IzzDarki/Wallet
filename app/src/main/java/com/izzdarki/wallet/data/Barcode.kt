package com.izzdarki.wallet.data

data class Barcode(
    val code: String,
    var type: Int,
    var showText: Boolean,
) {
    companion object {
        const val TYPE_AZTEC = 1
        const val TYPE_DATA_MATRIX = 2
        const val TYPE_PDF_417 = 4
        const val TYPE_QR = 5
        const val TYPE_CODABAR = 8
        const val TYPE_CODE_39 = 9
        const val TYPE_CODE_93 = 10
        const val TYPE_CODE_128 = 11
        const val TYPE_EAN_8 = 12
        const val TYPE_EAN_13 = 13
        const val TYPE_ITF = 14
        const val TYPE_UPC_A = 15
        const val TYPE_UPC_E = 16

    }
}

