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

import android.Manifest
import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceCategory
import android.preference.PreferenceFragment
import android.preference.SwitchPreference
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.jmstudios.redmoon.R

import com.jmstudios.redmoon.activity.ShadesActivity
import com.jmstudios.redmoon.preference.FilterTimePreference
import com.jmstudios.redmoon.preference.UseLocationPreference
import com.jmstudios.redmoon.presenter.ShadesPresenter

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator

import java.util.*

class ShadesFragment : PreferenceFragment() {

    private val DEBUG = true
    private lateinit var mPresenter: ShadesPresenter
    private lateinit var mView: View
    private lateinit var mHelpSnackbar: Snackbar

    // Preferences
    private val automaticFilterPrefCategory: PreferenceCategory
        get() = (preferenceScreen.findPreference
                (getString(R.string.pref_key_automatic_filter_category)) as PreferenceCategory)

    private val darkThemePref: SwitchPreference
        get() = (preferenceScreen.findPreference
                (getString(R.string.pref_key_dark_theme)) as SwitchPreference)

    private val lowerBrightnessPref: SwitchPreference
        get() = (preferenceScreen.findPreference
                (getString(R.string.pref_key_control_brightness)) as SwitchPreference)

    private val automaticFilterPref: SwitchPreference
        get() = (preferenceScreen.findPreference
                (getString(R.string.pref_key_automatic_filter)) as SwitchPreference)

    private val useLocationPref: UseLocationPreference
        get() = (preferenceScreen.findPreference
                (getString(R.string.pref_key_use_location)) as UseLocationPreference)

    private val automaticTurnOnPref: FilterTimePreference
        get() = (preferenceScreen.findPreference
                (getString(R.string.pref_key_custom_start_time)) as FilterTimePreference)

    private val automaticTurnOffPref: FilterTimePreference
        get() = (preferenceScreen.findPreference
                (getString(R.string.pref_key_custom_end_time)) as FilterTimePreference)

    private val otherPrefCategory: Preference
        get() = preferenceScreen.findPreference(getString(R.string.pref_key_other))

    private val automaticSuspendPref: Preference
        get() = preferenceScreen.findPreference(getString(R.string.pref_key_automatic_suspend))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.preferences)

        darkThemePref.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    activity.recreate()
                    true
                }

        if (!hasWriteSettingsPermission) lowerBrightnessPref.isChecked = false

        lowerBrightnessPref.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    val checked = newValue as Boolean
                    if (checked && getWriteSettingsPermission()) {
                        return@OnPreferenceChangeListener false
                    }
                    true
                }

        automaticFilterPref.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    onAutomaticFilterPreferenceChange(newValue as Boolean)
                    true
                }

            /* requestLocationPermission() */
            /* updateFilterTimesFromSun() */
            /* locationPref.searchLocation(true); */
            /* locationPref.searchLocation(false) */
            /* int duration = Toast.LENGTH_SHORT; */
            /* Toast toast = Toast.makeText */
            /*     (mContext, mContext.getString */
            /*      (R.string.toast_warning_no_location), duration); */
            /* toast.show(); */

        useLocationPref.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    //TODO: Get location permission 
                    updateLocationPrefs(newValue as Boolean)
                    true
                }


        // Automatic suspend
        val automaticSuspendOn = (activity as ShadesActivity).settingsModel.automaticSuspend
        automaticSuspendPref.setSummary(if (automaticSuspendOn)
            R.string.text_switch_on
        else
            R.string.text_switch_off)
    }

    override fun onResume() {
        super.onResume()

        val automaticSuspendOn = (activity as ShadesActivity).settingsModel.automaticSuspend
        automaticSuspendPref.setSummary(if (automaticSuspendOn)
            R.string.text_switch_on
        else
            R.string.text_switch_off)
    }

    private val hasWriteSettingsPermission: Boolean
        get() = if (android.os.Build.VERSION.SDK_INT < 23) true
                else Settings.System.canWrite(context)

    @TargetApi(23) // Safe to call on all APIs but Android Studio doesn't know
    private fun getWriteSettingsPermission(): Boolean {
        if (!hasWriteSettingsPermission) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                Uri.parse("package:" + context.packageName))
            startActivityForResult(intent, -1)
        }
        return (hasWriteSettingsPermission)
        }

    private fun onAutomaticFilterPreferenceChange(auto: Boolean) {
        useLocationPref.setEnabled(auto)
        updateLocationPrefs(useLocationPref.isChecked)
        if (!auto) {
            automaticTurnOnPref.setEnabled(false)
            automaticTurnOffPref.setEnabled(false)
        }
    }

    private fun removePref(category: PreferenceCategory, pref: Preference?) {
        if (pref != null) category.removePreference(pref)
    }

    private fun addPref(category: PreferenceCategory, pref: Preference?) {
        if (pref == null) category.addPreference(pref)
    }

    private fun updateLocationPrefs(sun: Boolean) {
        useLocationPref.updateSummary()
        val location = useLocationPref.location
        if (!sun) {
            if (DEBUG) Log.i(TAG, "Location Disabled")
            automaticTurnOnPref.setToCustomTime()
            automaticTurnOffPref.setToCustomTime()
        } else if (location == "not set") {
            if (DEBUG) Log.i(TAG, "Location Not Set")
            automaticTurnOnPref.setToSunTime("19:30")
            automaticTurnOffPref.setToSunTime("06:30")
        } else {
            if (DEBUG) Log.i(TAG, "Location Set")
            val latitude = java.lang.Double.parseDouble(location.split(",")[0])
            val longitude = java.lang.Double.parseDouble(location.split(",")[1])

            val sunriseSunsetLocation = com.luckycatlabs.sunrisesunset.dto.Location(latitude, longitude)
            val calculator = SunriseSunsetCalculator(sunriseSunsetLocation, TimeZone.getDefault())

            val sunsetTime = calculator.getOfficialSunsetForDate(Calendar.getInstance())
            automaticTurnOnPref.setToSunTime(sunsetTime)

            val sunriseTime = calculator.getOfficialSunriseForDate(Calendar.getInstance())
            automaticTurnOffPref.setToSunTime(sunriseTime)
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission
                (activity, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 0)
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
        otherPrefCategory.isEnabled = true
        automaticSuspendPref.isEnabled = enabled

        val auto = automaticFilterPref.isChecked
        useLocationPref.isEnabled = auto
        val sun = true
        automaticTurnOnPref.isEnabled = auto && !sun
        automaticTurnOffPref.isEnabled = auto && !sun

    }

    private fun setAllPreferencesEnabled(enabled: Boolean) {
        val root = preferenceScreen
        for (i in 0..root.preferenceCount - 1) {
            root.getPreference(i).isEnabled = enabled
        }
    }

    private fun showHelpSnackbar() {
        mHelpSnackbar = Snackbar.make(mView, activity.getString(R.string.help_snackbar_text),
                Snackbar.LENGTH_INDEFINITE)

        if ((activity as ShadesActivity).settingsModel.darkThemeFlag) {
            val group = mHelpSnackbar.view as ViewGroup
            group.setBackgroundColor(ContextCompat.getColor(activity, R.color.snackbar_color_dark_theme))

            val snackbarTextId = android.support.design.R.id.snackbar_text
            val textView = group.findViewById(snackbarTextId) as TextView
            textView.setTextColor(ContextCompat.getColor(activity, R.color.text_color_dark_theme))
        }

        mHelpSnackbar.show()
    }

    companion object {
        private val TAG = "ShadesFragment"
        private val DEBUG = true
    }
}// Android Fragments require an explicit public default constructor for re-creation


