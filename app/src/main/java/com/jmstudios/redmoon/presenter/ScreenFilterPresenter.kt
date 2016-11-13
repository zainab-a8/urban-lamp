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
import com.jmstudios.redmoon.helper.AbstractAnimatorListener
import com.jmstudios.redmoon.helper.FilterCommandFactory
import com.jmstudios.redmoon.helper.FilterCommandParser
import com.jmstudios.redmoon.helper.ProfilesHelper
import com.jmstudios.redmoon.manager.ScreenManager
import com.jmstudios.redmoon.manager.WindowViewManager
import com.jmstudios.redmoon.model.ProfilesModel
import com.jmstudios.redmoon.model.SettingsModel
import com.jmstudios.redmoon.receiver.NextProfileCommandReceiver
import com.jmstudios.redmoon.receiver.OrientationChangeReceiver
import com.jmstudios.redmoon.receiver.ScreenStateReceiver
import com.jmstudios.redmoon.receiver.SwitchAppWidgetProvider
import com.jmstudios.redmoon.service.ScreenFilterService
import com.jmstudios.redmoon.service.ServiceLifeCycleController
import com.jmstudios.redmoon.thread.CurrentAppMonitoringThread
import com.jmstudios.redmoon.view.ScreenFilterView

class ScreenFilterPresenter(private val mView: ScreenFilterView,
                            private val mSettingsModel: SettingsModel,
                            private val mServiceController: ServiceLifeCycleController,
                            private val mContext: Context,
                            private val mWindowViewManager: WindowViewManager,
                            private val mScreenManager: ScreenManager,
                            private var mNotificationBuilder: NotificationCompat.Builder,
                            private val mFilterCommandFactory: FilterCommandFactory,
                            private val mFilterCommandParser: FilterCommandParser) : OrientationChangeReceiver.OnOrientationChangeListener, SettingsModel.OnSettingsChangedListener, ScreenStateReceiver.ScreenStateListener {
    private var mCamThread: CurrentAppMonitoringThread? = null
    private val mScreenStateReceiver: ScreenStateReceiver
    private var screenOff: Boolean = false

    private val mShuttingDown = false
    private var mScreenFilterOpen = false

    lateinit private var mColorAnimator: ValueAnimator
    lateinit private var mDimAnimator: ValueAnimator
    lateinit private var mIntensityAnimator: ValueAnimator

    private val mOnState = OnState()
    private val mPauseState = PauseState()
    private val mPreviewState = PreviewState()
    private val mSuspendState = SuspendState()
    private var mCurrentState: State = mPauseState

    // Screen brightness state
    private var oldScreenBrightness: Int = 0
    private var oldIsAutomaticBrightness: Boolean = false

    init {
        mScreenStateReceiver = ScreenStateReceiver(this)
        oldScreenBrightness = -1

        mCurrentState.onScreenFilterCommand(ScreenFilterService.COMMAND_PAUSE)
    }

    private fun refreshForegroundNotification() {
        val context = mView.context

        val profilesModel = ProfilesModel(context)

        val title = context.getString(R.string.app_name)
        val color = ContextCompat.getColor(context, R.color.color_primary)

        val smallIconResId = R.drawable.notification_icon_half_moon
        val contentText: String
        val pauseOrResumeDrawableResId: Int
        val pauseOrResumeCommand: Intent
        val pauseOrResumeActionText: String

        if (isPaused) {
            Log.d(TAG, "Creating notification while in pause state")
            contentText = context.getString(R.string.paused)
            pauseOrResumeDrawableResId = R.drawable.ic_play
            pauseOrResumeCommand = mFilterCommandFactory.createCommand(ScreenFilterService.COMMAND_ON)
            pauseOrResumeActionText = context.getString(R.string.resume_action)
        } else {
            Log.d(TAG, "Creating notification while NOT in pause state")
            contentText = context.getString(if (mSettingsModel.automaticSuspend)
                R.string.running_no_warning
            else
                R.string.running)
            pauseOrResumeDrawableResId = R.drawable.ic_pause
            pauseOrResumeCommand = mFilterCommandFactory.createCommand(ScreenFilterService.COMMAND_PAUSE)
            pauseOrResumeActionText = context.getString(R.string.pause_action)
        }

        val shadesActivityIntent = Intent(context, ShadesActivity::class.java)
        shadesActivityIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        val pauseOrResumePI = PendingIntent.getService(context, REQUEST_CODE_ACTION_PAUSE_OR_RESUME,
                pauseOrResumeCommand, PendingIntent.FLAG_UPDATE_CURRENT)

        val settingsPI = PendingIntent.getActivity(context, REQUEST_CODE_ACTION_SETTINGS,
                shadesActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val nextProfileIntent = Intent(context, NextProfileCommandReceiver::class.java)

        val nextProfilePI = PendingIntent.getBroadcast(context, REQUEST_CODE_NEXT_PROFILE,
                nextProfileIntent, 0)

        mNotificationBuilder = NotificationCompat.Builder(mContext)
        mNotificationBuilder.setSmallIcon(smallIconResId).setContentTitle(title).setContentText(contentText).setColor(color).setContentIntent(settingsPI).addAction(pauseOrResumeDrawableResId,
                pauseOrResumeActionText,
                pauseOrResumePI).addAction(R.drawable.ic_next_profile,
                ProfilesHelper.getProfileName(profilesModel, mSettingsModel.profile, context),
                nextProfilePI).setPriority(Notification.PRIORITY_MIN)

        if (isPaused) {
            Log.d(TAG, "Creating a dismissible notification")
            val mNotificationManager = mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build())
        } else {
            Log.d(TAG, "Creating a persistent notification")
            mServiceController.startForeground(NOTIFICATION_ID, mNotificationBuilder.build())
        }
    }

    fun onScreenFilterCommand(command: Intent) {
        val commandFlag = mFilterCommandParser.parseCommandFlag(command)

        if (mShuttingDown) {
            Log.i(TAG, "In the process of shutting down; ignoring command: " + commandFlag)
            return
        }

        if (DEBUG)
            Log.i(TAG, String.format("Handling command: %d in current state: %s",
                    commandFlag, mCurrentState))

        mCurrentState.onScreenFilterCommand(commandFlag)
    }

    //region OnSettingsChangedListener
    override fun onPauseStateChanged(pauseState: Boolean) {
        //Broadcast to keep appwidgets in sync
        if (DEBUG) Log.i(TAG, "Sending update broadcast")
        val updateAppWidgetIntent = Intent(mContext, SwitchAppWidgetProvider::class.java)
        updateAppWidgetIntent.action = SwitchAppWidgetProvider.ACTION_UPDATE
        updateAppWidgetIntent.putExtra(SwitchAppWidgetProvider.EXTRA_POWER, !pauseState)
        mContext.sendBroadcast(updateAppWidgetIntent)
    }

    override fun onDimLevelChanged(dimLevel: Int) {
        if (!isPaused || isPreviewing) {
            cancelRunningAnimator(mDimAnimator)

            mView.filterDimLevel = dimLevel
        }
    }

    override fun onIntensityLevelChanged(intensityLevel: Int) {
        if (!isPaused || isPreviewing) {
            cancelRunningAnimator(mIntensityAnimator)

            mView.filterIntensityLevel = intensityLevel
        }
    }

    override fun onColorChanged(color: Int) {
        if (!isPaused || isPreviewing) {
            mView.colorTempProgress = color
        }
    }

    override fun onAutomaticFilterChanged(automaticFilter: Boolean) {
    }

    override fun onAutomaticTurnOnChanged(turnOnTime: String) {
    }

    override fun onAutomaticTurnOffChanged(turnOffTime: String) {
    }

    override fun onLowerBrightnessChanged(lowerBrightness: Boolean) {
        if (DEBUG) Log.i(TAG, "Lower brightness flag changed to: " + lowerBrightness)
        if (!isPaused) {
            if (lowerBrightness) {
                saveOldBrightnessState()
                setBrightnessState(0, false, mContext)
            } else {
                restoreBrightnessState()
            }
        }
    }

    override fun onProfileChanged(profile: Int) {
        refreshForegroundNotification()
    }

    override fun onAutomaticSuspendChanged(automaticSuspend: Boolean) {
        if (mCurrentState === mOnState) {
            if (automaticSuspend) {
                startAppMonitoring()
            } else {
                stopAppMonitoring()
            }
        }
    }

    private fun animateShadesColor(toColor: Int) {
        cancelRunningAnimator(mColorAnimator)

        val fromColor = mView.colorTempProgress

        mColorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor)
        mColorAnimator.duration = FADE_DURATION_MS.toLong()
        mColorAnimator.addUpdateListener { valueAnimator -> mView.colorTempProgress = valueAnimator.animatedValue as Int }

        mColorAnimator.start()
    }

    private fun animateDimLevel(toDimLevel: Int, listener: Animator.AnimatorListener?) {
        cancelRunningAnimator(mDimAnimator)

        val fromDimLevel = mView.filterDimLevel

        mDimAnimator = ValueAnimator.ofInt(fromDimLevel, toDimLevel)
        mDimAnimator.duration = FADE_DURATION_MS.toLong()
        mDimAnimator.addUpdateListener { valueAnimator -> mView.filterDimLevel = valueAnimator.animatedValue as Int }

        if (listener != null) {
            mDimAnimator.addListener(listener)
        }

        mDimAnimator.start()
    }

    private fun animateIntensityLevel(toIntensityLevel: Int, listener: Animator.AnimatorListener?) {
        cancelRunningAnimator(mIntensityAnimator)

        val fromIntensityLevel = mView.filterIntensityLevel

        mIntensityAnimator = ValueAnimator.ofInt(fromIntensityLevel, toIntensityLevel)
        mIntensityAnimator.duration = FADE_DURATION_MS.toLong()
        mIntensityAnimator.addUpdateListener { valueAnimator -> mView.filterIntensityLevel = valueAnimator.animatedValue as Int }

        if (listener != null) {
            mIntensityAnimator.addListener(listener)
        }

        mIntensityAnimator.start()
    }

    private val isPaused: Boolean
        get() = mCurrentState.isPaused

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
        if (mSettingsModel.brightnessControlFlag) {
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
        mSettingsModel.brightnessAutomatic = oldIsAutomaticBrightness
        mSettingsModel.brightnessLevel = oldScreenBrightness
    }

    private fun restoreBrightnessState() {
        setBrightnessState(mSettingsModel.brightnessLevel,
                mSettingsModel.brightnessAutomatic,
                mContext)
    }

    @Suppress("DEPRECATION")
    fun startAppMonitoring() {
        if (DEBUG) Log.i(TAG, "Starting app monitoring")
        val powerManager = mContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT_WATCH) {
            screenOff = !powerManager.isInteractive
        } else {
            screenOff = !powerManager.isScreenOn
        }

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
        wlp.buttonBrightness = (if (mSettingsModel.dimButtonsFlag) 0 else -1).toFloat()

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
        if (!mScreenFilterOpen || !mCurrentState.isPaused) {
            return
        }

        // Close the window once the fade-out animation is complete
        mWindowViewManager.closeWindow(mView)
        mScreenFilterOpen = false
    }

    private fun moveToState(commandFlag: Int) {
        when (commandFlag) {
            ScreenFilterService.COMMAND_PAUSE -> moveToState(mPauseState)

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
        mSettingsModel.pauseState = isPaused
    }


    private abstract inner class State {
        abstract fun onActivation(prevState: State)
        abstract val isPaused: Boolean

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

            animateDimLevel(mSettingsModel.dimLevel, null)
            animateIntensityLevel(mSettingsModel.intensityLevel, null)

            if (mSettingsModel.brightnessControlFlag) {
                saveOldBrightnessState()
                setBrightnessState(0, false, mContext)
            }

            if (mSettingsModel.automaticSuspend) {
                startAppMonitoring()
            }
        }

        override val isPaused: Boolean
            get() = false
    }

    private inner class PauseState : State() {
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

            if (mSettingsModel.brightnessControlFlag) {
                restoreBrightnessState()
            }

            if (mSettingsModel.automaticSuspend) {
                stopAppMonitoring()
            }
        }

        override val isPaused: Boolean
            get() = true
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

            val dim = mSettingsModel.dimLevel
            val intensity = mSettingsModel.intensityLevel
            val filterColor = mSettingsModel.color

            mView.filterDimLevel = dim
            mView.filterIntensityLevel = intensity
            mView.colorTempProgress = filterColor
        }

        override fun onScreenFilterCommand(commandFlag: Int) {
            if (DEBUG) Log.d(TAG, String.format("Preview, got command: %d", commandFlag))
            when (commandFlag) {
                ScreenFilterService.COMMAND_ON -> stateToReturnTo = mOnState

                ScreenFilterService.COMMAND_PAUSE -> {
                    if (DEBUG) Log.d(TAG, String.format("State to return to changed to %d while in preview mode", commandFlag))
                    stateToReturnTo = mPauseState
                }

                ScreenFilterService.COMMAND_SHOW_PREVIEW -> {
                    pressesActive++
                    if (DEBUG)
                        Log.d(TAG,
                                String.format("%d presses active", pressesActive))
                }

                ScreenFilterService.COMMAND_HIDE_PREVIEW -> {
                    pressesActive--
                    if (DEBUG)
                        Log.d(TAG,
                                String.format("%d presses active", pressesActive))

                    if (pressesActive <= 0) {
                        if (DEBUG)
                            Log.d(TAG, String.format("Moving back to state %d", stateToReturnTo))
                        if (isPaused) {
                            mServiceController.stopForeground(false)
                            closeScreenFilter()
                        }
                        moveToState(stateToReturnTo)
                    }
                }
            }
        }

        override val isPaused: Boolean
            get() = stateToReturnTo.isPaused
    }

    /* This state is used when the filter is suspended temporarily,
     * because the user is in an excluded app (for example the package
     * installer). It stops the filter like in the PauseState, but
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
                ScreenFilterService.COMMAND_PAUSE -> moveToState(commandFlag)
                ScreenFilterService.COMMAND_ON,
                    // Suspended is a subset of on, so there is nothing to do
                ScreenFilterService.COMMAND_SHOW_PREVIEW, ScreenFilterService.COMMAND_HIDE_PREVIEW -> {
                }
            }// Preview is ignored when the filter is suspended
        }

        override val isPaused: Boolean
            get() = stateToReturnTo.isPaused
    }

    companion object {
        private val TAG = "ScreenFilterPresenter"
        private val DEBUG = false

        val NOTIFICATION_ID = 1
        private val REQUEST_CODE_ACTION_SETTINGS = 1000
        private val REQUEST_CODE_ACTION_PAUSE_OR_RESUME = 3000
        private val REQUEST_CODE_NEXT_PROFILE = 4000

        val FADE_DURATION_MS = 1000

        // Statically used by BootReceiver
        fun setBrightnessState(brightness: Int, automatic: Boolean, context: Context) {
            if (android.os.Build.VERSION.SDK_INT >= 23 && !Settings.System.canWrite(context))
                return
            if (DEBUG) Log.i(TAG, "Setting brightness to: $brightness, automatic: $automatic")
            if (brightness >= 0) {
                val resolver = context.contentResolver
                Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
                Settings.System.putInt(resolver, "screen_brightness_mode", if (automatic) 1 else 0)
            }
        }
    }
}
