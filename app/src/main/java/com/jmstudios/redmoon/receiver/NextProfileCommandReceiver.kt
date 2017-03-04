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
package com.jmstudios.redmoon.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import com.jmstudios.redmoon.helper.ProfilesHelper
import com.jmstudios.redmoon.model.ProfilesModel
import com.jmstudios.redmoon.model.Config
import com.jmstudios.redmoon.util.Log

class NextProfileCommandReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i("Next profile requested")

        // Here we just change the profile (cycles back to default
        // when it reaches the max).
        val profile = Config.profile
        val amProfiles = Config.amountProfiles
        val newProfile = if (profile + 1 >= amProfiles) 1
                         else profile + 1
        Config.profile = newProfile

        // Next update the other settings that are based on the
        // profile
        if (newProfile != 0) {
            // We need a ProfilesModel to get the properties of the
            // profile from the index
            val profilesModel = ProfilesModel(context)
            val profileObject = ProfilesHelper.getProfile(profilesModel, newProfile, context)

            Config.dim = profileObject.mDimProgress
            Config.intensity = profileObject.mIntensityProgress
            Config.color = profileObject.mColorProgress
        }
    }
}
