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

package com.jmstudios.redmoon.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import android.view.WindowManager

import com.jmstudios.redmoon.event.command
import com.jmstudios.redmoon.manager.ScreenManager
import com.jmstudios.redmoon.manager.WindowViewManager
import com.jmstudios.redmoon.presenter.ScreenFilterPresenter
import com.jmstudios.redmoon.receiver.OrientationChangeReceiver
import com.jmstudios.redmoon.receiver.SwitchAppWidgetProvider
import com.jmstudios.redmoon.view.ScreenFilterView

import org.greenrobot.eventbus.EventBus

class ScreenFilterService : Service(), ServiceLifeCycleController {

    lateinit private var mPresenter: ScreenFilterPresenter
    private var mOrientationReceiver: OrientationChangeReceiver? = null

    override fun onCreate() {
        super.onCreate()

        if (DEBUG) Log.i(TAG, "onCreate")

        // Initialize helpers and managers
        val context = this
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Wire MVP classes
        mPresenter = ScreenFilterPresenter(ScreenFilterView(context), this, context, 
                                           WindowViewManager(windowManager),
                                           ScreenManager(this, windowManager))

        // Make Presenter listen to settings changes and orientation changes
        EventBus.getDefault().register(mPresenter)
        if (mOrientationReceiver == null) {
            val orientationIntentFilter = IntentFilter()
            orientationIntentFilter.addAction(Intent.ACTION_CONFIGURATION_CHANGED)

            mOrientationReceiver = OrientationChangeReceiver(mPresenter)
            registerReceiver(mOrientationReceiver, orientationIntentFilter)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (DEBUG) Log.i(TAG, String.format("onStartCommand(%s, %d, %d", intent, flags, startId))

        // Do not attempt to restart if the hosting process is killed by Android
        return Service.START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        // Prevent binding.
        return null
    }

    override fun onDestroy() {
        if (DEBUG) Log.i(TAG, "onDestroy")

        EventBus.getDefault().unregister(mPresenter)
        unregisterReceiver(mOrientationReceiver)
        mOrientationReceiver = null

        //Broadcast to keep appwidgets in sync
        if (DEBUG) Log.i(TAG, "Sending update broadcast")
        val updateAppWidgetIntent = Intent(this, SwitchAppWidgetProvider::class.java)
        updateAppWidgetIntent.action = SwitchAppWidgetProvider.ACTION_UPDATE
        updateAppWidgetIntent.putExtra(SwitchAppWidgetProvider.EXTRA_POWER, false)
        sendBroadcast(updateAppWidgetIntent)

        super.onDestroy()
    }

    companion object {
        val VALID_COMMAND_START = 0
        val COMMAND_ON = 0
        val COMMAND_OFF = 1
        val COMMAND_SHOW_PREVIEW = 2
        val COMMAND_HIDE_PREVIEW = 3
        val COMMAND_START_SUSPEND = 4
        val COMMAND_STOP_SUSPEND = 5
        val VALID_COMMAND_END = 5

        val BUNDLE_KEY_COMMAND = "jmstudios.bundle.key.COMMAND"

        private val TAG = "ScreenFilterService"
        private val DEBUG = false

        val intent = { ctx: Context -> Intent(ctx, ScreenFilterService::class.java) }

        fun start(context: Context) {
            if (DEBUG) Log.i(TAG, "Received start request")
            context.startService(intent(context))
        }

        fun stop(context: Context) {
            if (DEBUG) Log.i(TAG, "Received stop request")
            context.stopService(intent(context))
        }

        fun moveToState(commandFlag: Int) {
            EventBus.getDefault().postSticky(command(commandFlag))
        }
    }
}
