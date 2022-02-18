package com.izzdarki.wallet.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextPaint;
import android.view.ViewGroup;
import android.widget.Space;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.widget.NestedScrollView;

import izzdarki.wallet.R;

public class ScrollAnimationImageView
        extends androidx.appcompat.widget.AppCompatImageView
        implements NestedScrollView.OnScrollChangeListener
{
    static public final double widthToCornerRadiusRatio = 53.98 / 3.18; // 16.974842767295597484276729559748; // used to calculate corner radius in pixels
    static protected float offset = 0;

    protected NestedScrollView scrollView;
    protected LinearLayoutCompat linearLayout;
    protected Space spaceInLinearLayout;
    protected Bitmap frontImage = null;
    protected Bitmap backImage = null;
    protected Bitmap.Config config;
    protected boolean hidden = false;
    protected String frontText = null;
    protected String backText = null;

    public ScrollAnimationImageView(Context context) {
        super(context);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (offset == 0) {// init static variable offset
            offset = (int) getResources().getDimension(R.dimen.default_padding);
            //offset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, offsetDP, getResources().getDisplayMetrics());
        }
    }

    public void addToScrollView(NestedScrollView scrollView) {
        this.scrollView = scrollView;
        scrollView.setOnScrollChangeListener(this);

        this.linearLayout = (LinearLayoutCompat) scrollView.getChildAt(0);
        spaceInLinearLayout = new Space(scrollView.getContext());
        updateSpace();
        linearLayout.addView(spaceInLinearLayout);

        // This causes drawing the animation when soft keyboard comes up
        // drawAnimation is invoked a few times, but not regularly, so that should be fine
        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(this::drawAnimation);
    }


    public void hide() {
        hidden = true;
        setBackgroundResource(android.R.color.transparent);
    }

    public void unHide() {
        hidden = false;
        drawAnimation();
    }

    public boolean isHidden() {
        return hidden;
    }


    public Bitmap getFrontImage() {
        return frontImage;
    }

    public Bitmap getBackImage() {
        return backImage;
    }

    public void setFrontText(String frontText) {
        this.frontText = frontText;
    }

    public void setBackText(String backText) {
        this.backText = backText;
    }

    public String getFrontText() {
        return frontText;
    }

    public String getBackText() {
        return backText;
    }

    @Override
    public void setImageBitmap(Bitmap frontImage) { // can't delete this method
        setFrontImage(frontImage);
    }

    public void setFrontImage(Bitmap frontImage) {
        setImage(frontImage, true);
    }

    public void removeFrontImage() {
        frontImage = null;
        if (backImage == null)
            setImageResource(android.R.color.transparent);
        updateSpace();
        drawAnimation();
    }

    public void removeBackImage() {
        backImage = null;
        if (frontImage == null)
            setImageResource(android.R.color.transparent);
        updateSpace();
        drawAnimation();
    }

    public void setBackImage(Bitmap backImage) {
        setImage(backImage, false);
    }

    public void updateSpace() {
        if (spaceInLinearLayout != null)
            spaceInLinearLayout.setLayoutParams(new LinearLayoutCompat.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getSpaceHeight()));
    }

    public int getSpaceHeight() {
        // returns the space that this View needs
        if (frontImage != null && backImage != null)
            return frontImage.getHeight() + getExtraFrontImageOffsetBefore() + getAnimationSpace() + getFrontImageOffset(); // enhancement: bottomSpaceHeight could be placed at the bottom of the LinearLayout, so that there is enough space for scrolling, but no gap between the ScrollAnimationView and the view below
        else if (frontImage != null)
            return frontImage.getHeight() + getFrontImageOffset();
        else if (backImage != null)
            return backImage.getHeight() + getBackImageOffset();
        else
            return 0;
    }

    @Override
    public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        drawAnimation();
    }

    public void drawAnimation() {
        if (!hidden && (frontImage != null || backImage != null)) {
            Bitmap frame = Bitmap.createBitmap(linearLayout.getWidth(), getHeight(), config);
            Canvas canvas = new Canvas(frame);

            float spaceY = spaceInLinearLayout.getY() - scrollView.getScrollY();

            if (frontImage != null && backImage != null) {
                float frontY = spaceY - getFrontImageOffset() + getExtraFrontImageOffsetBefore();
                float animationY = frontY + getAnimationSpace();
                float backY = animationY;
                float endY = backY + getBackImageOffset() + backImage.getHeight();

                if (frontY > 0) { // front
                    float x = (linearLayout.getWidth() - frontImage.getWidth()) / 2f;
                    float y = getFrontImageOffset() + frontY;
                    canvas.drawBitmap(frontImage, x, y, null);
                    drawTextOnCanvas(frontText, x, y, canvas, frontImage.getHeight());
                }

                else if (animationY > 0) { // animation
                    float animationProgress = 1f - animationY / getAnimationSpace();

                    Bitmap animationImage;
                    String text;
                    try {
                        if (animationProgress < 0.5) {
                            animationImage = Bitmap.createScaledBitmap(frontImage, frontImage.getWidth(), (int) (frontImage.getHeight() * (1 - animationProgress * 2)), false);
                            text = frontText;
                        }
                        else {
                            animationImage = Bitmap.createScaledBitmap(backImage, backImage.getWidth(), (int) (backImage.getHeight() * (animationProgress * 2 - 1)), false);
                            text = backText;
                        }
                    } catch (IllegalArgumentException e) { // half way through the animation (bitmap height == 0)
                        super.setImageResource(android.R.color.transparent);
                        return;
                    }

                    float x = (linearLayout.getWidth() - animationImage.getWidth()) / 2f;
                    float y = (getHeight() - animationImage.getHeight()) / 2.0f;
                    canvas.drawBitmap(animationImage, x, y, null);
                    drawTextOnCanvas(text, x, y, canvas, animationImage.getHeight());

                } else if (endY > 0) { // back
                    float x = (linearLayout.getWidth() - backImage.getWidth()) / 2f;
                    float y = getBackImageOffset() + backY;
                    canvas.drawBitmap(backImage, x, y, null);
                    drawTextOnCanvas(backText, x, y, canvas, backImage.getHeight());
                }

                else { // view is not on screen
                    super.setImageResource(android.R.color.transparent);
                    return;
                }
            }
            else if (frontImage != null) {
                float x = (linearLayout.getWidth() - frontImage.getWidth()) / 2f;
                canvas.drawBitmap(frontImage, x, spaceY, null);
                drawTextOnCanvas(frontText, x, spaceY, canvas, frontImage.getHeight());
            }
            else { // backImage != null
                float x = (linearLayout.getWidth() - backImage.getWidth()) / 2f;
                canvas.drawBitmap(backImage, x, spaceY, null);
                drawTextOnCanvas(backText, x, spaceY, canvas, backImage.getHeight());
            }

            super.setImageBitmap(frame);
        }
    }

    /**
     * When this view is close to y = 0 in the linear layout in the scroll view, an extra offset before the front image is needed, so that the front image starts centered vertically, this is only needed if there is a front and a back image
     * @return the extra needed offset or 0 if nothing extra needed
     */
    protected int getExtraFrontImageOffsetBefore() {
        int offset = (int) (getFrontImageOffset() - spaceInLinearLayout.getY());
        return Math.max(offset, 0);
    }

    protected int getFrontImageOffset() {
        return (int) ((getHeight() - frontImage.getHeight()) / 2f);
    }

    protected int getBackImageOffset() {
        return (int) ((getHeight() - backImage.getHeight()) / 2f);
    }

    protected int getMaxImageHeight() {
        return (int) (getHeight() - 2 * offset);
    }

    protected int getAnimationSpace() {
        return getMaxImageHeight();
    }

    protected void setImage(@NonNull Bitmap image, boolean isFront) {
        Matrix matrix = new Matrix();

        if (image.getWidth() > image.getHeight()) { // rotate
            matrix.postRotate(90);
            image = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
        }

        double scaleVertical = (double) getMaxImageHeight() / image.getHeight(); // scale to fit the view vertically
        double scaleHorizontal = (double) (linearLayout.getWidth()) / image.getWidth(); // scale to fit view horizontally
        double scale = Math.min(scaleVertical, scaleHorizontal);
        image = Bitmap.createScaledBitmap(image, (int) (image.getWidth() * scale), (int) (image.getHeight() * scale), false);

        if (isFront)
            frontImage = getRoundedCornerBitmap(image);
        else
            backImage = getRoundedCornerBitmap(image);

        config = image.getConfig();
        updateSpace();
        drawAnimation();

        if (frontImage != null && backImage != null && frontImage.getConfig() != backImage.getConfig())
            throw new IllegalStateException("ScrollAnimationView: front and back images have to have the same configuration");
    }

    protected static void drawTextOnCanvas(String text, float x, float y, Canvas canvas, int imageHeight) {
        if (text != null) {
            final int bannerSize = 100;
            final int roundPx = (int) (canvas.getWidth() / widthToCornerRadiusRatio);

            if (imageHeight > bannerSize + roundPx) {
                float fadeOutProgress;
                if (imageHeight - (bannerSize + roundPx) > ((bannerSize + roundPx) * 3))
                    fadeOutProgress = 1.0f;
                else
                    fadeOutProgress = ((float) (imageHeight - (bannerSize + roundPx))) / ((bannerSize + roundPx) * 3);

                TextPaint paint = new TextPaint();

                paint.setColor(Color.argb((int) (150 * fadeOutProgress), 0, 0, 0));
                paint.setStyle(Paint.Style.FILL);

                canvas.drawRect(new RectF(x, y + roundPx, x + canvas.getWidth(), y + bannerSize + roundPx), paint);

                paint.setColor(Color.argb((int) (255 * fadeOutProgress), 255, 255, 255));
                paint.setTextSize((float)bannerSize / 2);
                canvas.drawText(text, 0, text.length(), x + (canvas.getWidth() - paint.measureText(text)) / 2, y + roundPx + paint.getTextSize() * 1.25f, paint);
            }
        }
    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap
        .getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);


        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final int roundPx = (int) (canvas.getWidth() / widthToCornerRadiusRatio);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }
}