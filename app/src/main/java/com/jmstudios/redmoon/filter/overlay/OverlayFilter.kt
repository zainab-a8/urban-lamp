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
package com.jmstudios.redmoon.filter.overlay

import android.content.Context
import com.jmstudios.redmoon.filter.Filter
import com.jmstudios.redmoon.model.Config
import com.jmstudios.redmoon.model.Profile
import com.jmstudios.redmoon.util.*

import java.util.concurrent.ScheduledExecutorService

import org.greenrobot.eventbus.Subscribe

class OverlayFilter(context: Context, executor: ScheduledExecutorService) : Filter {
    private val mOverlay = Overlay(context)
    private val mOrientationReceiver = OrientationChangeReceiver(context, mOverlay)
    private val mBrightnessManager = BrightnessManager(context)
    private val mCurrentAppMonitor = CurrentAppMonitor(context, executor)

    override fun start() {
        Log.i("start()")
        filtering = true
        appMonitoring = true
        listeningForEvents = true
    }

    override fun setColor(profile: Profile) {
        Log.i("setColor($profile)")
        mOverlay.color = profile.filterColor
        filtering = !profile.isOff
        mBrightnessManager.brightnessLowered = profile.lowerBrightness
    }

    override fun stop() {
        Log.i("stop()")
        filtering = false
        appMonitoring = false
        listeningForEvents = false
    }

    private var listeningForEvents: Boolean = false
        set(value) {
            if (value != field) {
                field = value
                if (value) {
                    EventBus.register(this)
                } else {
                    EventBus.unregister(this)
                }
            }
        }

    private var filtering: Boolean = false
        set(value) {
            if (value == field) {
                if (Config.buttonBacklightFlag == "dim") {
                    mOverlay.reLayout()
                }
            } else {
                field = value
                if (value) {
                    mOverlay.show()
                    mOrientationReceiver.register()
                } else {
                    mOverlay.hide()
                    mOrientationReceiver.unregister()
                    mBrightnessManager.brightnessLowered = false
                }
            }
        }

    private var appMonitoring: Boolean = false
        set(value) {
            if (value != field) {
                field = value
                if (value) {
                    mCurrentAppMonitor.start()
                } else {
                    mCurrentAppMonitor.stop()
                }
            }
        }

    @Subscribe fun onButtonBacklightChanged(event: buttonBacklightChanged) {
        if (filtering) mOverlay.reLayout()
    }

    @Subscribe fun onSecureSuspendChanged(event: secureSuspendChanged) {
        appMonitoring = Config.secureSuspend
    }

    companion object : Logger()
}
