/*
 * Copyright (c) 2016 Marien Raat <marienraat@riseup.net>
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
 */
package com.jmstudios.redmoon.fragment

import android.os.Bundle
import android.preference.PreferenceFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import org.greenrobot.eventbus.EventBus

abstract class EventPreferenceFragment : PreferenceFragment() {
    private lateinit var mView: View

    // This can be deleted if we don't end up using showHelpSnackBar, commented out below
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val v = super.onCreateView(inflater, container, savedInstanceState)
        mView = v
        return v
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    /* private fun showHelpSnackbar() { */
    /*     mHelpSnackbar = Snackbar.make(mView, activity.getString(R.string.help_snackbar_text), */
    /*             Snackbar.LENGTH_INDEFINITE) */

    /*     if (Config.darkThemeFlag) { */
    /*         val group = mHelpSnackbar.view as ViewGroup */
    /*         group.setBackgroundColor(ContextCompat.getColor(activity, R.color.snackbar_color_dark_theme)) */

    /*         val snackbarTextId = android.support.design.R.id.snackbar_text */
    /*         val textView = group.findViewById(snackbarTextId) as TextView */
    /*         textView.setTextColor(ContextCompat.getColor(activity, R.color.text_color_dark_theme)) */
    /*     } */

    /*     mHelpSnackbar.show() */
    /* } */
}
