/*
 * Copyright (c) 2016 Marien Raat <marienraat@riseup.net>
 * Copyright (c) 2017  Stephen Michel <s@smichel.me>
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
package com.jmstudios.redmoon.thread

import android.annotation.TargetApi
import android.app.ActivityManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.ContextWrapper
import android.util.Log

import com.jmstudios.redmoon.model.Config
import com.jmstudios.redmoon.service.ScreenFilterService

import java.lang.Thread
import java.util.TreeMap

class CurrentAppMonitoringThread(private val mContext: Context) : Thread() {

    init {
        if (DEBUG) Log.d(TAG, "CurrentAppMonitoringThread created")
    }

    override fun run() {
        if (DEBUG) Log.i(TAG, "CurrentAppMonitoringThread running")

        try {
            while (!Thread.interrupted()) {
                val currentApp = getCurrentApp(mContext)

                if (DEBUG) Log.d(TAG, String.format("Current app is: %s", currentApp))

                val state = if (isAppSecured(currentApp)) ScreenFilterService.Command.START_SUSPEND
                            else ScreenFilterService.Command.STOP_SUSPEND
                ScreenFilterService.moveToState(state)
                Thread.sleep(1000)
            }
        } catch (e: InterruptedException) {
        }

        if (DEBUG) Log.i(TAG, "Shutting down CurrentAppMonitoringThread")
    }

    private fun isAppSecured(app: String): Boolean {
        return app == "com.android.packageinstaller" ||
                app == "eu.chainfire.supersu" ||
                app == "com.koushikdutta.superuser" ||
                app == "me.phh.superuser" ||
                app == "com.owncloud.android" ||
                app == "com.google.android.packageinstaller"
    }

    @TargetApi(22) // Safe to call at all api levels but Studio doesn't know that
    companion object {
        private val TAG = "CurrentAppMonitoring"
        private val DEBUG = true

        fun isAppMonitoringWorking(context: Context): Boolean {
            return getCurrentApp(context) != ""
        }

        private fun getCurrentApp(context: Context): String {
            // http://stackoverflow.com/q/33581311
            if (Config.atLeastAPI(21)) {
                return getCurrentAppUsingUsageStats(context)
            } else {
                return getCurrentAppUsingActivityManager(context)
            }
        }

        @TargetApi(22) // Safe to call at all api levels but Studio doesn't know that
        private fun getCurrentAppUsingUsageStats(context: Context): String {
            try {
                if (Config.atLeastAPI(21)) {
                    // Although the UsageStatsManager was added in API
                    // 21, the constant to specify the
                    // UsageStatsManager wasn't added until API 22. So
                    // we use the value of that constant on API 21.
                    val usageStatsServiceString =
                        if (Config.atLeastAPI(22)) Context.USAGE_STATS_SERVICE
                        else "usagestats"
                    val usm = context.getSystemService(usageStatsServiceString)
                                                                as UsageStatsManager
                    val time = System.currentTimeMillis()
                    val appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,
                            time - 1000 * 1000, time)

                    if (appList != null && appList.size > 0) {
                        val mySortedMap = TreeMap<Long, UsageStats>()
                        for (usageStats in appList) {
                            mySortedMap.put(usageStats.lastTimeUsed,
                                    usageStats)
                        }
                        if (!mySortedMap.isEmpty()) {
                            val packageName = mySortedMap[mySortedMap.lastKey()]!!.packageName
                            return packageName ?: ""
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore exceptions to allow the user to determine if it
                // works him/herself
            }

            return ""
        }

        @Suppress("DEPRECATION") // Needed for pre-lollipop compatibility
        private fun getCurrentAppUsingActivityManager(context: Context): String {
            if (Config.belowAPI(21)) {
                val am = ContextWrapper(context).baseContext
                          .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                return am.getRunningTasks(1)[0].topActivity.packageName
            }
            return ""
        }
    }
}
