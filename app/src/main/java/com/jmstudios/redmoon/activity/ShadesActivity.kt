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
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast

import com.jmstudios.redmoon.R
import com.jmstudios.redmoon.application.RedMoonApplication

import com.jmstudios.redmoon.event.*
import com.jmstudios.redmoon.fragment.*
import com.jmstudios.redmoon.helper.Util
import com.jmstudios.redmoon.model.Config
import com.jmstudios.redmoon.service.ScreenFilterService

import org.greenrobot.eventbus.EventBus

class ShadesActivity : AppCompatActivity() {

    private val context = this


    internal val fragment: FilterFragment
        get() = fragmentManager.findFragmentByTag(FRAGMENT_TAG_FILTER) as FilterFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        ScreenFilterService.start(this)
        val intent = intent
        if (DEBUG) Log.i(TAG, "Got intent")

        // Wire MVP classes
        val fromShortcut = intent.getBooleanExtra(EXTRA_FROM_SHORTCUT_BOOL, false)
        if (fromShortcut) {
            toggleAndFinish()
        }

        if (Config.darkThemeFlag) setTheme(R.style.AppThemeDark)

        setContentView(R.layout.activity_shades)
        super.onCreate(savedInstanceState)

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

        if (!Config.introShown) {
            startIntro()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_activity_menu, menu)
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
        setTitle(R.string.time_toggle_title)
    }

    fun launchSecureSuspendFragment() {
        val newFragment = SecureSuspendFragment()
        fragmentManager.beginTransaction()
                       .replace(R.id.fragment_container, newFragment, FRAGMENT_TAG_SECURE_SUSPEND)
                       .addToBackStack(null)
                       .commit()
        setTitle(R.string.time_toggle_preference_activity)
    }

    @TargetApi(23) // Android Studio can't figure out that this is safe to call at any API level
    private fun getOverlayPermission(): Boolean {
        // TODO: Verify this should be 'at least' and not 'less than' API 23
        if (Util.atLeastAPI(23)) return true

        if (!Settings.canDrawOverlays(context)) {
            val builder = AlertDialog.Builder(context)

            builder.setMessage(R.string.overlay_dialog_message)
                   .setTitle(R.string.overlay_dialog_title)
                   .setPositiveButton(R.string.ok_dialog) { dialog, id ->
                       val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                       Uri.parse("package:" + packageName))
                       startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE) }
                   .show()
        }
        return Settings.canDrawOverlays(context)
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onNewIntent(intent: Intent) {
        val fromShortcut = intent.getBooleanExtra(EXTRA_FROM_SHORTCUT_BOOL, false)
        if (fromShortcut) {
            toggleAndFinish()
        }
    }

    override fun onStop() {
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

    private fun startIntro() {
        val introIntent = Intent(this, Intro::class.java)
        startActivity(introIntent)

        Config.introShown = true
    }

    val colorTempProgress: Int
        get() = Config.color

    val intensityLevelProgress: Int
        get() = Config.intensity

    val dimLevelProgress: Int
        get() = Config.dim

    private fun toggleAndFinish() {
        EventBus.getDefault().postSticky(moveToState(
                if (Config.filterIsOn) ScreenFilterService.COMMAND_ON
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
