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
package com.jmstudios.redmoon.receiver

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.util.Log

import com.jmstudios.redmoon.R

class LocationUpdater(private val mContext: Context) : LocationListener {
    private var mHandler: locationUpdateHandler? = null

    interface locationUpdateHandler {
        fun handleLocationFound()
        fun handleLocationSearchFailed()
    }

    fun updateLocation(handler: locationUpdateHandler) {
        mHandler = handler
        val locationManager = mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) &&
            ActivityCompat.checkSelfPermission
            (mContext, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    0, 0f, this)
        } else
            handler.handleLocationSearchFailed()
    }

    override fun onLocationChanged(location: Location) {
        if (DEBUG) Log.i(TAG, "Location search succeeded")
        if (mHandler != null) mHandler.handleLocationFound()
        mContext?: return
        val locationManager = mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission
            (mContext, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED)
            locationManager.removeUpdates(this)

        val prefKey = mContext.getString(R.string.pref_key_location)
        val sp = PreferenceManager.getDefaultSharedPreferences(mContext)
        val editor = sp.edit()
        val l = location.latitude.toString() + "," + location.longitude.toString()
        editor.putString(prefKey, l)
        editor.apply()
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
        if (DEBUG) Log.i(TAG, "Status changed for " + provider)
    }

    override fun onProviderEnabled(provider: String) {
    }

    override fun onProviderDisabled(provider: String) {
        if (DEBUG) Log.i(TAG, "Location search failed")
        mContext?: return
        val locationManager = mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission
            (mContext, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(this)
        }

        if (mHandler != null) mHandler.handleLocationSearchFailed()
    }

    companion object {
        private val TAG = "LocationUpdate"
        private val DEBUG = false
    }
}
