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

import android.annotation.TargetApi
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.Preference
import android.preference.SwitchPreference
import android.provider.Settings
import android.support.design.widget.FloatingActionButton
import android.util.Log
import android.widget.Toast

import com.jmstudios.redmoon.R

import com.jmstudios.redmoon.application.RedMoonApplication
import com.jmstudios.redmoon.event.*
import com.jmstudios.redmoon.helper.Util
import com.jmstudios.redmoon.model.Config
import com.jmstudios.redmoon.preference.ColorSeekBarPreference
import com.jmstudios.redmoon.preference.DimSeekBarPreference
import com.jmstudios.redmoon.preference.IntensitySeekBarPreference
import com.jmstudios.redmoon.service.ScreenFilterService

import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class FilterFragment : EventPreferenceFragment() {
    private var hasShownWarningToast = false

    // Preferences
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
        get() = (preferenceScreen.findPreference(getString(R.string.pref_key_time_toggle)))

    private val automaticSuspendPref: Preference
        get() = preferenceScreen.findPreference(getString(R.string.pref_key_automatic_suspend))

    private val mToggleFab: FloatingActionButton
        get() = activity.findViewById(R.id.toggle_fab) as FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.filter_preferences)

        if (!Util.hasWriteSettingsPermission) lowerBrightnessPref.isChecked = false

        lowerBrightnessPref.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    val checked = newValue as Boolean
                    if (checked && getWriteSettingsPermission()) {
                        return@OnPreferenceChangeListener false
                    }
                    true
                }

        mToggleFab.setOnClickListener {
        if (DEBUG) Log.i(TAG, "FAB clicked while filter is: " + Config.filterIsOn)
            if (!hasShownWarningToast || !Config.filterIsOn) {
                val duration = Toast.LENGTH_SHORT
                val toast = Toast.makeText(RedMoonApplication.app,
                                           getString(R.string.toast_warning_install),
                                           duration)
                toast.show()
                hasShownWarningToast = true
            }

            EventBus.getDefault().postSticky(moveToState(
                    if (Config.filterIsOn) ScreenFilterService.COMMAND_OFF
                    else ScreenFilterService.COMMAND_ON))
        }

        updateAutomaticSuspendSummary()
    }

    override fun onResume() {
        super.onResume()

        mToggleFab.show()
        updateFabIcon()

        // When the fragment is not on the screen, but the user
        // updates the profile through the notification. the
        // profile spinner and the seekbars will have missed this
        // change. To update them correctly, we artificially change
        // these settings.
        /* val intensity = Config.intensityLevel */
        /* Config.intensityLevel = if (intensity == 0) 1 else 0 */
        /* Config.intensityLevel = intensity */

        /* val dim = Config.dimLevel */
        /* Config.dimLevel = if (dim == 0) 1 else 0 */
        /* Config.dimLevel = dim */

        /* val color = Config.color */
        /* Config.color = if (color == 0) 1 else 0 */
        /* Config.color = color */

        // The profile HAS to be updated last, otherwise the spinner
        // will switched to custom.
        /* val profile = Config.profile */
        /* Config.profile = if (profile == 0) 1 else 0 */
        /* Config.profile = profile */

        updateAutomaticSuspendSummary()
    }

    override fun onPause() {
        mToggleFab.hide()
        super.onPause()
    }

    private fun updateAutomaticSuspendSummary() {
        // TODO: Show the time here instead of just "on" or "off"
        automaticSuspendPref.setSummary(if (Config.automaticSuspend) R.string.text_switch_on
                                        else R.string.text_switch_off)
    }

    private fun updateFabIcon() {
        mToggleFab.setImageResource(if (Config.filterIsOn) R.drawable.fab_pause
                                    else R.drawable.fab_start)
    }

    @TargetApi(23) // Safe to call on all APIs but Android Studio doesn't know
    private fun getWriteSettingsPermission(): Boolean {
        if (!Util.hasWriteSettingsPermission) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                Uri.parse("package:" + context.packageName))
            startActivityForResult(intent, -1)
        }
        return (Util.hasWriteSettingsPermission)
        }

    //region presenter
    @Subscribe
    fun onFilterIsOnChanged(event: filterIsOnChanged) {
        updateFabIcon()
    }

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

    @Subscribe
    fun onthemeChanged(event: themeChanged) {
        activity.recreate()
    }
    //endregion

    companion object {
        private val TAG = "FilterFragment"
        private val DEBUG = true
    }
}// Android Fragments require an explicit public default constructor for re-creation


