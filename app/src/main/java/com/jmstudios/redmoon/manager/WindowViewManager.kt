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

import android.view.View
import android.view.WindowManager

/**
 * Convenience class that wraps [WindowManager] for dependency injection.
 */
class WindowViewManager(private val mWindowManager: WindowManager) {

    /**
     * Creates and opens a new Window to display `view`.
     * @param view the view to render in the new Window.
     * *
     * @param wlp the [android.view.WindowManager.LayoutParams] to use when laying out the window.
     */
    fun openWindow(view: View, wlp: WindowManager.LayoutParams) {
        mWindowManager.addView(view, wlp)
    }

    /**
     * Triggers a Window undergo a screen measurement and layout pass with the provided
     * [android.view.WindowManager.LayoutParams].
     *
     * @param view the Window containing this view will have its LayoutParams set to `wlp`.
     * *
     * @param wlp the new LayoutParams to set on the Window.
     */
    fun reLayoutWindow(view: View, wlp: WindowManager.LayoutParams) {
        mWindowManager.updateViewLayout(view, wlp)
    }

    /**
     * Closes the Window that is currently displaying `view`.
     * @param view the Window containing this view will be closed.
     */
    fun closeWindow(view: View) {
        mWindowManager.removeView(view)
    }
}
