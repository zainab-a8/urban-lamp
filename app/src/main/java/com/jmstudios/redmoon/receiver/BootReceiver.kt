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
package com.jmstudios.redmoon.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log

import com.jmstudios.redmoon.helper.DismissNotificationRunnable
import com.jmstudios.redmoon.helper.FilterCommandFactory
import com.jmstudios.redmoon.helper.FilterCommandSender
import com.jmstudios.redmoon.model.SettingsModel
import com.jmstudios.redmoon.presenter.ScreenFilterPresenter
import com.jmstudios.redmoon.service.ScreenFilterService

import java.util.Calendar

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (DEBUG) Log.i(TAG, "Boot broadcast received!")

        val commandSender = FilterCommandSender(context)
        val commandFactory = FilterCommandFactory(context)
        val onCommand = commandFactory.createCommand(ScreenFilterService.COMMAND_ON)
        val pauseCommand = commandFactory.createCommand(ScreenFilterService.COMMAND_PAUSE)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val settingsModel = SettingsModel(context.resources, sharedPreferences)

        val pausedBeforeReboot = settingsModel.pauseState

        // If the filter was on when the device was powered down and the
        // automatic brightness setting is on, then it still uses the
        // dimmed brightness and we need to restore the saved brightness
        // before proceeding.
        if (!pausedBeforeReboot && settingsModel.brightnessControlFlag) {
            ScreenFilterPresenter.setBrightnessState(settingsModel.brightnessLevel,
                    settingsModel.brightnessAutomatic,
                    context)
        }

        AutomaticFilterChangeReceiver.scheduleNextOnCommand(context)
        AutomaticFilterChangeReceiver.scheduleNextPauseCommand(context)

        val isPausePredicted = getPredictedPauseState(pausedBeforeReboot, settingsModel)
        commandSender.send(if (isPausePredicted)
            pauseCommand
        else
            onCommand)

        if (isPausePredicted) {
            // We want to dismiss the notification if the filter is paused
            // automatically.
            // However, the filter fades out and the notification is only
            // refreshed when this animation has been completed.  To make sure
            // that the new notification is removed we create a new runnable to
            // be excecuted 100 ms after the filter has faded out.
            val handler = Handler()

            val runnable = DismissNotificationRunnable(context)
            handler.postDelayed(runnable, (ScreenFilterPresenter.FADE_DURATION_MS + 100).toLong())
        }
        return
    }

    companion object {
        private val TAG = "BootReceiver"
        private val DEBUG = false

        private fun getPredictedPauseState(pausedBeforeReboot: Boolean,
                                           model: SettingsModel): Boolean {
            if (model.automaticFilter) {
                val now = Calendar.getInstance()

                val onTime = model.automaticTurnOnTime
                val onHour = Integer.parseInt(onTime.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0])
                val onMinute = Integer.parseInt(onTime.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
                val on = Calendar.getInstance()
                on.set(Calendar.HOUR_OF_DAY, onHour)
                on.set(Calendar.MINUTE, onMinute)

                if (on.after(now))
                    on.add(Calendar.DATE, -1)

                val offTime = model.automaticTurnOffTime
                val offHour = Integer.parseInt(offTime.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0])
                val offMinute = Integer.parseInt(offTime.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
                val off = Calendar.getInstance()
                off.set(Calendar.HOUR_OF_DAY, offHour)
                off.set(Calendar.MINUTE, offMinute)

                while (off.before(on))
                    off.add(Calendar.DATE, 1)

                if (DEBUG) {
                    Log.d(TAG, "On: $onTime, off: $offTime")
                    Log.d(TAG, "On DAY_OF_MONTH: " + Integer.toString(on.get(Calendar.DAY_OF_MONTH)))
                    Log.d(TAG, "Off DAY_OF_MONTH: " + Integer.toString(off.get(Calendar.DAY_OF_MONTH)))
                }

                return !(now.after(on) && now.before(off))
            } else {
                return pausedBeforeReboot
            }
        }
    }
}
