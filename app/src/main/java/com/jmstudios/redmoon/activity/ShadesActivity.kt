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

import com.jmstudios.redmoon.event.*
import com.jmstudios.redmoon.fragment.*
import com.jmstudios.redmoon.model.SettingsModel
import com.jmstudios.redmoon.presenter.ShadesPresenter
import com.jmstudios.redmoon.service.ScreenFilterService

import org.greenrobot.eventbus.EventBus

class ShadesActivity : AppCompatActivity() {

    lateinit internal var mPresenter: ShadesPresenter
    lateinit var mSettingsModel: SettingsModel
        private set
    lateinit private var mSwitch: Switch
    private val context = this

    private var hasShownWarningToast = false

    internal val fragment: FilterFragment
        get() = fragmentManager.findFragmentByTag(FRAGMENT_TAG_FILTER) as FilterFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        ScreenFilterService.start(this)
        val intent = intent
        if (DEBUG) Log.i(TAG, "Got intent")

        // Wire MVP classes
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        mSettingsModel = SettingsModel(resources, sharedPreferences)

        val fromShortcut = intent.getBooleanExtra(EXTRA_FROM_SHORTCUT_BOOL, false)
        if (fromShortcut) {
            toggleAndFinish()
        }


        if (mSettingsModel.darkThemeFlag) setTheme(R.style.AppThemeDark)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shades)

        val view: FilterFragment

        // Only create and attach a new fragment on the first Activity creation.
        // On Activity re-creation, retrieve the existing fragment stored in the FragmentManager.
        if (savedInstanceState == null) {
            if (DEBUG) Log.i(TAG, "onCreate - First creation")

            view = FilterFragment()

            fragmentManager.beginTransaction()
                           .replace(R.id.fragment_container, view, FRAGMENT_TAG_FILTER)
                           .commit()
        } else {
            if (DEBUG) Log.i(TAG, "onCreate - Re-creation")

            view = fragmentManager.findFragmentByTag(FRAGMENT_TAG_FILTER) as FilterFragment
        }

        mPresenter = ShadesPresenter(view, mSettingsModel, context)

        if (!mSettingsModel.introShown) {
            startIntro()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_activity_menu, menu)

        mSwitch = menu.findItem(R.id.screen_filter_switch).actionView as Switch
        mSwitch.isChecked = mSettingsModel.filterIsOn
        mSwitch.setOnClickListener {
            if (getOverlayPermission()) {
                EventBus.getDefault().postSticky(moveToState(
                          if (mSwitch.isChecked) ScreenFilterService.COMMAND_ON
                          else ScreenFilterService.COMMAND_OFF))
            } else {
                mSwitch.isChecked = false
            }
        }

        return true
    }

    override fun onDestroy() {
        ScreenFilterService.stop(this) 
        super.onDestroy()
    }

    fun launchTimeToggleFragment() {
        val newFragment = TimeToggleFragment()
        fragmentManager.beginTransaction()
                       .replace(R.id.fragment_container, newFragment, FRAGMENT_TAG_TIME_TOGGLE)
                       .addToBackStack(null)
                       .commit()
        setTitle(R.string.automatic_filter_title)
    }

    fun launchSecureSuspendFragment() {
        val newFragment = SecureSuspendFragment()
        fragmentManager.beginTransaction()
                       .replace(R.id.fragment_container, newFragment, FRAGMENT_TAG_SECURE_SUSPEND)
                       .addToBackStack(null)
                       .commit()
        setTitle(R.string.automatic_suspend_preference_activity)
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

    override fun onStart() {
        super.onStart()
        /* setSwitch(!mSettingsModel.filterIsOn) */
        mSettingsModel.openSettingsChangeListener()
        EventBus.getDefault().register(mPresenter)
    }

    override fun onNewIntent(intent: Intent) {
        val fromShortcut = intent.getBooleanExtra(EXTRA_FROM_SHORTCUT_BOOL, false)
        if (fromShortcut) {
            toggleAndFinish()
        }
    }

    override fun onStop() {
        mSettingsModel.closeSettingsChangeListener()
        EventBus.getDefault().unregister(mPresenter)
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
        if (hasShownWarningToast || mSettingsModel.automaticSuspend)
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

        mSettingsModel.introShown = true
    }

    val colorTempProgress: Int
        get() = mSettingsModel.color

    val intensityLevelProgress: Int
        get() = mSettingsModel.intensityLevel

    val dimLevelProgress: Int
        get() = mSettingsModel.dimLevel

    private fun toggleAndFinish() {
        EventBus.getDefault().postSticky(moveToState(
                if (mSettingsModel.filterIsOn) ScreenFilterService.COMMAND_ON
                else ScreenFilterService.COMMAND_OFF))
        finish()
    }

    companion object {
        private val TAG = "ShadesActivity"
        private val DEBUG = true
        private val FRAGMENT_TAG_FILTER = "jmstudios.fragment.tag.FILTER"
        private val FRAGMENT_TAG_TIME_TOGGLE = "jmstudios.fragment.tag.TIME_TOGGLE"
        private val FRAGMENT_TAG_SECURE_SUSPEND = "jmstudios.fragment.tag.SECURE_SUSPEND"


        val EXTRA_FROM_SHORTCUT_BOOL = "com.jmstudios.redmoon.activity.ShadesActivity.EXTRA_FROM_SHORTCUT_BOOL"
        var OVERLAY_PERMISSION_REQ_CODE = 1234
    }
}
