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

import com.jmstudios.redmoon.event.*
import com.jmstudios.redmoon.model.Config
import com.jmstudios.redmoon.util.appContext
import com.jmstudios.redmoon.util.hasLocationPermission

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator
import org.greenrobot.eventbus.EventBus

import java.util.*

/**
 * When the service starts, we request location updates. When we get a new
 * location fix, we shut down the service. When we shut down (for any reason),
 * we update the location with the last known location. This way, even if we
 * didn't get a fix before the service was stopped, we might be able to get
 * something more recent than the last time red moon updated location.
 */
class LocationUpdateService: Service(), LocationListener {
    private enum class LocationProvider {
        NETWORK, GPS
    }

    private var mLocationProvider = LocationProvider.NETWORK

    private val locationManager: LocationManager
        get() = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val locationServicesEnabled: Boolean
        get() = locationManager.isProviderEnabled(when (mLocationProvider) {
            LocationProvider.NETWORK -> locationProviderNetwork
            LocationProvider.GPS     -> locationProviderGps
        })
    
    private val lastKnownLocation: Location?
        get() {
            val networkLocation = locationManager.getLastKnownLocation(locationProviderNetwork)
            if (networkLocation != null) {
                return networkLocation
            } else {
                return locationManager.getLastKnownLocation(locationProviderGps)
            }
        }

    override fun onCreate() {
        super.onCreate()
        if (DEBUG) Log.i(TAG, "onCreate")
        if (hasLocationPermission) {
            if (DEBUG) Log.i(TAG, "Requesting location updates")
            if (DEBUG) Log.i(TAG, "List of providers + ${locationManager.allProviders}")
            if (locationManager.allProviders.contains(locationProviderNetwork)) {
                mLocationProvider = LocationProvider.NETWORK
                locationManager.requestLocationUpdates(locationProviderNetwork, 0, 0f, this)
            } else if (locationManager.allProviders.contains(locationProviderGps)) {
                // Fall back on GPS if there is not network provider
                mLocationProvider = LocationProvider.GPS
                locationManager.requestLocationUpdates(locationProviderGps, 0, 0f, this)
            } else {
                if (DEBUG) Log.i(TAG, "No suitable location providers available, stopping.")
                stopSelf()
            }
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (DEBUG) Log.i(TAG, "onStartCommand($intent, $flags, $startId)")

        if (!hasLocationPermission) {
            EventBus.getDefault().post(locationAccessDenied())
            stopSelf()
        } else if (!locationServicesEnabled) {
            EventBus.getDefault().post(locationServicesDisabled())
        } else {
            // TODO: startForeground() and show notification instead of sending event
            EventBus.getDefault().post(locationUpdating())
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

    override fun onProviderEnabled(provider: String) {
        // TODO: startForeground() and show notification instead of sending event
        EventBus.getDefault().post(locationUpdating())
    }

    override fun onProviderDisabled(provider: String) {
        if (DEBUG) Log.i(TAG, "Location search failed, using last known location")
        stopSelf()
    }

    override fun onDestroy() {
        if (DEBUG) Log.i(TAG, "onDestroy")
        if (hasLocationPermission) {
            locationManager.removeUpdates(this)
            updateLocation(lastKnownLocation)
        }
        super.onDestroy()
    }

    private fun updateLocation(location: Location?) {
        location?.apply {
            val sunLocation = com.luckycatlabs.sunrisesunset.dto.Location(latitude, longitude)
            val calculator  = SunriseSunsetCalculator(sunLocation, TimeZone.getDefault())
            Config.sunsetTime  = calculator.getOfficialSunsetForDate(Calendar.getInstance())
            Config.sunriseTime = calculator.getOfficialSunriseForDate(Calendar.getInstance())
            Config.location = latitude.toString() + "," + longitude.toString()
        }
    }

    companion object {
        private val TAG = "LocationUpdateService"
        private val DEBUG = true
        private val locationProviderNetwork = LocationManager.NETWORK_PROVIDER
        private val locationProviderGps = LocationManager.GPS_PROVIDER

        //val FOREGROUND = true
        //val BACKGROUND = false

        private val intent: Intent
            get() = Intent(appContext, LocationUpdateService::class.java)

        fun start() {
            if (DEBUG) Log.i(TAG, "Received start request")
            appContext.startService(intent)
        }
    }
}
