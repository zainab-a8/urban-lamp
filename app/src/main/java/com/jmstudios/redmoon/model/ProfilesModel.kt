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
package com.jmstudios.redmoon.model

import android.content.Context
import android.content.SharedPreferences

import java.util.ArrayList

import com.jmstudios.redmoon.util.Log

/**
 * This class manages the SharedPreference that store all custom
 * filter profiles added by the user.

 * The profiles are stored in a separate SharedPreference, with per
 * profile a key given by "$PROFILE_NAME_$ID", where $PROFILE_NAME is
 * the name given to the profile by the user and $ID is the position
 * of the list of profiles, starting with 0 for the first custom
 * profile created by the user. A string is associated with every key
 * with the format "$PROGRESS_COLOR,$PROGRESS_INTENSITY,$PROGRESS_DIM"
 */
class ProfilesModel(context: Context) {

    private val mSharedPrefs: SharedPreferences
    private val mPrefsContentsMap: Map<String, *>
    var profiles: ArrayList<Profile>
        private set

    init {
        Log.i("Creating ProfilesModel")

        mSharedPrefs = context.getSharedPreferences(preferenceName, mode)
        mPrefsContentsMap = mSharedPrefs.all as Map<String, String>
        profiles = ArrayList<Profile>()

        parsePrefsContents()
    }

    fun addProfile(profile: Profile) {
        Log.i("Adding new profile")
        profiles.add(profile)

        updateSharedPreferences()
    }

    fun getProfile(index: Int): Profile {
        return profiles[index]
    }

    fun removeProfile(index: Int) {
        profiles.removeAt(index)

        updateSharedPreferences()
    }

    private fun parsePrefsContents() {
        Log.i("Parsing preference contents")

        profiles = ArrayList<Profile>()

        val amProfiles = mPrefsContentsMap.entries.size
        Log.d("Allocating " + amProfiles)
        profiles.ensureCapacity(amProfiles)

        Log.d("Allocated " + amProfiles)

        for (i in 0..amProfiles - 1) {
            Log.d("Parsing " + i)
            val profileEntry = findProfileEntry(i)
            profiles.add(parseProfile(profileEntry))
        }

        Log.d("Done parsing preference contents. Parsed $amProfiles profiles.")
    }

    private fun findProfileEntry(index: Int): String {
        Log.i("Finding entry at " + index)
        for ((key, value) in mPrefsContentsMap) {
            if (getIndexFromString(key) == index)
                return key + "@" + value as String
        }
        return "Profile not found_0,0,0"
    }

    private fun getIndexFromString(keyString: String): Int {
        Log.i("Parsing index from string: " + keyString)
        val length = keyString.length
        val idIndex = keyString.lastIndexOf('_') + 1
        val idString = keyString.substring(idIndex, length)

        Log.i("Found idString: " + idString)

        return Integer.parseInt(idString)
    }

    private fun parseProfile(entry: String): Profile {
        Log.i("Parsing entry: " + entry)
        val key = entry.substring(0, entry.lastIndexOf("@"))
        val values = entry.substring(entry.lastIndexOf("@") + 1, entry.length)

        val profileName = getProfileNameFromString(key)

        val progressValues = values
        val firstComma = progressValues.indexOf(',')
        val colorProgress = Integer.parseInt(progressValues.substring(0, firstComma))

        val secondComma = progressValues.indexOf(',', firstComma + 1)
        val intensityProgress = Integer.parseInt(progressValues.substring(firstComma + 1, secondComma))

        val dimProgress = Integer.parseInt(progressValues.substring(secondComma + 1, progressValues.length))

        val profile = Profile(profileName, colorProgress, intensityProgress, dimProgress)
        return profile
    }

    private fun getProfileNameFromString(keyString: String): String {
        val nameEndIndex = keyString.lastIndexOf('_')
        val profileNameString = keyString.substring(0, nameEndIndex)

        return profileNameString
    }

    private fun updateSharedPreferences() {
        Log.i("Updating SharedPreferences")
        val editor = mSharedPrefs.edit()
        editor.clear()

        for ((i, profile) in profiles.withIndex()) {
            editor.putString(profile.getKey(i), profile.values)
        }

        editor.apply()
        Log.d("Done updating SharedPreferences")
    }

    class Profile(var mProfileName: String, var mColorProgress: Int,
                  var mIntensityProgress: Int, var mDimProgress: Int) {

        fun getKey(index: Int): String {
            val id = Integer.toString(index)
            return mProfileName + "_" + id
        }

        val values: String
            get() = Integer.toString(mColorProgress) + "," +
                    Integer.toString(mIntensityProgress) + "," +
                    Integer.toString(mDimProgress)
    }

    companion object {
        private const val preferenceName = "com.jmstudios.redmoon.PROFILES_PREFERENCE"
        private const val mode = Context.MODE_PRIVATE
    }
}
