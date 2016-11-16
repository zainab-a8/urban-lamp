/*
 * Copyright (c) 2016 Marien Raat <marienraat@riseup.net>
 *
 *  This file is free software: you may copy, redistribute and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This file is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *  FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.jmstudios.redmoon.preference

import android.app.AlertDialog
import android.content.Context
import android.content.res.TypedArray
import android.preference.Preference
import android.text.InputType
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner

import com.jmstudios.redmoon.R

import com.jmstudios.redmoon.activity.ShadesActivity
import com.jmstudios.redmoon.event.*
import com.jmstudios.redmoon.helper.ProfilesHelper
import com.jmstudios.redmoon.model.ProfilesModel
import com.jmstudios.redmoon.model.SettingsModel
import org.greenrobot.eventbus.Subscribe

import java.util.ArrayList
import java.util.Arrays

class ProfileSelectorPreference(private val mContext: Context, attrs: AttributeSet) : Preference(mContext, attrs), OnItemSelectedListener {

    lateinit private var mProfileSpinner: Spinner
    lateinit private var mProfileActionButton: Button
    lateinit internal var mArrayAdapter: ArrayAdapter<CharSequence>
    private var mProfile: Int = 0
    lateinit private var mView: View
    private val mProfilesModel: ProfilesModel

    private var mDefaultOperations: ArrayList<CharSequence>? = null

    private var currentColor: Int = 0
    private var currentIntensity: Int = 0
    private var currentDim: Int = 0

    private var mIsListenerRegistered: Boolean = false

    // Settings model from the activity to save the ammount of profiles
    private val mSettingsModel: SettingsModel

    init {
        layoutResource = R.layout.preference_profile_selector
        mProfilesModel = ProfilesModel(mContext)
        mIsListenerRegistered = false
        mSettingsModel = (context as ShadesActivity).mSettingsModel
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getInteger(index, DEFAULT_VALUE)
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        if (restorePersistedValue) {
            mProfile = getPersistedInt(DEFAULT_VALUE)
        } else {
            mProfile = (defaultValue as Int?)?: 0
            persistInt(mProfile)
        }
    }

    override fun onBindView(view: View) {
        super.onBindView(view)

        mView = view

        mProfileSpinner = view.findViewById(R.id.profile_spinner) as Spinner
        mProfileActionButton = view.findViewById(R.id.profile_action_button) as Button

        initLayout()

        updateButtonSetup()
    }

    private fun initLayout() {
        if (DEBUG) Log.i(TAG, "Starting initLayout")
        // The default operations first need to be converted to an ArrayList,
        // because the ArrayAdapter will turn it into an AbstractList otherwise,
        // which doesn't support certain actions, like adding elements.
        // See: http://stackoverflow.com/a/3200631
        mDefaultOperations = ArrayList<CharSequence>(Arrays.asList(*context.resources.getStringArray(R.array.standard_profiles_array)))
        mArrayAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, mDefaultOperations)
        mArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        readProfiles()

        mProfileSpinner.adapter = mArrayAdapter
        mProfileSpinner.setSelection(mProfile)
        mProfileSpinner.onItemSelectedListener = this
    }

    private fun updateButtonSetup() {
        if (mProfile > DEFAULT_OPERATIONS_AM - 1) {
            if (DEBUG) Log.i(TAG, "Setting remove button")
            mProfileActionButton.text = context.resources.getString(R.string.button_remove_profile)
            mProfileActionButton.setOnClickListener { openRemoveProfileDialog() }

        } else {
            if (DEBUG) Log.i(TAG, "Setting add button")
            mProfileActionButton.text = context.resources.getString(R.string.button_add_profile)
            mProfileActionButton.setOnClickListener { openAddNewProfileDialog() }
        }
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View,
                                pos: Int, id: Long) {
        if (DEBUG) Log.i(TAG, "Item $pos selected")
        mProfile = pos
        persistInt(mProfile)
        updateButtonSetup()

        // Update the dependent settings
        if (mProfile != 0) {
            // We need a ProfilesModel to get the properties of the
            // profile from the index
            val profilesModel = ProfilesModel(mContext)
            val profileObject = ProfilesHelper.getProfile(profilesModel, mProfile, mContext)

            mSettingsModel.dimLevel = profileObject.mDimProgress
            mSettingsModel.intensityLevel = profileObject.mIntensityProgress
            mSettingsModel.color = profileObject.mColorProgress
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
    }

    private fun openRemoveProfileDialog() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(context.resources.getString(R.string.remove_profile_dialog_title))

        val okString = context.resources.getString(R.string.button_remove_profile)
        val cancelString = context.resources.getString(R.string.cancel_dialog)

        builder.setPositiveButton(okString) { dialog, which ->
            mProfilesModel.removeProfile(mProfile - DEFAULT_OPERATIONS_AM)
            mProfile = 0
            initLayout()

            updateAmmountProfiles()
        }

        builder.setNegativeButton(cancelString) { dialog, which -> dialog.cancel() }

        builder.show()
    }

    private fun openAddNewProfileDialog() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(context.resources.getString(R.string.add_new_profile_dialog_title))

        val nameInput = EditText(context)
        nameInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        nameInput.hint = context.resources.getString(R.string.add_new_profile_edit_hint)

        builder.setView(nameInput)

        val okString = context.resources.getString(R.string.ok_dialog)
        val cancelString = context.resources.getString(R.string.cancel_dialog)

        builder.setPositiveButton(okString) { dialog, which ->
            if (nameInput.text.toString().trim { it <= ' ' } != "") {
                val profile = ProfilesModel.Profile(nameInput.text.toString(),
                        colorTemperatureProgress,
                        intensityLevelProgress,
                        dimLevelProgress)

                mProfilesModel.addProfile(profile)
                mArrayAdapter.add(profile.mProfileName as CharSequence)

                mProfileSpinner.setSelection(mProfilesModel.profiles.size - 1 + DEFAULT_OPERATIONS_AM)

                updateAmmountProfiles()
            } else {
                dialog.cancel()
            }
        }

        builder.setNegativeButton(cancelString) { dialog, which -> dialog.cancel() }

        builder.show()
    }

    //Section: Reading and writing preference states

    private var colorTemperatureProgress: Int
        get() = (context as ShadesActivity).colorTempProgress
        set(progress) {
            currentColor = progress
            val fragment = (context as ShadesActivity).fragment
            val colorPref = fragment.findPreference(context.resources.getString(R.string.pref_key_shades_color_temp)) as ColorSeekBarPreference

            colorPref.mColorTempSeekBar.progress = progress
        }

    private var intensityLevelProgress: Int
        get() = (context as ShadesActivity).intensityLevelProgress
        set(progress) {
            currentIntensity = progress
            val fragment = (context as ShadesActivity).fragment
            val intensityPref = fragment.findPreference(context.resources.getString(R.string.pref_key_shades_intensity_level)) as IntensitySeekBarPreference

            intensityPref.mIntensityLevelSeekBar.progress = progress
        }

    private var dimLevelProgress: Int
        get() = (context as ShadesActivity).dimLevelProgress
        set(progress) {
            currentDim = progress
            val fragment = (context as ShadesActivity).fragment
            val dimPref = fragment.findPreference(context.resources.getString(R.string.pref_key_shades_dim_level)) as DimSeekBarPreference

            dimPref.mDimLevelSeekBar.progress = progress
        }

    //Section: Reading and writing profiles

    /**
     * Reads the profiles saved in the SharedPreference in the spinner
     */
    fun readProfiles() {
        val profiles = mProfilesModel.profiles

        for (profile in profiles) {
            mArrayAdapter.add(profile.mProfileName as CharSequence)
        }
    }

    /**
     * Updates the ammount of profiles in the shared preferences
     */
    private fun updateAmmountProfiles() {
        val ammountProfiles = mProfilesModel.profiles.size + DEFAULT_OPERATIONS_AM
        if (DEBUG) Log.i(TAG, "There are now $ammountProfiles profiles.")
        mSettingsModel.ammountProfiles = ammountProfiles
    }

    @Subscribe fun onDimLevelChanged(event: dimLevelChanged) {
        val dimLevel = event.newValue
        if (dimLevel == currentDim) return
        mProfileSpinner.setSelection(0)
    }

    @Subscribe fun onIntensityLevelChanged(event: intensityLevelChanged) {
        val intensityLevel = event.newValue
        if (intensityLevel == currentIntensity) return
        mProfileSpinner.setSelection(0)
    }

    @Subscribe fun onColorChanged(event: colorChanged) {
        val color = event.newValue
        if (color == currentColor) return
        mProfileSpinner.setSelection(0)
    }

    @Subscribe fun onProfileChanged(event: profileChanged) {
        val profile = event.newValue
        mProfile = profile
        mProfileSpinner.setSelection(mProfile)

        if (mProfile != 0) {
            val newProfile = ProfilesHelper.getProfile(mProfilesModel, mProfile, mContext)

            currentDim = newProfile.mDimProgress
            currentIntensity = newProfile.mIntensityProgress
            currentColor = newProfile.mColorProgress
        }
    }

    companion object {
        val DEFAULT_VALUE = 1

        private val TAG = "ProfileSelectorPref"
        private val DEBUG = false

        val DEFAULT_OPERATIONS_AM = 3
    }
}
