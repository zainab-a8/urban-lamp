package com.jmstudios.redmoon.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;

import com.jmstudios.redmoon.preference.ColorSeekBarPreference;
import com.jmstudios.redmoon.preference.DimSeekBarPreference;
import com.jmstudios.redmoon.preference.IntensitySeekBarPreference;


public class ScreenFilterView extends View {
    public static final int MIN_DIM       = 0;
    public static final int MIN_INTENSITY = 0;
    private static final float MAX_DIM    = 100f;
    private static final float MIN_ALPHA  = 0x00;
    private static final float MAX_ALPHA  = 0.75f;
    private static final float MAX_DARKEN = 0.75f;

    public static final float DIM_MAX_ALPHA        = 0.9f;
    private static final float INTENSITY_MAX_ALPHA  = 0.75f;
    private static final float ALPHA_ADD_MULTIPLIER = 0.75f;

    private int mDimLevel = DimSeekBarPreference.DEFAULT_VALUE;
    private int mIntensityLevel = IntensitySeekBarPreference.DEFAULT_VALUE;
    private int mColorTempProgress = ColorSeekBarPreference.DEFAULT_VALUE;
    private int mRgbColor = rgbFromColorTemperature(mColorTempProgress);

    public ScreenFilterView(Context context) {
        super(context);
    }

    public int getFilterDimLevel() {
        return mDimLevel;
    }

    public int getFilterIntensityLevel() {
        return mIntensityLevel;
    }

    public int getColorTempProgress() {
        return mColorTempProgress;
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
     * Sets the progress of the color temperature slider of the screen filter.
     *
     * @param colorTempProgress the progress of the color temperature slider.
     */
    public void setColorTempProgress(int colorTempProgress) {
        int colorTemperature = 500 + colorTempProgress * 30;

        mRgbColor = rgbFromColorTemperature(colorTemperature);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int filterColor = getFilterColor(mRgbColor, mDimLevel, mIntensityLevel);

        canvas.drawColor(filterColor);
    }

    public static int rgbFromColorProgress(int colorTempProgress) {
        int colorTemperature = 500 + colorTempProgress * 30;

         return rgbFromColorTemperature(colorTemperature);
    }

    private static int rgbFromColorTemperature(int colorTemperature) {
        int alpha = 255; // alpha is managed seperately

        // After: http://www.tannerhelland.com/4435/convert-temperature-rgb-algorithm-code/
        double temp = ((double) colorTemperature) / 100.0f;

        double red;
        if (temp <= 66)
            red = 255.0;
        else {
            red = temp - 60;
            red = 329.698727446 * Math.pow(red, -0.1332047592);
            if (red < 0) red = 0;
            if (red > 255) red = 255;
        }

        double green;
        if (temp <= 66) {
            green = temp;
            green = 99.4708025861 * Math.log(green) - 161.1195681661;
            if (green < 0) green = 0;
            if (green > 255) green = 255;
        } else {
            green = temp - 60;
            green = 288.1221695283 * Math.pow(green, -0.0755148492);
            if (green < 0) green = 0;
            if (green > 255) green = 255;
        }

        double blue;
        if (temp >= 66)
            blue = 255;
        else {
            if (temp < 19)
                blue = 0;
            else {
                blue = temp - 10;
                blue = 138.5177312231 * Math.log(blue) - 305.0447927307;
                if (blue < 0) blue = 0;
                if (blue > 255) blue = 255;
            }
        }

        return Color.argb(alpha, (int) red, (int) green, (int) blue);
    }

    public static int getIntensityColor(int intensityLevel, int colorTempProgress) {
        int rgbColor = rgbFromColorTemperature(500 + colorTempProgress * 30);
        float red = ((float) Color.red(rgbColor));
        float green = ((float) Color.green(rgbColor));
        float blue = ((float) Color.blue(rgbColor));
        float intensity = 1.0f - ((float) intensityLevel) / 100.0f;

        return Color.argb(255,
                          (int) (red + ((255.0f - red) * intensity)),
                          (int) (green + ((255.0f - green) * intensity)),
                          (int) (blue + ((255.0f - blue) * intensity)));
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

    private static float colorBitsToFloat(int bits) {
        return (float) bits / 255.0f;
    }

    private static int floatToColorBits(float color) {
        return (int) (color * 255.0f);
    }
}
