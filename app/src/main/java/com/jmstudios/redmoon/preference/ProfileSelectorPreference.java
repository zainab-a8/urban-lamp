package com.jmstudios.redmoon.preference;

import android.preference.Preference;
import android.widget.Spinner;
import android.view.View;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.content.Context;
import android.content.res.TypedArray;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import java.util.ArrayList;
import java.util.Arrays;

import com.jmstudios.redmoon.R;
import com.jmstudios.redmoon.model.ProfilesModel;

public class ProfileSelectorPreference extends Preference
    implements OnItemSelectedListener {
    public static final int DEFAULT_VALUE = 1;

    private Spinner mProfileSpinner;
    ArrayAdapter<CharSequence> mArrayAdapter;
    private int mProfile;
    private View mView;
    private ProfilesModel mProfilesModel;

    public ProfileSelectorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_profile_selector);

        mProfilesModel = new ProfilesModel(context);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInteger(index, DEFAULT_VALUE);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            mProfile = getPersistedInt(DEFAULT_VALUE);
        } else {
            mProfile = (Integer) defaultValue;
            persistInt(mProfile);
        }
    }

    @Override
    protected void onBindView(@NonNull View view) {
        super.onBindView(view);

        mView = view;

        mProfileSpinner = (Spinner) view.findViewById(R.id.profile_spinner);
        initLayout();
    }

    private void initLayout() {
        // The default operations first need to be converted to an ArrayList,
        // because the ArrayAdapter will turn it into an AbstractList otherwise,
        // which doesn't support certain actions, like adding elements.
        // See: http://stackoverflow.com/a/3200631
        ArrayList<CharSequence> defaultOperations = new ArrayList<CharSequence>
            (Arrays.asList(getContext().getResources().getStringArray(R.array.standard_profiles_array)));
        mArrayAdapter = new ArrayAdapter<CharSequence>
            (getContext(), android.R.layout.simple_spinner_item, defaultOperations);
        mArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        readProfiles();

        mProfileSpinner.setAdapter(mArrayAdapter);
        mProfileSpinner.setSelection(mProfile);
        mProfileSpinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        mProfile = pos;
        persistInt(mProfile);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    //Section: Reading and writing profiles

    /**
     * Reads the profiles saved in the SharedPreference in the spinner
     */
    public void readProfiles() {
        ArrayList<ProfilesModel.Profile> profiles = mProfilesModel.getProfiles();

        for (ProfilesModel.Profile profile : profiles) {
            mArrayAdapter.add((CharSequence) profile.mProfileName);
        }
    }
}
