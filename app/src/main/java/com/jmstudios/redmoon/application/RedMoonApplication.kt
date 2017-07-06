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

import com.jmstudios.redmoon.helper.EventBus
import com.jmstudios.redmoon.helper.Logger

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

    companion object : Logger() {
        lateinit var app: RedMoonApplication
    }
}
