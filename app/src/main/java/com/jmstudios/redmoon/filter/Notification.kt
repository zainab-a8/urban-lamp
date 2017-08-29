/*
 * Copyright (c) 2016  Marien Raat <marienraat@riseup.net>
 * Copyright (c) 2017  Stephen Michel <s@smichel.me>
 * SPDX-License-Identifier: GPL-3.0+
 */
package com.jmstudios.redmoon.filter

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat

import com.jmstudios.redmoon.R

import com.jmstudios.redmoon.ui.MainActivity
import com.jmstudios.redmoon.util.*

private const val REQUEST_CODE_ACTION_SETTINGS = 1000
private const val REQUEST_CODE_ACTION_TOGGLE   = 3000
private const val REQUEST_CODE_NEXT_PROFILE    = 4000
private const val REQUEST_CODE_ACTION_STOP     = 5000

fun getNotification(filterIsOn: Boolean): Notification {
    fun servicePI(code: Int, intent: Intent) = PendingIntent.getService(
            appContext, code, intent, PendingIntent.FLAG_UPDATE_CURRENT)

    fun activityPI(code: Int, intent: Intent) = PendingIntent.getActivity(
            appContext, code, intent, PendingIntent.FLAG_UPDATE_CURRENT)

    fun broadcastPI(code: Int, intent: Intent) = PendingIntent.getBroadcast(
            appContext, code, intent, 0)

    val nb = NotificationCompat.Builder(appContext).apply {
        // Set notification appearance
        setSmallIcon(R.drawable.notification_icon_half_moon)
        color    = ContextCompat.getColor(appContext, R.color.color_primary)
        priority = Notification.PRIORITY_MIN

        if (belowAPI(24)) { setContentTitle(getString(R.string.app_name)) }
        setSubText(activeProfile.name)
        setContentText(getString(if (filterIsOn) {
            R.string.notification_status_running
        } else {
            R.string.notification_status_paused
        }))

        // Open Red Moon when tapping notification body
        val mainActivityIntent = intent(MainActivity::class).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        setContentIntent(activityPI(REQUEST_CODE_ACTION_SETTINGS, mainActivityIntent))

        // Turn off Red Moon when the notification is manually dismissed
        setDeleteIntent(servicePI(REQUEST_CODE_ACTION_STOP, Command.OFF.intent))

        // Add toggle action
        if (filterIsOn) {
            addAction(R.drawable.ic_pause,
                      getString(R.string.notification_action_pause),
                      servicePI(REQUEST_CODE_ACTION_TOGGLE, Command.PAUSE.intent))
        } else {
            addAction(R.drawable.ic_play,
                      getString(R.string.notification_action_resume),
                      servicePI(REQUEST_CODE_ACTION_TOGGLE, Command.ON.intent))
        }

        // Add profile switch action
        val nextProfileText   = getString(R.string.notification_action_next_filter)
        val nextProfileIntent = intent(NextProfileCommandReceiver::class)
        val nextProfilePI     = broadcastPI(REQUEST_CODE_NEXT_PROFILE, nextProfileIntent)
        addAction(R.drawable.ic_skip_next_white_36dp, nextProfileText, nextProfilePI)
    }
    return nb.build()
}
