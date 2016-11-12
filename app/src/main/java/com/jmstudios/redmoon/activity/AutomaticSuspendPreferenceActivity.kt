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
package com.jmstudios.redmoon.activity

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceManager
import android.view.MenuItem

import com.jmstudios.redmoon.R

import com.jmstudios.redmoon.model.SettingsModel
import com.jmstudios.redmoon.preference.SwitchBarPreference

class AutomaticSuspendPreferenceActivity : AppCompatPreferenceActivity() {

    private var mSummaryPreference: Preference? = null
    private var mSwitchBarPreference: SwitchBarPreference? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val preferenceScreen = preferenceManager.createPreferenceScreen(this)
        setPreferenceScreen(preferenceScreen)

        mSwitchBarPreference = SwitchBarPreference(this, this)
        mSwitchBarPreference!!.key = resources.getString(R.string.pref_key_automatic_suspend)
        preferenceScreen.addPreference(mSwitchBarPreference)

        mSummaryPreference = Preference(this)
        mSummaryPreference!!.layoutResource = R.layout.automatic_suspend_summary
        mSummaryPreference!!.isSelectable = false
        preferenceScreen.addPreference(mSummaryPreference)
    }

    override fun onApplyThemeResource(theme: Resources.Theme, resid: Int, first: Boolean) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val settingsModel = SettingsModel(resources, sharedPreferences)

        if (settingsModel.darkThemeFlag)
            super.onApplyThemeResource(theme, R.style.AppThemeDark, first)
        else
            super.onApplyThemeResource(theme, resid, first)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            RESULT_USAGE_ACCESS -> mSwitchBarPreference!!.usageStatsPermissionAttempted()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // The only item is the home button
        finish()
        return true
    }

    companion object {
        val RESULT_USAGE_ACCESS = 1
    }
}
