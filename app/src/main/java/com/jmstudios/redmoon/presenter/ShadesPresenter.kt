/*
 * Copyright (c) 2016  Marien Raat <marienraat@riseup.net>
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
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *     Copyright (c) 2015 Chris Nguyen
 *
 *     Permission to use, copy, modify, and/or distribute this software
 *     for any purpose with or without fee is hereby granted, provided
 *     that the above copyright notice and this permission notice appear
 *     in all copies.
 *
 *     THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL
 *     WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED
 *     WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE
 *     AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR
 *     CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS
 *     OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *     NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN
 *     CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package com.jmstudios.redmoon.presenter

import android.content.Context
import android.util.Log

import com.jmstudios.redmoon.R

import com.jmstudios.redmoon.activity.ShadesActivity
import com.jmstudios.redmoon.event.*
import com.jmstudios.redmoon.fragment.FilterFragment
import com.jmstudios.redmoon.model.SettingsModel
import com.jmstudios.redmoon.preference.ColorSeekBarPreference
import com.jmstudios.redmoon.preference.DimSeekBarPreference
import com.jmstudios.redmoon.preference.IntensitySeekBarPreference
import com.jmstudios.redmoon.receiver.AutomaticFilterChangeReceiver

import org.greenrobot.eventbus.Subscribe

class ShadesPresenter(private val mView: FilterFragment,
                      private val mSettingsModel: SettingsModel,
                      private val mContext: Context) {
    private val mActivity: ShadesActivity

    init {
        mActivity = mContext as ShadesActivity
    }

    //region OnSettingsChangedListener
    @Subscribe fun onFilterIsOnChanged(event: filterIsOnChanged) {
        val filterIsOn = event.newValue
        if (DEBUG) Log.i(TAG, "Filter is on changed to: " + filterIsOn)
        mActivity.setSwitch(filterIsOn)
        if (filterIsOn) {
            mActivity.displayInstallWarningToast()
        }
    }

    @Subscribe fun onDimLevelChanged(event: dimLevelChanged) {
        val dimLevel = event.newValue
        val pref = mView.preferenceScreen.findPreference(mContext.getString(R.string.pref_key_shades_dim_level)) as DimSeekBarPreference
        pref.setProgress(dimLevel)
    }

    @Subscribe fun onIntensityLevelChanged(event: intensityLevelChanged) {
        val intensityLevel = event.newValue
        val pref = mView.preferenceScreen.findPreference(mContext.getString(R.string.pref_key_shades_intensity_level)) as IntensitySeekBarPreference
        pref.setProgress(intensityLevel)
    }

    @Subscribe fun onColorChanged(event: colorChanged) {
        val color = event.newValue
        val pref = mView.preferenceScreen.findPreference(mContext.getString(R.string.pref_key_shades_color_temp)) as ColorSeekBarPreference
        pref.setProgress(color)
    }

    @Subscribe fun onAutomaticFilterChanged(event: automaticFilterChanged) {
        val automaticFilter = event.newValue
        if (DEBUG) Log.i(TAG, "Filter mode changed to " + automaticFilter)
        AutomaticFilterChangeReceiver.cancelAlarms(mContext)
        if (automaticFilter) {
            AutomaticFilterChangeReceiver.scheduleNextOnCommand(mContext)
            AutomaticFilterChangeReceiver.scheduleNextOffCommand(mContext)
        }
    }

    @Subscribe fun onAutomaticTurnOnChanged(event: automaticTurnOnChanged) {
        AutomaticFilterChangeReceiver.cancelTurnOnAlarm(mContext)
        AutomaticFilterChangeReceiver.scheduleNextOnCommand(mContext)
    }

    @Subscribe fun onAutomaticTurnOffChanged(event: automaticTurnOffChanged) {
        AutomaticFilterChangeReceiver.cancelOffAlarm(mContext)
        AutomaticFilterChangeReceiver.scheduleNextOffCommand(mContext)
    }

    companion object {
        private val TAG = "ShadesPresenter"
        private val DEBUG = true
    }
    //endregion
}
