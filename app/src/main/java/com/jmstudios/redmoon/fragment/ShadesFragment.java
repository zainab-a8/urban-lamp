package com.jmstudios.redmoon.fragment;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.SwitchCompat;

import com.jmstudios.redmoon.R;
import com.jmstudios.redmoon.presenter.ShadesPresenter;
import com.jmstudios.redmoon.activity.ShadesActivity;

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

        String openOnStartupKey = getString(R.string.pref_key_always_open_on_startup);
        String resumeAfterRebootPrefKey= getString(R.string.pref_key_keep_running_after_reboot);

        PreferenceScreen prefScreen = getPreferenceScreen();
        final CheckBoxPreference openOnStartupPref = (CheckBoxPreference) prefScreen.findPreference(openOnStartupKey);
        final CheckBoxPreference resumeAfterRebootPref = (CheckBoxPreference) prefScreen.findPreference(resumeAfterRebootPrefKey);

        openOnStartupPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                boolean checked = openOnStartupPref.isChecked();
                if (checked) {
                    resumeAfterRebootPref.setEnabled(false);
                } else {
                    resumeAfterRebootPref.setEnabled(true);
                }

                return false;
            }
        });

        resumeAfterRebootPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                boolean checked = resumeAfterRebootPref.isChecked();
                if (checked) {
                    openOnStartupPref.setEnabled(false);
                } else {
                    openOnStartupPref.setEnabled(true);
                }

                return false;
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
