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

import android.os.Bundle
import android.preference.Preference
import android.preference.SwitchPreference
import android.util.Log
import android.widget.Toast
import android.support.v4.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

import com.jmstudios.redmoon.R
import com.jmstudios.redmoon.event.*

import com.jmstudios.redmoon.model.Config
import com.jmstudios.redmoon.preference.TimePickerPreference
import com.jmstudios.redmoon.receiver.TimeToggleChangeReceiver
import com.jmstudios.redmoon.service.LocationUpdateService

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

    private val useLocationPref: SwitchPreference
        get() = (preferenceScreen.findPreference
                (getString(R.string.pref_key_use_location)) as SwitchPreference)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.time_toggle_preferences)
        updatePrefs()

        useLocationPref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->
                val on = newValue as Boolean
                if (on && !isLocationPermissionGranted()) {
                    ActivityCompat.requestPermissions(getActivity(),
                                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                                0)
                    false
                } else {
                   true
                }
            }

        locationPref.onPreferenceClickListener =
            Preference.OnPreferenceClickListener { pref ->
                LocationUpdateService.start()
                true
            }
    }

    private fun isLocationPermissionGranted(): Boolean {
        return (ContextCompat.checkSelfPermission(getActivity(),
            (Manifest.permission.ACCESS_COARSE_LOCATION))
            == PackageManager.PERMISSION_GRANTED)
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
        locationPref.summary = when {
            mIsSearchingLocation                -> getString(R.string.searching_location)
            Config.location == DEFAULT_LOCATION -> getString(R.string.location_not_set)
            else -> with (Config.location) {
                val x = indexOf(",")
                val latitude = java.lang.Double.parseDouble(substring(0, x))
                val longitude = java.lang.Double.parseDouble(substring(x + 1, length))

                val latitudeStr = getString(R.string.latitude_short)
                val longitudeStr = getString(R.string.longitude_short)

                "$latitudeStr: $latitude, $longitudeStr: $longitude"
            }
        }
    }

    private fun updateTimePrefs() {
        val auto = Config.timeToggle
        val useLocation = Config.useLocation
        val enabled = auto && !useLocation
        if (DEBUG) Log.i(TAG, "auto: $auto, useLocation: $useLocation, enabled: $enabled")
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
        if (Config.useLocation) { LocationUpdateService.start() }
        updateTimePrefs()
    }

    @Subscribe
    fun onLocationUpdating(event: locationUpdating) {
        mIsSearchingLocation = true
        updateLocationPref()
    }

    @Subscribe
    fun onLocationChanged(event: locationChanged) {
        mIsSearchingLocation = false
        updateLocationPref()
    }

    @Subscribe
    fun onLocationAccessDenied(event: locationAccessDenied) {
        if (mIsSearchingLocation) {
            Config.requestLocationPermission(activity)
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
