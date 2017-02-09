/*
 * Copyright (c) 2016 Marien Raat <marienraat@riseup.net>
 * Copyright (c) 2017  Stephen Michel <s@smichel.me>
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
package com.jmstudios.redmoon.fragment

import android.Manifest
import android.os.Bundle
import android.preference.Preference
import android.preference.SwitchPreference
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.widget.Toast

import com.jmstudios.redmoon.R
import com.jmstudios.redmoon.event.*

import com.jmstudios.redmoon.model.Config
import com.jmstudios.redmoon.preference.TimePickerPreference
import com.jmstudios.redmoon.receiver.TimeToggleChangeReceiver
import com.jmstudios.redmoon.service.LocationUpdateService

import java.util.*

import org.greenrobot.eventbus.Subscribe


class TimeToggleFragment : EventPreferenceFragment() {
    private var mIsSearchingLocation = false

    // Preferences
    private val timeTogglePref: SwitchPreference
        get() = (preferenceScreen.findPreference
                (getString(R.string.pref_key_time_toggle)) as SwitchPreference)

    private val automaticTurnOnPref: TimePickerPreference
        get() = (preferenceScreen.findPreference
                (getString(R.string.pref_key_custom_turn_on_time)) as TimePickerPreference)

    private val automaticTurnOffPref: TimePickerPreference
        get() = (preferenceScreen.findPreference
                (getString(R.string.pref_key_custom_turn_off_time)) as TimePickerPreference)

    private val locationPref: Preference
        get() = preferenceScreen.findPreference(getString(R.string.pref_key_location))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.time_toggle_preferences)
        updatePrefs()

        locationPref.onPreferenceClickListener =
            Preference.OnPreferenceClickListener { pref ->
                mIsSearchingLocation = true
                pref.summary = getString(R.string.searching_location)
                LocationUpdateService.start(activity)
                true
            }
    }

    private fun updatePrefs() {
        updateSwitchBarTitle()
        updateTimePrefs()
        updateLocationPref()
    }

    private fun updateSwitchBarTitle() {
        timeTogglePref.setTitle(
                if (Config.timeToggle) R.string.text_switch_on
                else R.string.text_switch_off
        )
    }

    private fun updateLocationPref() {
        val l = Config.location
        when {
            mIsSearchingLocation -> locationPref.summary = getString(R.string.searching_location)
            l === DEFAULT_LOCATION -> locationPref.summary = getString(R.string.location_not_set)
            else -> {
                val latitudeStr = getString(R.string.latitude_short)
                val longitudeStr = getString(R.string.longitude_short)

                val x = l.indexOf(",")
                val latitude = java.lang.Double.parseDouble(l.substring(0, x))
                val longitude = java.lang.Double.parseDouble(l.substring(x+1, l.length))

                locationPref.summary = String.format(Locale.getDefault(), "%s: %.2f %s: %.2f",
                                                     latitudeStr, latitude, longitudeStr, longitude)
            }
        }
    }

    private fun updateTimePrefs() {
        val auto = Config.timeToggle
        val useLocation = Config.useLocation
        val enabled = auto && !useLocation
        if (DEBUG) Log.i(TAG, String.format("auto: %s, useLocation: %s, enabled: %s",
                                            auto, useLocation, enabled))
        automaticTurnOnPref.isEnabled = enabled
        automaticTurnOffPref.isEnabled = enabled
        automaticTurnOnPref.summary = Config.automaticTurnOnTime
        automaticTurnOffPref.summary = Config.automaticTurnOffTime
    }

    //region presenter
    @Subscribe
    fun onTimeToggleChanged(event: timeToggleChanged) {
        if (DEBUG) Log.i(TAG, "Filter mode changed to " + Config.timeToggle)
        updatePrefs()
        if (Config.timeToggle) {
            TimeToggleChangeReceiver.rescheduleOnCommand(activity)
            TimeToggleChangeReceiver.rescheduleOffCommand(activity)
        } else {
            TimeToggleChangeReceiver.cancelAlarms(activity)
        }
    }

    @Subscribe
    fun onCustomTurnOnTimeChanged(event: customTurnOnTimeChanged) {
        TimeToggleChangeReceiver.rescheduleOnCommand(activity)
    }

    @Subscribe
    fun onCustomTurnOffTimeChanged(event: customTurnOffTimeChanged) {
        TimeToggleChangeReceiver.rescheduleOffCommand(activity)
    }

    @Subscribe
    fun onUseLocationChanged(event: useLocationChanged) {
        mIsSearchingLocation = true
        updateLocationPref()
        updateTimePrefs()
    }

    @Subscribe
    fun onLocationChanged(event: locationChanged) {
        updateLocationPref()
    }

    @Subscribe
    fun requestLocationPermission(event: locationAccessDenied) {
        if (mIsSearchingLocation && !Config.hasLocationPermission) {
            ActivityCompat.requestPermissions(activity,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 0)
            mIsSearchingLocation = false
        }
    }

    @Subscribe
    fun onLocationServicesDisabled(event: locationServicesDisabled) {
        if (mIsSearchingLocation) {
            val toast = Toast.makeText(activity,
                                       getString(R.string.toast_warning_no_location),
                                       Toast.LENGTH_SHORT)
            toast.show()
            mIsSearchingLocation = false
            updateLocationPref()
        }
    }

    @Subscribe
    fun onSunsetTimeChanged(event: sunsetTimeChanged) {
        TimeToggleChangeReceiver.rescheduleOnCommand(activity)
    }

    @Subscribe
    fun onSunriseTimeChanged(event: sunriseTimeChanged) {
        TimeToggleChangeReceiver.rescheduleOffCommand(activity)
    }
    //endregion

    companion object {
        private val TAG = "TimeToggleFragment"
        private val DEBUG = true
        val DEFAULT_LOCATION = "not set"
    }
}// Android Fragments require an explicit public default constructor for re-creation
