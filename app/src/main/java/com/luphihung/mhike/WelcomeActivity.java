package com.luphihung.mhike;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * One-time introduction screen shown on the very first launch.
 * After the user taps "Get started" the app remembers this in
 * SharedPreferences and boots straight into the hike list from then on.
 */
public class WelcomeActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "mhike_prefs";
    private static final String KEY_WELCOME_SEEN = "welcome_seen";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (prefs.getBoolean(KEY_WELCOME_SEEN, false)) {
            // Returning user: skip the introduction entirely.
            openHome();
            return;
        }

        setContentView(R.layout.activity_welcome);
        findViewById(R.id.button_get_started).setOnClickListener(v -> {
            prefs.edit().putBoolean(KEY_WELCOME_SEEN, true).apply();
            openHome();
        });
    }

    private void openHome() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
