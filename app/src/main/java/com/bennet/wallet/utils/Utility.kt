package com.bennet.wallet.utils

import androidx.annotation.ColorInt
import android.util.TypedValue
import com.bennet.wallet.R
import android.graphics.Bitmap
import kotlin.Throws
import android.graphics.BitmapFactory
import android.app.Activity
import android.content.Context
import android.widget.EditText
import kotlin.jvm.JvmOverloads
import android.content.SharedPreferences
import android.graphics.Color
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.bennet.wallet.BuildConfig
import java.io.*
import java.lang.AssertionError
import java.lang.RuntimeException
import java.lang.StringBuilder
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

object Utility {

    const val inputTypeTextNormal =
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
    const val inputTypeTextHiddenPassword =
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    const val inputTypeTextVisiblePassword =
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

    class IDGenerator(
        private val idsList: List<Int>
    ) {
        fun generateID(): Int {
            var ID: Int
            val random = Random()
            do {
                ID = random.nextInt()
            } while (idsList.contains(ID))
            return ID
        }
    }

    @JvmStatic
    @ColorInt
    fun getDefaultBackgroundColor(context: Context): Int {
        val value = TypedValue()
        context.theme.resolveAttribute(
            android.R.attr.colorBackground,
            value,
            true
        ) // kind of works
        return value.data
    }

    infix fun Int.downUntil(to: Int): IntProgression {
        if (to >= Int.MAX_VALUE) return IntRange.EMPTY
        return this downTo (to + 1)
    }

    @JvmStatic
    fun dpToPx(context: Context, dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        )
    }

    @JvmStatic
    fun pxToDp(context: Context, px: Float): Float {
        return px / context.resources.displayMetrics.density
    }

    @JvmStatic
    fun createStringNCopies(n: Int, copies: String?): String {
        val str = StringBuilder("")
        for (i in 0 until n)
            str.append(copies)
        return str.toString()
    }

    @JvmStatic
    fun isMahlerFile(context: Context, imageFile: File): Boolean {
        return imageFile.name.contains(context.getString(R.string.mahler_card_front_image_file_name))
    }

    @JvmStatic
    @ColorInt
    fun makeRGB(a: Int, r: Int, g: Int, b: Int): Int {
        return a and 0xff shl 24 or (r and 0xff shl 16) or (g and 0xff shl 8) or (b and 0xff)
    }

    @JvmStatic
    fun getRelativeLuminance(@ColorInt color: Int): Double {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return (0.2126 * red + 0.7152 * green + 0.0722 * blue) / 255.0
    }

    @JvmStatic
    fun isColorDark(@ColorInt color: Int): Boolean {
        return getRelativeLuminance(color) < 0.5
    }

    @JvmStatic
    @ColorInt
    fun getAverageColorRGB(bitmap: Bitmap): Int {
        @ColorInt val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var redTotal: Long = 0
        var greenTotal: Long = 0
        var blueTotal: Long = 0
        for (pixel in pixels) {
            redTotal += Color.red(pixel).toLong()
            greenTotal += Color.green(pixel).toLong()
            blueTotal += Color.blue(pixel).toLong()
        }
        return makeRGB(
            0xff,
            (redTotal / pixels.size).toInt(),
            (greenTotal / pixels.size).toInt(),
            (blueTotal / pixels.size).toInt()
        )
    }

    @JvmStatic
    @ColorInt
    fun getAverageColorARGB(@ColorInt color1: Int, @ColorInt color2: Int): Int {
        return makeRGB(
            (Color.alpha(color1) + Color.alpha(color2)) / 2,
            (Color.red(color1) + Color.red(color2)) / 2,
            (Color.green(color1) + Color.green(color2)) / 2,
            (Color.blue(color1) + Color.blue(color2)) / 2
        )
    }

    @JvmStatic
    private fun convertColorIntToLab(@ColorInt color: Int): IntArray {
        // copied from https://stackoverflow.com/questions/9018016/how-to-compare-two-colors-for-similarity-difference
        val x: Double
        val y: Double
        val z: Double
        val ls: Double
        val fas: Double
        val fbs: Double
        val eps = (216.0f / 24389.0f).toDouble()
        val k = (24389.0f / 27.0f).toDouble()
        val Xr = 0.964221 // reference white D50
        val Yr = 1.0
        val Zr = 0.825211

        // RGB to XYZ
        var r = (color.red / 255.0f).toDouble() //R 0..1
        var g = (color.green / 255.0f).toDouble() //G 0..1
        var b = (color.blue / 255.0f).toDouble() //B 0..1

        // assuming sRGB (D65)
        r = if (r <= 0.04045) r / 12
        else ((r + 0.055) / 1.055).pow(2.4)

        g = if (g <= 0.04045) g / 12
        else ((g + 0.055) / 1.055).pow(2.4)

        b = if (b <= 0.04045) b / 12
        else ((b + 0.055) / 1.055).pow(2.4)

        x = 0.436052025f * r + 0.385081593f * g + 0.143087414f * b
        y = 0.222491598f * r + 0.71688606f * g + 0.060621486f * b
        z = 0.013929122f * r + 0.097097002f * g + 0.71418547f * b

        // XYZ to Lab
        val xr = x / Xr
        val yr = y / Yr
        val zr = z / Zr

        val fx = if (xr > eps) xr.pow(1 / 3.0)
        else ((k * xr + 16.0) / 116.0)

        val fy = if (yr > eps) yr.pow(1 / 3.0)
        else ((k * yr + 16.0) / 116.0)

        val fz = if (zr > eps) zr.pow(1 / 3.0)
        else (k * zr + 16.0) / 116

        ls = 116 * fy - 16
        fas = 500 * (fx - fy)
        fbs = 200 * (fy - fz)
        val lab = IntArray(3)
        lab[0] = (2.55 * ls + 0.5).toInt()
        lab[1] = (fas + 0.5).toInt()
        lab[2] = (fbs + 0.5).toInt()
        return lab
    }

    @JvmStatic
    private fun getRelativeColorDistance(@ColorInt color1: Int, @ColorInt color2: Int): Double {
        val lab1 = convertColorIntToLab(color1)
        val lab2 = convertColorIntToLab(color2)
        return sqrt(
            (
                    lab2[0] - lab1[0]).toDouble().pow(2.0)
                    + (lab2[1] - lab1[1]).toDouble().pow(2.0)
                    + (lab2[2] - lab1[2]).toDouble().pow(2.0)
        ) / 255
    }

    @JvmStatic
    fun areColorsSimilar(@ColorInt color1: Int, @ColorInt color2: Int): Boolean {
        return getRelativeColorDistance(color1, color2) < 0.1
    }

    @JvmStatic
    @Throws(IOException::class)
    fun moveFile(fromFile: File, toDirectory: String): File {
        val outputFile = copyFile(fromFile, toDirectory)
        fromFile.delete()
        return outputFile
    }

    @JvmStatic
    @Throws(IOException::class)
    fun copyFile(fromFile: File, toDirectory: String): File {
        val fileName = fromFile.name
        val fromStream = FileInputStream(fromFile)
        return copyFile(fromStream, toDirectory, fileName)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun copyFile(fromStream: InputStream, toDirectory: String, fileName: String): File {
        val toDirectoryFile = File(toDirectory)
        if (!toDirectoryFile.exists()) toDirectoryFile.mkdirs()
        val toFile = File(toDirectoryFile, fileName)
        toFile.createNewFile()
        return copyFile(fromStream, toFile)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun copyFile(fromStream: InputStream, toFile: File): File {
        val toStream = FileOutputStream(toFile)
        copyFile(fromStream, toStream)
        return toFile
    }

    @JvmStatic
    @Throws(IOException::class)
    fun copyFile(from: InputStream, to: OutputStream) {
        //Utility.Timer timer = new Utility.Timer("Utility: copy file");

        // copy selected file (transfer bytes)
        try {
            val buf = ByteArray(1024)
            var len: Int
            while (from.read(buf).also { len = it } > 0) to.write(buf, 0, len)
        } finally {
            from.close()
            to.flush()
            to.close()
            //timer.end();
        }
    }

    /**
     * Calculates a scale factor to fit an object of given size to certain max size
     * @param maxShortSide maximum length of short side after scale (-1 for ignore short side)
     * @param maxLongSide maximum length of long side after scale (-1 for ignore long side)
     * @param shortSide length of short side of source
     * @param longSide length of long side of source
     * @return calculated scale (1.0d for unscaled)
     */
    @JvmStatic
    fun getScaleForMaxSize(
        maxShortSide: Int,
        maxLongSide: Int,
        shortSide: Int,
        longSide: Int
    ): Double {
        if (maxShortSide != -1 || maxLongSide != -1) {
            if (maxLongSide == -1) { // max short given
                if (shortSide > maxShortSide) // scale for short side
                    return maxShortSide.toDouble() / shortSide
            } else if (maxShortSide == -1) { // max long given
                if (longSide > maxLongSide) // scale for long side
                    return maxLongSide.toDouble() / longSide
            } else { // max long and max short given
                if (shortSide > maxShortSide && longSide > maxLongSide) { // scale for both sides
                    val scaleShort = maxShortSide.toDouble() / shortSide
                    val scaleLong = maxLongSide.toDouble() / longSide
                    return max(scaleShort, scaleLong)
                } else if (shortSide > maxShortSide) { // scale for short side
                    return maxShortSide.toDouble() / shortSide
                } else if (longSide > maxLongSide) // scale for long side
                    return maxLongSide.toDouble() / longSide
            }
        }
        return 1.0
    }

    /**
     * Calculates a scale factor to fit a bitmap to certain max size
     * @param maxShortSide maximum length of short side after scale (-1 for ignore short side)
     * @param maxLongSide maximum length of long side after scale (-1 for ignore long side)
     * @param bitmap bitmap to calculate scale for
     * @return calculated scale (1.0d for unscaled)
     */
    @JvmStatic
    fun getScaleForMaxSize(maxShortSide: Int, maxLongSide: Int, bitmap: Bitmap): Double {
        return getScaleForMaxSize(
            maxShortSide,
            maxLongSide,
            min(bitmap.width, bitmap.height),
            max(bitmap.width, bitmap.height)
        )
    }

    /**
     * Calculates a scale factor to fit an image file to certain max size
     * @param maxShortSide maximum length of short side after scale (-1 for ignore short side)
     * @param maxLongSide maximum length of long side after scale (-1 for ignore long side)
     * @param imageFile image file to calculate scale for
     * @return calculated scale (1.0d for unscaled)
     */
    @JvmStatic
    fun getScaleForMaxSize(maxShortSide: Int, maxLongSide: Int, imageFile: File): Double {
        if (maxShortSide != -1 || maxLongSide != -1) {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(imageFile.absolutePath, options)
            val shortSide = min(options.outWidth, options.outHeight)
            val longSide = max(options.outWidth, options.outHeight)
            return getScaleForMaxSize(maxShortSide, maxLongSide, shortSide, longSide)
        }
        return 1.0
    }

    /**
     * Scales a bitmap and saves it to a file
     * @param scale scale value
     * @param bitmap source bitmap
     * @param toImageFile destination file
     */
    @JvmStatic
    fun scaleBitmapToFile(scale: Double, bitmap: Bitmap, toImageFile: File?) {
        //Utility.Timer timerMain = new Utility.Timer("Utility: scale image file");
        var scaledBitmap = bitmap
        if (scale != 0.0) {
            //Utility.Timer timer = new Utility.Timer("Utility: create scaled bitmap");
            scaledBitmap = Bitmap.createScaledBitmap(
                scaledBitmap,
                (scaledBitmap.width * scale).toInt(),
                (scaledBitmap.height * scale).toInt(),
                false
            )
            //timer.end();
            var out: FileOutputStream? = null
            try {
                //Utility.Timer timer1 = new Utility.Timer("Utility: save scaled bitmap as file");
                out = FileOutputStream(toImageFile)
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
                //timer1.end();
            } catch (e: IOException) {
                throw AssertionError(e)
            } finally {
                if (out != null) {
                    try {
                        out.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
            //timerMain.end();
        }
    }

    @JvmStatic
    fun hideKeyboard(activity: Activity) {
        // from https://stackoverflow.com/questions/1109022/how-do-you-close-hide-the-android-soft-keyboard-using-java

        //Find the currently focused view, so we can grab the correct window token from it
        var view = activity.currentFocus
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) view = View(activity)
        hideKeyboard(view)
    }

    @JvmStatic
    fun hideKeyboard(view: View) {
        // from https://stackoverflow.com/questions/1109022/how-do-you-close-hide-the-android-soft-keyboard-using-java
        val imm =
            view.context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    @JvmStatic
    fun showKeyboard(activity: Activity) {
        var view = activity.currentFocus
        if (view == null) view = View(activity)
        showKeyboard(view)
    }

    @JvmStatic
    fun showKeyboard(view: View) {
        val imm =
            view.context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_FORCED)
    }

    @JvmStatic
    fun restartInput(view: View) {
        val imm =
            view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.restartInput(view)
    }

    @JvmStatic
    fun setImeOptionsAndRestart(editText: EditText, imeOptions: Int) {
        editText.imeOptions = imeOptions
        restartInput(editText)
    }

    open class Timer internal constructor(name: String) {
        protected var name: String = name
        private var startMillis: Long = -1

        companion object {
            protected var count = 0
        }

        init {
            if (BuildConfig.DEBUG) {
                this.name = name
                startMillis = System.currentTimeMillis()
                count++
            }
        }

        @JvmOverloads
        fun end(text: String? = null) {
            if (BuildConfig.DEBUG) {
                val endMillis = System.currentTimeMillis()
                Log.d(
                    "timing",
                    indentation + name + ": " + (endMillis - startMillis).toInt() + "ms" + if (text != null && text != "") " ($text)" else ""
                )
                count--
            }
        }

        private val indentation: String
            get() {
                val indentation = StringBuilder()
                for (i in 0 until count) indentation.append("\t")
                return indentation.toString()
            }
    }

    /**
     * Array class, that can be saved and extracted from preferences
     * @param T type of the elements
     */
    open class PreferenceArray<T>(
        preferenceString: String?,
        protected var operations: Operations<T>,
        private var separator: String = DEFAULT_SEPARATOR
    ) : ArrayList<T>() {
        interface Operations<T> {
            /**
             * method used to encode element
             * @param element element to be encoded
             * @return string, that is saved in preferences to later reconstruct the given `element`
             */
            fun elementToString(element: T): String

            /**
             * method used to decode element
             * @param string encoded element
             * @return element, that got reconstructed from `string`
             */
            fun stringToElement(string: String): T

            /**
             * method used to check if element is ok to be encoded and saved in preferences <br></br>
             * When calling [.toPreference] or [.toPreference] this method is used to check if all elements are ok
             * @param  element to be checked
             * @return true if element can be encoded and saved in preferences, false otherwise
             */
            fun isElementOK(element: T): Boolean
        }

        class ElementNotOkException internal constructor(
            preferenceArray: PreferenceArray<*>,
            var position: Int
        ) : RuntimeException("Wrong element \"" + preferenceArray[position] + "\" at position " + position + " in " + preferenceArray)

        companion object {
            var DEFAULT_SEPARATOR = ";%&"

            /**
             * Returns String representation of array, that can be used to construct another array
             * @param array null is ok
             */
            @JvmStatic
            @Throws(ElementNotOkException::class)
            fun toPreference(array: PreferenceArray<*>?): String? {
                return array?.toPreference()
            }
        }

        init {
            if (preferenceString != null && preferenceString != "") {
                for (string in preferenceString.split(separator.toRegex()).toTypedArray())
                    add(operations.stringToElement(string))
            }
        }

        /**
         * Returns String representation of array, that can be used to construct another array <br></br>
         * Alternative method [.toPreference] will also work with null instances
         */
        @Throws(ElementNotOkException::class)
        fun toPreference(): String? {
            val stringBuilder = StringBuilder()
            if (size > 0) {
                for (element in this) {
                    if (operations.isElementOK(element))
                        stringBuilder.append(operations.elementToString(element)).append(
                            separator
                        ) else throw ElementNotOkException(this, indexOf(element))
                }
                return stringBuilder.substring(0, stringBuilder.length - 1)
            } else
                return null
        }

        @Throws(ElementNotOkException::class)
        fun saveInPreference(
            preferenceEditor: SharedPreferences.Editor,
            preferenceKey: String?
        ) {
            preferenceEditor.putString(preferenceKey, this.toPreference()).apply()
        }
    }

    /**
     * Wrapper around an [ArrayList<Int>]. Has functionality for converting the list to a String and creating the list from a String
     */
    class PreferenceArrayInt @JvmOverloads constructor(preferenceString: String? = null) :
        PreferenceArray<Int>(preferenceString, object : Operations<Int> {
            override fun elementToString(element: Int): String {
                return element.toString()
            }

            override fun stringToElement(string: String): Int {
                return string.toInt()
            }

            override fun isElementOK(element: Int): Boolean {
                return true
            }
        }, ",")

    /* AutoSavePreferenceArray not used currently
    open class AutoSavePreferenceArray<T> : PreferenceArray<T> {
        protected var preferences: SharedPreferences? = null
        private var key: String? = null

        constructor(operations: Operations<T>, preferences: SharedPreferences, key: String) : super(preferences.getString(key, null), operations) {
            init(preferences, key)
        }

        constructor(
            operations: Operations<T>,
            separator: String,
            preferences: SharedPreferences,
            key: String
        ) : super(preferences.getString(key, null), operations, separator) {
            init(preferences, key)
        }

        private fun init(preferences: SharedPreferences, key: String) {
            this.preferences = preferences
            this.key = key
        }

        override fun add(element: T): Boolean {
            return if (operations.isElementOK(element)) {
                val returnVal = super.add(element)
                try {
                    saveInPreference(preferences!!.edit(), key)
                } catch (ignored: ElementNotOkException) {
                    // can't occur in this place, because t is already checked to be ok
                }
                returnVal
            } else false
        }

        @Throws(RuntimeException::class)
        override fun add(index: Int, element: T) {
            if (operations.isElementOK(element)) {
                super.add(index, element)
                try {
                    saveInPreference(preferences!!.edit(), key)
                } catch (ignored: ElementNotOkException) {
                    // can't occur in this place, because t is already checked to be ok
                }
            } else throw RuntimeException(ElementNotOkException(this, indexOf(element)))
        }

        override fun remove(element: T): Boolean {
            val returnVal = super.remove(element)
            try {
                saveInPreference(preferences!!.edit(), key)
            } catch (ignored: ElementNotOkException) {
                // can't occur in this place, because t is already checked to be ok
            }
            return returnVal
        }

        override fun removeAt(index: Int): T {
            val returnVal: T = super.removeAt(index)
            try {
                saveInPreference(preferences!!.edit(), key)
            } catch (ignored: ElementNotOkException) {
                // can't occur in this place, because t is already checked to be ok
            }
            return returnVal
        }

        override fun removeRange(fromIndex: Int, toIndex: Int) {
            super.removeRange(fromIndex, toIndex)
            try {
                saveInPreference(preferences!!.edit(), key)
            } catch (ignored: ElementNotOkException) {
                // can't occur in this place, because t is already checked to be ok
            }
        }
    }

    class AutoSavePreferenceArrayInt(preferences: SharedPreferences, key: String) :
        AutoSavePreferenceArray<Int>(object : Operations<Int> {
            override fun elementToString(element: Int): String {
                return element.toString()
            }

            override fun stringToElement(string: String): Int {
                return string.toInt()
            }

            override fun isElementOK(element: Int): Boolean {
                return true
            }
        }, ",", preferences, key)
     */
}
