/*
 * Copyright (c) 2016  Marien Raat <marienraat@riseup.net>
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
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *     Copyright (c) 2015 Chris Nguyen
 *
 *     Permission to use, copy, modify, and/or distribute this software
 *     for any purpose with or without fee is hereby granted, provided
 *     that the above copyright notice and this permission notice appear
 *     in all copies.
 *
 *     THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL
 *     WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED
 *     WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE
 *     AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR
 *     CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS
 *     OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *     NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN
 *     CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package com.jmstudios.redmoon.model

import android.preference.PreferenceManager

import com.jmstudios.redmoon.R

import com.jmstudios.redmoon.application.RedMoonApplication
import com.jmstudios.redmoon.fragment.TimeToggleFragment
import com.jmstudios.redmoon.preference.ColorSeekBarPreference
import com.jmstudios.redmoon.preference.DimSeekBarPreference
import com.jmstudios.redmoon.preference.IntensitySeekBarPreference
import com.jmstudios.redmoon.helper.Util

/**
 * This singleton provides access to get and set Shades settings
 */
object Config {
    private val mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(RedMoonApplication.app)

    var filterIsOn: Boolean
        get() = mSharedPrefs.getBoolean(Util.getString(R.string.pref_key_filter_is_on), false)
        set(isOn) = mSharedPrefs.edit()
                                .putBoolean(Util.getString(R.string.pref_key_filter_is_on), isOn)
                                .apply()
    
    var dim: Int
        get() = mSharedPrefs.getInt(Util.getString(R.string.pref_key_dim),
                                    DimSeekBarPreference.DEFAULT_VALUE)
        set(dim) = mSharedPrefs.edit().putInt(Util.getString(R.string.pref_key_dim), dim).apply()

    var intensity: Int
        get() = mSharedPrefs.getInt(Util.getString(R.string.pref_key_intensity),
                                    IntensitySeekBarPreference.DEFAULT_VALUE)
        set(i) = mSharedPrefs.edit()
                             .putInt(Util.getString(R.string.pref_key_intensity), i)
                             .apply()

    var color: Int
        get() = mSharedPrefs.getInt(Util.getString(R.string.pref_key_color),
                                    ColorSeekBarPreference.DEFAULT_VALUE)
        set(c) = mSharedPrefs.edit().putInt(Util.getString(R.string.pref_key_color), c).apply()

    val darkThemeFlag: Boolean
        get() = mSharedPrefs.getBoolean(Util.getString(R.string.pref_key_dark_theme), false)

    val lowerBrightness: Boolean
        get() = mSharedPrefs.getBoolean(Util.getString(R.string.pref_key_lower_brightness), false)

    val automaticFilter: Boolean
        get() = mSharedPrefs.getBoolean(Util.getString(R.string.pref_key_time_toggle), false)
    
    val dimButtons: Boolean
        get() = mSharedPrefs.getBoolean(Util.getString(R.string.pref_key_dim_buttons), true)

    
    var brightnessAutomatic: Boolean
        get() = mSharedPrefs.getBoolean(Util.getString(R.string.pref_key_brightness_automatic),true)
        set(auto) = mSharedPrefs.edit()
                                .putBoolean(Util.getString(R.string.pref_key_brightness_automatic),
                                            auto)
                                .apply()
    
    var brightnessLevel: Int
        get() = mSharedPrefs.getInt(Util.getString(R.string.pref_key_brightness_level), 0)
        set(level) = mSharedPrefs.edit()
                                 .putInt(Util.getString(R.string.pref_key_brightness_level), level)
                                 .apply()
    
    var profile: Int
        get() = mSharedPrefs.getInt(Util.getString(R.string.pref_key_profile_spinner), 1)
        set(p) = mSharedPrefs.edit()
                             .putInt(Util.getString(R.string.pref_key_profile_spinner), p)
                             .apply()
    
    var ammountProfiles: Int
        get() = mSharedPrefs.getInt(Util.getString(R.string.pref_key_num_profiles), 3)
        set(num) = mSharedPrefs.edit()
                               .putInt(Util.getString(R.string.pref_key_num_profiles), num)
                               .apply()

    var introShown: Boolean
        get() = mSharedPrefs.getBoolean(Util.getString(R.string.pref_key_intro_shown), false)
        set(shown) = mSharedPrefs.edit()
                                 .putBoolean(Util.getString(R.string.pref_key_intro_shown), shown)
                                 .apply()
    
    val automaticSuspend: Boolean
        get() = mSharedPrefs.getBoolean(Util.getString(R.string.pref_key_automatic_suspend), false)

    
    var location: String
        get() = mSharedPrefs.getString(Util.getString(R.string.pref_key_location),
                                       TimeToggleFragment.DEFAULT_LOCATION)
        set(shown) = mSharedPrefs.edit()
                                 .putString(Util.getString(R.string.pref_key_location), shown)
                                 .apply()

    val useLocation: Boolean
        get() = mSharedPrefs.getBoolean(Util.getString(R.string.pref_key_use_location), false)

    val customTurnOnTime: String
        get() = mSharedPrefs.getString(Util.getString(R.string.pref_key_custom_turn_on_time), "22:00")

    val customTurnOffTime: String
        get() = mSharedPrefs.getString(Util.getString(R.string.pref_key_custom_turn_off_time), "06:00")

    var sunsetTime: String
        get() = mSharedPrefs.getString(Util.getString(R.string.pref_key_sunset_time), "19:30")
        set(time) = mSharedPrefs.edit()
                                .putString(Util.getString(R.string.pref_key_sunset_time), time)
                                .apply()

    var sunriseTime: String
        get() = mSharedPrefs.getString(Util.getString(R.string.pref_key_sunrise_time), "06:30")
        set(time) = mSharedPrefs.edit()
                                .putString(Util.getString(R.string.pref_key_sunrise_time), time)
                                .apply()
}
