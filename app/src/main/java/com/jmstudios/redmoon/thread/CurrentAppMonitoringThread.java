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
package com.jmstudios.redmoon.thread;

import android.content.Context;
import android.app.usage.UsageStatsManager;
import android.app.ActivityManager;
import android.util.Log;
import android.app.usage.UsageStats;
import android.content.ContextWrapper;
import android.content.Intent;

import java.util.SortedMap;
import java.util.List;
import java.util.TreeMap;
import java.lang.Thread;

import com.jmstudios.redmoon.helper.FilterCommandFactory;
import com.jmstudios.redmoon.helper.FilterCommandSender;
import com.jmstudios.redmoon.service.ScreenFilterService;

public class CurrentAppMonitoringThread extends Thread {
    private static final String TAG = "CurrentAppMonitoring";
    private static final boolean DEBUG = true;

    private Context mContext;

    private FilterCommandSender commandSender;
    private Intent startSuspendCommand;
    private Intent stopSuspendCommand;

    public CurrentAppMonitoringThread(Context context) {
        if (DEBUG) Log.d(TAG, "CurrentAppMonitoringThread created");
        mContext = context;

        FilterCommandFactory commandFactory =
            new FilterCommandFactory(mContext);
        commandSender = new FilterCommandSender(mContext);
        startSuspendCommand = commandFactory.createCommand
            (ScreenFilterService.COMMAND_START_SUSPEND);
        stopSuspendCommand = commandFactory.createCommand
            (ScreenFilterService.COMMAND_STOP_SUSPEND);
    }

    public void run() {
        if (DEBUG) Log.i(TAG, "CurrentAppMonitoringThread running");
        while (true) {
            String currentApp = "";

            // http://stackoverflow.com/q/33581311
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                UsageStatsManager usm = (UsageStatsManager) mContext.getSystemService("usagestats");
                long time = System.currentTimeMillis();
                List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,
                                                               time - 1000 * 1000, time);

                if (appList != null && appList.size() > 0) {
                    SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
                    for (UsageStats usageStats : appList) {
                        mySortedMap.put(usageStats.getLastTimeUsed(),
                                        usageStats);
                    }
                    if (mySortedMap != null && !mySortedMap.isEmpty()) {
                        currentApp = mySortedMap.get(
                                                     mySortedMap.lastKey()).getPackageName();
                    }
                }
            } else {
                ActivityManager am = (ActivityManager) (new ContextWrapper(mContext))
                    .getBaseContext().getSystemService(mContext.ACTIVITY_SERVICE);
                currentApp = am.getRunningTasks(1).get(0).topActivity.getPackageName();
            }

            if (DEBUG) Log.d(TAG, String.format("Current app is: %s", currentApp));

            if (currentApp.equals("com.android.packageinstaller"))
                sendStartSuspendCommand();
            else
                sendStopSuspendCommand();

            try {
                sleep(1000);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sendStartSuspendCommand() {
        if (DEBUG) Log.i(TAG, "Send a start suspend command");

        commandSender.send(startSuspendCommand);
    }

    public void sendStopSuspendCommand() {
        if (DEBUG) Log.i(TAG, "Send a stop suspend command");

        commandSender.send(stopSuspendCommand);
    }
}
