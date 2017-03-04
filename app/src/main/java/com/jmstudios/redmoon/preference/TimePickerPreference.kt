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
package com.jmstudios.redmoon.preference

import android.content.Context
import android.content.res.TypedArray
import android.preference.DialogPreference
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.View
import android.widget.TimePicker

import com.jmstudios.redmoon.R
import com.jmstudios.redmoon.util.atLeastAPI

open class TimePickerPreference(context: Context, attrs: AttributeSet) : DialogPreference(context, attrs) {

    lateinit private var mTimePicker: TimePicker
    protected var mTime: String = DEFAULT_VALUE

    @Suppress("DEPRECATION") // Need deprecated 'currentMinute' for API<23
    private var currentMinute: Int
        get() = if (atLeastAPI(23)) mTimePicker.minute
                else mTimePicker.currentMinute
        set(m) = if (atLeastAPI(23)) mTimePicker.minute = m
                 else mTimePicker.currentMinute = m

    @Suppress("DEPRECATION") // Need deprecated 'currentHour' for API<23
    private var currentHour: Int
        get() = if (atLeastAPI(23)) mTimePicker.hour
                else mTimePicker.currentHour
        set(h) = if (atLeastAPI(23)) mTimePicker.hour = h
                 else mTimePicker.currentHour = h

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getString(index)
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        if (restorePersistedValue) {
            mTime = getPersistedString(DEFAULT_VALUE)
        } else {
            mTime = (defaultValue as String?)?: DEFAULT_VALUE
            persistString(mTime)
        }
        summary = mTime
    }

    override fun onBindView(view: View) {
        super.onBindView(view)
    }

    override fun onCreateDialogView(): View {
        positiveButtonText = context.resources.getString(R.string.set_dialog)
        negativeButtonText = context.resources.getString(R.string.cancel_dialog)
        mTimePicker = TimePicker(context)
        mTimePicker.setIs24HourView(DateFormat.is24HourFormat(context))
        return mTimePicker
    }

    override fun onBindDialogView(v: View) {
        super.onBindDialogView(v)
        currentHour = Integer.parseInt(mTime.split(":".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()[0])
        currentMinute = Integer.parseInt(mTime.split(":".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()[1])
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)

        val hour = currentHour
        val minute = currentMinute
        mTime = (if (hour < 10) "0" else "") + Integer.toString(hour) + ":" +
                (if (minute < 10) "0" else "") + Integer.toString(minute)

        persistString(mTime)
        summary = mTime
    }

    companion object {
        const val DEFAULT_VALUE = "00:00"
    }
}
