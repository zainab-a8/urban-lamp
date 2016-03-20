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

import com.jmstudios.redmoon.R;

public class ProfileSelectorPreference extends Preference
    implements OnItemSelectedListener {
    public static final int DEFAULT_VALUE = 1;

    private Spinner mProfileSpinner;
    private int mProfile;
    private View mView;

    public ProfileSelectorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_profile_selector);
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
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource
            (getContext(), R.array.standard_profiles_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mProfileSpinner.setAdapter(adapter);
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
}
