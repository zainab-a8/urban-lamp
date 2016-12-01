
/*
 * Copyright (c) 2016 Marien Raat <marienraat@riseup.net>
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

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.preference.Preference
import android.preference.SwitchPreference
import android.provider.Settings

import com.jmstudios.redmoon.R

import com.jmstudios.redmoon.thread.CurrentAppMonitoringThread

class SecureSuspendFragment : EventPreferenceFragment() {

    private val appMonitoringIsWorking: Boolean
        get() = CurrentAppMonitoringThread.isAppMonitoringWorking(context)

    private val mSwitchBarPreference: SwitchPreference
        get() = (preferenceScreen.findPreference
                (getString(R.string.pref_key_secure_suspend)) as SwitchPreference)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.secure_suspend_preferences)

        setSwitchBarTitle(mSwitchBarPreference.isChecked)

        mSwitchBarPreference.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->
                val on = newValue as Boolean
                if (!on) {
                    setSwitchBarTitle(on)
                    true
                } else {
                    if (!appMonitoringIsWorking) createEnableUsageStatsDialog()
                    val b = appMonitoringIsWorking
                    setSwitchBarTitle(b && on)
                    b
                }
            }
    }

    private fun setSwitchBarTitle(on: Boolean) {
        mSwitchBarPreference.setTitle(
                if (on) R.string.text_switch_on
                else R.string.text_switch_off
        )
    }

    // TODO: Fix on API < 21
    private fun createEnableUsageStatsDialog() {
        val builder = AlertDialog.Builder(activity)

        builder.setMessage(R.string.usage_stats_dialog_message)
               .setTitle(R.string.usage_stats_dialog_title)
               .setPositiveButton(R.string.ok_dialog) { dialog, id ->
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    startActivityForResult(intent, RESULT_USAGE_ACCESS)
        }

        builder.show()
    }

    companion object {
        val RESULT_USAGE_ACCESS = 1
        private val DEBUG = false
    }
}
