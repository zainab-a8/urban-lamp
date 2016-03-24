package com.jmstudios.redmoon.receiver;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.jmstudios.redmoon.R;
import com.jmstudios.redmoon.helper.FilterCommandFactory;
import com.jmstudios.redmoon.model.SettingsModel;
import com.jmstudios.redmoon.service.ScreenFilterService;

public class SwitchAppWidgetProvider extends AppWidgetProvider {
    public final static String ACTION_TOGGLE = "com.jmstudios.redmoon.action.APPWIDGET_TOGGLE";
    public final static String ACTION_UPDATE = "com.jmstudios.redmoon.action.APPWIDGET_UPDATE";
    public final static String EXTRA_POWER = "com.jmstudios.redmoon.action.APPWIDGET_EXTRA_POWER";
    private final static String TAG = "SwitchAppWidgetProvider";
    private final static boolean DEBUG = true;

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        if(DEBUG) Log.i(TAG, "Updating!");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SettingsModel settingsModel = new SettingsModel(context.getResources(), sharedPreferences);

        for(int i = 0; i < appWidgetIds.length; i++) {
            int appWidgetId = appWidgetIds[i];

            Intent toggleIntent = new Intent(context, SwitchAppWidgetProvider.class);
            toggleIntent.setAction(SwitchAppWidgetProvider.ACTION_TOGGLE);
            PendingIntent togglePendingIntent = PendingIntent.getBroadcast(context, 0, toggleIntent, 0);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget_switch);
            views.setOnClickPendingIntent(R.id.widget_pause_play_button, togglePendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
            updateImage(context, settingsModel.getShadesPauseState());
        }
    }

    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(SwitchAppWidgetProvider.ACTION_TOGGLE)) toggle(context);
        else if(intent.getAction().equals(SwitchAppWidgetProvider.ACTION_UPDATE))
            updateImage(context, intent.getBooleanExtra(SwitchAppWidgetProvider.EXTRA_POWER, false));
        else super.onReceive(context, intent);
    }

    void toggle(Context context) {
        FilterCommandFactory commandFactory = new FilterCommandFactory(context);
        Intent onCommand = commandFactory.createCommand(ScreenFilterService.COMMAND_ON);
        Intent pauseCommand = commandFactory.createCommand(ScreenFilterService.COMMAND_PAUSE);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SettingsModel settingsModel = new SettingsModel(context.getResources(), sharedPreferences);

        if(settingsModel.getShadesPauseState() || !(settingsModel.getShadesPowerState())) {
            context.startService(onCommand);
        } else {
            context.startService(pauseCommand);
        }
    }

    void updateImage(Context context, boolean powerState) {
        if(DEBUG) Log.i(TAG, "Updating image!");
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget_switch);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName appWidgetComponent = new ComponentName(context, SwitchAppWidgetProvider.class.getName());

        int drawable;

        if(!powerState) drawable = R.drawable.ic_play;
        else drawable = R.drawable.ic_pause;

        views.setInt(R.id.widget_pause_play_button, "setImageResource", drawable);
        appWidgetManager.updateAppWidget(appWidgetComponent, views);
    }
}
