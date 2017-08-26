/*
 * Copyright (c) 2016  Marien Raat <marienraat@riseup.net>
 * Copyright (c) 2017  Stephen Michel <s@smichel.me>
 * SPDX-License-Identifier: GPL-3.0+
 */
package com.jmstudios.redmoon.filter

import android.animation.Animator
import android.animation.ValueAnimator

import com.jmstudios.redmoon.filter.manager.AbstractAnimatorListener
import com.jmstudios.redmoon.model.Profile
import com.jmstudios.redmoon.util.EventBus
import com.jmstudios.redmoon.util.Logger
import com.jmstudios.redmoon.util.activeProfile
import com.jmstudios.redmoon.util.filterIsOn
import org.greenrobot.eventbus.Subscribe

class FilterManager(private val filter: Filter) {

    private val Profile.off: Profile
        get() = this.copy(intensity = 0, dimLevel = 0)

    private var mProfile = activeProfile.off
        set(value) {
            field = value
            filter.setColor(value)
        }

    private val mAnimator: ValueAnimator

    init {
        mAnimator = ValueAnimator.ofObject(ProfileEvaluator(), mProfile).apply {
            addUpdateListener { valueAnimator ->
                mProfile = valueAnimator.animatedValue as Profile
            }
        }
    }

    fun turnOn(time: Int) {
        Log.i("turnOn($time)")
        if (!filterIsOn) {
            animateTo(activeProfile, time, object : AbstractAnimatorListener {
                override fun onAnimationStart(animator: Animator) {
                    EventBus.register(this@FilterManager)
                    filterIsOn = true
                    filter.start()
                }
            })
        }
    }

    @Subscribe fun onProfileUpdated(profile: Profile) {
        mProfile = profile
    }

    fun turnOff(time: Int, after: () -> Unit = {}) {
        if (filterIsOn) {
            animateTo(mProfile.off, time, object : AbstractAnimatorListener {
                override fun onAnimationEnd(animator: Animator) {
                    EventBus.unregister(this@FilterManager)
                    filter.stop()
                    filterIsOn = false
                    after()
                }
            })
        } else {
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

    companion object : Logger()
}