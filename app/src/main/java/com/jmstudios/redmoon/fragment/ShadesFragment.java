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
import android.provider.Settings;
import android.os.Build.VERSION;
import android.net.Uri;
import android.content.Intent;
import android.os.Bundle;

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

        String darkThemePrefKey= getString(R.string.pref_key_dark_theme);
        String lowerBrightnessPrefKey = getString(R.string.pref_key_control_brightness);

        PreferenceScreen prefScreen = getPreferenceScreen();
        final CheckBoxPreference darkThemePref = (CheckBoxPreference)
            prefScreen.findPreference(darkThemePrefKey);
        final CheckBoxPreference lowerBrightnessPref = (CheckBoxPreference)
            prefScreen.findPreference(lowerBrightnessPrefKey);

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
