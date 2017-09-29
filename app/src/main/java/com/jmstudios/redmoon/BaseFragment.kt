/*
 * Copyright (c) 2017 Stephen Michel <s@smichel.me>
 * SPDX-License-Identifier: GPL-3.0+
 */
package com.jmstudios.redmoon

import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.TwoStatePreference
import android.support.design.widget.BaseTransientBottomBar.BaseCallback
import android.support.design.widget.Snackbar
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.jmstudios.redmoon.R

import com.jmstudios.redmoon.model.Profile
import com.jmstudios.redmoon.model.Config
import com.jmstudios.redmoon.ui.preference.SeekBarPreference
import com.jmstudios.redmoon.ui.preference.ProfileSelectorPreference
import com.jmstudios.redmoon.util.*

abstract class BaseFragment : PreferenceFragment() {

    private lateinit var mSnackbar: Snackbar
    private lateinit var padding: Preference

    private val needsPermissions: Boolean
        get() = !Permission.Overlay.isGranted
             || (Config.scheduleOn && Config.useLocation && !Permission.Location.isGranted)
             || (Config.secureSuspend && !Permission.UsageStats.isGranted)
             || (Config.lowerBrightness && !Permission.WriteSettings.isGranted)

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("onCreate()")
        super.onCreate(savedInstanceState)

        padding = Preference(activity).apply {
            layoutResource = R.layout.preference_padding
            isSelectable = false
            order = 100 // always at bottom
        }
    }

    override fun onStart() {
        Log.i("onStart")
        super.onStart()

        mSnackbar = Snackbar.make(view, R.string.permission_snackbar_text, Snackbar.LENGTH_INDEFINITE).apply {
            setAction(R.string.permission_snackbar_action) {
                Log.i("snackbar tapped")
            }
            addCallback(object: BaseCallback<Snackbar>() {
                override fun onShown(s: Snackbar) {
                    Log.i("onShown()")
                    preferenceScreen.addPreference(padding)
                }
                override fun onDismissed(s: Snackbar, event: Int) {
                    Log.i("onDismissed()")
                    preferenceScreen.removePreference(padding)
                }
            })
        }

        if (Config.darkThemeFlag) {
            val group = mSnackbar.view as ViewGroup
            group.setBackgroundColor(getColor(R.color.snackbar_color_dark_theme))

            val snackbarTextId = android.support.design.R.id.snackbar_text
            val textView = group.findViewById<TextView>(snackbarTextId)
            textView.setTextColor(getColor(R.color.text_color_dark_theme))
        }

        if (needsPermissions) {
            mSnackbar.show()
        } else {
            mSnackbar.dismiss()
        }
    }

    companion object: Logger()
}

