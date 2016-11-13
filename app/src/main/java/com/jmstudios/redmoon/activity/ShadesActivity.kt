/*
 * Copyright (c) 2016  Marien Raat <marienraat@riseup.net>
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
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *     Copyright (c) 2015 Chris Nguyen
 *
 *     Permission to use, copy, modify, and/or distribute this software
 *     for any purpose with or without fee is hereby granted, provided
 *     that the above copyright notice and this permission notice appear
 *     in all copies.
 *
 *     THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL
 *     WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED
 *     WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE
 *     AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR
 *     CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS
 *     OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *     NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN
 *     CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package com.jmstudios.redmoon.activity

import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Switch
import android.widget.Toast

import com.jmstudios.redmoon.R

import com.jmstudios.redmoon.fragment.ShadesFragment
import com.jmstudios.redmoon.helper.FilterCommandFactory
import com.jmstudios.redmoon.helper.FilterCommandSender
import com.jmstudios.redmoon.model.SettingsModel
import com.jmstudios.redmoon.presenter.ShadesPresenter
import com.jmstudios.redmoon.service.ScreenFilterService

class ShadesActivity : AppCompatActivity() {

    lateinit private var mPresenter: ShadesPresenter
    lateinit var fragment: ShadesFragment
        private set
    lateinit var settingsModel: SettingsModel
        private set
    lateinit private var mSwitch: Switch
    lateinit private var mFilterCommandFactory: FilterCommandFactory
    lateinit private var mFilterCommandSender: FilterCommandSender
    private val context = this

    private var hasShownWarningToast = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val intent = intent
        if (DEBUG) Log.i(TAG, "Got intent")

        // Wire MVP classes
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        settingsModel = SettingsModel(resources, sharedPreferences)
        mFilterCommandFactory = FilterCommandFactory(this)
        mFilterCommandSender = FilterCommandSender(this)

        val fromShortcut = intent.getBooleanExtra(EXTRA_FROM_SHORTCUT_BOOL, false)
        if (fromShortcut) {
            toggleAndFinish()
        }


        if (settingsModel.darkThemeFlag) setTheme(R.style.AppThemeDark)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shades)

        val fragmentManager = fragmentManager

        val view: ShadesFragment

        // Only create and attach a new fragment on the first Activity creation.
        // On Activity re-creation, retrieve the existing fragment stored in the FragmentManager.
        if (savedInstanceState == null) {
            if (DEBUG) Log.i(TAG, "onCreate - First creation")

            view = ShadesFragment()

            fragmentManager.beginTransaction().replace(R.id.fragment_container, view, FRAGMENT_TAG_SHADES).commit()
        } else {
            if (DEBUG) Log.i(TAG, "onCreate - Re-creation")

            view = fragmentManager.findFragmentByTag(FRAGMENT_TAG_SHADES) as ShadesFragment
        }

        mPresenter = ShadesPresenter(view, settingsModel,
                context)
        view.registerPresenter(mPresenter)

        // Make Presenter listen to settings changes
        settingsModel.addOnSettingsChangedListener(mPresenter)

        fragment = view

        if (!settingsModel.introShown) {
            startIntro()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_activity_menu, menu)

        val item = menu.findItem(R.id.screen_filter_switch)
        mSwitch = item.actionView as Switch
        mSwitch.isChecked = settingsModel.pauseState
        mSwitch.setOnClickListener {
            if (getOverlayPermission()) {
                sendCommand(if (mSwitch.isChecked)
                    ScreenFilterService.COMMAND_ON
                else
                    ScreenFilterService.COMMAND_PAUSE)
            } else {
                mSwitch.isChecked = false
            }
        }

        return true
    }

    fun setSwitch(onState: Boolean) {
        mSwitch.isChecked = onState
    }

    @TargetApi(23) // Android Studio can't figure out that this is safe to call at any API level
    private fun getOverlayPermission(): Boolean {
        // http://stackoverflow.com/a/3993933
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            return true
        }

        if (!Settings.canDrawOverlays(context)) {
            val builder = AlertDialog.Builder(context)

            builder.setMessage(R.string.overlay_dialog_message).setTitle(R.string.overlay_dialog_title).setPositiveButton(R.string.ok_dialog) { dialog, id ->
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + packageName))
                startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
            }

            builder.show()
        }
        return Settings.canDrawOverlays(context)
    }

    private fun sendCommand(command: Int) {
        val iCommand = mFilterCommandFactory.createCommand(command)
        mFilterCommandSender.send(iCommand)
    }

    override fun onStart() {
        super.onStart()
        settingsModel.openSettingsChangeListener()
        mPresenter.onStart()
    }

    override fun onResume() {
        super.onResume()

        // When the activity is not on the screen, but the user
        // updates the profile through the notification. the
        // notification spinner and the seekbars will have missed this
        // change. To update them correctly, we artificially change
        // these settings.
        val intensity = settingsModel.intensityLevel
        settingsModel.intensityLevel = if (intensity == 0) 1 else 0
        settingsModel.intensityLevel = intensity

        val dim = settingsModel.dimLevel
        settingsModel.dimLevel = if (dim == 0) 1 else 0
        settingsModel.dimLevel = dim

        val color = settingsModel.color
        settingsModel.color = if (color == 0) 1 else 0
        settingsModel.color = color

        // The profile HAS to be updated last, otherwise the spinner
        // will switched to custom.
        val profile = settingsModel.profile
        settingsModel.profile = if (profile == 0) 1 else 0
        settingsModel.profile = profile
    }

    override fun onNewIntent(intent: Intent) {
        val fromShortcut = intent.getBooleanExtra(EXTRA_FROM_SHORTCUT_BOOL, false)
        if (fromShortcut) {
            toggleAndFinish()
        }
    }

    override fun onStop() {
        settingsModel.closeSettingsChangeListener()
        super.onStop()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        when (item.itemId) {
            R.id.show_intro_button -> {
                startIntro()
                return true
            }
            R.id.view_github -> {
                val github = resources.getString(R.string.project_page_url)
                val projectIntent = Intent(Intent.ACTION_VIEW).setData(Uri.parse(github))
                startActivity(projectIntent)
                val email = resources.getString(R.string.contact_email_adress)
                val emailIntent = Intent(Intent.ACTION_VIEW).setData(Uri.parse(email))
                startActivity(emailIntent)
                return super.onOptionsItemSelected(item)
            }
            R.id.email_developer -> {
                val email = resources.getString(R.string.contact_email_adress)
                val emailIntent = Intent(Intent.ACTION_VIEW).setData(Uri.parse(email))
                startActivity(emailIntent)
                return super.onOptionsItemSelected(item)
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }


    fun displayInstallWarningToast() {
        if (hasShownWarningToast || settingsModel.automaticSuspend)
            return

        val duration = Toast.LENGTH_SHORT
        val toast = Toast.makeText(applicationContext,
                getString(R.string.toast_warning_install),
                duration)
        toast.show()

        hasShownWarningToast = true
    }

    private fun startIntro() {
        val introIntent = Intent(this, Intro::class.java)
        startActivity(introIntent)

        settingsModel.introShown = true
    }

    val colorTempProgress: Int
        get() = settingsModel.color

    val intensityLevelProgress: Int
        get() = settingsModel.intensityLevel

    val dimLevelProgress: Int
        get() = settingsModel.dimLevel

    private fun toggleAndFinish() {
        val paused = settingsModel.pauseState
        sendCommand(if (paused)
            ScreenFilterService.COMMAND_ON
        else
            ScreenFilterService.COMMAND_PAUSE)
        finish()
    }

    companion object {
        private val TAG = "ShadesActivity"
        private val DEBUG = false
        private val FRAGMENT_TAG_SHADES = "jmstudios.fragment.tag.SHADES"

        val EXTRA_FROM_SHORTCUT_BOOL = "com.jmstudios.redmoon.activity.ShadesActivity.EXTRA_FROM_SHORTCUT_BOOL"
        var OVERLAY_PERMISSION_REQ_CODE = 1234
    }
}
