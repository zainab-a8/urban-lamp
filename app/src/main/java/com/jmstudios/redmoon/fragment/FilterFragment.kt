/*
 * Copyright (c) 2016  Marien Raat <marienraat@riseup.net>
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

import android.os.Bundle
import android.preference.Preference
import android.preference.SwitchPreference
import android.util.Log

import com.jmstudios.redmoon.R

import com.jmstudios.redmoon.event.*
import com.jmstudios.redmoon.model.Config
import com.jmstudios.redmoon.preference.ColorSeekBarPreference
import com.jmstudios.redmoon.preference.DimSeekBarPreference
import com.jmstudios.redmoon.preference.IntensitySeekBarPreference
import com.jmstudios.redmoon.preference.ProfileSelectorPreference
import com.jmstudios.redmoon.util.hasWriteSettingsPermission
import com.jmstudios.redmoon.util.requestWriteSettingsPermission

import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class FilterFragment : EventPreferenceFragment() {
    //private var hasShownWarningToast = false

    // Preferences
    private val profileSelectorPref: ProfileSelectorPreference
             get() = (preferenceScreen.findPreference
                     (getString(R.string.pref_key_profile_spinner)) as ProfileSelectorPreference)

    private val colorPref: ColorSeekBarPreference
        get() = (preferenceScreen.findPreference
                (getString(R.string.pref_key_color)) as ColorSeekBarPreference)

    private val intensityPref: IntensitySeekBarPreference
        get() = (preferenceScreen.findPreference
                (getString(R.string.pref_key_intensity)) as IntensitySeekBarPreference)

    private val dimPref: DimSeekBarPreference
        get()= (preferenceScreen.findPreference
               (getString(R.string.pref_key_dim)) as DimSeekBarPreference)

    private val lowerBrightnessPref: SwitchPreference
        get() = (preferenceScreen.findPreference
                (getString(R.string.pref_key_lower_brightness)) as SwitchPreference)

    private val timeTogglePref: Preference
        get() = (preferenceScreen.findPreference(getString(R.string.pref_key_time_toggle_header)))

    private val secureSuspendPref: Preference
        get() = preferenceScreen.findPreference(getString(R.string.pref_key_secure_suspend_header))

    private val darkThemePref: SwitchPreference
        get() = (preferenceScreen.findPreference
                (getString(R.string.pref_key_dark_theme)) as SwitchPreference)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.filter_preferences)

        if (!hasWriteSettingsPermission) { lowerBrightnessPref.isChecked = false }
        updateSecureSuspendSummary()
        updateTimeToggleSummary()

        lowerBrightnessPref.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    val checked = newValue as Boolean
                    if (checked) { requestWriteSettingsPermission(activity) } else { true }
                }

        /* Normally we'd change theme via an event after the setting gets
         * changed, but for some reason doing it that way makes the activity
         * scroll to the top when it gets recreated, which is rather jarring.
         * Doing it this way keeps the same scroll position, which is nice. */
        darkThemePref.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, _ ->
                    activity.recreate()
                    true
                }
    }

    override fun onResume() {
        if (DEBUG) Log.i(TAG, "onResume")
        super.onResume()
        EventBus.getDefault().register(profileSelectorPref)
        updateSecureSuspendSummary()
        updateTimeToggleSummary()
    }

    override fun onPause() {
        EventBus.getDefault().unregister(profileSelectorPref)
        super.onPause()
    }

    private fun updateTimeToggleSummary() {
        timeTogglePref.setSummary(if (Config.timeToggle) R.string.text_switch_on
                                  else R.string.text_switch_off)
    }

    private fun updateSecureSuspendSummary() {
        secureSuspendPref.setSummary(if (Config.secureSuspend) R.string.text_switch_on
                                     else R.string.text_switch_off)
    }

    //region presenter
    @Subscribe
    fun onColorChanged(event: colorChanged) {
        colorPref.setProgress(Config.color)
    }

    @Subscribe
    fun onIntensityLevelChanged(event: intensityChanged) {
        intensityPref.setProgress(Config.intensity)
    }

    @Subscribe
    fun onDimLevelChanged(event: dimChanged) {
        dimPref.setProgress(Config.dim)
    }
    //endregion

    companion object {
        private val TAG = "FilterFragment"
        private val DEBUG = true
    }
}// Android Fragments require an explicit public default constructor for re-creation
