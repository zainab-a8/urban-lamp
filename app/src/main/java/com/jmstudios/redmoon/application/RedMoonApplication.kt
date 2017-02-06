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
 */

package com.jmstudios.redmoon.application

import android.app.Application
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log

import com.jmstudios.redmoon.R
import com.jmstudios.redmoon.event.*

import org.greenrobot.eventbus.EventBus

class RedMoonApplication: Application(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val mSharedPrefs: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(this)

    override fun onCreate() {
        app = this
        super.onCreate()
        //EventBus.builder().addIndex(eventBusIndex()).installDefaultEventBus()
        mSharedPrefs.registerOnSharedPreferenceChangeListener(this)
        if (DEBUG) Log.d(TAG, "Opened Settings change listener")
    }

    // Only called in emulated environments. In production, just gets killed.
    override fun onTerminate() {
        mSharedPrefs.unregisterOnSharedPreferenceChangeListener(this)
        if (DEBUG) Log.d(TAG, "Closed Settings change listener")
        super.onTerminate()
    }

    //region OnSharedPreferenceChangeListener
    override fun onSharedPreferenceChanged(sp: SharedPreferences, key: String) {
        if (DEBUG) Log.i(TAG, "onPreferenceChanged: " + key)
        EventBus.getDefault().post(when (key) {
            getString(R.string.pref_key_filter_is_on)         -> filterIsOnChanged()
            getString(R.string.pref_key_dim)                  -> dimChanged()
            getString(R.string.pref_key_intensity)            -> intensityChanged()
            getString(R.string.pref_key_color)                -> colorChanged()
            getString(R.string.pref_key_dark_theme)           -> themeChanged()
            getString(R.string.pref_key_time_toggle)          -> timeToggleChanged()
            getString(R.string.pref_key_custom_turn_on_time)  -> customTurnOnTimeChanged()
            getString(R.string.pref_key_custom_turn_off_time) -> customTurnOffTimeChanged()
            getString(R.string.pref_key_lower_brightness)     -> lowerBrightnessChanged()
            getString(R.string.pref_key_profile_spinner)      -> profileChanged()
            getString(R.string.pref_key_use_location)         -> useLocationChanged()
            getString(R.string.pref_key_location)             -> locationChanged()
            getString(R.string.pref_key_sunset_time)          -> sunsetTimeChanged()
            getString(R.string.pref_key_sunrise_time)         -> sunriseTimeChanged()
            else -> return
            /* Preferences for which no Event is posted */
            // getString(R.string.pref_key_lower_brightness)
            // getString(R.string.pref_key_brightness_level)
            // getString(R.string.pref_key_num_profiles)
            // getString(R.string.pref_key_intro_shown)
            // getString(R.string.pref_key_dim_buttons)
        })
    }

    //endregion
    companion object {
        private val TAG = "RedMoonApplication"
        private val DEBUG = true

        lateinit var app: RedMoonApplication
    }
}
