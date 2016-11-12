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
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.preference.Preference
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView

import com.jmstudios.redmoon.R

import com.jmstudios.redmoon.activity.ShadesActivity
import com.jmstudios.redmoon.helper.FilterCommandFactory
import com.jmstudios.redmoon.helper.FilterCommandSender
import com.jmstudios.redmoon.service.ScreenFilterService
import com.jmstudios.redmoon.view.ScreenFilterView

class IntensitySeekBarPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {

    var mIntensityLevelSeekBar: SeekBar? = null
    private var mIntensityLevel: Int = 0
    private var mView: View? = null
    private var mCommandSender: FilterCommandSender? = null
    private var mCommandFactory: FilterCommandFactory? = null

    init {

        layoutResource = R.layout.preference_intensity_seekbar
    }

    fun setProgress(progress: Int) {
        if (mIntensityLevelSeekBar != null) {
            mIntensityLevelSeekBar!!.progress = progress
        } else {
            mIntensityLevel = progress
        }
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getInteger(index, DEFAULT_VALUE)
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any) {
        if (restorePersistedValue) {
            mIntensityLevel = getPersistedInt(DEFAULT_VALUE)
        } else {
            mIntensityLevel = defaultValue as Int
            persistInt(mIntensityLevel)
        }
    }

    override fun onBindView(view: View) {
        super.onBindView(view)

        mView = view

        mIntensityLevelSeekBar = view.findViewById(R.id.intensity_level_seekbar) as SeekBar
        initLayout()
    }

    private fun initLayout() {
        mCommandSender = FilterCommandSender(mView!!.context)
        mCommandFactory = FilterCommandFactory(mView!!.context)
        mIntensityLevelSeekBar!!.progress = mIntensityLevel

        mIntensityLevelSeekBar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mIntensityLevel = progress
                persistInt(mIntensityLevel)

                updateMoonIconColor()
                updateProgressText()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                Log.i(TAG, "Touch down on a seek bar")

                val showPreviewCommand = mCommandFactory!!.createCommand(ScreenFilterService.COMMAND_SHOW_PREVIEW)
                mCommandSender!!.send(showPreviewCommand)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                Log.d(TAG, "Released a seek bar")

                val hidePreviewCommand = mCommandFactory!!.createCommand(ScreenFilterService.COMMAND_HIDE_PREVIEW)
                mCommandSender!!.send(hidePreviewCommand)
            }
        })

        updateMoonIconColor()
        updateProgressText()
    }

    fun updateMoonIconColor() {
        if (!isEnabled) return

        val colorTempProgress = (context as ShadesActivity).colorTempProgress

        val color = ScreenFilterView.getIntensityColor(mIntensityLevel, colorTempProgress)

        val moonIcon = mView!!.findViewById(R.id.moon_icon_intensity) as ImageView

        val colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY)

        moonIcon.colorFilter = colorFilter
    }

    private fun updateProgressText() {
        val progress = Integer.toString(mIntensityLevel)
        val suffix = "%"

        val progressText = mView!!.findViewById(R.id.current_intensity_level) as TextView
        progressText.text = progress + suffix
    }

    companion object {
        val DEFAULT_VALUE = 50
        private val TAG = "IntensitySeekBarPref"
    }
}
