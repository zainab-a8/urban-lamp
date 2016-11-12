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

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.TypedArray
import android.preference.Preference
import android.provider.Settings
import android.view.View
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.Switch
import android.widget.TextView

import com.jmstudios.redmoon.R

import com.jmstudios.redmoon.activity.AutomaticSuspendPreferenceActivity
import com.jmstudios.redmoon.thread.CurrentAppMonitoringThread

class SwitchBarPreference(private val mContext: Context, private val mActivity: Activity) : Preference(mContext), CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    private var mValue: Boolean = false
    private var mTextView: TextView? = null
    private var mSwitch: Switch? = null

    init {
        layoutResource = R.layout.switch_bar
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getBoolean(index, DEFAULT_VALUE)
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any) {
        if (restorePersistedValue) {
            mValue = getPersistedBoolean(DEFAULT_VALUE)
        } else {
            mValue = defaultValue as Boolean
            persistBoolean(mValue)
        }
    }

    override fun onBindView(view: View) {
        super.onBindView(view)

        mTextView = view.findViewById(R.id.switch_text) as TextView
        mTextView!!.setText(if (mValue) R.string.text_switch_on else R.string.text_switch_off)

        val bar = view.findViewById(R.id.frame_layout_switch_bar) as FrameLayout
        bar.setBackgroundColor(mContext.resources.getColor(R.color.switch_bar_background))
        bar.setOnClickListener(this)

        mSwitch = view.findViewById(R.id.switch_bar_switch) as Switch
        mSwitch!!.setOnCheckedChangeListener(this)
        mSwitch!!.isChecked = mValue
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        if (isChecked && !CurrentAppMonitoringThread.isAppMonitoringWorking(mContext)) {
            createEnableUsageStatsDialog()

            buttonView.isChecked = false
            mValue = false
        } else {
            mValue = isChecked
        }

        persistBoolean(mValue)

        mTextView!!.setText(if (mValue) R.string.text_switch_on else R.string.text_switch_off)
    }

    override fun onClick(v: View) {
        mSwitch!!.toggle()
    }

    fun usageStatsPermissionAttempted() {
        if (CurrentAppMonitoringThread.isAppMonitoringWorking(mContext)) {
            mSwitch!!.isChecked = true
        }
    }

    private fun createEnableUsageStatsDialog() {
        val builder = AlertDialog.Builder(mContext)

        builder.setMessage(R.string.usage_stats_dialog_message).setTitle(R.string.usage_stats_dialog_title).setPositiveButton(R.string.ok_dialog) { dialog, id ->
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            mActivity.startActivityForResult(intent, AutomaticSuspendPreferenceActivity.RESULT_USAGE_ACCESS)
        }

        builder.show()
    }

    companion object {
        val DEFAULT_VALUE = false

        private val TAG = "SwitchBarPreference"
        private val DEBUG = false
    }
}
