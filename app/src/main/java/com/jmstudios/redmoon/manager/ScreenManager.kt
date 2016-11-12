/* Copyright (c) 2015 Chris Nguyen
**
** Permission to use, copy, modify, and/or distribute this software for
** any purpose with or without fee is hereby granted, provided that the
** above copyright notice and this permission notice appear in all copies.
**
** THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL
** WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED
** WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR
** BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES
** OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,
** WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION,
** ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS
** SOFTWARE.
*/
package com.jmstudios.redmoon.manager

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.WindowManager

class ScreenManager(context: Context, private val mWindowManager: WindowManager) {

    private val mResources: Resources

    private var mStatusBarHeight = -1
    private var mNavigationBarHeight = -1

    init {
        mResources = context.resources
    }

    val screenHeight: Int
        get() {
            val display = mWindowManager.defaultDisplay
            val dm = DisplayMetrics()
            display.getRealMetrics(dm)

            var screenHeight = dm.heightPixels + statusBarHeightPx

            if (inPortrait()) {
                screenHeight += navigationBarHeightPx
            }

            return screenHeight
        }

    val statusBarHeightPx: Int
        get() {
            if (mStatusBarHeight == -1) {
                val statusBarHeightId = mResources.getIdentifier("status_bar_height", "dimen", "android")

                if (statusBarHeightId > 0) {
                    mStatusBarHeight = mResources.getDimensionPixelSize(statusBarHeightId)
                    if (DEBUG) Log.i(TAG, "Found Status Bar Height: " + mStatusBarHeight)
                } else {
                    mStatusBarHeight = dpToPx(DEFAULT_STATUS_BAR_HEIGHT_DP.toFloat()).toInt()
                    if (DEBUG) Log.i(TAG, "Using default Status Bar Height: " + mStatusBarHeight)
                }
            }

            return mStatusBarHeight
        }

    val navigationBarHeightPx: Int
        get() {
            if (mNavigationBarHeight == -1) {
                val navBarHeightId = mResources.getIdentifier("navigation_bar_height", "dimen", "android")

                if (navBarHeightId > 0) {
                    mNavigationBarHeight = mResources.getDimensionPixelSize(navBarHeightId)
                    if (DEBUG) Log.i(TAG, "Found Navigation Bar Height: " + mNavigationBarHeight)
                } else {
                    mNavigationBarHeight = dpToPx(DEFAULT_NAV_BAR_HEIGHT_DP.toFloat()).toInt()
                    if (DEBUG) Log.i(TAG, "Using default Navigation Bar Height: " + mNavigationBarHeight)
                }
            }

            return mNavigationBarHeight
        }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, mResources.displayMetrics)
    }

    private fun inPortrait(): Boolean {
        return mResources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    }

    companion object {
        private val TAG = "ScreenManager"
        private val DEBUG = false

        private val DEFAULT_NAV_BAR_HEIGHT_DP = 48
        private val DEFAULT_STATUS_BAR_HEIGHT_DP = 25
    }
}
