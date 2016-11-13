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
package com.jmstudios.redmoon.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.util.Log

import com.jmstudios.redmoon.helper.ProfilesHelper
import com.jmstudios.redmoon.model.ProfilesModel
import com.jmstudios.redmoon.model.SettingsModel

class NextProfileCommandReceiver : BroadcastReceiver() {

    lateinit private var mSettingsModel: SettingsModel

    override fun onReceive(context: Context, intent: Intent) {
        if (DEBUG) Log.i(TAG, "Next profile requested")

        val standardSp = PreferenceManager.getDefaultSharedPreferences(context)
        mSettingsModel = SettingsModel(context.resources, standardSp)

        // Here we just change the profile (cycles back to default
        // when it reaches the max).
        val profile = mSettingsModel.profile
        val amProfiles = mSettingsModel.ammountProfiles
        val newProfile = if (profile + 1 >= amProfiles)
            1
        else
            profile + 1
        mSettingsModel.profile = newProfile

        // Next update the other settings that are based on the
        // profile
        if (newProfile != 0) {
            // We need a ProfilesModel to get the properties of the
            // profile from the index
            val profilesModel = ProfilesModel(context)
            val profileObject = ProfilesHelper.getProfile(profilesModel, newProfile, context)

            mSettingsModel.dimLevel = profileObject.mDimProgress
            mSettingsModel.intensityLevel = profileObject.mIntensityProgress
            mSettingsModel.color = profileObject.mColorProgress
        }
    }

    companion object {
        val DEBUG = false
        val TAG = "NextProfileCommandRcv"
    }
}
