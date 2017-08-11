/*
 * Copyright (c) 2016 Marien Raat <marienraat@riseup.net>
 * Copyright (c) 2017 Stephen Michel <s@smichel.me>
 * SPDX-License-Identifier: GPL-3.0+
 */
package com.jmstudios.redmoon.model

import android.content.Context
import android.content.SharedPreferences

import com.jmstudios.redmoon.R

import com.jmstudios.redmoon.util.*

private const val PREFERENCE_NAME = "com.jmstudios.redmoon.PROFILES_PREFERENCE"
private const val MODE = Context.MODE_PRIVATE

private val defaultProfiles: Map<Profile, String> = mapOf(
        Profile(10, 30, 40, false) to getString(R.string.filter_name_default),
        Profile(20, 60, 78, false) to getString(R.string.filter_name_bed_reading),
        Profile(0, 0, 60, false) to getString(R.string.filter_name_dim_only))

private val prefs: SharedPreferences
    get() = appContext.getSharedPreferences(PREFERENCE_NAME, MODE)

private var modelOutdated: Boolean = true

private val _model: Map<Profile, String>
    get() = prefs.all.mapKeys { (k, _) -> Profile.parse(k) }.mapValues { (_, v) -> v as String }

private var model: Map<Profile, String> = _model
    get() {
        if (modelOutdated) {
            field = _model
            modelOutdated = false
        }
        return field
    }

object ProfilesModel : Map<Profile, String> by model {
    val profiles: List<Profile>
        get() = ProfilesModel.keys.sorted()

    fun indexOf(profile: Profile): Int {
        return profiles.indexOf(profile)
    }

    fun profileAfter(profile: Profile): Profile = when {
        indexOf(profile) < profiles.lastIndex -> profiles[indexOf(profile) + 1]
        Config.custom.isSaved -> profiles[0]
        else -> Config.custom
    }

    val Profile.isSaved: Boolean
        get() = ProfilesModel.containsKey(this)

    fun save(profile: Profile, name: String) {
        if (!profile.isSaved || this.containsValue(name)) {
            prefs.edit().putString(profile.toString(), name).apply()
            modelOutdated = true
            EventBus.post(profilesUpdated())
        }
    }

    fun delete(profile: Profile) {
        profile.let {
            if (it.isSaved) {
                Config.custom = it
                prefs.edit().remove(it.toString()).apply()
                modelOutdated = true
                EventBus.post(profilesUpdated())
            }
        }
    }

    fun restoreDefaultProfiles() {
        val editor = prefs.edit()
        defaultProfiles.forEach { profile, name ->
            editor.putString(profile.toString(), name)
        }
        editor.apply()
        modelOutdated = true
        EventBus.post(profilesUpdated())
    }
}
