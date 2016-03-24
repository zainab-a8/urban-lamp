package com.jmstudios.redmoon.activity;

import android.app.FragmentManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.support.v7.widget.SwitchCompat;
import android.widget.CompoundButton;
import android.provider.Settings;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION;
import android.widget.Toast;

import com.jmstudios.redmoon.R;
import com.jmstudios.redmoon.fragment.ShadesFragment;
import com.jmstudios.redmoon.helper.FilterCommandFactory;
import com.jmstudios.redmoon.helper.FilterCommandSender;
import com.jmstudios.redmoon.model.SettingsModel;
import com.jmstudios.redmoon.presenter.ShadesPresenter;
import com.jmstudios.redmoon.service.ScreenFilterService;

public class ShadesActivity extends AppCompatActivity {
    private static final String TAG = "ShadesActivity";
    private static final boolean DEBUG = true;
    private static final String FRAGMENT_TAG_SHADES = "jmstudios.fragment.tag.SHADES";

    public static int OVERLAY_PERMISSION_REQ_CODE = 1234;

    private ShadesPresenter mPresenter;
    private ShadesFragment mFragment;
    private SettingsModel mSettingsModel;
    private SwitchCompat mSwitch;
    private ShadesActivity context = this;

    private boolean hasShownWarningToast = false;
    private boolean ignoreNextSwitchChange = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Wire MVP classes
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mSettingsModel = new SettingsModel(getResources(), sharedPreferences);
        FilterCommandFactory filterCommandFactory = new FilterCommandFactory(this);
        FilterCommandSender filterCommandSender = new FilterCommandSender(this);

        if (mSettingsModel.getDarkThemeFlag()) setTheme(R.style.AppThemeDark);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shades);

        FragmentManager fragmentManager = getFragmentManager();

        ShadesFragment view;

        // Only create and attach a new fragment on the first Activity creation.
        // On Activity re-creation, retrieve the existing fragment stored in the FragmentManager.
        if (savedInstanceState == null) {
            if (DEBUG) Log.i(TAG, "onCreate - First creation");

            view = new ShadesFragment();

            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, view, FRAGMENT_TAG_SHADES)
                    .commit();
        } else {
            if (DEBUG) Log.i(TAG, "onCreate - Re-creation");

            view = (ShadesFragment) fragmentManager.findFragmentByTag(FRAGMENT_TAG_SHADES);
        }

        mPresenter = new ShadesPresenter(view, mSettingsModel, filterCommandFactory, filterCommandSender);
        view.registerPresenter(mPresenter);

        // Make Presenter listen to settings changes
        mSettingsModel.addOnSettingsChangedListener(mPresenter);

        mFragment = view;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_menu, menu);

        final MenuItem item = menu.findItem(R.id.screen_filter_switch);
        mSwitch = (SwitchCompat) item.getActionView();
        mSwitch.setChecked(mSettingsModel.getShadesPowerState());
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (ignoreNextSwitchChange) {
                        if (DEBUG) Log.i(TAG, "Switch change ignored");
                        ignoreNextSwitchChange = false;
                        return;
                    }

                    // http://stackoverflow.com/a/3993933
                    if (android.os.Build.VERSION.SDK_INT >= 23) {
                        if (!Settings.canDrawOverlays(context)) {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                       Uri.parse("package:" + getPackageName()));
                            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
                        }

                        if (Settings.canDrawOverlays(context)) {
                            mPresenter.sendCommand(isChecked ?
                                                   ScreenFilterService.COMMAND_ON :
                                                   ScreenFilterService.COMMAND_OFF);
                            displayInstallWarningToast();
                        } else {
                            buttonView.setChecked(false);
                        }
                    } else {
                        mPresenter.sendCommand(isChecked ?
                                               ScreenFilterService.COMMAND_ON :
                                               ScreenFilterService.COMMAND_OFF);
                        if (isChecked)
                            displayInstallWarningToast();
                    }
                }
            });

        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mSettingsModel.openSettingsChangeListener();
        mPresenter.onStart();
    }

    @Override
    protected void onStop() {
        mSettingsModel.closeSettingsChangeListener();
        super.onStop();
    }

    public void displayInstallWarningToast() {
        if (hasShownWarningToast) return;

        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(getApplicationContext(),
                                     getString(R.string.toast_warning_install),
                                     duration);
        toast.show();

        hasShownWarningToast = true;
    }

    public SwitchCompat getSwitch() {
        return mSwitch;
    }

    public int getColorTempProgress() {
        return mSettingsModel.getShadesColor();
    }

    public int getIntensityLevelProgress() {
        return mSettingsModel.getShadesIntensityLevel();
    }

    public void setIgnoreNextSwitchChange(boolean ignore) {
        ignoreNextSwitchChange = ignore;
    }

    public int getDimLevelProgress() {
        return mSettingsModel.getShadesDimLevel();
    }

    public ShadesFragment getFragment() {
        return mFragment;
    }

    public SettingsModel getSettingsModel() {
        return mSettingsModel;
    }
}
