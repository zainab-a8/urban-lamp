/*
 * Copyright (c) 2016  Marien Raat <marienraat@riseup.net>
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
import android.annotation.TargetApi
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

import com.jmstudios.redmoon.activity.MainActivity
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

class ScreenFilterPresenter(private val mServiceController: ServiceLifeCycleController,
                            private val mContext: Context,
                            private val mWindowViewManager: WindowViewManager,
                            private val mScreenManager: ScreenManager):
                        OrientationChangeReceiver.OnOrientationChangeListener,
                        ScreenStateReceiver.ScreenStateListener {
    private var mView: ScreenFilterView = mWindowViewManager.mView
    private var mCamThread: CurrentAppMonitoringThread? = null
    private val mScreenStateReceiver = ScreenStateReceiver(this)
    private var screenOff: Boolean = false

    private val mOnState = OnState()
    private val mOffState = OffState()
    private val mPreviewState = PreviewState()
    private val mSuspendState = SuspendState()

    private var mCurrentState: State = InitState()

    // Screen brightness state
    private var oldScreenBrightness: Int = 0
    private var oldIsAutomaticBrightness: Boolean = false

    private val filterIsOn: Boolean
        get() = mCurrentState.filterIsOn

    init {
        oldScreenBrightness = -1
        mCurrentState.onScreenFilterCommand(ScreenFilterService.Command.OFF)
        if (DEBUG) Log.d(TAG, "Filter is on? $filterIsOn")
        Config.filterIsOn = filterIsOn
    }

    private fun refreshForegroundNotification() {
        val context = mView.context
        val profilesModel = ProfilesModel(context)

        val title = context.getString(R.string.app_name)
        val color = ContextCompat.getColor(context, R.color.color_primary)
        val smallIconResId = R.drawable.notification_icon_half_moon
        val nextProfile = ProfilesHelper.getProfileName(profilesModel, Config.profile, context)

        val offOrOnDrawableResId: Int
        val offOrOnCommand: Intent
        val offOrOnActionText: String
            Log.d(TAG, "Creating notification while in " + mCurrentState)
        if (filterIsOn) {
            offOrOnDrawableResId = R.drawable.ic_stop
            offOrOnCommand = ScreenFilterService.command(ScreenFilterService.Command.OFF)
            offOrOnActionText = context.getString(R.string.action_off)
        } else {
            offOrOnDrawableResId = R.drawable.ic_play
            offOrOnCommand = ScreenFilterService.command(ScreenFilterService.Command.ON)
            offOrOnActionText = context.getString(R.string.action_on)
        }

        val mainActivityIntent = Intent(context, MainActivity::class.java)
        mainActivityIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        val offOrOnPI = PendingIntent.getService(context, REQUEST_CODE_ACTION_OFF_OR_ON,
                offOrOnCommand, PendingIntent.FLAG_UPDATE_CURRENT)

        val settingsPI = PendingIntent.getActivity(context, REQUEST_CODE_ACTION_SETTINGS,
                mainActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val nextProfileIntent = Intent(context, NextProfileCommandReceiver::class.java)

        val nextProfilePI = PendingIntent.getBroadcast(context, REQUEST_CODE_NEXT_PROFILE,
                nextProfileIntent, 0)

        val nb = NotificationCompat.Builder(mContext)
        val nextProfileActionText = context.getString(R.string.action_next_profile)
        nb.setSmallIcon(smallIconResId)
          .setContentTitle(title)
          .setContentText(nextProfile)
          .setColor (color)
          .setContentIntent(settingsPI)
          .addAction(offOrOnDrawableResId, offOrOnActionText, offOrOnPI)
          .addAction(R.drawable.ic_next_profile, nextProfileActionText, nextProfilePI)
          .setPriority(Notification.PRIORITY_MIN)

        if (filterIsOn) {
            Log.d(TAG, "Creating a persistent notification")
            mServiceController.startForeground(NOTIFICATION_ID, nb.build())
        } else {
            val nm = mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID, nb.build())
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

    private fun reLayoutScreenFilter() {
        mWindowViewManager.reLayoutWindow(createFilterLayoutParams())
    }
    //endregion

    //region events
    @Subscribe
    fun onPowerStateChanged(event: filterIsOnChanged) {
        updateWidgets()
        // If an app like Tasker wants to do something each time
        // Red Moon is toggled, it can listen for this event
        val intent = Intent()
        intent.action = BROADCAST_ACTION
        intent.putExtra(BROADCAST_FIELD, filterIsOn)
        mContext.sendBroadcast(intent)
    }
    @Subscribe
    fun onDimLevelChanged(event: dimChanged) {
        mCurrentState.onDimLevelChanged()
    }
    @Subscribe
    fun onIntensityLevelChanged(event: intensityChanged) {
        mCurrentState.onIntensityLevelChanged()
    }

    @Subscribe
    fun onColorChanged(event: colorChanged) {
        mCurrentState.onColorChanged()
    }

    @Subscribe
    fun onLowerBrightnessChanged(event: lowerBrightnessChanged) {
        if (Config.hasWriteSettingsPermission) {
            mCurrentState.onLowerBrightnessChanged()
        } else {
            EventBus.getDefault().post(changeBrightnessDenied())
        }
    }

    @Subscribe
    fun onProfileChanged(event: profileChanged) {
        refreshForegroundNotification()
    }

    @Subscribe
    fun onSecureSuspendChanged(event: secureSuspendChanged) {
        mCurrentState.onSecureSuspendChanged()
    }
    //endregion

    //region ScreenStateListener
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
    //endregion

    //region OnOrientationChangeListener
    override fun onPortraitOrientation() {
        reLayoutScreenFilter()
    }

    override fun onLandscapeOrientation() {
        reLayoutScreenFilter()
    }
    //endregion

    fun onScreenFilterCommand(command: ScreenFilterService.Command) {
        if (DEBUG) Log.i(TAG, "Handling command " + command.name + " in current state: " + mCurrentState)

        mCurrentState.onScreenFilterCommand(command)
    }

    @Suppress("DEPRECATION")
    fun startAppMonitoring() {
        if (DEBUG) Log.i(TAG, "Starting app monitoring")
        val powerManager = mContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        screenOff = if (Config.atLeastAPI(20)) @TargetApi(20){!powerManager.isInteractive}
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

    fun updateWidgets() {
        //Broadcast to keep appwidgets in sync
        if (DEBUG) Log.i(TAG, "Sending update broadcast")
        val updateAppWidgetIntent = Intent(mContext, SwitchAppWidgetProvider::class.java)
        updateAppWidgetIntent.action = SwitchAppWidgetProvider.ACTION_UPDATE
        updateAppWidgetIntent.putExtra(SwitchAppWidgetProvider.EXTRA_POWER, filterIsOn)
        mContext.sendBroadcast(updateAppWidgetIntent)
    }

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

    private abstract inner class State {
        abstract val filterIsOn: Boolean
        abstract fun onActivation(prevState: State)

        internal fun openScreenFilter() {
            mWindowViewManager.openWindow(createFilterLayoutParams())
        }

        open internal fun closeScreenFilter() {
            mWindowViewManager.closeWindow()
        }

        open fun onScreenFilterCommand(command: ScreenFilterService.Command) {
            moveToState(nextState(command))
        }

        open internal val nextState: (ScreenFilterService.Command) -> State = {
            when (it) {
                ScreenFilterService.Command.OFF           -> mOffState
                ScreenFilterService.Command.ON            -> mOnState
                ScreenFilterService.Command.SHOW_PREVIEW  -> mPreviewState
                ScreenFilterService.Command.START_SUSPEND -> mSuspendState
                else -> this
            }
        }

        protected fun moveToState(newState: State) {
            if (DEBUG) Log.i(TAG, "Transitioning from $mCurrentState to $newState")
            if (Config.hasOverlayPermission) {
                if (newState === this) { return }
                if (DEBUG) Log.i(TAG, "Passed check for same state.")
                mCurrentState = newState
                mCurrentState.onActivation(this)
            } else {
                if (DEBUG) Log.i(TAG, "No overlay permission.")
                EventBus.getDefault().post(overlayPermissionDenied())
            }
        }

        open fun onDimLevelChanged() {}
        open fun onColorChanged() {}
        open fun onIntensityLevelChanged() {}
        open fun onLowerBrightnessChanged() {}
        open fun onSecureSuspendChanged() {}

        override fun toString(): String {
            return javaClass.simpleName
        }
    }
    
    private inner class InitState : State() {
        override val filterIsOn = false
        override fun onActivation(prevState: State) {}
    }

    private inner class OnState : State() {
        override val filterIsOn = true
        override fun onActivation(prevState: State) {
            refreshForegroundNotification()
            openScreenFilter()
            Config.filterIsOn = filterIsOn
            mView.animateDimLevel(Config.dim, null)
            mView.animateIntensityLevel(Config.intensity, null)

            if (Config.lowerBrightness) {
                saveOldBrightnessState()
                setBrightnessState(0, false, mContext)
            }
            if (Config.secureSuspend) startAppMonitoring()
        }

        override fun closeScreenFilter() {
            if (DEBUG) Log.i(TAG, "Filter is turning on again; don't close it.")
        }

        override val nextState: (ScreenFilterService.Command) -> State = {
            if (it == ScreenFilterService.Command.TOGGLE) mOffState
            else super.nextState(it)
        }

        override fun onDimLevelChanged() {
            val dim = Config.dim
            mView.cancelDimAnimator()
            mView.filterDimLevel = dim
        }
        override fun onIntensityLevelChanged() {
            val intensityLevel = Config.intensity
            mView.cancelIntensityAnimator()
            mView.filterIntensityLevel = intensityLevel
        }
        override fun onColorChanged() {
            mView.colorTempProgress = Config.color
        }
        override fun onLowerBrightnessChanged() {
            val lowerBrightness = Config.lowerBrightness
            if (DEBUG) Log.i(TAG, "Lower brightness flag changed to: " + lowerBrightness)
            if (lowerBrightness) {
                saveOldBrightnessState()
                setBrightnessState(0, false, mContext)
            } else {
                restoreBrightnessState()
            }
        }
        override fun onSecureSuspendChanged() {
            if (Config.secureSuspend) startAppMonitoring()
            else stopAppMonitoring()
        }
    }

    private inner class OffState : State() {
        override val filterIsOn = false
        override fun onActivation(prevState: State) {
            Config.filterIsOn = filterIsOn
            mServiceController.stopForeground(false)
            refreshForegroundNotification()

            if (prevState === mPreviewState) {
                closeScreenFilter()
            } else {
                mView.animateIntensityLevel(ScreenFilterView.MIN_INTENSITY, null)
                mView.animateDimLevel(ScreenFilterView.MIN_DIM, object : AbstractAnimatorListener() {
                    override fun onAnimationCancel(animator: Animator) { mCurrentState.closeScreenFilter() }
                    override fun onAnimationEnd(animator: Animator) { mCurrentState.closeScreenFilter() }
                })
            }

            if (Config.lowerBrightness) restoreBrightnessState()
            if (Config.secureSuspend) stopAppMonitoring()
        }

        override val nextState: (ScreenFilterService.Command) -> State = {
            if (it == ScreenFilterService.Command.TOGGLE) mOnState
            else super.nextState(it)
        }
    }

    /* This State is used to present the filter to the user when (s)he
     * is holding one of the seekbars to adjust the filter. It turns
     * on the filter and saves what state it should be when it will be
     * turned off.
     */
    private inner class PreviewState : State() {
        lateinit var stateToReturnTo: State
        var pressesActive: Int = 0

        override val filterIsOn: Boolean
            get() = stateToReturnTo.filterIsOn

        override fun onActivation(prevState: State) {
            stateToReturnTo = prevState
            Config.filterIsOn = filterIsOn
            pressesActive = 1
            refreshForegroundNotification()
            // If the animators are not canceled, preview does not work when filter is turning off
            mView.cancelDimAnimator()
            mView.cancelIntensityAnimator()
            openScreenFilter()

            mView.filterDimLevel = Config.dim
            mView.filterIntensityLevel = Config.intensity
            mView.colorTempProgress = Config.color
        }

        override fun onScreenFilterCommand(command: ScreenFilterService.Command) {
            if (DEBUG) Log.d(TAG, "Preview, got command: " + command.name)
            when (command) {
                ScreenFilterService.Command.SHOW_PREVIEW -> {
                    pressesActive++
                    if (DEBUG) Log.d(TAG, String.format("%d presses active", pressesActive))
                }
                ScreenFilterService.Command.HIDE_PREVIEW -> {
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
                else -> stateToReturnTo = nextState(command)
            }
        }

        override val nextState: (ScreenFilterService.Command) -> State = {
            stateToReturnTo.nextState(it)
        }

        override fun onDimLevelChanged() {
            val dim = Config.dim
            mView.cancelDimAnimator()
            mView.filterDimLevel = dim
        }
        override fun onIntensityLevelChanged() {
            val intensityLevel = Config.intensity
            mView.cancelDimAnimator()
            mView.filterIntensityLevel = intensityLevel
        }
        override fun onColorChanged() {
            mView.colorTempProgress = Config.color
        }
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
        
        override val filterIsOn: Boolean
            get() = stateToReturnTo.filterIsOn

        override fun onActivation(prevState: State) {
            stateToReturnTo = prevState
            mServiceController.stopForeground(false)
            closeScreenFilter()
            refreshForegroundNotification()
        }

        override fun onScreenFilterCommand(command: ScreenFilterService.Command) {
            if (DEBUG) Log.d(TAG, "In Suspend, got command: " + command.name)
            when (command) {
                ScreenFilterService.Command.STOP_SUSPEND -> moveToState(stateToReturnTo)
                ScreenFilterService.Command.START_SUSPEND -> {}
                else -> stateToReturnTo = nextState(command)
            }
        }

        override val nextState: (ScreenFilterService.Command) -> State = {
            stateToReturnTo.nextState(it)
        }
    }

    companion object {
        private const val TAG = "ScreenFilterPresenter"
        private const val DEBUG = true

        const val NOTIFICATION_ID = 1
        private const val REQUEST_CODE_ACTION_SETTINGS = 1000
        private const val REQUEST_CODE_ACTION_OFF_OR_ON = 3000
        private const val REQUEST_CODE_NEXT_PROFILE = 4000

        const val FADE_DURATION_MS = 1000

        const val BROADCAST_ACTION = "com.jmstudios.redmoon.RED_MOON_TOGGLED"
        const val BROADCAST_FIELD = "jmstudios.bundle.key.FILTER_IS_ON"

        // Statically used by BootReceiver
        fun setBrightnessState(brightness: Int, automatic: Boolean, context: Context) {
            if (DEBUG) Log.i(TAG, "Setting brightness to: $brightness, automatic: $automatic")
            @TargetApi(23) if (Config.atLeastAPI(23) && !Settings.System.canWrite(context)) return
            if (brightness >= 0) {
                val resolver = context.contentResolver
                Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
                Settings.System.putInt(resolver, "screen_brightness_mode", if (automatic) 1 else 0)
            }
        }
    }
}
