package com.cngu.shades.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;

import com.cngu.shades.preference.ColorPickerPreference;
import com.cngu.shades.preference.DimSeekBarPreference;


public class ScreenFilterView extends View {
    public static final int MIN_DIM      = 0;
    private static final float MAX_DIM   = 100f;
    private static final float MIN_ALPHA = 0x00;
    private static final float MAX_ALPHA = (int) (0xFF * 0.75);
    private static final float MAX_DARKEN = 0.75f;

    private static final float DIM_ALPHA_INFLUENCE       = 0.5f;
    private static final float INTENSITY_ALPHA_INFLUENCE = 0.5f;

    private int mDimLevel = DimSeekBarPreference.DEFAULT_VALUE;
    private int mIntensityLevel = 50; // TODO: default value
    private int mAlpha = levelsToAlpha(mDimLevel, mIntensityLevel);
    private int mRgbColor = ColorPickerPreference.DEFAULT_VALUE;

    public ScreenFilterView(Context context) {
        super(context);
    }

    public int getFilterDimLevel() {
        return mDimLevel;
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
        mAlpha = levelsToAlpha(mDimLevel, mIntensityLevel);
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
        mAlpha = levelsToAlpha(mDimLevel, mIntensityLevel);
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
        float darken = (MAX_DIM - (mDimLevel * MAX_DARKEN)) / MAX_DIM;
        canvas.drawColor(Color.argb(mAlpha,
                                    (int) ((float) Color.red(mRgbColor) * darken),
                                    (int) ((float) Color.green(mRgbColor) * darken),
                                    (int) ((float) Color.blue(mRgbColor) * darken)));
    }

    private static int levelsToAlpha(int dimLevel, int intensityLevel) {
        float totalRelativeValue = ((float) dimLevel) * DIM_ALPHA_INFLUENCE +
            ((float) intensityLevel) * INTENSITY_ALPHA_INFLUENCE;
        return (int) mapToRange(totalRelativeValue, MIN_DIM, MAX_DIM, MIN_ALPHA, MAX_ALPHA);
    }

    private static float mapToRange(float value, float minInput, float maxInput,
                                    float minOutput, float maxOutput) {
        return (value - minInput) * ((maxOutput - minOutput) / (maxInput - minInput)) + minOutput;
    }
}
