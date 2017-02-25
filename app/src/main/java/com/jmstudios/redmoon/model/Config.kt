/*
 * Copyright (c) 2016  Marien Raat <marienraat@riseup.net>
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

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog

import com.jmstudios.redmoon.R

import com.jmstudios.redmoon.application.RedMoonApplication
import com.jmstudios.redmoon.event.locationPermissionDialogClosed
import com.jmstudios.redmoon.fragment.TimeToggleFragment
import com.jmstudios.redmoon.preference.ColorSeekBarPreference
import com.jmstudios.redmoon.preference.DimSeekBarPreference
import com.jmstudios.redmoon.preference.IntensitySeekBarPreference

import org.greenrobot.eventbus.EventBus

/**
 * TODO: Better comment.
 * This singleton provides a way to interact with settings and permissions
 */
object Config {
    //region utilities
    private val mContext = RedMoonApplication.app
    private val lp = Manifest.permission.ACCESS_FINE_LOCATION
    private val granted = PackageManager.PERMISSION_GRANTED
    //private val OVERLAY_PERMISSION_REQ_CODE = 1111
    private val LOCATION_PERMISSION_REQ_CODE = 2222

    val atLeastAPI: (Int) -> Boolean = { it <= android.os.Build.VERSION.SDK_INT }
    val belowAPI: (Int) -> Boolean = { it > android.os.Build.VERSION.SDK_INT }
    val getString: (Int) -> String = { mContext.getString(it) }

    val hasLocationPermission: Boolean
        get() = ContextCompat.checkSelfPermission(mContext, lp) == granted

    val hasWriteSettingsPermission: Boolean
        get() = if (atLeastAPI(23)) Settings.System.canWrite(mContext) else true

    val hasOverlayPermission: Boolean
        get() = if (atLeastAPI(23)) Settings.canDrawOverlays(mContext) else true

    val automaticTurnOnTime: String
        get() = if (useLocation) sunsetTime else customTurnOnTime

    val automaticTurnOffTime: String
        get() = if (useLocation) sunriseTime else customTurnOffTime

    fun requestLocationPermission(activity: Activity): Boolean {
        if (!hasLocationPermission) {
            val permission = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            ActivityCompat.requestPermissions(activity, permission, LOCATION_PERMISSION_REQ_CODE)
        }
        return hasLocationPermission
    }

    fun requestWriteSettingsPermission(context: Context): Boolean {
        if (!hasWriteSettingsPermission) @TargetApi(23) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                Uri.parse("package:" + context.packageName))
            val builder = AlertDialog.Builder(context)
            builder.setMessage(R.string.write_settings_dialog_message)
                   .setTitle(R.string.write_settings_dialog_title)
                   .setPositiveButton(R.string.ok_dialog) { dialog, id ->
                       context.startActivity(intent)
                   }.show()
        }
        return hasWriteSettingsPermission
    }

    fun requestOverlayPermission(context: Context): Boolean {
        if (!hasOverlayPermission) @TargetApi(23) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + context.packageName))
            val builder = AlertDialog.Builder(context)
            builder.setMessage(R.string.overlay_dialog_message)
                   .setTitle(R.string.overlay_dialog_title)
                   .setPositiveButton(R.string.ok_dialog) { dialog, id ->
                       context.startActivity(intent)
                   }.show()
        }
        return hasOverlayPermission
    }

    fun onRequestPermissionsResult(requestCode: Int) {
        if (requestCode == Config.LOCATION_PERMISSION_REQ_CODE) {
            EventBus.getDefault().post(locationPermissionDialogClosed())
        }
    }

    val activeTheme: Int
        get() = if (darkThemeFlag) { R.style.AppThemeDark } else { R.style.AppTheme }

    //endregion

    //region preferences
    private val mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext)

    var filterIsOn: Boolean
        get() = mSharedPrefs.getBoolean(getString(R.string.pref_key_filter_is_on), false)
        set(isOn) {
            mSharedPrefs.edit()
                        .putBoolean(getString(R.string.pref_key_filter_is_on), isOn)
                        .apply()
        }
    
    var dim: Int
        get() = mSharedPrefs.getInt(getString(R.string.pref_key_dim),
                                    DimSeekBarPreference.DEFAULT_VALUE)
        set(dim) = mSharedPrefs.edit().putInt(getString(R.string.pref_key_dim), dim).apply()

    var intensity: Int
        get() = mSharedPrefs.getInt(getString(R.string.pref_key_intensity),
                                    IntensitySeekBarPreference.DEFAULT_VALUE)
        set(i) = mSharedPrefs.edit()
                             .putInt(getString(R.string.pref_key_intensity), i)
                             .apply()

    var color: Int
        get() = mSharedPrefs.getInt(getString(R.string.pref_key_color),
                                    ColorSeekBarPreference.DEFAULT_VALUE)
        set(c) = mSharedPrefs.edit().putInt(getString(R.string.pref_key_color), c).apply()

    private val darkThemeFlag: Boolean
        get() = mSharedPrefs.getBoolean(getString(R.string.pref_key_dark_theme), false)

    val lowerBrightness: Boolean
        get() = mSharedPrefs.getBoolean(getString(R.string.pref_key_lower_brightness), false)

    val timeToggle: Boolean
        get() = mSharedPrefs.getBoolean(getString(R.string.pref_key_time_toggle), false)
    
    val dimButtons: Boolean
        get() = mSharedPrefs.getBoolean(getString(R.string.pref_key_dim_buttons), true)

    
    var brightnessAutomatic: Boolean
        get() = mSharedPrefs.getBoolean(getString(R.string.pref_key_brightness_automatic),true)
        set(auto) = mSharedPrefs.edit()
                                .putBoolean(getString(R.string.pref_key_brightness_automatic), auto)
                                .apply()
    
    var brightnessLevel: Int
        get() = mSharedPrefs.getInt(getString(R.string.pref_key_brightness_level), 0)
        set(level) = mSharedPrefs.edit()
                                 .putInt(getString(R.string.pref_key_brightness_level), level)
                                 .apply()
    
    var profile: Int
        get() = mSharedPrefs.getInt(getString(R.string.pref_key_profile_spinner), 1)
        set(p) = mSharedPrefs.edit()
                             .putInt(getString(R.string.pref_key_profile_spinner), p)
                             .apply()
    
    var amountProfiles: Int
        get() = mSharedPrefs.getInt(getString(R.string.pref_key_num_profiles), 3)
        set(num) = mSharedPrefs.edit()
                               .putInt(getString(R.string.pref_key_num_profiles), num)
                               .apply()

    var introShown: Boolean
        get() = mSharedPrefs.getBoolean(getString(R.string.pref_key_intro_shown), false)
        set(shown) = mSharedPrefs.edit()
                                 .putBoolean(getString(R.string.pref_key_intro_shown), shown)
                                 .apply()
    
    val secureSuspend: Boolean
        get() = mSharedPrefs.getBoolean(getString(R.string.pref_key_secure_suspend), false)

    var location: String
        get() = mSharedPrefs.getString(getString(R.string.pref_key_location),
                                       TimeToggleFragment.DEFAULT_LOCATION)
        set(shown) = mSharedPrefs.edit()
                                 .putString(getString(R.string.pref_key_location), shown)
                                 .apply()

    val useLocation: Boolean
        get() = mSharedPrefs.getBoolean(getString(R.string.pref_key_use_location), false)

    val customTurnOnTime: String
        get() = mSharedPrefs.getString(getString(R.string.pref_key_custom_turn_on_time), "22:00")

    val customTurnOffTime: String
        get() = mSharedPrefs.getString(getString(R.string.pref_key_custom_turn_off_time), "06:00")

    var sunsetTime: String
        get() = mSharedPrefs.getString(getString(R.string.pref_key_sunset_time), "19:30")
        set(time) = mSharedPrefs.edit()
                                .putString(getString(R.string.pref_key_sunset_time), time)
                                .apply()

    var sunriseTime: String
        get() = mSharedPrefs.getString(getString(R.string.pref_key_sunrise_time), "06:30")
        set(time) = mSharedPrefs.edit()
                                .putString(getString(R.string.pref_key_sunrise_time), time)
                                .apply()
    //endregion
}
