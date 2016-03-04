package com.cngu.shades.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;

import com.cngu.shades.preference.ColorPickerPreference;
import com.cngu.shades.preference.DimSeekBarPreference;
import com.cngu.shades.preference.IntensitySeekBarPreference;


public class ScreenFilterView extends View {
    public static final int MIN_DIM       = 0;
    public static final int MIN_INTENSITY = 0;
    private static final float MAX_DIM    = 100f;
    private static final float MIN_ALPHA  = 0x00;
    private static final float MAX_ALPHA  = 0.75f;
    private static final float MAX_DARKEN = 0.75f;

    private static final float DIM_MAX_ALPHA        = 0.9f;
    private static final float INTENSITY_MAX_ALPHA  = 0.75f;
    private static final float ALPHA_ADD_MULTIPLIER = 0.75f;

    private int mDimLevel = DimSeekBarPreference.DEFAULT_VALUE;
    private int mIntensityLevel = IntensitySeekBarPreference.DEFAULT_VALUE;
    private int mRgbColor = ColorPickerPreference.DEFAULT_VALUE;

    public ScreenFilterView(Context context) {
        super(context);
    }

    public int getFilterDimLevel() {
        return mDimLevel;
    }

    public int getFilterIntensityLevel() {
        return mIntensityLevel;
    }

    public int getFilterRgbColor() {
        return mRgbColor;
    }

    /**
     * Sets the dim level of the screen filter.
     *
     * @param dimLevel value between 0 and 100, inclusive, where 0 is doesn't darken, and 100 is the
     *                 maximum allowed dim level determined by the system, but is guaranteed to
     *                 never be fully opaque.
     */
    public void setFilterDimLevel(int dimLevel) {
        mDimLevel = dimLevel;
        invalidate();
    }

    /**
     * Sets the intensity of the screen filter.
     *
     * @param intensityLevel value between 0 and 100, inclusive, where 0 doesn't color the filter,
     *                       and 100 is the maximum allowed intensity determined by the system, but
     *                       is guaranteed to never be fully opaque.
     */
    public void setFilterIntensityLevel(int intensityLevel) {
        mIntensityLevel = intensityLevel;
        invalidate();
    }

    /**
     * Sets the color tint of the screen filter.
     *
     * @param color RGB color represented by a 32-bit int; the format is the same as the one defined
     *              in {@link android.graphics.Color}, but the alpha byte is ignored.
     */
    public void setFilterRgbColor(int color) {
        mRgbColor = color;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int filterColor = getFilterColor(mRgbColor, mDimLevel, mIntensityLevel);

        canvas.drawColor(filterColor);
    }

    private int getFilterColor(int rgbColor, int dimLevel, int intensityLevel) {
        int intensityColor = Color.argb(floatToColorBits(((float) intensityLevel / 100.0f)),
                                          Color.red(rgbColor),
                                          Color.green(rgbColor),
                                          Color.blue(rgbColor));
        int dimColor = Color.argb(floatToColorBits(((float) dimLevel / 100.0f)), 0, 0, 0);
        return addColors(dimColor, intensityColor);
    }

    private int addColors(int color1, int color2) {
        float alpha1 = colorBitsToFloat(Color.alpha(color1));
        float alpha2 = colorBitsToFloat(Color.alpha(color2));
        float red1 = colorBitsToFloat(Color.red(color1));
        float red2 = colorBitsToFloat(Color.red(color2));
        float green1 = colorBitsToFloat(Color.green(color1));
        float green2 = colorBitsToFloat(Color.green(color2));
        float blue1 = colorBitsToFloat(Color.blue(color1));
        float blue2 = colorBitsToFloat(Color.blue(color2));

        // See: http://stackoverflow.com/a/10782314

        // Alpha changed to allow more controll
        float fAlpha = alpha2 * INTENSITY_MAX_ALPHA +
            (DIM_MAX_ALPHA - alpha2 * INTENSITY_MAX_ALPHA) * alpha1;
        alpha1 *= ALPHA_ADD_MULTIPLIER;
        alpha2 *= ALPHA_ADD_MULTIPLIER;
        
        int alpha = floatToColorBits(fAlpha);
        int red = floatToColorBits((red1 * alpha1 + red2 * alpha2 * (1.0f - alpha1)) / fAlpha);
        int green = floatToColorBits((green1 * alpha1 + green2 * alpha2 * (1.0f - alpha1)) / fAlpha);
        int blue = floatToColorBits((blue1 * alpha1 + blue2 * alpha2 * (1.0f - alpha1)) / fAlpha);

        return Color.argb(alpha, red, green, blue);
    }

    private float colorBitsToFloat(int bits) {
        return (float) bits / 255.0f;
    }

    private int floatToColorBits(float color) {
        return (int) (color * 255.0f);
    }
}
