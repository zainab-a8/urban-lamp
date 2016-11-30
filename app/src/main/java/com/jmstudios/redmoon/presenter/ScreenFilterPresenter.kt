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

package com.jmstudios.redmoon.presenter

import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.PowerManager
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.Gravity
import android.view.WindowManager

import com.jmstudios.redmoon.R

import com.jmstudios.redmoon.activity.ShadesActivity
import com.jmstudios.redmoon.event.*
import com.jmstudios.redmoon.helper.*
import com.jmstudios.redmoon.manager.ScreenManager
import com.jmstudios.redmoon.manager.WindowViewManager
import com.jmstudios.redmoon.model.Config
import com.jmstudios.redmoon.model.ProfilesModel
import com.jmstudios.redmoon.receiver.NextProfileCommandReceiver
import com.jmstudios.redmoon.receiver.OrientationChangeReceiver
import com.jmstudios.redmoon.receiver.ScreenStateReceiver
import com.jmstudios.redmoon.receiver.SwitchAppWidgetProvider
import com.jmstudios.redmoon.service.ScreenFilterService
import com.jmstudios.redmoon.service.ServiceLifeCycleController
import com.jmstudios.redmoon.thread.CurrentAppMonitoringThread
import com.jmstudios.redmoon.view.ScreenFilterView

import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class ScreenFilterPresenter(private val mView: ScreenFilterView,
                            private val mServiceController: ServiceLifeCycleController,
                            private val mContext: Context,
                            private val mWindowViewManager: WindowViewManager,
                            private val mScreenManager: ScreenManager):
                        OrientationChangeReceiver.OnOrientationChangeListener,
                        ScreenStateReceiver.ScreenStateListener {
    private var mCamThread: CurrentAppMonitoringThread? = null
    private val mScreenStateReceiver: ScreenStateReceiver
    private var screenOff: Boolean = false

    private val mShuttingDown = false
    private var mScreenFilterOpen = false

    private var mColorAnimator: ValueAnimator? = null
    private var mDimAnimator: ValueAnimator? = null
    private var mIntensityAnimator: ValueAnimator? = null

    private val mOnState = OnState()
    private val mOffState = OffState()
    private val mPreviewState = PreviewState()
    private val mSuspendState = SuspendState()
    private var mCurrentState: State = mOffState

    // Screen brightness state
    private var oldScreenBrightness: Int = 0
    private var oldIsAutomaticBrightness: Boolean = false

    init {
        mScreenStateReceiver = ScreenStateReceiver(this)
        oldScreenBrightness = -1

        mCurrentState.onScreenFilterCommand(ScreenFilterService.COMMAND_OFF)
    }

    private fun refreshForegroundNotification() {
        val context = mView.context

        val profilesModel = ProfilesModel(context)

        val title = context.getString(R.string.app_name)
        val color = ContextCompat.getColor(context, R.color.color_primary)

        val smallIconResId = R.drawable.notification_icon_half_moon
        val contentText: String
        val offOrOnDrawableResId: Int
        val offOrOnCommand: Intent
        val offOrOnActionText: String

        if (filterIsOn) {
            Log.d(TAG, "Creating notification while NOT in off state")
            contentText = context.getString(
                    if (Config.automaticSuspend) R.string.running_no_warning
                    else R.string.running)
            offOrOnDrawableResId = R.drawable.ic_stop
            offOrOnCommand = ScreenFilterService.intent(mContext)
            offOrOnCommand.putExtra(ScreenFilterService.BUNDLE_KEY_COMMAND, ScreenFilterService.COMMAND_OFF)
            offOrOnActionText = context.getString(R.string.action_off)
        } else {
            Log.d(TAG, "Creating notification while in off state")
            contentText = context.getString(R.string.off)
            offOrOnDrawableResId = R.drawable.ic_play
            offOrOnCommand = ScreenFilterService.intent(mContext)
            offOrOnCommand.putExtra(ScreenFilterService.BUNDLE_KEY_COMMAND, ScreenFilterService.COMMAND_ON)
            offOrOnActionText = context.getString(R.string.resume_action)
        }

        val shadesActivityIntent = Intent(context, ShadesActivity::class.java)
        shadesActivityIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        val offOrOnPI = PendingIntent.getService(context, REQUEST_CODE_ACTION_OFF_OR_ON,
                offOrOnCommand, PendingIntent.FLAG_UPDATE_CURRENT)

        val settingsPI = PendingIntent.getActivity(context, REQUEST_CODE_ACTION_SETTINGS,
                shadesActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val nextProfileIntent = Intent(context, NextProfileCommandReceiver::class.java)

        val nextProfilePI = PendingIntent.getBroadcast(context, REQUEST_CODE_NEXT_PROFILE,
                nextProfileIntent, 0)

        val nb = NotificationCompat.Builder(mContext)
        nb.setSmallIcon(smallIconResId)
          .setContentTitle(title)
          .setContentText(contentText)
          .setColor (color)
          .setContentIntent(settingsPI)
          .addAction(offOrOnDrawableResId, offOrOnActionText, offOrOnPI)
          .addAction(R.drawable.ic_next_profile,
                     ProfilesHelper.getProfileName(profilesModel, Config.profile, context),
                     nextProfilePI)
          .setPriority(Notification.PRIORITY_MIN)

        if (filterIsOn) {
            Log.d(TAG, "Creating a persistent notification")
            mServiceController.startForeground(NOTIFICATION_ID, nb.build())
        } else {
            Log.d(TAG, "Creating a dismissible notification")
            val nm = mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID, nb.build())
        }
    }

    @Subscribe
    fun onScreenFilterCommand(event: moveToState) {
        val commandFlag = event.commandFlag

        if (mShuttingDown) {
            Log.i(TAG, "In the process of shutting down; ignoring command: " + commandFlag)
            return
        }

        if (DEBUG)
            Log.i(TAG, String.format("Handling command: %d in current state: %s",
                    commandFlag, mCurrentState))

        mCurrentState.onScreenFilterCommand(commandFlag)
    }

    @Subscribe
    fun onOffStateChanged(event: filterIsOnChanged) {
        //Broadcast to keep appwidgets in sync
        if (DEBUG) Log.i(TAG, "Sending update broadcast")
        val updateAppWidgetIntent = Intent(mContext, SwitchAppWidgetProvider::class.java)
        updateAppWidgetIntent.action = SwitchAppWidgetProvider.ACTION_UPDATE
        updateAppWidgetIntent.putExtra(SwitchAppWidgetProvider.EXTRA_POWER, Config.filterIsOn)
        mContext.sendBroadcast(updateAppWidgetIntent)
    }

    @Subscribe
    fun onDimLevelChanged(event: dimChanged) {
        val dim = Config.dim
        if (filterIsOn || isPreviewing) {
            cancelRunningAnimator(mDimAnimator)

            mView.filterDimLevel = dim
        }
    }

    @Subscribe
    fun onIntensityLevelChanged(event: intensityChanged) {
        val intensityLevel = Config.intensity
        if (filterIsOn || isPreviewing) {
            cancelRunningAnimator(mIntensityAnimator)

            mView.filterIntensityLevel = intensityLevel
        }
    }

    @Subscribe
    fun onColorChanged(event: colorChanged) {
        if (filterIsOn || isPreviewing) {
            mView.colorTempProgress = Config.color
        }
    }

    @Subscribe
    fun onLowerBrightnessChanged(event: lowerBrightnessChanged) {
        if (Util.hasWriteSettingsPermission) {
            val lowerBrightness = Config.lowerBrightness
            if (DEBUG) Log.i(TAG, "Lower brightness flag changed to: " + lowerBrightness)
            if (filterIsOn) {
                if (lowerBrightness) {
                    saveOldBrightnessState()
                    setBrightnessState(0, false, mContext)
                } else {
                    restoreBrightnessState()
                }
            }
        } else {
            EventBus.getDefault().post(changeBrightnessDenied())
        }
    }

    @Subscribe
    fun onProfileChanged(event: profileChanged) {
        refreshForegroundNotification()
    }

    @Subscribe
    fun onAutomaticSuspendChanged(event: automaticSuspendChanged) {
        if (mCurrentState === mOnState) {
            if (Config.automaticSuspend) startAppMonitoring()
            else stopAppMonitoring()
        }
    }

    private fun animateShadesColor(toColor: Int) {
        cancelRunningAnimator(mColorAnimator)

        val fromColor = mView.colorTempProgress

        mColorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor)
        mColorAnimator!!.duration = FADE_DURATION_MS.toLong()
        mColorAnimator!!.addUpdateListener { valueAnimator -> mView.colorTempProgress = valueAnimator.animatedValue as Int }

        mColorAnimator!!.start()
    }

    private fun animateDimLevel(toDimLevel: Int, listener: Animator.AnimatorListener?) {
        cancelRunningAnimator(mDimAnimator)

        val fromDimLevel = mView.filterDimLevel

        mDimAnimator = ValueAnimator.ofInt(fromDimLevel, toDimLevel)
        mDimAnimator!!.duration = FADE_DURATION_MS.toLong()
        mDimAnimator!!.addUpdateListener { valueAnimator -> mView.filterDimLevel = valueAnimator.animatedValue as Int }

        if (listener != null) {
            mDimAnimator!!.addListener(listener)
        }

        mDimAnimator!!.start()
    }

    private fun animateIntensityLevel(toIntensityLevel: Int, listener: Animator.AnimatorListener?) {
        cancelRunningAnimator(mIntensityAnimator)

        val fromIntensityLevel = mView.filterIntensityLevel

        mIntensityAnimator = ValueAnimator.ofInt(fromIntensityLevel, toIntensityLevel)
        mIntensityAnimator!!.duration = FADE_DURATION_MS.toLong()
        mIntensityAnimator!!.addUpdateListener { valueAnimator -> mView.filterIntensityLevel = valueAnimator.animatedValue as Int }

        if (listener != null) {
            mIntensityAnimator!!.addListener(listener)
        }

        mIntensityAnimator!!.start()
    }

    private val filterIsOn: Boolean
        get() = mCurrentState.filterIsOn

    private val isPreviewing: Boolean
        get() = mCurrentState === mPreviewState

    private fun cancelRunningAnimator(animator: Animator?) {
        if (animator != null && animator.isRunning) {
            animator.cancel()
        }
    }
    //endregion

    //region OnOrientationChangeListener
    override fun onPortraitOrientation() {
        reLayoutScreenFilter()
    }

    override fun onLandscapeOrientation() {
        reLayoutScreenFilter()
    }
    //endregion

    private fun saveOldBrightnessState() {
        if (Config.lowerBrightness) {
            val resolver = mContext.contentResolver
            try {
                oldScreenBrightness = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS)
                oldIsAutomaticBrightness = 1 == Settings.System.getInt(resolver, "screen_brightness_mode")
            } catch (e: SettingNotFoundException) {
                Log.e(TAG, "Error reading brightness state", e)
                oldIsAutomaticBrightness = false
            }

        } else {
            oldScreenBrightness = -1
        }
        Config.brightnessAutomatic = oldIsAutomaticBrightness
        Config.brightnessLevel = oldScreenBrightness
    }

    private fun restoreBrightnessState() {
        setBrightnessState(Config.brightnessLevel,
                Config.brightnessAutomatic,
                mContext)
    }

    @Suppress("DEPRECATION")
    fun startAppMonitoring() {
        if (DEBUG) Log.i(TAG, "Starting app monitoring")
        val powerManager = mContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        screenOff = if (Util.atLeastAPI(20)) !powerManager.isInteractive
                    else !powerManager.isScreenOn

        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        filter.addAction(Intent.ACTION_SCREEN_ON)
        mContext.registerReceiver(mScreenStateReceiver, filter)

        if (mCamThread == null && !screenOff) {
            mCamThread = CurrentAppMonitoringThread(mContext)
            mCamThread!!.start()
        }
    }

    fun stopAppMonitoring() {
        if (DEBUG) Log.i(TAG, "Stopping app monitoring")
        if (mCamThread != null) {
            if (!mCamThread!!.isInterrupted) {
                mCamThread!!.interrupt()
            }
            mCamThread = null
        }

        try {
            mContext.unregisterReceiver(mScreenStateReceiver)
        } catch (e: IllegalArgumentException) {
            // Catch errors when receiver is unregistered more than
            // once, it is not a problem, so we just ignore it.
        }

    }

    override fun onScreenTurnedOn() {
        if (DEBUG) Log.i(TAG, "Screen turn on received")
        screenOff = false

        if (mCamThread == null) {
            mCamThread = CurrentAppMonitoringThread(mContext)
            mCamThread!!.start()
        }
    }

    override fun onScreenTurnedOff() {
        if (DEBUG) Log.i(TAG, "Screen turn off received")
        screenOff = true

        if (mCamThread != null) {
            if (!mCamThread!!.isInterrupted) {
                mCamThread!!.interrupt()
            }
            mCamThread = null
        }
    }

    private fun createFilterLayoutParams(): WindowManager.LayoutParams {
        val wlp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                mScreenManager.screenHeight,
                0,
                -mScreenManager.statusBarHeightPx,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT)

        wlp.gravity = Gravity.TOP or Gravity.START
        wlp.buttonBrightness = (if (Config.dimButtons) 0 else -1).toFloat()

        return wlp
    }

    private fun openScreenFilter() {
        if (!mScreenFilterOpen) {
            // Display the transparent filter
            mWindowViewManager.openWindow(mView, createFilterLayoutParams())
            mScreenFilterOpen = true
        }
    }

    private fun reLayoutScreenFilter() {
        if (!mScreenFilterOpen) {
            return
        }
        mWindowViewManager.reLayoutWindow(mView, createFilterLayoutParams())
    }

    private fun closeScreenFilter() {
        if (!mScreenFilterOpen || mCurrentState.filterIsOn) {
            return
        }

        // Close the window once the fade-out animation is complete
        mWindowViewManager.closeWindow(mView)
        mScreenFilterOpen = false
    }

    private fun moveToState(commandFlag: Int) {
        when (commandFlag) {
            ScreenFilterService.COMMAND_OFF -> moveToState(mOffState)
            ScreenFilterService.COMMAND_ON -> moveToState(mOnState)
            ScreenFilterService.COMMAND_SHOW_PREVIEW -> moveToState(mPreviewState)
            ScreenFilterService.COMMAND_START_SUSPEND -> moveToState(mSuspendState)
        }
    }

    private fun moveToState(newState: State) {
        if (DEBUG)
            Log.i(TAG, String.format("Transitioning state from %s to %s",
                    mCurrentState, newState))
        if (newState === mCurrentState) return

        val oldState = mCurrentState
        mCurrentState = newState

        mCurrentState.onActivation(oldState)
        Config.filterIsOn = filterIsOn
    }


    private abstract inner class State {
        abstract fun onActivation(prevState: State)
        abstract val filterIsOn: Boolean

        open fun onScreenFilterCommand(commandFlag: Int) {
            moveToState(commandFlag)
        }

        override fun toString(): String {
            return javaClass.simpleName
        }
    }

    private inner class OnState : State() {
        override fun onActivation(prevState: State) {
            refreshForegroundNotification()

            openScreenFilter()

            animateDimLevel(Config.dim, null)
            animateIntensityLevel(Config.intensity, null)

            if (Config.lowerBrightness) {
                saveOldBrightnessState()
                setBrightnessState(0, false, mContext)
            }

            if (Config.automaticSuspend) {
                startAppMonitoring()
            }
        }

        override val filterIsOn = true
    }

    private inner class OffState : State() {
        override fun onActivation(prevState: State) {
            mServiceController.stopForeground(false)
            refreshForegroundNotification()

            if (prevState === mPreviewState) {
                mServiceController.stopForeground(false)
                closeScreenFilter()
            } else {
                animateIntensityLevel(ScreenFilterView.MIN_INTENSITY, null)
                animateDimLevel(ScreenFilterView.MIN_DIM, object : AbstractAnimatorListener() {
                    override fun onAnimationCancel(animator: Animator) {
                        closeScreenFilter()
                    }

                    override fun onAnimationEnd(animator: Animator) {
                        closeScreenFilter()
                    }
                })
            }

            if (Config.lowerBrightness) {
                restoreBrightnessState()
            }

            if (Config.automaticSuspend) {
                stopAppMonitoring()
            }
        }

        override val filterIsOn = false
    }

    /* This State is used to present the filter to the user when (s)he
     * is holding one of the seekbars to adjust the filter. It turns
     * on the filter and saves what state it should be when it will be
     * turned off.
     */
    private inner class PreviewState : State() {
        lateinit var stateToReturnTo: State
        var pressesActive: Int = 0

        override fun onActivation(prevState: State) {
            stateToReturnTo = prevState
            pressesActive = 1
            refreshForegroundNotification()
            openScreenFilter()

            mView.filterDimLevel = Config.dim
            mView.filterIntensityLevel = Config.intensity
            mView.colorTempProgress = Config.color
        }

        override fun onScreenFilterCommand(commandFlag: Int) {
            if (DEBUG) Log.d(TAG, String.format("Preview, got command: %d", commandFlag))
            when (commandFlag) {
                ScreenFilterService.COMMAND_ON -> stateToReturnTo = mOnState

                ScreenFilterService.COMMAND_OFF -> {
                    if (DEBUG) Log.d(TAG, String.format("State to return to changed to %d while in preview mode", commandFlag))
                    stateToReturnTo = mOffState
                }

                ScreenFilterService.COMMAND_SHOW_PREVIEW -> {
                    pressesActive++
                    if (DEBUG) Log.d(TAG, String.format("%d presses active", pressesActive))
                }

                ScreenFilterService.COMMAND_HIDE_PREVIEW -> {
                    pressesActive--
                    if (DEBUG) Log.d(TAG, String.format("%d presses active", pressesActive))

                    if (pressesActive <= 0) {
                        if (false) Log.d(TAG, String.format("Moving back to state %d", stateToReturnTo))
                        if (!filterIsOn) {
                            mServiceController.stopForeground(false)
                            closeScreenFilter()
                        }
                        moveToState(stateToReturnTo)
                    }
                }
            }
        }

        override val filterIsOn: Boolean
            get() = stateToReturnTo.filterIsOn
    }

    /* This state is used when the filter is suspended temporarily,
     * because the user is in an excluded app (for example the package
     * installer). It stops the filter like in the OffState, but
     * doesn't change the UI, switch or brightness state just like the
     * PreviewState. Like the PreviewState, it logs changes to the
     * state and applies them when the suspend state is deactivated.
     */
    private inner class SuspendState : State() {
        lateinit var stateToReturnTo: State

        override fun onActivation(prevState: State) {
            stateToReturnTo = prevState
            mServiceController.stopForeground(false)
            closeScreenFilter()
            refreshForegroundNotification()
        }

        override fun onScreenFilterCommand(commandFlag: Int) {
            if (DEBUG) Log.d(TAG, String.format("Suspend, got command: %d", commandFlag))
            when (commandFlag) {
                ScreenFilterService.COMMAND_STOP_SUSPEND -> moveToState(stateToReturnTo)
                ScreenFilterService.COMMAND_OFF -> moveToState(commandFlag)
                ScreenFilterService.COMMAND_ON,
                    // Suspended is a subset of on, so there is nothing to do
                ScreenFilterService.COMMAND_SHOW_PREVIEW,
                ScreenFilterService.COMMAND_HIDE_PREVIEW -> { }
            }// Preview is ignored when the filter is suspended
        }

        override val filterIsOn: Boolean
            get() = stateToReturnTo.filterIsOn
    }

    companion object {
        private val TAG = "ScreenFilterPresenter"
        private val DEBUG = true

        val NOTIFICATION_ID = 1
        private val REQUEST_CODE_ACTION_SETTINGS = 1000
        private val REQUEST_CODE_ACTION_OFF_OR_ON = 3000
        private val REQUEST_CODE_NEXT_PROFILE = 4000

        val FADE_DURATION_MS = 1000

        // Statically used by BootReceiver
        fun setBrightnessState(brightness: Int, automatic: Boolean, context: Context) {
            if (DEBUG) Log.i(TAG, "Setting brightness to: $brightness, automatic: $automatic")
            if (Util.atLeastAPI(23) && !Settings.System.canWrite(context)) return
            if (brightness >= 0) {
                val resolver = context.contentResolver
                Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
                Settings.System.putInt(resolver, "screen_brightness_mode", if (automatic) 1 else 0)
            }
        }
    }
}
