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
package com.jmstudios.redmoon.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ScreenStateReceiver extends BroadcastReceiver {
    private static final String TAG = "ScreenStateReceiver";
    private static final boolean DEBUG = true;

    private ScreenStateListener mListener;

    public interface ScreenStateListener {
        public abstract void onScreenTurnedOn();
        public abstract void onScreenTurnedOff();
    }

    public ScreenStateReceiver(ScreenStateListener listener) {
        mListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (DEBUG) Log.i(TAG, "Intent received");

        if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
            if (DEBUG) Log.i(TAG, "Screen turned on");

            if (mListener != null)
                mListener.onScreenTurnedOn();
        } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
            if (DEBUG) Log.i(TAG, "Screen turned off");

            if (mListener != null)
                mListener.onScreenTurnedOff();
        }
    }
}
