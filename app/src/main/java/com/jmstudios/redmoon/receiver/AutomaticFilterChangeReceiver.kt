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
package com.jmstudios.redmoon.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import java.util.GregorianCalendar
import java.util.TimeZone

class AutomaticFilterChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (DEBUG) Log.i(TAG, "Alarm received")
        val commandSender = FilterCommandSender(context)
        val commandFactory = FilterCommandFactory(context)
        val onCommand = commandFactory.createCommand(ScreenFilterService.COMMAND_ON)
        val pauseCommand = commandFactory.createCommand(ScreenFilterService.COMMAND_PAUSE)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val settingsModel = SettingsModel(context.resources, sharedPreferences)

        val turnOn = intent.data.toString() == "turnOnIntent"

        if (turnOn) {
            commandSender.send(onCommand)
            cancelTurnOnAlarm(context)
            scheduleNextOnCommand(context)
        } else {
            commandSender.send(pauseCommand)
            cancelPauseAlarm(context)
            scheduleNextPauseCommand(context)

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

        // TODO: add "&& settingsModel.getUseLocation()"
        if (settingsModel.automaticFilter) {
            val updater = LocationUpdater(context, object : LocationUpdater.updateHandler {
                override fun handleFound() {}
                override fun handleFailed() {}
            })
            updater.update()
        }
    }

    companion object {
        private val TAG = "AutomaticFilterChange"
        private val DEBUG = false

        fun scheduleNextOnCommand(context: Context) {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val settingsModel = SettingsModel(context.resources, sharedPreferences)

            if (settingsModel.automaticFilter) {
                val time: String
                time = settingsModel.automaticTurnOnTime

                val turnOnIntent = Intent(context, AutomaticFilterChangeReceiver::class.java)
                turnOnIntent.data = Uri.parse("turnOnIntent")
                turnOnIntent.putExtra("turn_on", true)

                scheduleNextAlarm(context, time, turnOnIntent, false)
            }
        }

        fun scheduleNextPauseCommand(context: Context) {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val settingsModel = SettingsModel(context.resources, sharedPreferences)

            if (settingsModel.automaticFilter) {
                val time = settingsModel.automaticTurnOffTime

                val pauseIntent = Intent(context, AutomaticFilterChangeReceiver::class.java)
                pauseIntent.putExtra("turn_on", false)
                pauseIntent.data = Uri.parse("pauseIntent")

                scheduleNextAlarm(context, time, pauseIntent, false)
            }
        }

        fun scheduleNextAlarm(context: Context, time: String, operation: Intent, timeInUtc: Boolean) {
            val calendar = GregorianCalendar()
            calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.split(":".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()[0]))
            calendar.set(Calendar.MINUTE, Integer.parseInt(time.split(":".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()[1]))

            val now = GregorianCalendar()
            now.add(Calendar.SECOND, 1)
            if (calendar.before(now)) {
                calendar.add(Calendar.DATE, 1)
            }
            if (!timeInUtc)
                calendar.timeZone = TimeZone.getTimeZone("UTC")

            if (DEBUG) Log.i(TAG, "Scheduling alarm for " + calendar.toString())

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val pendingIntent = PendingIntent.getBroadcast(context, 0, operation, 0)

            if (android.os.Build.VERSION.SDK_INT >= 19) {
                alarmManager.setExact(AlarmManager.RTC, calendar.timeInMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC, calendar.timeInMillis, pendingIntent)
            }
        }

        fun cancelAlarms(context: Context) {
            cancelPauseAlarm(context)
            cancelTurnOnAlarm(context)
        }

        fun cancelPauseAlarm(context: Context) {
            val commands = Intent(context, AutomaticFilterChangeReceiver::class.java)
            commands.data = Uri.parse("pauseIntent")
            val pendingIntent = PendingIntent.getBroadcast(context, 0, commands, 0)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        }

        fun cancelTurnOnAlarm(context: Context) {
            val commands = Intent(context, AutomaticFilterChangeReceiver::class.java)
            commands.data = Uri.parse("turnOnIntent")
            val pendingIntent = PendingIntent.getBroadcast(context, 0, commands, 0)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        }
    }
}
