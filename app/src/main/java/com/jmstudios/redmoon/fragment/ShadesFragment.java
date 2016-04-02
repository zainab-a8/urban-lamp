package com.jmstudios.redmoon.fragment;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.ListPreference;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.SwitchCompat;
import android.provider.Settings;
import android.os.Build.VERSION;
import android.net.Uri;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

import com.jmstudios.redmoon.R;
import com.jmstudios.redmoon.presenter.ShadesPresenter;
import com.jmstudios.redmoon.activity.ShadesActivity;
import com.jmstudios.redmoon.preference.TimePickerPreference;

public class ShadesFragment extends PreferenceFragment {
    private static final String TAG = "ShadesFragment";
    private static final boolean DEBUG = true;

    private ShadesPresenter mPresenter;
    private int mShadesFabIconResId = -1;

    public ShadesFragment() {
        // Android Fragments require an explicit public default constructor for re-creation
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        String darkThemePrefKey= getString(R.string.pref_key_dark_theme);
        String lowerBrightnessPrefKey = getString(R.string.pref_key_control_brightness);
        String automaticFilterPrefKey = getString(R.string.pref_key_automatic_filter);
        String automaticTurnOnPrefKey = getString(R.string.pref_key_custom_start_time);
        String automaticTurnOffPrefKey = getString(R.string.pref_key_custom_end_time);

        PreferenceScreen prefScreen = getPreferenceScreen();
        final CheckBoxPreference darkThemePref = (CheckBoxPreference)
            prefScreen.findPreference(darkThemePrefKey);
        final CheckBoxPreference lowerBrightnessPref = (CheckBoxPreference)
            prefScreen.findPreference(lowerBrightnessPrefKey);
        final ListPreference automaticFilterPref = (ListPreference)
            prefScreen.findPreference(automaticFilterPrefKey);
        final TimePickerPreference automaticTurnOnPref = (TimePickerPreference)
            prefScreen.findPreference(automaticTurnOnPrefKey);
        final TimePickerPreference automaticTurnOffPref = (TimePickerPreference)
            prefScreen.findPreference(automaticTurnOffPrefKey);

        darkThemePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                getActivity().recreate();
                return false;
            }
        });

        if (android.os.Build.VERSION.SDK_INT >= 23 &&
            !Settings.System.canWrite(getContext())) lowerBrightnessPref.setChecked(false);

        lowerBrightnessPref.setOnPreferenceChangeListener
            (new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean checked = (Boolean) newValue;
                if (checked && android.os.Build.VERSION.SDK_INT >= 23 &&
                    !Settings.System.canWrite(getContext())) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                               Uri.parse("package:" +
                                                         getContext().getPackageName()));
                    startActivityForResult(intent, -1);
                    return false;
                }

                return true;
            }
        });

        if (!automaticFilterPref.getValue().toString().equals("custom")) {
            automaticTurnOnPref.setEnabled(false);
            automaticTurnOffPref.setEnabled(false);
        }

        automaticFilterPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (!newValue.toString().equals("custom")) {
                        automaticTurnOnPref.setEnabled(false);
                        automaticTurnOffPref.setEnabled(false);
                    } else {
                        automaticTurnOnPref.setEnabled(true);
                        automaticTurnOffPref.setEnabled(true);
                    }

                    if (newValue.toString().equals("sun") && ContextCompat.checkSelfPermission
                        (getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(getActivity(), new String[]
                            {Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
                        return false;
                    }
                    return true;
                }
            });

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = super.onCreateView(inflater, container, savedInstanceState);

        return v;
    }

    public void registerPresenter(@NonNull ShadesPresenter presenter) {
        mPresenter = presenter;

        if (DEBUG) Log.i(TAG, "Registered Presenter");
    }

    public void setSwitchOn(boolean on, boolean paused) {
        ShadesActivity activity = (ShadesActivity) getActivity();
        SwitchCompat filterSwitch = activity.getSwitch();
        if (filterSwitch != null) {
            activity.setIgnoreNextSwitchChange(paused);
            filterSwitch.setChecked(!on);
        }
    }
}
