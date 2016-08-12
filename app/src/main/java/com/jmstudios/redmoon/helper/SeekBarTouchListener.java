/*
 * Copyright (c) 2016 Marien Raat <marienraat@riseup.net>
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
 */
package com.jmstudios.redmoon.helper;

import android.view.MotionEvent;
import android.view.View;
import android.util.Log;
import android.content.Intent;

import com.jmstudios.redmoon.helper.FilterCommandFactory;
import com.jmstudios.redmoon.helper.FilterCommandSender;
import com.jmstudios.redmoon.service.ScreenFilterService;

/* This is a touch listener used for the three bars that control the
 * filter. It sends a command to the presenter when touched down,
 * after which the presenter will turn on the filter if it isn't
 * on. When the touch is released it will tell the presenter to
 * restore the filter again.
 */
public class SeekBarTouchListener implements View.OnTouchListener {
    private static final String TAG = "SeekBarTouchListener";
    private static final boolean DEBUG = false;

    @Override
    public boolean onTouch(View v, MotionEvent motionEvent) {
        FilterCommandSender commandSender = new FilterCommandSender(v.getContext());
        FilterCommandFactory commandFactory = new FilterCommandFactory(v.getContext());

        switch (motionEvent.getAction()) {
        case MotionEvent.ACTION_DOWN:
            Log.i(TAG, "Touch down on a seek bar");

            Intent showPreviewCommand = commandFactory.createCommand
                (ScreenFilterService.COMMAND_SHOW_PREVIEW);
            commandSender.send(showPreviewCommand);
            break;

        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            Log.d(TAG, "Released a seek bar");

            Intent hidePreviewCommand = commandFactory.createCommand
                (ScreenFilterService.COMMAND_HIDE_PREVIEW);
            commandSender.send(hidePreviewCommand);
            break;
        }
        return false;
    }
}
