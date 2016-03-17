package com.jmstudios.redmoon.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.ImageView;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.Color;

import com.jmstudios.redmoon.R;

public class DimSeekBarPreference extends Preference {
    public static final int DEFAULT_VALUE = 50;

    private SeekBar mDimLevelSeekBar;
    private int mDimLevel;
    private View mView;

    public DimSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_dim_seekbar);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInteger(index, DEFAULT_VALUE);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            mDimLevel = getPersistedInt(DEFAULT_VALUE);
        } else {
            mDimLevel = (Integer) defaultValue;
            persistInt(mDimLevel);
        }
    }

    @Override
    protected void onBindView(@NonNull View view) {
        super.onBindView(view);

        mView = view;

        mDimLevelSeekBar = (SeekBar) view.findViewById(R.id.dim_level_seekbar);
        initLayout();
    }

    private void initLayout() {
        mDimLevelSeekBar.setProgress(mDimLevel);

        mDimLevelSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mDimLevel = progress;
                persistInt(mDimLevel);

                updateMoonIconColor();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        updateMoonIconColor();
    }

    private void updateMoonIconColor() {
        int lightness = 102 + (int) (((float) (100 - mDimLevel)) * (2.55f * 0.6f));
        int color = Color.rgb(lightness, lightness, lightness);

        ImageView moonIcon = (ImageView) mView.findViewById(R.id.moon_icon_dim);

        PorterDuffColorFilter colorFilter
            = new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY);

        moonIcon.setColorFilter(colorFilter);
    }
}
