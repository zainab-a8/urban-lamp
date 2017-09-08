/*
 * Copyright (c) 2016  Marien Raat <marienraat@riseup.net>
 * Copyright (c) 2017  Stephen Michel <s@smichel.me>
 * SPDX-License-Identifier: GPL-3.0+
 */
package com.jmstudios.redmoon.filter.overlay

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager

import com.jmstudios.redmoon.model.Config
import com.jmstudios.redmoon.filter.Command
import com.jmstudios.redmoon.filter.ScreenStateReceiver
import com.jmstudios.redmoon.util.*

import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class CurrentAppMonitor(
        private val mContext: Context,
        private val mExecutor: ScheduledExecutorService)
    : ScreenStateReceiver.ScreenStateListener {

    private val screenStateReceiver = ScreenStateReceiver(this)
    private var mAppChecker = CurrentAppChecker(mContext)
    private var mFuture: ScheduledFuture<*>? = null

    private var lastApp: String = ""

    private val handleCurrentApp = Runnable {
        val currentApp = mAppChecker.currentApp
        Log.i("Current app is: $currentApp, last was: $lastApp")
        when(currentApp) {
            lastApp -> {} // only respond when the app has changed
            "com.android.packageinstaller",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "me.phh.superuser",
            "com.owncloud.android",
            "com.google.android.packageinstaller" -> Command.SUSPEND.send()
            else -> Command.RESUME.send()
        }
        lastApp = currentApp
    }

    private val powerManager: PowerManager
        get() = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager

    private val screenOn: Boolean
        get() = powerManager.run {
            if (atLeastAPI(20)) isInteractive else @Suppress("DEPRECATION") isScreenOn
        }

    private var isMonitoring: Boolean = false

    override fun onScreenTurnedOn() {
        Log.i("Screen turn on received")
        startMonitoring()
    }

    override fun onScreenTurnedOff() {
        Log.i("Screen turn off received")
        stopMonitoring()
    }

    fun start() = when {
        !Config.secureSuspend -> Log.i("Can't start; monitoring is disabled.")
        isMonitoring -> Log.i("Monitoring is already started")
        else -> {
            Log.i("Starting app monitoring")
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON )
            }
            mContext.registerReceiver(screenStateReceiver, filter)
            if (screenOn) startMonitoring()
            isMonitoring = true
        }
    }

    fun stop() = if (!isMonitoring) {
        Log.i("Monitoring is already stopped")
    } else {
        Log.i("Stopping app monitoring")
        stopMonitoring()
        try {
            mContext.unregisterReceiver(screenStateReceiver)
        } catch (e: IllegalArgumentException) {
            // Catch errors when receiver is unregistered more than once.
            // It is not a problem, so we just ignore it.
        }
        isMonitoring = false
    }

    private fun startMonitoring() {
        mFuture = mExecutor.scheduleWithFixedDelay(handleCurrentApp, 0, 1, TimeUnit.SECONDS)
    }

    private fun stopMonitoring() {
        mFuture?.cancel(true)
    }

    companion object : Logger()
}
