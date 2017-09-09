/*
 * Copyright (c) 2016 Marien Raat <marienraat@riseup.net>
 * Copyright (c) 2017  Stephen Michel <s@smichel.me>
 * SPDX-License-Identifier: GPL-3.0+
 */
package com.jmstudios.redmoon.ui

import android.preference.Preference
import android.preference.PreferenceFragment

fun PreferenceFragment.pref(resId: Int): Preference {
    return preferenceScreen.findPreference(getString(resId))
}
