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
import android.content.res.TypedArray
import android.preference.DialogPreference
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.View
import android.widget.TimePicker

import com.jmstudios.redmoon.R

open class TimePickerPreference(context: Context, attrs: AttributeSet) : DialogPreference(context, attrs) {

    private var mTimePicker: TimePicker? = null
    protected var mTime: String = DEFAULT_VALUE

    init {
        positiveButtonText = getContext().resources.getString(R.string.set_dialog)
        negativeButtonText = getContext().resources.getString(R.string.cancel_dialog)
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getString(index)
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any) {
        if (restorePersistedValue) {
            mTime = getPersistedString(DEFAULT_VALUE)
        } else {
            mTime = defaultValue as String
            persistString(mTime)
        }
        summary = mTime
    }

    override fun onBindView(view: View) {
        super.onBindView(view)
    }

    override fun onCreateDialogView(): View {
        mTimePicker = TimePicker(context)
        mTimePicker!!.setIs24HourView(DateFormat.is24HourFormat(context))
        return mTimePicker!!
    }

    override fun onBindDialogView(v: View) {
        super.onBindDialogView(v)

        val hour = Integer.parseInt(mTime.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0])
        val minute = Integer.parseInt(mTime.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            mTimePicker!!.hour = hour
            mTimePicker!!.minute = minute
        } else {
            mTimePicker!!.currentHour = hour
            mTimePicker!!.currentMinute = minute
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)

        if (positiveResult) {
            var hour = 0
            var minute = 0
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                hour = mTimePicker!!.hour
                minute = mTimePicker!!.minute
            } else {
                hour = mTimePicker!!.currentHour
                minute = mTimePicker!!.currentMinute
            }

            mTime = (if (hour < 10) "0" else "") + Integer.toString(hour) + ":" +
                    (if (minute < 10) "0" else "") + Integer.toString(minute)
            persistString(mTime)
        }
        summary = mTime
    }

    companion object {
        val DEFAULT_VALUE = "00:00"

        private val TAG = "TimePickerPref"
        private val DEBUG = false
    }
}
