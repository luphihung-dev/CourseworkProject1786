package com.luphihung.mhike;

import android.app.Application;

/**
 * Applies the user's saved light/dark theme before any screen opens,
 * so the whole app starts in the mode chosen on the welcome screen.
 */
public class MHikeApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        WelcomeActivity.applySavedTheme(
                getSharedPreferences(WelcomeActivity.PREFS_NAME, MODE_PRIVATE));
    }
}
