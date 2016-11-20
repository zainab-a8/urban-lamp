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
package com.jmstudios.redmoon.thread

import android.annotation.TargetApi
import android.app.ActivityManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.util.Log

import com.jmstudios.redmoon.helper.FilterCommandFactory
import com.jmstudios.redmoon.helper.FilterCommandSender
import com.jmstudios.redmoon.service.ScreenFilterService

import java.lang.Thread
import java.util.TreeMap

class CurrentAppMonitoringThread(private val mContext: Context) : Thread() {

    private val commandSender: FilterCommandSender
    private val startSuspendCommand: Intent
    private val stopSuspendCommand: Intent

    init {
        if (DEBUG) Log.d(TAG, "CurrentAppMonitoringThread created")

        val commandFactory = FilterCommandFactory(mContext)
        commandSender = FilterCommandSender(mContext)
        startSuspendCommand = commandFactory.createCommand(ScreenFilterService.COMMAND_START_SUSPEND)
        stopSuspendCommand = commandFactory.createCommand(ScreenFilterService.COMMAND_STOP_SUSPEND)
    }

    override fun run() {
        if (DEBUG) Log.i(TAG, "CurrentAppMonitoringThread running")

        try {
            while (!Thread.interrupted()) {
                val currentApp = getCurrentApp(mContext)

                if (DEBUG) Log.d(TAG, String.format("Current app is: %s", currentApp))

                if (isAppSecured(currentApp))
                    sendStartSuspendCommand()
                else
                    sendStopSuspendCommand()

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
                app == "me.phh.superuser"
    }

    private fun sendStartSuspendCommand() {
        if (DEBUG) Log.i(TAG, "Send a start suspend command")

        commandSender.send(startSuspendCommand)
    }

    private fun sendStopSuspendCommand() {
        if (DEBUG) Log.i(TAG, "Send a stop suspend command")

        commandSender.send(stopSuspendCommand)
    }

    companion object {
        private val TAG = "CurrentAppMonitoring"
        private val DEBUG = false

        fun isAppMonitoringWorking(context: Context): Boolean {
            return getCurrentApp(context) != ""
        }

        private fun getCurrentApp(context: Context): String {
            // http://stackoverflow.com/q/33581311
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                return getCurrentAppUsingUsageStats(context)
            } else {
                return getCurrentAppUsingActivityManager(context)
            }
        }

       @TargetApi(21)
        private fun getCurrentAppUsingUsageStats(context: Context): String {
            try {
                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE)
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
            if (android.os.Build.VERSION.SDK_INT < 21) {
                val am = ContextWrapper(context).baseContext
                          .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                return am.getRunningTasks(1)[0].topActivity.packageName
            }
            return ""
        }
    }
}
