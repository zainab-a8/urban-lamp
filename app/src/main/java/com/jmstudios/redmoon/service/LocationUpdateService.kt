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
 *     Copyright (c) 2016 Zoraver <https://github.com/Zoraver>
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

package com.jmstudios.redmoon.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log

import com.jmstudios.redmoon.application.RedMoonApplication
import com.jmstudios.redmoon.event.*
import com.jmstudios.redmoon.model.Config

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator
import org.greenrobot.eventbus.EventBus

import java.util.*

class LocationUpdateService: Service(), LocationListener {

    private val mContext = RedMoonApplication.app

    private val mLocationManager: LocationManager
        get() = mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val locationServicesEnabled: Boolean
        get() = mLocationManager.isProviderEnabled(locationProvider)
                    
    private val lastKnownLocation: Location?
        get() = mLocationManager.getLastKnownLocation(locationProvider)

    override fun onCreate() {
        super.onCreate()
        if (DEBUG) Log.i(TAG, "onCreate")
        if (Config.hasLocationPermission)
                mLocationManager.requestLocationUpdates(locationProvider, 0, 0f, this)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (DEBUG) Log.i(TAG, String.format("onStartCommand(%s, %d, %d", intent, flags, startId))
        
        if (Config.hasLocationPermission) {
            if (!locationServicesEnabled) {
                EventBus.getDefault().post(locationServicesDisabled())
            } else {
                //search for location
            }
        } else {
            EventBus.getDefault().post(locationAccessDenied())
            stopSelf()
        }
        
        // Do not attempt to restart if the hosting process is killed by Android
        return Service.START_NOT_STICKY
    }

    // Prevent binding.
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onLocationChanged(location: Location) {
        if (DEBUG) Log.i(TAG, "Location search succeeded")
        stopSelf()
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
        if (DEBUG) Log.i(TAG, "Status changed for " + provider)
    }

    override fun onProviderEnabled(provider: String) { }

    override fun onProviderDisabled(provider: String) {
        if (DEBUG) Log.i(TAG, "Location search failed, using last known location")
        stopSelf()
    }

    override fun onDestroy() {
        if (DEBUG) Log.i(TAG, "onDestroy")
        if (Config.hasLocationPermission) {
            mLocationManager.removeUpdates(this)
            updateLocation(lastKnownLocation)
        }
        super.onDestroy()
    }

    private fun updateLocation(location: Location?) {
        if (Config.hasLocationPermission) mLocationManager.removeUpdates(this)

        if (location != null) {
            val latitude    = location.latitude
            val longitude   = location.longitude
            val sunLocation = com.luckycatlabs.sunrisesunset.dto.Location(latitude, longitude)
            val calculator  = SunriseSunsetCalculator(sunLocation, TimeZone.getDefault())
            Config.sunsetTime  = calculator.getOfficialSunsetForDate(Calendar.getInstance())
            Config.sunriseTime = calculator.getOfficialSunriseForDate(Calendar.getInstance())
            Config.location = latitude.toString() + "," + longitude.toString()
        }
    }

    companion object {
        private val TAG = "LocationUpdateService"
        private val DEBUG = false
        private val locationProvider = LocationManager.NETWORK_PROVIDER;

        val FOREGROUND = true
        val BACKGROUND = false

        private val intent = { ctx: Context -> Intent(ctx, LocationUpdateService::class.java) }

        fun start(context: Context) {
            if (DEBUG) Log.i(TAG, "Received start request")
            context.startService(intent(context))
        }
    }
}
