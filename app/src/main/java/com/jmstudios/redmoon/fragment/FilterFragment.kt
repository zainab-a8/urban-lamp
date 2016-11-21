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
import android.preference.PreferenceFragment
import android.preference.SwitchPreference
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.jmstudios.redmoon.R

import com.jmstudios.redmoon.activity.ShadesActivity
import com.jmstudios.redmoon.model.SettingsModel
import com.jmstudios.redmoon.preference.ProfileSelectorPreference
import org.greenrobot.eventbus.EventBus

class FilterFragment : PreferenceFragment() {

    private lateinit var mView: View
    private lateinit var mHelpSnackbar: Snackbar
    private val mSettingsModel: SettingsModel
        get() = (activity as ShadesActivity).mSettingsModel

    // Preferences
    private val profileSelectorPref: ProfileSelectorPreference
        get() = (preferenceScreen.findPreference
                (getString(R.string.pref_key_profile_spinner)) as ProfileSelectorPreference)

    private val darkThemePref: SwitchPreference
        get() = (preferenceScreen.findPreference
                (getString(R.string.pref_key_dark_theme)) as SwitchPreference)

    private val lowerBrightnessPref: SwitchPreference
        get() = (preferenceScreen.findPreference
                (getString(R.string.pref_key_control_brightness)) as SwitchPreference)
    
    private val timeTogglePref: Preference
        get() = (preferenceScreen.findPreference (getString(R.string.pref_key_time_toggle)))

    private val otherPrefCategory: Preference
        get() = preferenceScreen.findPreference(getString(R.string.pref_key_other))

    private val automaticSuspendPref: Preference
        get() = preferenceScreen.findPreference(getString(R.string.pref_key_automatic_suspend))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.filter_preferences)

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

        // TODO: Add time toggle pref here
        timeTogglePref.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    (activity as ShadesActivity).launchTimeToggleFragment()
                    true
                }

        // Automatic suspend
        automaticSuspendPref.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    (activity as ShadesActivity).launchSecureSuspendFragment()
                    true
                }
        val automaticSuspendOn = (activity as ShadesActivity).mSettingsModel.automaticSuspend
        automaticSuspendPref.setSummary(if (automaticSuspendOn)
            R.string.text_switch_on
        else
            R.string.text_switch_off)
    }

    override fun onStart() {
        EventBus.getDefault().register(profileSelectorPref)
        super.onStart()
    }

    override fun onStop(){
        EventBus.getDefault().unregister(profileSelectorPref)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()

        // When the fragment is not on the screen, but the user
        // updates the profile through the notification. the
        // profile spinner and the seekbars will have missed this
        // change. To update them correctly, we artificially change
        // these settings.
        /* val intensity = mSettingsModel.intensityLevel */
        /* mSettingsModel.intensityLevel = if (intensity == 0) 1 else 0 */
        /* mSettingsModel.intensityLevel = intensity */

        /* val dim = mSettingsModel.dimLevel */
        /* mSettingsModel.dimLevel = if (dim == 0) 1 else 0 */
        /* mSettingsModel.dimLevel = dim */

        /* val color = mSettingsModel.color */
        /* mSettingsModel.color = if (color == 0) 1 else 0 */
        /* mSettingsModel.color = color */

        // The profile HAS to be updated last, otherwise the spinner
        // will switched to custom.
        /* val profile = mSettingsModel.profile */
        /* mSettingsModel.profile = if (profile == 0) 1 else 0 */
        /* mSettingsModel.profile = profile */

        // TODO: Add time toggle pref here
        
        val automaticSuspendOn = mSettingsModel.automaticSuspend
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val v = super.onCreateView(inflater, container, savedInstanceState)
        mView = v
        return v
    }

    private fun setPreferencesEnabled(enabled: Boolean) {
        val root = preferenceScreen
        for (i in 0..root.preferenceCount - 1) {
            root.getPreference(i).isEnabled = enabled
        }
        otherPrefCategory.isEnabled = true
        automaticSuspendPref.isEnabled = enabled
    }

    /* private fun showHelpSnackbar() { */
    /*     mHelpSnackbar = Snackbar.make(mView, activity.getString(R.string.help_snackbar_text), */
    /*             Snackbar.LENGTH_INDEFINITE) */

    /*     if (mSettingsModel.darkThemeFlag) { */
    /*         val group = mHelpSnackbar.view as ViewGroup */
    /*         group.setBackgroundColor(ContextCompat.getColor(activity, R.color.snackbar_color_dark_theme)) */

    /*         val snackbarTextId = android.support.design.R.id.snackbar_text */
    /*         val textView = group.findViewById(snackbarTextId) as TextView */
    /*         textView.setTextColor(ContextCompat.getColor(activity, R.color.text_color_dark_theme)) */
    /*     } */

    /*     mHelpSnackbar.show() */
    /* } */

    companion object {
        private val TAG = "FilterFragment"
        private val DEBUG = true
    }
}// Android Fragments require an explicit public default constructor for re-creation


