package com.jmstudios.redmoon.receiver;

import android.location.LocationListener;
import android.content.Context;
import android.location.Location;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.location.LocationManager;

import java.util.Calendar;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;

import com.jmstudios.redmoon.receiver.AutomaticFilterChangeReceiver;

public class LocationSunCommandListener implements LocationListener {
    private Context mContext;
    private boolean mIsTurnOn;
    private LocationManager mManager;

    public LocationSunCommandListener(Context context, boolean isTurnOn,
                                      LocationManager manager) {
        mContext = context;
        mIsTurnOn = isTurnOn;
        mManager = manager;
    }

    @Override
    public void onLocationChanged(Location location) {
        SunriseSunsetCalculator calculator =
            AutomaticFilterChangeReceiver.getLocationCalculator(mContext, location);
        if (mIsTurnOn) {
            String time = calculator.getOfficialSunsetForDate(Calendar.getInstance());
            Intent turnOnIntent = new Intent(mContext, AutomaticFilterChangeReceiver.class);
            turnOnIntent.setData(Uri.parse("turnOnIntent"));
            turnOnIntent.putExtra("turn_on", true);
            AutomaticFilterChangeReceiver.scheduleNextAlarm(mContext, time, turnOnIntent, true);
        } else {
            String time = calculator.getOfficialSunriseForDate(Calendar.getInstance());
            Intent pauseIntent = new Intent(mContext, AutomaticFilterChangeReceiver.class);
            pauseIntent.putExtra("turn_on", false);
            pauseIntent.setData(Uri.parse("pauseIntent"));
            AutomaticFilterChangeReceiver.scheduleNextAlarm(mContext, time, pauseIntent, true);
        }
        mManager.removeUpdates(this);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}
}
