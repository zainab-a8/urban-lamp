/*
 * Copyright (c) 2016 Marien Raat <marienraat@riseup.net>
 * Copyright (c) 2017  Stephen Michel <s@smichel.me>
 * SPDX-License-Identifier: GPL-3.0+
 */
package com.jmstudios.redmoon

import android.app.AlertDialog
import android.preference.PreferenceFragment
import android.content.Intent
import android.os.Bundle
import android.preference.Preference
import android.preference.SwitchPreference
import android.provider.Settings

import com.jmstudios.redmoon.securesuspend.CurrentAppChecker
import com.jmstudios.redmoon.R
import com.jmstudios.redmoon.util.*

class SecureSuspendFragment : BaseFragment() {

    private val switchBar: SwitchPreference
        get() = pref(R.string.pref_key_secure_suspend)

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("onCreate()")
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.secure_suspend_preferences)

        setSwitchBarTitle(switchBar.isChecked)

        switchBar.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, on ->
                setSwitchBarTitle(on as Boolean)
                true
            }
    }

    private fun setSwitchBarTitle(on: Boolean) {
        val text = if (on) R.string.text_switch_on else R.string.text_switch_off
        switchBar.setTitle(text)
    }

    companion object : Logger()
}
