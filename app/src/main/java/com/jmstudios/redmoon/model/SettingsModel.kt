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
import com.jmstudios.redmoon.event.*
import android.util.Log

import com.jmstudios.redmoon.R

import com.jmstudios.redmoon.preference.ColorSeekBarPreference
import com.jmstudios.redmoon.preference.DimSeekBarPreference
import com.jmstudios.redmoon.preference.IntensitySeekBarPreference
import org.greenrobot.eventbus.EventBus

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
class SettingsModel(resources: Resources, private val mSharedPrefs: SharedPreferences) : SharedPreferences.OnSharedPreferenceChangeListener {
    private val mFilterIsOnPrefKey = resources.getString(R.string.pref_key_shades_pause_state)
    private val mDimPrefKey = resources.getString(R.string.pref_key_shades_dim_level)
    private val mIntensityPrefKey = resources.getString(R.string.pref_key_shades_intensity_level)
    private val mColorPrefKey = resources.getString(R.string.pref_key_shades_color_temp)
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

    var filterIsOn: Boolean
        get() = mSharedPrefs.getBoolean(mFilterIsOnPrefKey, false)
        set(state) = mSharedPrefs.edit().putBoolean(mFilterIsOnPrefKey, state).apply()

    var dimLevel: Int
        get() = mSharedPrefs.getInt(mDimPrefKey, DimSeekBarPreference.DEFAULT_VALUE)
        set(dimLevel) = mSharedPrefs.edit().putInt(mDimPrefKey, dimLevel).apply()

    var intensityLevel: Int
        get() = mSharedPrefs.getInt(mIntensityPrefKey, IntensitySeekBarPreference.DEFAULT_VALUE)
        set(intensityLevel) = mSharedPrefs.edit().putInt(mIntensityPrefKey, intensityLevel).apply()

    var color: Int
        get() = mSharedPrefs.getInt(mColorPrefKey, ColorSeekBarPreference.DEFAULT_VALUE)
        set(color) = mSharedPrefs.edit().putInt(mColorPrefKey, color).apply()

    val darkThemeFlag: Boolean
        get() = mSharedPrefs.getBoolean(mDarkThemePrefKey, false)

    val brightnessControlFlag: Boolean
        get() = mSharedPrefs.getBoolean(mBrightnessControlPrefKey, false)

    val automaticFilter: Boolean
        get() = mSharedPrefs.getBoolean(mAutomaticFilterPrefKey, false)

    val automaticTurnOnTime: String
        get() = mSharedPrefs.getString(mAutomaticTurnOnPrefKey, "22:00")

    val automaticTurnOffTime: String
        get() = mSharedPrefs.getString(mAutomaticTurnOffPrefKey, "06:00")

    val dimButtonsFlag: Boolean
        get() = mSharedPrefs.getBoolean(mDimButtonsPrefKey, true)

    var brightnessAutomatic: Boolean
        get() = mSharedPrefs.getBoolean(mBrightnessAutomaticPrefKey, true)
        set(automatic) = mSharedPrefs.edit().putBoolean(mBrightnessAutomaticPrefKey, automatic).apply()

    var brightnessLevel: Int
        get() = mSharedPrefs.getInt(mBrightnessLevelPrefKey, 0)
        set(level) = mSharedPrefs.edit().putInt(mBrightnessLevelPrefKey, level).apply()

    var profile: Int
        get() = mSharedPrefs.getInt(mProfilePrefKey, 1)
        set(profile) = mSharedPrefs.edit().putInt(mProfilePrefKey, profile).apply()

    var ammountProfiles: Int
        get() = mSharedPrefs.getInt(mAmmountProfilesPrefKey, 3)
        set(ammountProfiles) = mSharedPrefs.edit().putInt(mAmmountProfilesPrefKey, ammountProfiles).apply()

    var introShown: Boolean
        get() = mSharedPrefs.getBoolean(mIntroShownPrefKey, false)
        set(shown) = mSharedPrefs.edit().putBoolean(mIntroShownPrefKey, shown).apply()

    val automaticSuspend: Boolean
        get() = mSharedPrefs.getBoolean(mAutomaticSuspendPrefKey, false)

    fun openSettingsChangeListener() {
        mSharedPrefs.registerOnSharedPreferenceChangeListener(this)

        if (DEBUG) Log.d(TAG, "Opened Settings change listener")
    }

    fun closeSettingsChangeListener() {
        mSharedPrefs.unregisterOnSharedPreferenceChangeListener(this)

        if (DEBUG) Log.d(TAG, "Closed Settings change listener")
    }

    //region OnSharedPreferenceChangeListener
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        EventBus.getDefault().post(when (key) {
            mFilterIsOnPrefKey        -> filterIsOnChanged(filterIsOn)
            mDimPrefKey               -> dimLevelChanged(dimLevel)
            mIntensityPrefKey         -> intensityLevelChanged(intensityLevel)
            mColorPrefKey             -> colorChanged(color)
            mAutomaticFilterPrefKey   -> automaticFilterChanged(automaticFilter)
            mAutomaticTurnOnPrefKey   -> automaticTurnOnChanged(automaticTurnOnTime)
            mAutomaticTurnOffPrefKey  -> automaticTurnOffChanged(automaticTurnOffTime)
            mBrightnessControlPrefKey -> lowerBrightnessChanged(brightnessControlFlag)
            mProfilePrefKey           -> profileChanged(profile)
            mAutomaticSuspendPrefKey  -> automaticSuspendChanged(automaticSuspend)
            else                      -> return
        })
    }
    //endregion

    companion object {
        private val TAG = "SettingsModel"
        private val DEBUG = false
    }
}
