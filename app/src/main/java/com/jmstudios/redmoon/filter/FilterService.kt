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

package com.jmstudios.redmoon.filter

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Service
import android.content.Intent
import android.os.IBinder

import com.jmstudios.redmoon.filter.manager.AbstractAnimatorListener
import com.jmstudios.redmoon.filter.overlay.OverlayFilter
import com.jmstudios.redmoon.model.Profile
import com.jmstudios.redmoon.util.*

import java.util.concurrent.Executors

import org.greenrobot.eventbus.Subscribe

class FilterService : Service() {

    private lateinit var mFilter: Filter
    private val mAnimator: ValueAnimator
    private val mExecutor = Executors.newSingleThreadScheduledExecutor()

    private var mProfile = activeProfile.off
        set(value) {
            field = value
            mFilter.setColor(value)
        }

    init {
        mAnimator = ValueAnimator.ofObject(ProfileEvaluator(), mProfile).apply {
            addUpdateListener { valueAnimator ->
                mProfile = valueAnimator.animatedValue as Profile
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i("onCreate")
        mFilter = OverlayFilter(this, mExecutor)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i("onStartCommand($intent, $flags, $startId)")
        if (Permission.Overlay.isGranted) {
            Command.handle(intent, this)
        } else {
            Log.i("Overlay permission denied.")
            EventBus.post(overlayPermissionDenied())
            stopSelf()
        }

        // Do not attempt to restart if the hosting process is killed by Android
        return Service.START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null // Prevent binding.

    override fun onDestroy() {
        Log.i("onDestroy")
        if (filterIsOn) {
            Log.w("Service killed while filter was on!")
            filterIsOn = false
            mFilter.stop()
        }
        mExecutor.shutdownNow()
        super.onDestroy()
    }

    fun start(time: Int) {
        Log.i("start($time)")
        if (!filterIsOn) {
            animateTo(activeProfile, time, object : AbstractAnimatorListener {
                override fun onAnimationStart(animator: Animator) {
                    EventBus.register(this@FilterService)
                    filterIsOn = true
                    mFilter.start()
                }
            })
        }
    }

    fun suspend(time: Int, after: () -> Unit = {}) {
        if (filterIsOn) {
            animateTo(mProfile.off, time, object : AbstractAnimatorListener {
                override fun onAnimationEnd(animator: Animator) {
                    EventBus.unregister(this@FilterService)
                    filterIsOn = false
                    after()
                }
            })
        } else {
            after()
        }
    }

    fun pause(time: Int, after: () -> Unit = {}) {
        suspend(time) {
            mFilter.stop()
            after()
        }
    }

    private fun animateTo(profile: Profile, time: Int, listener: Animator.AnimatorListener) {
        mAnimator.run {
            setObjectValues(mProfile, profile)
            Log.i("animating. Fraction is: $animatedFraction")
            duration = (if (isRunning) time * animatedFraction else time.toFloat()).toLong()
            removeAllListeners()
            addListener(listener)
            Log.i("Animating from $mProfile to $profile in $duration")
            start()
        }
    }

    @Subscribe fun onProfileUpdated(profile: Profile) {
        mProfile = profile
    }

    companion object : Logger()
}
