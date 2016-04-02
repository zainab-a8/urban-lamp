package com.jmstudios.redmoon.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.app.AlarmManager;
import android.os.Build.VERSION;
import android.net.Uri;
import android.location.LocationManager;
import android.widget.Toast;

import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.TimeZone;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;

import com.jmstudios.redmoon.R;

import com.jmstudios.redmoon.helper.FilterCommandFactory;
import com.jmstudios.redmoon.helper.FilterCommandSender;
import com.jmstudios.redmoon.model.SettingsModel;
import com.jmstudios.redmoon.service.ScreenFilterService;
import com.jmstudios.redmoon.receiver.LocationSunCommandListener;

public class AutomaticFilterChangeReceiver extends BroadcastReceiver {
    private static final String TAG = "AutomaticFilterChange";
    private static final boolean DEBUG = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (DEBUG) Log.i(TAG, "Alarm received");
        FilterCommandSender commandSender = new FilterCommandSender(context);
        FilterCommandFactory commandFactory = new FilterCommandFactory(context);
        Intent onCommand = commandFactory.createCommand(ScreenFilterService.COMMAND_ON);
        Intent pauseCommand = commandFactory.createCommand(ScreenFilterService.COMMAND_PAUSE);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SettingsModel settingsModel = new SettingsModel(context.getResources(), sharedPreferences);

        boolean turnOn = intent.getData().toString().equals("turnOnIntent");

        if (turnOn) {
            commandSender.send(onCommand);
            cancelTurnOnAlarm(context);
            scheduleNextOnCommand(context);
        } else {
            commandSender.send(pauseCommand);
            cancelPauseAlarm(context);
            scheduleNextPauseCommand(context);
        }
    }

    public static void scheduleNextOnCommand(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SettingsModel settingsModel = new SettingsModel(context.getResources(), sharedPreferences);

        if (!settingsModel.getAutomaticFilterMode().equals("never")) {
            String time;
            boolean timeInUtc;
            if (settingsModel.getAutomaticFilterMode().equals("custom")) {
                timeInUtc = false;
                time = settingsModel.getAutomaticTurnOnTime();
            } else {
                LocationManager manager = (LocationManager)
                    context.getSystemService(context.LOCATION_SERVICE);
                if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0,
                                                   new LocationSunCommandListener(context, true, manager));
                    return;
                } else {
                    android.location.Location lastLocation = manager
                        .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (lastLocation != null) {
                        SunriseSunsetCalculator calculator = getLocationCalculator(context, lastLocation);
                        time = calculator.getOfficialSunsetForDate(Calendar.getInstance());
                        timeInUtc = true;
                    } else {
                        displayNoLocationWarningToast(context);
                        timeInUtc = false;
                        time = settingsModel.getAutomaticTurnOnTime();
                    }
                }
            }
            Intent turnOnIntent = new Intent(context, AutomaticFilterChangeReceiver.class);
            turnOnIntent.setData(Uri.parse("turnOnIntent"));
            turnOnIntent.putExtra("turn_on", true);

            scheduleNextAlarm(context, time, turnOnIntent, timeInUtc);
        }
    }

    public static void scheduleNextPauseCommand(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SettingsModel settingsModel = new SettingsModel(context.getResources(), sharedPreferences);

        if (!settingsModel.getAutomaticFilterMode().equals("never")) {
            String time;
            boolean timeInUtc;
            if (settingsModel.getAutomaticFilterMode().equals("custom")) {
                timeInUtc = false;
                time = settingsModel.getAutomaticTurnOffTime();
            } else {
                LocationManager manager = (LocationManager)
                    context.getSystemService(context.LOCATION_SERVICE);
                if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0,
                                                   new LocationSunCommandListener(context, false, manager));
                    return;
                } else {
                    android.location.Location lastLocation = manager
                        .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (lastLocation != null) {
                        SunriseSunsetCalculator calculator = getLocationCalculator(context, lastLocation);
                        time = calculator.getOfficialSunriseForDate(Calendar.getInstance());
                        timeInUtc = true;
                    } else {
                        displayNoLocationWarningToast(context);
                        timeInUtc = false;
                        time = settingsModel.getAutomaticTurnOffTime();
                    }
                }
            }
            Intent pauseIntent = new Intent(context, AutomaticFilterChangeReceiver.class);
            pauseIntent.putExtra("turn_on", false);
            pauseIntent.setData(Uri.parse("pauseIntent"));

            scheduleNextAlarm(context, time, pauseIntent, timeInUtc);
        }
    }

    public static void scheduleNextAlarm(Context context, String time, Intent operation, boolean timeInUtc) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.split(":")[0]));
        calendar.set(Calendar.MINUTE, Integer.parseInt(time.split(":")[1]));

        GregorianCalendar now = new GregorianCalendar();
        now.add(Calendar.SECOND, 1);
        if (calendar.before(now)) {
            calendar.add(Calendar.DATE, 1);
        }
        if (!timeInUtc)
            calendar.setTimeZone(TimeZone.getTimeZone("UTC"));

        if (DEBUG) Log.i(TAG, "Scheduling alarm for " + calendar.toString());

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, operation, 0);

        if (android.os.Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    public static void cancelAlarms(Context context) {
        cancelPauseAlarm(context);
        cancelTurnOnAlarm(context);
    }

    public static void cancelPauseAlarm(Context context) {
        Intent commands = new Intent(context, AutomaticFilterChangeReceiver.class);
        commands.setData(Uri.parse("pauseIntent"));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, commands, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }

    public static void cancelTurnOnAlarm(Context context) {
        Intent commands = new Intent(context, AutomaticFilterChangeReceiver.class);
        commands.setData(Uri.parse("turnOnIntent"));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, commands, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }

    public static SunriseSunsetCalculator getLocationCalculator(Context context, android.location.Location location) {
        com.luckycatlabs.sunrisesunset.dto.Location sunriseSunsetLocation =
            new com.luckycatlabs.sunrisesunset.dto.Location(location.getLatitude(), location.getLongitude());
        return new SunriseSunsetCalculator(sunriseSunsetLocation, "GMT");
    }

    public static void displayNoLocationWarningToast(Context context) {
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText
            (context, context.getString(R.string.toast_warning_no_location), duration);
        toast.show();
    }
}
