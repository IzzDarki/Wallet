package com.izzdarki.wallet.utils

import android.graphics.Bitmap
import android.util.Log

/**
 * Provides functions to work with ScrollAnimationImageView using indices for front and back image.
 */

/** Handles update in [ScrollAnimationImageView] based on index, because cardView currently only supports front and back images */
fun ScrollAnimationImageView.updateBitmapAt(index: Int, bitmap: Bitmap?) {
    if (index == 0) {
        if (bitmap != null)
            this.frontImage = bitmap
        else
            this.removeFrontImage()
    }
    else if (index == 1) {
        if (bitmap != null)
            this.backImage = bitmap
        else
            this.removeBackImage()
    }
    Log.d("asdf", "Setting $index to $bitmap")
}

/**
 * Returns the bitmap used by [this] that corresponds to the image at given `index` in `credential.imagePaths` or null if `index` is out of bounds.
 * Method is needed because this currently only supports front and back images.
 */
fun ScrollAnimationImageView.getBitmapAt(index: Int): Bitmap? {
    return when (index) {
        0 -> this.frontImage
        1 -> this.backImage
        else -> null
    }
}

/**
 * Removes the image at given `index` in the card view and moves all images after it one index up.
 */
fun ScrollAnimationImageView.removeBitmapAt(index: Int, totalImagesBefore: Int) {
    for (i in index until totalImagesBefore - 1) {
        // Move all images one index down
        updateBitmapAt(i, getBitmapAt(i + 1))
    }
    updateBitmapAt(totalImagesBefore - 1, null) // Remove last image from card view
    Log.d("asdf", "card view has ${(0..1000).first { getBitmapAt(it) == null }} bitmaps")
}

/**
 * Inserts the given `bitmap` at given `index` in the card view and moves all images after it one index down.
 */
fun ScrollAnimationImageView.insertBitmapAt(index: Int, bitmap: Bitmap, totalImagesBefore: Int) {
    for (i in totalImagesBefore downTo index + 1) {
        // Move all images one index up
        updateBitmapAt(i, getBitmapAt(i - 1))
    }
    updateBitmapAt(index, bitmap) // Insert new image at index
    Log.d("asdf", "card view has ${(0..1000).first { getBitmapAt(it) == null }} bitmaps")
}
