/*
 * Copyright (c) 2016 Marien Raat <marienraat@riseup.net>
 * Copyright (c) 2017 Stephen Michel <s@smichel.me>
 * SPDX-License-Identifier: GPL-3.0+
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
package com.jmstudios.redmoon.filter.overlay

import android.content.Context
import android.graphics.Canvas
import android.view.View
import android.view.WindowManager

import com.jmstudios.redmoon.filter.manager.ScreenManager
import com.jmstudios.redmoon.model.Profile
import com.jmstudios.redmoon.model.Config
import com.jmstudios.redmoon.util.*

import org.greenrobot.eventbus.Subscribe

class Overlay(ctx: Context) : View(ctx),
                              OrientationChangeReceiver.OnOrientationChangeListener {

    private val mWindowManager = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mScreenManager = ScreenManager(ctx, mWindowManager)

    var color: Int = Profile(0, 0, 0, false).filterColor
        set(value) {
            field = value
            invalidate() // Forces call to onDraw
            if (filterIsOn && Config.buttonBacklightFlag == "dim") {
                updateLayout()
            }
        }

    override fun onDraw(canvas: Canvas) = canvas.drawColor(color)

    private fun updateLayout() = mWindowManager.updateViewLayout(this, mLayoutParams)

    private var mLayoutParams = mScreenManager.layoutParams
        get() = field.apply { buttonBrightness = Config.buttonBacklightLevel }

    fun show() {
        mWindowManager.addView(this, mLayoutParams)
    }

    fun hide() {
        mWindowManager.removeView(this)
    }

    override fun onOrientationChanged() {
        mLayoutParams = mScreenManager.layoutParams
        if (filterIsOn) {
            updateLayout()
        }
    }

    @Subscribe fun onButtonBacklightChanged(event: buttonBacklightChanged) {
        if (filterIsOn) {
            updateLayout()
        }
    }

    companion object : Logger()
}
