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

import com.jmstudios.redmoon.helper.FilterCommandFactory
import com.jmstudios.redmoon.helper.FilterCommandSender
import com.jmstudios.redmoon.service.ScreenFilterService
import com.jmstudios.redmoon.view.ScreenFilterView

class ColorSeekBarPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {

    lateinit var mColorTempSeekBar: SeekBar
    private var mProgress: Int = 0
    lateinit private var mView: View
    lateinit private var mCommandSender: FilterCommandSender
    lateinit private var mCommandFactory: FilterCommandFactory

    init {
        layoutResource = R.layout.preference_color_seekbar
    }

    fun setProgress(progress: Int) {
        mColorTempSeekBar.progress = progress
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getInteger(index, DEFAULT_VALUE)
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        if (restorePersistedValue) {
            mProgress = getPersistedInt(DEFAULT_VALUE)
        } else {
            mProgress = (defaultValue as Int?)?:0
            persistInt(mProgress)
        }
    }

    override fun onBindView(view: View) {
        super.onBindView(view)

        mView = view

        mColorTempSeekBar = view.findViewById(R.id.color_temp_seekbar) as SeekBar
        initLayout()
    }

    private fun initLayout() {
        mCommandSender = FilterCommandSender(mView.context)
        mCommandFactory = FilterCommandFactory(mView.context)
        mColorTempSeekBar.progress = mProgress

        mColorTempSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mProgress = progress
                persistInt(mProgress)

                updateMoonIconColor()
                updateProgressText()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                Log.i(TAG, "Touch down on a seek bar")

                val showPreviewCommand = mCommandFactory.createCommand(ScreenFilterService.COMMAND_SHOW_PREVIEW)
                mCommandSender.send(showPreviewCommand)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                Log.d(TAG, "Released a seek bar")

                val hidePreviewCommand = mCommandFactory.createCommand(ScreenFilterService.COMMAND_HIDE_PREVIEW)
                mCommandSender.send(hidePreviewCommand)
            }
        })

        updateMoonIconColor()
        updateProgressText()
    }

    private fun updateMoonIconColor() {
        if (!isEnabled) return

        val color = ScreenFilterView.rgbFromColorProgress(mProgress)

        val moonIcon = mView.findViewById(R.id.moon_icon_color) as ImageView

        val colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY)

        moonIcon.colorFilter = colorFilter
    }

    private fun updateProgressText() {
        val colorTemp = ScreenFilterView.getColorTempFromProgress(mProgress)

        val progressView = mView.findViewById(R.id.current_color_temperature) as TextView

        val unit = "K"
        val temp = Integer.toString(colorTemp)

        progressView.text = temp + unit
    }

    companion object {
        private val TAG = "ColorSeekBarPreference"
        private val DEBUG = false
        // Changes to DEFAULT_VALUE should be reflected in preferences.xml
        val DEFAULT_VALUE = 10
    }
}
