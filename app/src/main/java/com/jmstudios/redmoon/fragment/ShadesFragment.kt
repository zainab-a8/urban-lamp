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
package com.jmstudios.redmoon.fragment

import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.Manifest
import android.net.Uri
import android.os.Build.VERSION
import android.os.Bundle
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen
import android.preference.SwitchPreference
import android.provider.Settings
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ListView
import android.widget.Switch
import android.widget.TextView

import com.jmstudios.redmoon.R
import com.jmstudios.redmoon.activity.ShadesActivity
import com.jmstudios.redmoon.model.SettingsModel
import com.jmstudios.redmoon.presenter.ShadesPresenter
import com.jmstudios.redmoon.preference.FilterTimePreference
import com.jmstudios.redmoon.preference.LocationPreference
import com.jmstudios.redmoon.service.ScreenFilterService

class ShadesFragment : PreferenceFragment() {

    private var mPresenter: ShadesPresenter? = null
    private var mView: View? = null
    private var mHelpSnackbar: Snackbar? = null

    // Preferences
    private var darkThemePref: SwitchPreference? = null
    private var lowerBrightnessPref: SwitchPreference? = null
    private var automaticFilterPref: SwitchPreference? = null
    private var useLocationPref: SwitchPreference? = null
    private var locationPref: LocationPreference? = null
    private var automaticTurnOnPref: FilterTimePreference? = null
    private var automaticTurnOffPref: FilterTimePreference? = null
    private var otherPrefCategory: Preference? = null
    private var automaticSuspendPref: Preference? = null

    private val searchingLocation: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.preferences)

        val darkThemePrefKey = getString(R.string.pref_key_dark_theme)
        val lowerBrightnessPrefKey = getString(R.string.pref_key_control_brightness)
        val automaticFilterPrefKey = getString(R.string.pref_key_automatic_filter)
        val automaticTurnOnPrefKey = getString(R.string.pref_key_custom_start_time)
        val automaticTurnOffPrefKey = getString(R.string.pref_key_custom_end_time)
        val locationPrefKey = getString(R.string.pref_key_location)
        val useLocationPrefKey = getString(R.string.pref_key_use_location)
        val otherCategoryPrefKey = getString(R.string.pref_key_other)
        val automaticSuspendPrefKey = getString(R.string.pref_key_automatic_suspend)

        val prefScreen = preferenceScreen
        darkThemePref = prefScreen.findPreference(darkThemePrefKey) as SwitchPreference
        lowerBrightnessPref = prefScreen.findPreference(lowerBrightnessPrefKey) as SwitchPreference
        automaticFilterPref = prefScreen.findPreference(automaticFilterPrefKey) as SwitchPreference
        locationPref = prefScreen.findPreference(locationPrefKey) as LocationPreference
        useLocationPref = prefScreen.findPreference(useLocationPrefKey) as SwitchPreference
        automaticTurnOnPref = prefScreen.findPreference(automaticTurnOnPrefKey) as FilterTimePreference
        automaticTurnOffPref = prefScreen.findPreference(automaticTurnOffPrefKey) as FilterTimePreference
        otherPrefCategory = prefScreen.findPreference(otherCategoryPrefKey)
        automaticSuspendPref = prefScreen.findPreference(automaticSuspendPrefKey)

        darkThemePref!!.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    activity.recreate()
                    true
                }

        if (android.os.Build.VERSION.SDK_INT >= 23 && !Settings.System.canWrite(context))
            lowerBrightnessPref!!.isChecked = false

        lowerBrightnessPref!!.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            val checked = newValue as Boolean
            if (checked && android.os.Build.VERSION.SDK_INT >= 23 &&
                    !Settings.System.canWrite(context)) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse("package:" + context.packageName))
                startActivityForResult(intent, -1)
                return@OnPreferenceChangeListener false
            }

            true
        }

        val auto = automaticFilterPref!!.isChecked
        useLocationPref!!.isEnabled = auto
        val sun = true
        automaticTurnOnPref!!.isEnabled = auto && !sun
        automaticTurnOffPref!!.isEnabled = auto && !sun



        onAutomaticFilterPreferenceChange(automaticFilterPref!!, auto)

        automaticFilterPref!!.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue -> onAutomaticFilterPreferenceChange(preference, newValue) }

        locationPref!!.setOnLocationChangedListener {
            if (true) {
                updateFilterTimesFromSun()
            }
        }

        // Automatic suspend
        val automaticSuspendOn = (activity as ShadesActivity).settingsModel.automaticSuspend
        automaticSuspendPref!!.setSummary(if (automaticSuspendOn)
            R.string.text_switch_on
        else
            R.string.text_switch_off)
    }

    override fun onResume() {
        super.onResume()

        val automaticSuspendOn = (activity as ShadesActivity).settingsModel.automaticSuspend
        automaticSuspendPref!!.setSummary(if (automaticSuspendOn)
            R.string.text_switch_on
        else
            R.string.text_switch_off)
    }

    private fun onAutomaticFilterPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val auto = newValue as Boolean
        locationPref!!.isEnabled = auto
        val sun = true
        automaticTurnOnPref!!.isEnabled = false
        automaticTurnOffPref!!.isEnabled = false
        if (auto && ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 0)
            return false
        }

        // From something to sun
        if (auto) {
            // Update the FilterTimePreferences
            updateFilterTimesFromSun()

            // Attempt to get a new location
            locationPref!!.searchLocation(false)
        }

        // From sun to something
        // if (!sun) {
        //     automaticTurnOnPref.setToCustomTime();
        //     automaticTurnOffPref.setToCustomTime();
        // }

        return true
    }

    private fun updateFilterTimesFromSun() {
        val location = locationPref!!.location
        if (location == "not set") {
            automaticTurnOnPref!!.setToSunTime("19:30")
            automaticTurnOffPref!!.setToSunTime("06:30")
        } else {
            val androidLocation = Location(LocationManager.NETWORK_PROVIDER)
            androidLocation.latitude = java.lang.Double.parseDouble(location.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0])
            androidLocation.longitude = java.lang.Double.parseDouble(location.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])

            val sunsetTime = FilterTimePreference.getSunTimeFromLocation(androidLocation, true)
            automaticTurnOnPref!!.setToSunTime(sunsetTime)

            val sunriseTime = FilterTimePreference.getSunTimeFromLocation(androidLocation, false)
            automaticTurnOffPref!!.setToSunTime(sunriseTime)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val v = super.onCreateView(inflater, container, savedInstanceState)
        mView = v
        return v
    }

    fun registerPresenter(presenter: ShadesPresenter) {
        mPresenter = presenter

        if (DEBUG) Log.i(TAG, "Registered Presenter")
    }

    private fun setPreferencesEnabled(enabled: Boolean) {
        val root = preferenceScreen
        for (i in 0..root.preferenceCount - 1) {
            root.getPreference(i).isEnabled = enabled
        }
        otherPrefCategory!!.isEnabled = true
        automaticSuspendPref!!.isEnabled = enabled

        val auto = automaticFilterPref!!.isChecked
        locationPref!!.isEnabled = auto
        val sun = true
        automaticTurnOnPref!!.isEnabled = auto && !sun
        automaticTurnOffPref!!.isEnabled = auto && !sun

    }

    private fun setAllPreferencesEnabled(enabled: Boolean) {
        val root = preferenceScreen
        for (i in 0..root.preferenceCount - 1) {
            root.getPreference(i).isEnabled = enabled
        }
    }

    private fun showHelpSnackbar() {
        mHelpSnackbar = Snackbar.make(mView!!, activity.getString(R.string.help_snackbar_text),
                Snackbar.LENGTH_INDEFINITE)

        if ((activity as ShadesActivity).settingsModel.darkThemeFlag) {
            val group = mHelpSnackbar!!.view as ViewGroup
            group.setBackgroundColor(activity.resources.getColor(R.color.snackbar_color_dark_theme))

            val snackbarTextId = android.support.design.R.id.snackbar_text
            val textView = group.findViewById(snackbarTextId) as TextView
            textView.setTextColor(activity.resources.getColor(R.color.text_color_dark_theme))
        }

        mHelpSnackbar!!.show()
    }

    companion object {
        private val TAG = "ShadesFragment"
        private val DEBUG = true
    }
}// Android Fragments require an explicit public default constructor for re-creation
