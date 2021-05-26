package com.bennet.wallet;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Utility {
    static public float DPtoPX(DisplayMetrics displayMetrics, float DP) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DP, displayMetrics);
    }

    static public @ColorInt int makeRGB(int a, int r, int g, int b) {
        return  (a & 0xff) << 24 | (r & 0xff) << 16 | (g & 0xff) << 8 | (b & 0xff);
    }

    static public double getRelativeLuminance(@ColorInt int color) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return (0.2126 * red + 0.7152 * green + 0.0722 * blue) / 255d;
    }

    static public boolean isColorDark(@ColorInt int color) {
        return getRelativeLuminance(color) < 0.5d;
    }

    static public @ColorInt int getAverageColorRGB(Bitmap bitmap) {
        @ColorInt int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        long redTotal = 0;
        long greenTotal = 0;
        long blueTotal = 0;

        for (int pixel : pixels) {
            redTotal += Color.red(pixel);
            greenTotal += Color.green(pixel);
            blueTotal += Color.blue(pixel);
        }

        return makeRGB(0xff, (int) (redTotal / pixels.length), (int) (greenTotal / pixels.length), (int) (blueTotal / pixels.length));
    }

    static public @ColorInt int getAverageColorARGB(@ColorInt int color1, @ColorInt int color2) {
        return makeRGB((Color.alpha(color1) + Color.alpha(color2)) / 2,(Color.red(color1) + Color.red(color2)) / 2, (Color.green(color1) + Color.green(color2)) / 2, (Color.blue(color1) + Color.blue(color2)) / 2);
    }

    public static int[] convertColorIntToLab(@ColorInt int color) {
        // copied from https://stackoverflow.com/questions/9018016/how-to-compare-two-colors-for-similarity-difference

        final int R = Color.red(color);
        final int G = Color.green(color);
        final int B = Color.blue(color);

        double r, g, b, X, Y, Z, fx, fy, fz, xr, yr, zr;
        double Ls, fas, fbs;
        final double eps = 216.0f / 24389.0f;
        final double k = 24389.0f / 27.0f;

        double Xr = 0.964221f;  // reference white D50
        double Yr = 1.0f;
        double Zr = 0.825211f;

        // RGB to XYZ
        r = R / 255.0f; //R 0..1
        g = G / 255.0f; //G 0..1
        b = B / 255.0f; //B 0..1

        // assuming sRGB (D65)
        if (r <= 0.04045) r = r / 12;
        else r = (float)Math.pow((r + 0.055) / 1.055, 2.4);

        if (g <= 0.04045) g = g / 12;
        else g = (float)Math.pow((g + 0.055) / 1.055, 2.4);

        if (b <= 0.04045) b = b / 12;
        else b = (float)Math.pow((b + 0.055) / 1.055, 2.4);

        X = 0.436052025f * r + 0.385081593f * g + 0.143087414f * b;
        Y = 0.222491598f * r + 0.71688606f * g + 0.060621486f * b;
        Z = 0.013929122f * r + 0.097097002f * g + 0.71418547f * b;

        // XYZ to Lab
        xr = X / Xr;
        yr = Y / Yr;
        zr = Z / Zr;

        if (xr > eps) fx = (float)Math.pow(xr, 1 / 3.0);
        else fx = (float)((k * xr + 16.0) / 116.0);

        if (yr > eps) fy = (float)Math.pow(yr, 1 / 3.0);
        else fy = (float)((k * yr + 16.0) / 116.0);

        if (zr > eps) fz = (float)Math.pow(zr, 1 / 3.0);
        else fz = (float)((k * zr + 16.0) / 116);

        Ls = (116 * fy) - 16;
        fas = 500 * (fx - fy);
        fbs = 200 * (fy - fz);

        int[] lab = new int[3];
        lab[0] = (int)(2.55 * Ls + 0.5);
        lab[1] = (int)(fas + 0.5);
        lab[2] = (int)(fbs + 0.5);
        return lab;
    }

    static public double getRelativeColorDistance(@ColorInt int color1, @ColorInt int color2) {
        int[] lab1 = convertColorIntToLab(color1);
        int[] lab2 = convertColorIntToLab(color2);
        return Math.sqrt(Math.pow(lab2[0] - lab1[0], 2) + Math.pow(lab2[1] - lab1[1], 2) + Math.pow(lab2[2] - lab1[2], 2)) / 255;
    }

    static public boolean areColorsSimilar(@ColorInt int color1, @ColorInt int color2) {
        return getRelativeColorDistance(color1, color2) < 0.1;
    }


    static public File moveFile(File fromFile, String toDirectory) throws IOException {
        File outputFile = copyFile(fromFile, toDirectory);
        fromFile.delete();
        return outputFile;
    }

    static public File copyFile(File fromFile, String toDirectory) throws IOException {
        String fileName = fromFile.getName();
        FileInputStream fromStream = new FileInputStream(fromFile);

        return copyFile(fromStream, toDirectory, fileName);
    }

    static public File copyFile(InputStream fromStream, String toDirectory, String fileName) throws IOException {
        File toDirectoryFile = new File(toDirectory);
        if (!toDirectoryFile.exists())
            toDirectoryFile.mkdirs();

        File toFile = new File(toDirectoryFile, fileName);
        toFile.createNewFile();

        return copyFile(fromStream, toFile);
    }

    static public File copyFile(InputStream fromStream, File toFile) throws IOException {
        FileOutputStream toStream = new FileOutputStream(toFile);

        copyFile(fromStream, toStream);
        return toFile;
    }

    static public void copyFile(InputStream from, OutputStream to) throws IOException {
        //Utility.Timer timer = new Utility.Timer("Utility: copy file");

        // copy selected file (transfer bytes)
        try {
            byte[] buf = new byte[1024];
            int len;
            while ((len = from.read(buf)) > 0)
                to.write(buf, 0, len);

        } finally {
            from.close();
            to.flush();
            to.close();
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
    static public double getScaleForMaxSize(int maxShortSide, int maxLongSide, int shortSide, int longSide) {
        if (maxShortSide != -1 || maxLongSide != -1) {
            if (maxLongSide == -1) { // max short given
                if (shortSide > maxShortSide) // scale for short side
                    return  (double) maxShortSide / shortSide;
            } else if (maxShortSide == -1) { // max long given
                if (longSide > maxLongSide) // scale for long side
                    return (double) maxLongSide / longSide;
            } else { // max long and max short given
                if (shortSide > maxShortSide && longSide > maxLongSide) { // scale for both sides
                    double scaleShort = (double) maxShortSide / shortSide;
                    double scaleLong = (double) maxLongSide / longSide;
                    return Math.max(scaleShort, scaleLong);
                } else if (shortSide > maxShortSide) { // scale for short side
                    return (double) maxShortSide / shortSide;
                } else if (longSide > maxLongSide) // scale for long side
                    return (double) maxLongSide / longSide;
            }
        }
        return 1.0d;
    }

    /**
     * Calculates a scale factor to fit a bitmap to certain max size
     * @param maxShortSide maximum length of short side after scale (-1 for ignore short side)
     * @param maxLongSide maximum length of long side after scale (-1 for ignore long side)
     * @param bitmap bitmap to calculate scale for
     * @return calculated scale (1.0d for unscaled)
     */
    static public double getScaleForMaxSize(int maxShortSide, int maxLongSide, Bitmap bitmap) {
        return getScaleForMaxSize(maxShortSide, maxLongSide, Math.min(bitmap.getWidth(), bitmap.getHeight()), Math.max(bitmap.getWidth(), bitmap.getHeight()));
    }

    /**
     * Calculates a scale factor to fit an image file to certain max size
     * @param maxShortSide maximum length of short side after scale (-1 for ignore short side)
     * @param maxLongSide maximum length of long side after scale (-1 for ignore long side)
     * @param imageFile image file to calculate scale for
     * @return calculated scale (1.0d for unscaled)
     */
    static public double getScaleForMaxSize(int maxShortSide, int maxLongSide, File imageFile) {
        if (maxShortSide != -1 || maxLongSide != -1) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
            int shortSide = Math.min(options.outWidth, options.outHeight);
            int longSide = Math.max(options.outWidth, options.outHeight);

            return getScaleForMaxSize(maxShortSide, maxLongSide, shortSide, longSide);
        }
        return 1.0d;
    }

    /**
     * Scales a bitmap and saves it to a file
     * @param scale scale value
     * @param bitmap source bitmap
     * @param toImageFile destination file
     */
    static public void scaleBitmapToFile(double scale, Bitmap bitmap, File toImageFile) {
        //Utility.Timer timerMain = new Utility.Timer("Utility: scale image file");
        if (scale != 0) {
            //Utility.Timer timer = new Utility.Timer("Utility: create scaled bitmap");
            bitmap = Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * scale), (int) (bitmap.getHeight() * scale), false);
            //timer.end();

            FileOutputStream out = null;
            try {
                //Utility.Timer timer1 = new Utility.Timer("Utility: save scaled bitmap as file");
                out = new FileOutputStream(toImageFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                out.flush();
               //timer1.end();
            } catch (IOException e) {
                throw new AssertionError(e);
            }
            finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            //timerMain.end();
        }
    }


    static public void hideKeyboard(Activity activity) {
        // from https://stackoverflow.com/questions/1109022/how-do-you-close-hide-the-android-soft-keyboard-using-java

        //Find the currently focused view, so we can grab the correct window token from it
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null)
            view = new View(activity);
        hideKeyboard(activity, view);
    }

    static public void hideKeyboard(Context context, View view) {
        // from https://stackoverflow.com/questions/1109022/how-do-you-close-hide-the-android-soft-keyboard-using-java

        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    static public void showKeyboard(Activity activity) {
        View view = activity.getCurrentFocus();
        if (view == null)
            view = new View(activity);
        showKeyboard(activity, view);
    }

    static public void showKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, InputMethodManager.SHOW_FORCED);
    }

    static public void restartInput(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.restartInput(view);
    }

    /*
    public static class Timer {
        static protected int count = 0;
        protected String name;
        protected long startMillis = -1;

        Timer(String name) {
            if (BuildConfig.DEBUG) {
                this.name = name;
                startMillis = System.currentTimeMillis();
                count++;
            }
        }

        void end() {
            end(null);
        }

        void end(String text) {
            if (BuildConfig.DEBUG) {
                final long endMillis = System.currentTimeMillis();
                Log.d("timing", getIndentation() + name + ": " + (int) (endMillis - startMillis) + "ms" + (text != null && !text.equals("") ? (" (" + text + ")") : ("")));
                count--;
            }
        }

        private String getIndentation() {
            StringBuilder indentation = new StringBuilder();
            for (int i = 0; i < count; i++)
                indentation.append("\t");

            return indentation.toString();
        }
    }
    */

    /**
     * Array class, that can be saved and extracted from preferences
     * @param <T> type of the elements
     */
    public static class PreferenceArray<T> extends ArrayList<T> {
        public static String DEFAULT_SEPARATOR = ";%&";

        public interface Operations<T> {
            /**
             * method used to encode element
             * @param element element to be encoded
             * @return string, that is saved in preferences to later reconstruct the given {@code element}
             */
            String elementToString(T element);

            /**
             * method used to decode element
             * @param string encoded element
             * @return element, that got reconstructed from {@code string}
             */
            T stringToElement(String string);

            /**
             * method used to check if element is ok to be encoded and saved in preferences <br>
             * When calling {@link #toPreference()} or {@link #toPreference(PreferenceArray)} this method is used to check if all elements are ok
             * @param  element to be checked
             * @return true if element can be encoded and saved in preferences, false otherwise
             */
            boolean isElementOK(T element);
        }

        public static class ElementNotOkException extends RuntimeException {
            public int position;
            ElementNotOkException(PreferenceArray<?> preferenceArray, int position) {
                super("Wrong element \"" + preferenceArray.get(position) + "\" at position " + position + " in " + preferenceArray);
                this.position = position;
            }
        }

        protected Operations<T> operations;
        protected String separator;


        private PreferenceArray(String preferenceString, Operations<T> operations) {
            this(preferenceString, operations, DEFAULT_SEPARATOR);
        }

        private PreferenceArray(String preferenceString, Operations<T> operations, String separator) {
            super();
            this.operations = operations;
            this.separator = separator;
            if (preferenceString != null && !preferenceString.equals("")) {
                for (String string : preferenceString.split(separator))
                    add(operations.stringToElement(string));
            }
        }


        /**
         * Returns String representation of array, that can be used to construct another array <br>
         * Alternative method {@link #toPreference(PreferenceArray)} will also work with null instances
         */
        public String toPreference() throws ElementNotOkException {
            StringBuilder stringBuilder = new StringBuilder();
            if (size() > 0) {
                for (T element : this) {
                    if (operations.isElementOK(element))
                        stringBuilder.append(operations.elementToString(element)).append(separator);
                    else
                        throw new ElementNotOkException(this, indexOf(element));
                }
                return stringBuilder.substring(0, stringBuilder.length() - 1);
            }
            else
                return null;
        }

        /**
         * Returns String representation of array, that can be used to construct another array
         * @param array null is ok
         */
        static public String toPreference(PreferenceArray<?> array) throws ElementNotOkException {
            if (array != null)
                return array.toPreference();
            else
                return null;
        }

        public void saveInPreference(SharedPreferences.Editor preferenceEditor, String preferenceKey) throws ElementNotOkException {
            preferenceEditor.putString(preferenceKey, this.toPreference()).apply();
        }
    }

    public static class AutoSavePreferenceArray<T> extends PreferenceArray<T> {
        protected SharedPreferences preferences;
        protected String key;

        public AutoSavePreferenceArray(Operations<T> operations, SharedPreferences preferences, String key) {
            super(preferences.getString(key, null), operations);
            init(preferences, key);
        }

        public AutoSavePreferenceArray(Operations<T> operations, String separator, SharedPreferences preferences, String key) {
            super(preferences.getString(key, null), operations, separator);
            init(preferences, key);
        }

        private void init(SharedPreferences preferences, String key) {
            this.preferences = preferences;
            this.key = key;
        }

        @Override
        public boolean add(T t) {
            if (operations.isElementOK(t)) {
                boolean returnVal = super.add(t);
                try {
                    saveInPreference(preferences.edit(), key);
                } catch (ElementNotOkException ignored) {
                    // can't occur in this place, because t is already checked to be ok
                }
                return returnVal;
            }
            else
                return false;
        }

        @Override
        public void add(int index, T element) throws RuntimeException {
            if (operations.isElementOK(element)) {
                super.add(index, element);
                try {
                    saveInPreference(preferences.edit(), key);
                } catch (ElementNotOkException ignored) {
                    // can't occur in this place, because t is already checked to be ok
                }
            }
            else
                throw new RuntimeException(new ElementNotOkException(this, indexOf(element)));
        }

        @Override
        public boolean remove(@Nullable Object o) {
            boolean returnVal = super.remove(o);
            try {
                saveInPreference(preferences.edit(), key);
            } catch (ElementNotOkException ignored) {
                // can't occur in this place, because t is already checked to be ok
            }
            return returnVal;
        }

        @Override
        public T remove(int index) {
            T returnVal = super.remove(index);
            try {
                saveInPreference(preferences.edit(), key);
            } catch (ElementNotOkException ignored) {
                // can't occur in this place, because t is already checked to be ok
            }
            return returnVal;
        }

        @Override
        protected void removeRange(int fromIndex, int toIndex) {
            super.removeRange(fromIndex, toIndex);
            try {
                saveInPreference(preferences.edit(), key);
            } catch (ElementNotOkException ignored) {
                // can't occur in this place, because t is already checked to be ok
            }
        }
    }

    public static class PreferenceArrayInt extends PreferenceArray<Integer> {
        public PreferenceArrayInt(String preferenceString) {
            super(preferenceString, new Operations<Integer>() {
                @Override
                public String elementToString(Integer element) {
                    return String.valueOf(element);
                }

                @Override
                public Integer stringToElement(String string) {
                    return Integer.parseInt(string);
                }

                @Override
                public boolean isElementOK(Integer element) {
                    return true;
                }
            }, ",");
        }
    }

    public static class AutoSavePreferenceArrayInt extends AutoSavePreferenceArray<Integer> {

        public AutoSavePreferenceArrayInt(SharedPreferences preferences, String key) {
            super(new Operations<Integer>() {
                @Override
                public String elementToString(Integer element) {
                    return String.valueOf(element);
                }

                @Override
                public Integer stringToElement(String string) {
                    return Integer.parseInt(string);
                }

                @Override
                public boolean isElementOK(Integer element) {
                    return true;
                }
            }, ",", preferences, key);
        }
    }

    public static class IDGenerator {
        protected List<Integer> idsList;

        public IDGenerator(List<Integer> idsList) {
            this.idsList = idsList;
        }

        public IDGenerator() {}

        public void setIdsList(List<Integer> idsList) {
            this.idsList = idsList;
        }

        public int generateID() {
            int ID;
            do {
                ID = new Random().nextInt();
            } while (idsList.contains(ID));
            return ID;
        }

        public void deleteID(int ID) {
            idsList.remove((Integer) ID);
        }
    }
}
