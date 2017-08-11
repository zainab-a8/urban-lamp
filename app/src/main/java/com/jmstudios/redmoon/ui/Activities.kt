/*
 * Copyright (c) 2016 Marien Raat <marienraat@riseup.net>
 * Copyright (c) 2017  Stephen Michel <s@smichel.me>
 * SPDX-License-Identifier: GPL-3.0+
 */
package com.jmstudios.redmoon.ui

class SecureSuspendActivity : ThemedAppCompatActivity() {
    override val fragment = SecureSuspendFragment()
    override val tag = "jmstudios.fragment.tag.SECURE_SUSPEND"
}

class TimeToggleActivity : ThemedAppCompatActivity() {
    override val fragment = TimeToggleFragment()
    override val tag = "jmstudios.fragment.tag.TIME_TOGGLE"
}

class AboutActivity : ThemedAppCompatActivity() {
    override val fragment = AboutFragment()
    override val tag = "jmstudios.fragment.tag.ABOUT"
}
