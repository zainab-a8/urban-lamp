/*
 * Copyright (c) 2016  Marien Raat <marienraat@riseup.net>
 * Copyright (c) 2017  Stephen Michel <s@smichel.me>
 * SPDX-License-Identifier: GPL-3.0+
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *     Copyright (c) 2015 Chris Nguyen
 *     Copyright (c) 2016 Zoraver <https://github.com/Zoraver>
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

package com.jmstudios.redmoon.filter

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.jmstudios.redmoon.filter.overlay.OverlayFilter

import com.jmstudios.redmoon.util.*

private const val COMMAND_MISSING = -1

private const val NOTIFICATION_ID = 1

private const val DURATION_LONG    = 1000 // One second
private const val DURATION_SHORT   = 250
private const val DURATION_INSTANT = 0

class FilterService : Service() {
    val notificationManager: NotificationManager
        get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private lateinit var mFilter : FilterManager

    override fun onCreate() {
        super.onCreate()
        Log.i("onCreate")
        mFilter = FilterManager(OverlayFilter(this))
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i("onStartCommand($intent, $flags, $startId)")
        if (Permission.Overlay.isGranted) {
            val flag = intent.getIntExtra(Command.BUNDLE_KEY_COMMAND, COMMAND_MISSING)
            Log.i("Recieved flag: $flag")
            if (flag != COMMAND_MISSING) {
                Command.values()[flag].activate(this)
            }
        } else {
            Log.i("Overlay permission denied.")
            EventBus.post(overlayPermissionDenied())
            stopSelf()
        }

        // Do not attempt to restart if the hosting process is killed by Android
        return Service.START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null // Prevent binding.

    override fun onDestroy() {
        Log.i("onDestroy")
        filterIsOn = false
        super.onDestroy()
    }

    fun start() {
        Log.i("start()")
        startForeground(NOTIFICATION_ID, getNotification(true))
        mFilter.turnOn(DURATION_LONG)
    }

    fun preview() {
        Log.i("preview()")
        startForeground(NOTIFICATION_ID, getNotification(true))
        mFilter.turnOn(DURATION_INSTANT)
    }

    fun stop() {
        Log.i("pause()")
        stopForeground(true)
        mFilter.turnOff(DURATION_LONG) {
            stopSelf()
        }
    }

    fun pause() {
        Log.i("stop()")
        stopForeground(false)
        notificationManager.notify(NOTIFICATION_ID, getNotification(false))
        mFilter.turnOff(DURATION_SHORT)
    }

    companion object : Logger()
}
