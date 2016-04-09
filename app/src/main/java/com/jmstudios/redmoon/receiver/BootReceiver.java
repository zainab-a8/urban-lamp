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
package com.jmstudios.redmoon.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.jmstudios.redmoon.helper.FilterCommandFactory;
import com.jmstudios.redmoon.helper.FilterCommandSender;
import com.jmstudios.redmoon.model.SettingsModel;
import com.jmstudios.redmoon.service.ScreenFilterService;
import com.jmstudios.redmoon.receiver.AutomaticFilterChangeReceiver;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    private static final boolean DEBUG = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (DEBUG) Log.i(TAG, "Boot broadcast received!");

        FilterCommandSender commandSender = new FilterCommandSender(context);
        FilterCommandFactory commandFactory = new FilterCommandFactory(context);
        Intent onCommand = commandFactory.createCommand(ScreenFilterService.COMMAND_ON);
        Intent pauseCommand = commandFactory.createCommand(ScreenFilterService.COMMAND_PAUSE);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SettingsModel settingsModel = new SettingsModel(context.getResources(), sharedPreferences);

        boolean poweredOnBeforeReboot = settingsModel.getShadesPowerState();
        boolean pausedBeforeReboot = settingsModel.getShadesPauseState();

        // Handle "Always open on startup" flag
        boolean alwaysOpenOnBoot = false;
        if (alwaysOpenOnBoot) {
            if (DEBUG) Log.i(TAG, "\"Always open on startup\" flag was set; starting now.");

            AutomaticFilterChangeReceiver.scheduleNextOnCommand(context);
            AutomaticFilterChangeReceiver.scheduleNextPauseCommand(context);

            commandSender.send(onCommand);
            return;
        }

        // Handle "Keep running after reboot" flag
        boolean resumeAfterReboot = true;
        if (resumeAfterReboot) {
            if (DEBUG) Log.i(TAG, "\"Keep running after reboot\" flag was set.");

            if (poweredOnBeforeReboot) {
                if (DEBUG) Log.i(TAG, "Shades was on before reboot; resuming state.");

                AutomaticFilterChangeReceiver.scheduleNextOnCommand(context);
                AutomaticFilterChangeReceiver.scheduleNextPauseCommand(context);

                commandSender.send(pausedBeforeReboot ? pauseCommand : onCommand);
            } else {
                if (DEBUG) Log.i(TAG, "Shades was off before reboot; no state to resume from.");
            }
            return;
        }

        // Allow ScreenFilterService to sync its state and any shared preferences to "off" mode
        Intent offCommand = commandFactory.createCommand(ScreenFilterService.COMMAND_OFF);
        commandSender.send(offCommand);
    }
}
