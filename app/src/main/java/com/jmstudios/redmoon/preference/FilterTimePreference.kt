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
package com.jmstudios.redmoon.preference

import android.content.Context
import android.util.AttributeSet

class FilterTimePreference(context: Context, attrs: AttributeSet) : TimePickerPreference(context, attrs) {

    private var mIsCustom = true

    fun setToSunTime(time: String) {
        if (mIsCustom) {
            // Backup custom times
            val editor = sharedPreferences.edit()
            editor.putString(key + "_custom", mTime)
            editor.apply()
        }
        isEnabled = false
        mTime = time
        persistString(mTime)
        summary = mTime

        mIsCustom = false
    }

    fun setToCustomTime() {
        mIsCustom = true
        isEnabled = true

        mTime = sharedPreferences.getString(key + "_custom", TimePickerPreference.DEFAULT_VALUE)
        persistString(mTime)
        summary = mTime
    }

    companion object {
        val TAG = "FilterTimePreference"
        val DEBUG = false
    }
}
