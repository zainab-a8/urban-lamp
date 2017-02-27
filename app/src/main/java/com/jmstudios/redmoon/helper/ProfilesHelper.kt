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
package com.jmstudios.redmoon.helper

import android.content.Context

import com.jmstudios.redmoon.R

import com.jmstudios.redmoon.model.ProfilesModel

object ProfilesHelper {
    const val DEFAULT_OPERATIONS_AM = 3

    private val model: ProfilesModel
        get() = ProfilesModel()

    fun getProfileName(profile: Int, context: Context): String {
        if (profile < DEFAULT_OPERATIONS_AM) {
            return context.resources.getStringArray(R.array.standard_profiles_array)[profile]
        } else {
            return model.getProfile(profile - DEFAULT_OPERATIONS_AM).mName
        }
    }

    fun getProfile(profile: Int, context: Context): ProfilesModel.Profile {
        val name = getProfileName(profile, context)
        return when (profile) {
              // ProfilesModel.Profile(name, color, intensity, dim)
            0 -> ProfilesModel.Profile(name, 0, 0, 0)
            1 -> ProfilesModel.Profile(name, 10, 30, 40)
            2 -> ProfilesModel.Profile(name, 20, 60, 78)
            else -> model.getProfile(profile - DEFAULT_OPERATIONS_AM)
        }
    }
}
