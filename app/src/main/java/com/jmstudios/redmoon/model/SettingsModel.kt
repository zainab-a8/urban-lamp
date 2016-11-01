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

import java.util.ArrayList

import com.jmstudios.redmoon.R
import com.jmstudios.redmoon.preference.ColorSeekBarPreference
import com.jmstudios.redmoon.preference.DimSeekBarPreference
import com.jmstudios.redmoon.preference.IntensitySeekBarPreference

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
    private val mSettingsChangedListeners: ArrayList<OnSettingsChangedListener?>

    private val mPowerStatePrefKey: String
    private val mPauseStatePrefKey: String
    private val mDimPrefKey: String
    private val mIntensityPrefKey: String
    private val mColorPrefKey: String
    private val mOpenOnBootPrefKey: String
    private val mKeepRunningAfterRebootPrefKey: String
    private val mDarkThemePrefKey: String
    private val mBrightnessControlPrefKey: String
    private val mAutomaticFilterModePrefKey: String
    private val mAutomaticTurnOnPrefKey: String
    private val mAutomaticTurnOffPrefKey: String
    private val mDimButtonsPrefKey: String
    private val mBrightnessAutomaticPrefKey: String
    private val mBrightnessLevelPrefKey: String
    private val mProfilePrefKey: String
    private val mAmmountProfilesPrefKey: String
    private val mIntroShownPrefKey: String
    private val mAutomaticSuspendPrefKey: String

    init {
        mSettingsChangedListeners = ArrayList<OnSettingsChangedListener?>()

        mPowerStatePrefKey = resources.getString(R.string.pref_key_shades_power_state)
        mPauseStatePrefKey = resources.getString(R.string.pref_key_shades_pause_state)
        mDimPrefKey = resources.getString(R.string.pref_key_shades_dim_level)
        mIntensityPrefKey = resources.getString(R.string.pref_key_shades_intensity_level)
        mColorPrefKey = resources.getString(R.string.pref_key_shades_color_temp)
        mOpenOnBootPrefKey = resources.getString(R.string.pref_key_always_open_on_startup)
        mKeepRunningAfterRebootPrefKey = resources.getString(R.string.pref_key_keep_running_after_reboot)
        mDarkThemePrefKey = resources.getString(R.string.pref_key_dark_theme)
        mBrightnessControlPrefKey = resources.getString(R.string.pref_key_control_brightness)
        mAutomaticFilterModePrefKey = resources.getString(R.string.pref_key_automatic_filter)
        mAutomaticTurnOnPrefKey = resources.getString(R.string.pref_key_custom_start_time)
        mAutomaticTurnOffPrefKey = resources.getString(R.string.pref_key_custom_end_time)
        mDimButtonsPrefKey = resources.getString(R.string.pref_key_dim_buttons)
        mBrightnessAutomaticPrefKey = resources.getString(R.string.pref_key_brightness_automatic)
        mBrightnessLevelPrefKey = resources.getString(R.string.pref_key_brightness_level)
        mProfilePrefKey = resources.getString(R.string.pref_key_profile_spinner)
        mAmmountProfilesPrefKey = resources.getString(R.string.pref_key_ammount_profiles)
        mIntroShownPrefKey = resources.getString(R.string.pref_key_intro_shown)
        mAutomaticSuspendPrefKey = resources.getString(R.string.pref_key_automatic_suspend)
    }

    var shadesPowerState: Boolean
        get() = mSharedPreferences.getBoolean(mPowerStatePrefKey, false)
        set(state) = mSharedPreferences.edit().putBoolean(mPowerStatePrefKey, state).apply()

    var shadesPauseState: Boolean
        get() = mSharedPreferences.getBoolean(mPauseStatePrefKey, false)
        set(state) = mSharedPreferences.edit().putBoolean(mPauseStatePrefKey, state).apply()

    var shadesDimLevel: Int
        get() = mSharedPreferences.getInt(mDimPrefKey, DimSeekBarPreference.DEFAULT_VALUE)
        set(dimLevel) = mSharedPreferences.edit().putInt(mDimPrefKey, dimLevel).apply()

    var shadesIntensityLevel: Int
        get() = mSharedPreferences.getInt(mIntensityPrefKey, IntensitySeekBarPreference.DEFAULT_VALUE)
        set(intensityLevel) = mSharedPreferences.edit().putInt(mIntensityPrefKey, intensityLevel).apply()

    var shadesColor: Int
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

    val automaticFilterMode: String
        get() = mSharedPreferences.getString(mAutomaticFilterModePrefKey, "never")

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
                mPowerStatePrefKey          -> listener.onShadesPowerStateChanged(shadesPowerState)
                mPauseStatePrefKey          -> listener.onShadesPauseStateChanged(shadesPauseState)
                mDimPrefKey                 -> listener.onShadesDimLevelChanged(shadesDimLevel)
                mIntensityPrefKey           -> listener.onShadesIntensityLevelChanged(shadesIntensityLevel)
                mColorPrefKey               -> listener.onShadesColorChanged(shadesColor)
                mAutomaticFilterModePrefKey -> listener.onShadesAutomaticFilterModeChanged(automaticFilterMode)
                mAutomaticTurnOnPrefKey     -> listener.onShadesAutomaticTurnOnChanged(automaticTurnOnTime)
                mAutomaticTurnOffPrefKey    -> listener.onShadesAutomaticTurnOffChanged(automaticTurnOffTime)
                mBrightnessControlPrefKey   -> listener.onLowerBrightnessChanged(brightnessControlFlag)
                mProfilePrefKey             -> listener.onProfileChanged(profile)
                mAutomaticSuspendPrefKey    -> listener.onAutomaticSuspendChanged(automaticSuspend)
            }
        }
    }
    //endregion

    interface OnSettingsChangedListener {
        fun onShadesPowerStateChanged(powerState: Boolean)
        fun onShadesPauseStateChanged(pauseState: Boolean)
        fun onShadesDimLevelChanged(dimLevel: Int)
        fun onShadesIntensityLevelChanged(intensityLevel: Int)
        fun onShadesColorChanged(color: Int)
        fun onShadesAutomaticFilterModeChanged(automaticFilterMode: String)
        fun onShadesAutomaticTurnOnChanged(turnOnTime: String)
        fun onShadesAutomaticTurnOffChanged(turnOffTime: String)
        fun onLowerBrightnessChanged(lowerBrightness: Boolean)
        fun onProfileChanged(profile: Int)
        fun onAutomaticSuspendChanged(automaticSuspend: Boolean)
    }

    companion object {
        private val TAG = "SettingsModel"
        private val DEBUG = false
    }
}
