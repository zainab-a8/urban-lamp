/*
 * Copyright (c) 2016  Marien Raat <marienraat@riseup.net>
 *
 *  This file is free software: you may copy, redistribute and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This file is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *  FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *     Copyright (c) 2015 Chris Nguyen
 *
 *     Permission to use, copy, modify, and/or distribute this software
 *     for any purpose with or without fee is hereby granted, provided
 *     that the above copyright notice and this permission notice appear
 *     in all copies.
 *
 *     THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL
 *     WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED
 *     WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE
 *     AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR
 *     CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS
 *     OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *     NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN
 *     CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package com.jmstudios.redmoon.model

import android.content.SharedPreferences
import android.content.res.Resources
import android.util.Log

import com.jmstudios.redmoon.R

import com.jmstudios.redmoon.preference.ColorSeekBarPreference
import com.jmstudios.redmoon.preference.DimSeekBarPreference
import com.jmstudios.redmoon.preference.IntensitySeekBarPreference

import java.util.ArrayList

/**
 * This class provides access to get and set Shades settings, and also listen to settings changes.

 *
 * In order to listen to settings changes, invoke
 * [SettingsModel.addOnSettingsChangedListener] and
 * [SettingsModel.openSettingsChangeListener].

 *
 * **You must call [SettingsModel.closeSettingsChangeListener] when you are done
 * listening to changes.**

 *
 * To begin listening again, invoke [SettingsModel.openSettingsChangeListener].
 */
class SettingsModel(resources: Resources, private val mSharedPreferences: SharedPreferences) : SharedPreferences.OnSharedPreferenceChangeListener {
    private val mSettingsChangedListeners = ArrayList<OnSettingsChangedListener?>()

    private val mPauseStatePrefKey = resources.getString(R.string.pref_key_shades_pause_state)
    private val mDimPrefKey = resources.getString(R.string.pref_key_shades_dim_level)
    private val mIntensityPrefKey = resources.getString(R.string.pref_key_shades_intensity_level)
    private val mColorPrefKey = resources.getString(R.string.pref_key_shades_color_temp)
    private val mOpenOnBootPrefKey = resources.getString(R.string.pref_key_always_open_on_startup)
    private val mKeepRunningAfterRebootPrefKey = resources.getString(R.string.pref_key_keep_running_after_reboot)
    private val mDarkThemePrefKey = resources.getString(R.string.pref_key_dark_theme)
    private val mBrightnessControlPrefKey = resources.getString(R.string.pref_key_control_brightness)
    private val mAutomaticFilterPrefKey = resources.getString(R.string.pref_key_automatic_filter)
    private val mAutomaticTurnOnPrefKey = resources.getString(R.string.pref_key_custom_start_time)
    private val mAutomaticTurnOffPrefKey = resources.getString(R.string.pref_key_custom_end_time)
    private val mDimButtonsPrefKey = resources.getString(R.string.pref_key_dim_buttons)
    private val mBrightnessAutomaticPrefKey = resources.getString(R.string.pref_key_brightness_automatic)
    private val mBrightnessLevelPrefKey = resources.getString(R.string.pref_key_brightness_level)
    private val mProfilePrefKey = resources.getString(R.string.pref_key_profile_spinner)
    private val mAmmountProfilesPrefKey = resources.getString(R.string.pref_key_ammount_profiles)
    private val mIntroShownPrefKey = resources.getString(R.string.pref_key_intro_shown)
    private val mAutomaticSuspendPrefKey = resources.getString(R.string.pref_key_automatic_suspend)

    var pauseState: Boolean
        get() = mSharedPreferences.getBoolean(mPauseStatePrefKey, false)
        set(state) = mSharedPreferences.edit().putBoolean(mPauseStatePrefKey, state).apply()

    var dimLevel: Int
        get() = mSharedPreferences.getInt(mDimPrefKey, DimSeekBarPreference.DEFAULT_VALUE)
        set(dimLevel) = mSharedPreferences.edit().putInt(mDimPrefKey, dimLevel).apply()

    var intensityLevel: Int
        get() = mSharedPreferences.getInt(mIntensityPrefKey, IntensitySeekBarPreference.DEFAULT_VALUE)
        set(intensityLevel) = mSharedPreferences.edit().putInt(mIntensityPrefKey, intensityLevel).apply()

    var color: Int
        get() = mSharedPreferences.getInt(mColorPrefKey, ColorSeekBarPreference.DEFAULT_VALUE)
        set(color) = mSharedPreferences.edit().putInt(mColorPrefKey, color).apply()

    val openOnBootFlag: Boolean
        get() = mSharedPreferences.getBoolean(mOpenOnBootPrefKey, false)

    val resumeAfterRebootFlag: Boolean
        get() = mSharedPreferences.getBoolean(mKeepRunningAfterRebootPrefKey, false)

    val darkThemeFlag: Boolean
        get() = mSharedPreferences.getBoolean(mDarkThemePrefKey, false)

    val brightnessControlFlag: Boolean
        get() = mSharedPreferences.getBoolean(mBrightnessControlPrefKey, false)

    val automaticFilter: Boolean
        get() = mSharedPreferences.getBoolean(mAutomaticFilterPrefKey, false)

    val automaticTurnOnTime: String
        get() = mSharedPreferences.getString(mAutomaticTurnOnPrefKey, "22:00")

    val automaticTurnOffTime: String
        get() = mSharedPreferences.getString(mAutomaticTurnOffPrefKey, "06:00")

    val dimButtonsFlag: Boolean
        get() = mSharedPreferences.getBoolean(mDimButtonsPrefKey, true)

    var brightnessAutomatic: Boolean
        get() = mSharedPreferences.getBoolean(mBrightnessAutomaticPrefKey, true)
        set(automatic) = mSharedPreferences.edit().putBoolean(mBrightnessAutomaticPrefKey, automatic).apply()

    var brightnessLevel: Int
        get() = mSharedPreferences.getInt(mBrightnessLevelPrefKey, 0)
        set(level) = mSharedPreferences.edit().putInt(mBrightnessLevelPrefKey, level).apply()

    var profile: Int
        get() = mSharedPreferences.getInt(mProfilePrefKey, 1)
        set(profile) = mSharedPreferences.edit().putInt(mProfilePrefKey, profile).apply()

    var ammountProfiles: Int
        get() = mSharedPreferences.getInt(mAmmountProfilesPrefKey, 3)
        set(ammountProfiles) = mSharedPreferences.edit().putInt(mAmmountProfilesPrefKey, ammountProfiles).apply()

    var introShown: Boolean
        get() = mSharedPreferences.getBoolean(mIntroShownPrefKey, false)
        set(shown) = mSharedPreferences.edit().putBoolean(mIntroShownPrefKey, shown).apply()

    val automaticSuspend: Boolean
        get() = mSharedPreferences.getBoolean(mAutomaticSuspendPrefKey, false)

    fun addOnSettingsChangedListener(listener: OnSettingsChangedListener) {
        mSettingsChangedListeners.add(listener)
    }

    fun openSettingsChangeListener() {
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this)

        if (DEBUG) Log.d(TAG, "Opened Settings change listener")
    }

    fun closeSettingsChangeListener() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)

        if (DEBUG) Log.d(TAG, "Closed Settings change listener")
    }

    //region OnSharedPreferenceChangeListener
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        for (listener in mSettingsChangedListeners) {
            if (listener==null) {
                val i = mSettingsChangedListeners.indexOf(listener)
                mSettingsChangedListeners.removeAt(i)
            } else when (key) {
                mPauseStatePrefKey        -> listener.onPauseStateChanged(pauseState)
                mDimPrefKey               -> listener.onDimLevelChanged(dimLevel)
                mIntensityPrefKey         -> listener.onIntensityLevelChanged(intensityLevel)
                mColorPrefKey             -> listener.onColorChanged(color)
                mAutomaticFilterPrefKey   -> listener.onAutomaticFilterChanged(automaticFilter)
                mAutomaticTurnOnPrefKey   -> listener.onAutomaticTurnOnChanged(automaticTurnOnTime)
                mAutomaticTurnOffPrefKey  -> listener.onAutomaticTurnOffChanged(automaticTurnOffTime)
                mBrightnessControlPrefKey -> listener.onLowerBrightnessChanged(brightnessControlFlag)
                mProfilePrefKey           -> listener.onProfileChanged(profile)
                mAutomaticSuspendPrefKey  -> listener.onAutomaticSuspendChanged(automaticSuspend)
            }
        }
    }
    //endregion

    interface OnSettingsChangedListener {
        fun onPauseStateChanged(pauseState: Boolean)
        fun onDimLevelChanged(dimLevel: Int)
        fun onIntensityLevelChanged(intensityLevel: Int)
        fun onColorChanged(color: Int)
        fun onAutomaticFilterChanged(automaticFilter: Boolean)
        fun onAutomaticTurnOnChanged(turnOnTime: String)
        fun onAutomaticTurnOffChanged(turnOffTime: String)
        fun onLowerBrightnessChanged(lowerBrightness: Boolean)
        fun onProfileChanged(profile: Int)
        fun onAutomaticSuspendChanged(automaticSuspend: Boolean)
    }

    companion object {
        private val TAG = "SettingsModel"
        private val DEBUG = false
    }
}
