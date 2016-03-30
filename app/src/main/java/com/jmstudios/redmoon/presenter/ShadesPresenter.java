package com.jmstudios.redmoon.presenter;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.content.Context;
import android.util.Log;

import com.jmstudios.redmoon.R;
import com.jmstudios.redmoon.fragment.ShadesFragment;
import com.jmstudios.redmoon.helper.FilterCommandFactory;
import com.jmstudios.redmoon.helper.FilterCommandSender;
import com.jmstudios.redmoon.model.SettingsModel;
import com.jmstudios.redmoon.service.ScreenFilterService;
import com.jmstudios.redmoon.receiver.AutomaticFilterChangeReceiver;

public class ShadesPresenter implements SettingsModel.OnSettingsChangedListener {
    private static final String TAG = "ShadesPresenter";
    private static final boolean DEBUG = true;
;
    private ShadesFragment mView;
    private SettingsModel mSettingsModel;
    private FilterCommandFactory mFilterCommandFactory;
    private FilterCommandSender mFilterCommandSender;
    private Context mContext;

    public ShadesPresenter(@NonNull ShadesFragment view,
                           @NonNull SettingsModel settingsModel,
                           @NonNull FilterCommandFactory filterCommandFactory,
                           @NonNull FilterCommandSender filterCommandSender,
                           @NonNull Context context) {
        mView = view;
        mSettingsModel = settingsModel;
        mFilterCommandFactory = filterCommandFactory;
        mFilterCommandSender = filterCommandSender;
        mContext = context;
    }

    public void onStart() {
        boolean poweredOn = mSettingsModel.getShadesPowerState();
        boolean paused = mSettingsModel.getShadesPauseState();
        setShadesFabIcon(poweredOn, paused);
    }

    private void setShadesFabIcon(boolean poweredOn, boolean pauseState) {
        mView.setSwitchOn(!poweredOn || pauseState, pauseState);
    }

    public void onShadesFabClicked() {
        Intent command;
        if (mSettingsModel.getShadesPowerState() && !mSettingsModel.getShadesPauseState()) {
            command = mFilterCommandFactory.createCommand(ScreenFilterService.COMMAND_OFF);
        } else {
            command = mFilterCommandFactory.createCommand(ScreenFilterService.COMMAND_ON);
        }

        mFilterCommandSender.send(command);
    }

    public void sendCommand(int command) {
        Intent iCommand = mFilterCommandFactory.createCommand(command);
        mFilterCommandSender.send(iCommand);
    }

    //region OnSettingsChangedListener
    @Override
    public void onShadesPowerStateChanged(boolean powerState) {
        setShadesFabIcon(powerState, mSettingsModel.getShadesPauseState());
    }

    @Override
    public void onShadesPauseStateChanged(boolean pauseState) {
        setShadesFabIcon(mSettingsModel.getShadesPowerState(), pauseState);
    }

    @Override
    public void onShadesDimLevelChanged(int dimLevel) {/* do nothing */}

    @Override
    public void onShadesIntensityLevelChanged(int dimLevel) {/* do nothing */}

    @Override
    public void onShadesColorChanged(int color) {/* do nothing */}

    @Override
    public void onShadesAutomaticFilterModeChanged(String automaticFilterMode) {
        if (DEBUG) Log.i(TAG, "Filter mode changed to " + automaticFilterMode);
        if (!automaticFilterMode.equals("never")) {
            AutomaticFilterChangeReceiver.scheduleNextOnCommand(mContext);
            AutomaticFilterChangeReceiver.scheduleNextPauseCommand(mContext);
        } else {
            AutomaticFilterChangeReceiver.cancelAlarms(mContext);
        }
    }

    @Override
    public void onShadesAutomaticTurnOnChanged(String turnOnTime) {
        AutomaticFilterChangeReceiver.cancelTurnOnAlarm(mContext);
        AutomaticFilterChangeReceiver.scheduleNextOnCommand(mContext);
    }

    @Override
    public void onShadesAutomaticTurnOffChanged(String turnOffTime) {
        AutomaticFilterChangeReceiver.cancelPauseAlarm(mContext);
        AutomaticFilterChangeReceiver.scheduleNextPauseCommand(mContext);
    }
    //endregion
}
