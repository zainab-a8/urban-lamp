package com.jmstudios.redmoon.presenter;

import android.content.Intent;
import android.support.annotation.NonNull;

import com.jmstudios.redmoon.R;
import com.jmstudios.redmoon.fragment.ShadesFragment;
import com.jmstudios.redmoon.helper.FilterCommandFactory;
import com.jmstudios.redmoon.helper.FilterCommandSender;
import com.jmstudios.redmoon.model.SettingsModel;
import com.jmstudios.redmoon.service.ScreenFilterService;

public class ShadesPresenter implements SettingsModel.OnSettingsChangedListener {
    private ShadesFragment mView;
    private SettingsModel mSettingsModel;
    private FilterCommandFactory mFilterCommandFactory;
    private FilterCommandSender mFilterCommandSender;

    public ShadesPresenter(@NonNull ShadesFragment view,
                           @NonNull SettingsModel settingsModel,
                           @NonNull FilterCommandFactory filterCommandFactory,
                           @NonNull FilterCommandSender filterCommandSender) {
        mView = view;
        mSettingsModel = settingsModel;
        mFilterCommandFactory = filterCommandFactory;
        mFilterCommandSender = filterCommandSender;
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
    //endregion
}
