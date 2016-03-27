package com.jmstudios.redmoon.activity;

import android.app.Activity;
import android.util.Log;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.jmstudios.redmoon.R;
import com.jmstudios.redmoon.helper.FilterCommandFactory;
import com.jmstudios.redmoon.model.SettingsModel;
import com.jmstudios.redmoon.service.ScreenFilterService;
import com.jmstudios.redmoon.activity.ShadesActivity;

public class ShortcutCreationActivity extends Activity {
    public static final boolean DEBUG = true;
    public static final String TAG = "ShortcutCreation";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEBUG) Log.i(TAG, "Create ShortcutCreationActivity");
        super.onCreate(savedInstanceState);

        Intent shortcutIntent = new Intent(this, ShadesActivity.class);
        shortcutIntent.putExtra(ShadesActivity.EXTRA_FROM_SHORTCUT_BOOL, true);
        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // See: http://www.kind-kristiansen.no/2010/android-adding-desktop-shortcut-support-to-your-app/
        Intent.ShortcutIconResource iconResource =
            Intent.ShortcutIconResource.fromContext(this, R.drawable.toggle_icon);
         
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
                        getResources().getString(R.string.toggle_shortcut_title));
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
        setResult(RESULT_OK, intent);

        finish();
    }
}
