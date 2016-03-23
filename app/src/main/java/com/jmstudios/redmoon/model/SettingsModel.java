package com.jmstudios.redmoon.model;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;

import com.jmstudios.redmoon.R;
import com.jmstudios.redmoon.preference.ColorSeekBarPreference;
import com.jmstudios.redmoon.preference.DimSeekBarPreference;
import com.jmstudios.redmoon.preference.IntensitySeekBarPreference;

/**
 * This class provides access to get and set Shades settings, and also listen to settings changes.
 *
 * <p>In order to listen to settings changes, invoke
 * {@link SettingsModel#setOnSettingsChangedListener(OnSettingsChangedListener)} and
 * {@link SettingsModel#openSettingsChangeListener()}.
 *
 * <p><b>You must call {@link SettingsModel#closeSettingsChangeListener()} when you are done
 * listening to changes.</b>
 *
 * <p>To begin listening again, invoke {@link SettingsModel#openSettingsChangeListener()}.
 */
public class SettingsModel implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "SettingsModel";
    private static final boolean DEBUG = true;

    private SharedPreferences mSharedPreferences;
    private ArrayList<OnSettingsChangedListener> mSettingsChangedListeners;

    private String mPowerStatePrefKey;
    private String mPauseStatePrefKey;
    private String mDimPrefKey;
    private String mIntensityPrefKey;
    private String mColorPrefKey;
    private String mOpenOnBootPrefKey;
    private String mKeepRunningAfterRebootPrefKey;
    private String mDarkThemePrefKey;

    public SettingsModel(@NonNull Resources resources, @NonNull SharedPreferences sharedPreferences) {
        mSharedPreferences = sharedPreferences;
        mSettingsChangedListeners = new ArrayList<OnSettingsChangedListener>();

        mPowerStatePrefKey = resources.getString(R.string.pref_key_shades_power_state);
        mPauseStatePrefKey = resources.getString(R.string.pref_key_shades_pause_state);
        mDimPrefKey = resources.getString(R.string.pref_key_shades_dim_level);
        mIntensityPrefKey = resources.getString(R.string.pref_key_shades_intensity_level);
        mColorPrefKey = resources.getString(R.string.pref_key_shades_color_temp);
        mOpenOnBootPrefKey = resources.getString(R.string.pref_key_always_open_on_startup);
        mKeepRunningAfterRebootPrefKey = resources.getString(R.string.pref_key_keep_running_after_reboot);
        mDarkThemePrefKey = resources.getString(R.string.pref_key_dark_theme);
    }

    public boolean getShadesPowerState() {
        return mSharedPreferences.getBoolean(mPowerStatePrefKey, false);
    }

    public void setShadesPowerState(boolean state) {
        mSharedPreferences.edit().putBoolean(mPowerStatePrefKey, state).apply();
    }

    public boolean getShadesPauseState() {
        return mSharedPreferences.getBoolean(mPauseStatePrefKey, false);
    }

    public void setShadesPauseState(boolean state) {
        mSharedPreferences.edit().putBoolean(mPauseStatePrefKey, state).apply();
    }

    public int getShadesDimLevel() {
        return mSharedPreferences.getInt(mDimPrefKey, DimSeekBarPreference.DEFAULT_VALUE);
    }

    public int getShadesIntensityLevel() {
        return mSharedPreferences.getInt(mIntensityPrefKey, IntensitySeekBarPreference.DEFAULT_VALUE);
    }

    public int getShadesColor() {
        return mSharedPreferences.getInt(mColorPrefKey, ColorSeekBarPreference.DEFAULT_VALUE);
    }

    public boolean getOpenOnBootFlag() {
        return mSharedPreferences.getBoolean(mOpenOnBootPrefKey, false);
    }

    public boolean getResumeAfterRebootFlag() {
        return mSharedPreferences.getBoolean(mKeepRunningAfterRebootPrefKey, false);
    }

    public boolean getDarkThemeFlag() {
        return mSharedPreferences.getBoolean(mDarkThemePrefKey, false);
    }

    public void addOnSettingsChangedListener(OnSettingsChangedListener listener) {
        mSettingsChangedListeners.add(listener);
    }

    public void openSettingsChangeListener() {
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);

        if (DEBUG) Log.d(TAG, "Opened Settings change listener");
    }

    public void closeSettingsChangeListener() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);

        if (DEBUG) Log.d(TAG, "Closed Settings change listener");
    }

    //region OnSharedPreferenceChangeListener
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        for (OnSettingsChangedListener mSettingsChangedListener : mSettingsChangedListeners)
            if (mSettingsChangedListener == null) {
                mSettingsChangedListeners.remove(mSettingsChangedListeners.indexOf(mSettingsChangedListener));
            }

        if (key.equals(mPowerStatePrefKey))
        {
            boolean powerState = getShadesPowerState();
            for (OnSettingsChangedListener mSettingsChangedListener : mSettingsChangedListeners)
                mSettingsChangedListener.onShadesPowerStateChanged(powerState);
        }
        else if (key.equals(mPauseStatePrefKey))
        {
            boolean pauseState = getShadesPauseState();
            for (OnSettingsChangedListener mSettingsChangedListener : mSettingsChangedListeners)
                mSettingsChangedListener.onShadesPauseStateChanged(pauseState);
        }
        else if (key.equals(mDimPrefKey))
        {
            int dimLevel = getShadesDimLevel();
            for (OnSettingsChangedListener mSettingsChangedListener : mSettingsChangedListeners)
                mSettingsChangedListener.onShadesDimLevelChanged(dimLevel);
        }
        else if (key.equals(mIntensityPrefKey))
        {
            int intensityLevel = getShadesIntensityLevel();
            for (OnSettingsChangedListener mSettingsChangedListener : mSettingsChangedListeners)
                mSettingsChangedListener.onShadesIntensityLevelChanged(intensityLevel);
        }
        else if (key.equals(mColorPrefKey))
        {
            int color = getShadesColor();
            for (OnSettingsChangedListener mSettingsChangedListener : mSettingsChangedListeners)
                mSettingsChangedListener.onShadesColorChanged(color);
        }
    }
    //endregion

    public interface OnSettingsChangedListener {
        void onShadesPowerStateChanged(boolean powerState);
        void onShadesPauseStateChanged(boolean pauseState);
        void onShadesDimLevelChanged(int dimLevel);
        void onShadesIntensityLevelChanged(int intensityLevel);
        void onShadesColorChanged(int color);
    }
}
