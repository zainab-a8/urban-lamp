package com.jmstudios.redmoon.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuff;

import com.jmstudios.redmoon.R;
import com.jmstudios.redmoon.view.ScreenFilterView;

public class ColorSeekBarPreference extends Preference {
    private static final String TAG = "ColorSeekBarPreference";
    // Changes to DEFAULT_VALUE should be reflected in preferences.xml
    public static final int DEFAULT_VALUE = 10;

    public SeekBar mColorTempSeekBar;
    private int mProgress;
    private View mView;

    public ColorSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setLayoutResource(R.layout.preference_color_seekbar);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInteger(index, DEFAULT_VALUE);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            mProgress = getPersistedInt(DEFAULT_VALUE);
        } else {
            mProgress = (Integer) defaultValue;
            persistInt(mProgress);
        }
    }

    @Override
    protected void onBindView(@NonNull View view) {
        super.onBindView(view);

        mView = view;

        mColorTempSeekBar = (SeekBar) view.findViewById(R.id.color_temp_seekbar);
        initLayout();
    }

    private void initLayout() {
        mColorTempSeekBar.setProgress(mProgress);

        mColorTempSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mProgress = progress;
                persistInt(mProgress);

                updateMoonIconColor();
                updateProgressText();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        updateMoonIconColor();
        updateProgressText();
    }

    private void updateMoonIconColor() {
        int color = ScreenFilterView.rgbFromColorProgress(mProgress);

        ImageView moonIcon = (ImageView) mView.findViewById(R.id.moon_icon_color);

        PorterDuffColorFilter colorFilter
            = new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY);

        moonIcon.setColorFilter(colorFilter);
    }

    private void updateProgressText() {
        int colorTemp = ScreenFilterView.getColorTempFromProgress(mProgress);

        TextView progressView = (TextView) mView.findViewById(R.id.current_color_temperature);

        String unit = "K";
        String temp = Integer.toString(colorTemp);

        progressView.setText(temp + unit);
    }
}
