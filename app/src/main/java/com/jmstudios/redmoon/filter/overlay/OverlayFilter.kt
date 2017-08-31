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

import org.greenrobot.eventbus.Subscribe

class OverlayFilter (private val mContext: Context) : Filter {
    private val mOverlay = Overlay(mContext)
    private val mOrientationReceiver = OrientationChangeReceiver(mOverlay)
    private val mBrightnessManager = BrightnessManager(mContext)
    private val mCurrentAppMonitor = CurrentAppMonitor(mContext)

    private var mShown: Boolean = false

    override fun start() {
        Log.i("start()")
        showFilter()
        mCurrentAppMonitor.start()
        EventBus.register(mCurrentAppMonitor)
    }

    override fun setColor(profile: Profile) {
        Log.i("setColor($profile)")
        mOverlay.color = profile.filterColor
        if (profile.isOff) hideFilter() else showFilter()
        mBrightnessManager.run {
            if (profile.lowerBrightness) lower() else restore()
        }
    }

    override fun stop() {
        Log.i("stop()")
        hideFilter()
        mCurrentAppMonitor.stop()
        EventBus.unregister(mCurrentAppMonitor)
    }

    private fun showFilter() {
        if (mShown) {
            if (Config.buttonBacklightFlag == "dim") {
                mOverlay.updateLayout()
            }
        } else {
            mOverlay.show()
            mOrientationReceiver.register(mContext)
            EventBus.register(this)
            mShown = true
        }
    }

    private fun hideFilter() {
        if (mShown) {
            mOverlay.hide()
            EventBus.unregister(this)
            mOrientationReceiver.unregister(mContext)
            mBrightnessManager.restore()
            mShown = false
        }
    }

    @Subscribe fun onButtonBacklightChanged(event: buttonBacklightChanged) {
        mOverlay.updateLayout()
    }

    companion object : Logger()
}
