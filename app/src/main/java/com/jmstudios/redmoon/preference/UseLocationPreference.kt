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
 *
 */
package com.jmstudios.redmoon.preference

import android.content.Context
import android.preference.PreferenceManager
import android.preference.SwitchPreference
import android.util.AttributeSet

import com.jmstudios.redmoon.R

import java.util.Locale

class UseLocationPreference(private val mContext: Context, attrs: AttributeSet)
                                            : SwitchPreference(mContext, attrs) {
    private var mIsSearchingLocation = false

    /**
     * A convenience for the fragment that hosts this pref
     */
    val location: String
        get() {
            val prefKey = mContext.getString(R.string.pref_key_location)
            val sp = PreferenceManager.getDefaultSharedPreferences(mContext)
            return sp.getString(prefKey, DEFAULT_VALUE)
        }

    fun updateSummary() {
        val location = location
        if (mIsSearchingLocation) {
            summary = mContext.getString(R.string.searching_location)
        } else if (location === DEFAULT_VALUE) {
            summary = mContext.getString(R.string.location_not_set)
        } else {
            val shortLatitude = mContext.getString(R.string.latitude_short)
            val shortLongitude = mContext.getString(R.string.longitude_short)

            val x = location.indexOf(",")
            val latitude = java.lang.Double.parseDouble(location.substring(0, x))
            val longitude = java.lang.Double.parseDouble(location.substring(x+1, location.length))

            val summary = String.format(Locale.getDefault(), "%s: %.2f %s: %.2f",
                                        shortLatitude, latitude, shortLongitude, longitude)
            setSummary(summary)
        }
    }

    fun setIsSearchingLocation(searching: Boolean) {
        mIsSearchingLocation = searching
        updateSummary()
    }

    companion object {
        private val TAG = "LocationPreference"
        private val DEBUG = false

        val DEFAULT_VALUE = "not set"
    }
}
