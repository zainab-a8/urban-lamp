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

package com.jmstudios.redmoon.helper

import android.Manifest
import android.content.pm.PackageManager
import android.provider.Settings
import android.support.v4.app.ActivityCompat

import com.jmstudios.redmoon.application.RedMoonApplication
import com.jmstudios.redmoon.model.Config

object Util {
    private val mContext = RedMoonApplication.app

    fun atLeastAPI(api: Int): Boolean = android.os.Build.VERSION.SDK_INT >= api
    fun getString (resId : Int) : String = mContext.getString(resId)

    val hasWriteSettingsPermission: Boolean
        get() = if (atLeastAPI(23)) Settings.System.canWrite(mContext)
                else true

    private val lp = Manifest.permission.ACCESS_COARSE_LOCATION
    private val granted = PackageManager.PERMISSION_GRANTED
    val hasLocationPermission: Boolean
        get() = ActivityCompat.checkSelfPermission(mContext, lp) == granted

    val automaticTurnOnTime: String
        get() = if (Config.useLocation) Config.sunsetTime else Config.customTurnOnTime

    val automaticTurnOffTime: String
        get() = if (Config.useLocation) Config.sunriseTime else Config.customTurnOffTime
}
