/*
 * Copyright (c) 2016  Marien Raat <marienraat@riseup.net>
 * Copyright (c) 2017  Stephen Michel <s@smichel.me>
 * SPDX-License-Identifier: GPL-3.0+
 */
package com.jmstudios.redmoon.filter.overlay

import android.annotation.TargetApi
import android.app.ActivityManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.ContextWrapper

import com.jmstudios.redmoon.util.*

class CurrentAppChecker(private val context: Context) {
    val isWorking: Boolean = currentApp != ""

    val currentApp: String
        // http://stackoverflow.com/q/33581311
        get() = try {
            if (atLeastAPI(21)) {
                currentAppFromUsageStats
            } else {
                currentAppFromActivityManager
            }
        } catch (e: Exception) {
            ""
        }

    private val currentAppFromUsageStats: String
        @TargetApi(21) get() {
            // Although the UsageStatsManager was added in API 21, the
            // constant to specify it wasn't added until API 22.
            // So we use the value of that constant on API 21.
            val uss = if (belowAPI(22)) "usagestats" else @TargetApi(22) {
                Context.USAGE_STATS_SERVICE
            }
            val usm = context.getSystemService(uss) as UsageStatsManager
            val time = System.currentTimeMillis()
            // Only look at events in the past 10 seconds
            val eventList = usm.queryEvents(time - 1000 * 10, time)
            val event = UsageEvents.Event()

            tailrec fun findLastApp(app: String): String {
                return if (eventList.hasNextEvent()) {
                    eventList.getNextEvent(event)
//https://developer.android.com/reference/android/app/usage/UsageEvents.Event.html#getClassName()
                    // although it's not documented in the api reference above,
                    // one of the times the class name is null is when the
                    // notification shade is shown and this activity is not
                    // actually the one which will be restored.
                    if (event.className == null) {
                        findLastApp(app)
                    } else {
                        findLastApp(event.packageName)
                    }
                } else app
            }

            lastApp = findLastApp(lastApp)
            return lastApp
        }

    private val currentAppFromActivityManager: String
        @Suppress("DEPRECATION") get() {
            val bc = ContextWrapper(context).baseContext
            val am = bc.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            return am.getRunningTasks(1)[0].topActivity.packageName
        }

    companion object: Logger() {
        private var lastApp: String = ""
    }
}
