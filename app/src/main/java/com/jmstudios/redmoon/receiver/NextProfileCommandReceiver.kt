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
import android.util.Log

import com.jmstudios.redmoon.helper.ProfilesHelper
import com.jmstudios.redmoon.model.Config

class NextProfileCommandReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (DEBUG) Log.i(TAG, "Next profile requested")

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
            val profileObject = ProfilesHelper.getProfile(newProfile, context)

            Config.dim = profileObject.mDim
            Config.intensity = profileObject.mIntensity
            Config.color = profileObject.mColor
        }
    }

    companion object {
        val DEBUG = false
        val TAG = "NextProfileCommandRcv"
    }
}
