/*
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

package com.jmstudios.redmoon.application

import android.app.Application

import com.jmstudios.redmoon.event.*
import com.jmstudios.redmoon.helper.EventBus
import com.jmstudios.redmoon.model.Config
import com.jmstudios.redmoon.receiver.TimeToggleChangeReceiver
import com.jmstudios.redmoon.helper.Logger
import com.jmstudios.redmoon.receiver.SwitchAppWidgetProvider
import com.jmstudios.redmoon.util.*

import org.greenrobot.eventbus.Subscribe

private const val BROADCAST_ACTION = "com.jmstudios.redmoon.RED_MOON_TOGGLED"
private const val BROADCAST_FIELD  = "jmstudios.bundle.key.FILTER_IS_ON"

class RedMoonApplication: Application() {

    override fun onCreate() {
        app = this
        super.onCreate()
        //EventBus.builder().addIndex(eventBusIndex()).installDefaultEventBus()
        EventBus.register(this)
        Log.d("Opened Settings change listener")
    }

    // Only called in emulated environments. In production, just gets killed.
    override fun onTerminate() {
        EventBus.unregister(this)
        Log.d("Closed Settings change listener")
        super.onTerminate()
    }

    //region State updaters; Probably should refactor this.
    @Subscribe fun sync(event: filterIsOnChanged) {
        Log.i("Sending update broadcasts")
        //Broadcast to keep appwidgets in sync
        sendBroadcast(intent(SwitchAppWidgetProvider::class).apply {
            action = SwitchAppWidgetProvider.ACTION_UPDATE
            putExtra(SwitchAppWidgetProvider.EXTRA_POWER, Config.filterIsOn)
        })

        // If an app like Tasker wants to do something each time
        // Red Moon is toggled, it can listen for this event
        sendBroadcast(intent().apply {
            action = BROADCAST_ACTION
            putExtra(BROADCAST_FIELD, Config.filterIsOn)
        })
    }

    @Subscribe fun onProfileChanged(event: profileChanged) = activeProfile.run {
        Log.i("setProfile: ${Config.profile}")
        Log.i("color=$color, intensity=$intensity, dim=$dimLevel, lb=$lowerBrightness")
        Config.color           = color
        Config.intensity       = intensity
        Config.dimLevel        = dimLevel
        Config.lowerBrightness = lowerBrightness
    }

    @Subscribe fun onTimeToggleChanged(event: timeToggleChanged) = if (Config.timeToggle) {
        Log.i("Timer turned on")
        TimeToggleChangeReceiver.rescheduleOnCommand()
        TimeToggleChangeReceiver.rescheduleOffCommand()
    } else {
        Log.i("Timer turned on")
        TimeToggleChangeReceiver.cancelAlarms()
    }

    @Subscribe fun onCustomTurnOnTimeChanged(event: customTurnOnTimeChanged) {
        TimeToggleChangeReceiver.rescheduleOnCommand()
    }

    @Subscribe fun onCustomTurnOffTimeChanged(event: customTurnOffTimeChanged) {
        TimeToggleChangeReceiver.rescheduleOffCommand()
    }

    @Subscribe fun onLocationChanged(event: locationChanged) {
        TimeToggleChangeReceiver.rescheduleOffCommand()
        TimeToggleChangeReceiver.rescheduleOnCommand()
    }
    //endregion

    companion object : Logger() {
        lateinit var app: RedMoonApplication
    }
}
