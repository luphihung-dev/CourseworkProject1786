package com.luphihung.mhike;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.button.MaterialButtonToggleGroup;

import com.luphihung.mhike.widget.RainView;

/**
 * One-time introduction screen shown on the very first launch.
 * The scenery slowly pans (Ken Burns effect) and the sunny/rainy toggle
 * previews the app's light or dark theme — rainy mode adds an animated
 * rain shower. The choice is applied when the user taps "Get started".
 */
public class WelcomeActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "mhike_prefs";
    public static final String KEY_DARK_THEME = "dark_theme";
    private static final String KEY_WELCOME_SEEN = "welcome_seen";

    private SharedPreferences prefs;
    private ImageView sceneryImage;
    private RainView rainView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (prefs.getBoolean(KEY_WELCOME_SEEN, false)) {
            // Returning user: skip the introduction entirely.
            openHome();
            return;
        }

        setContentView(R.layout.activity_welcome);
        sceneryImage = findViewById(R.id.image_scenery);
        rainView = findViewById(R.id.rain_view);

        MaterialButtonToggleGroup weatherToggle = findViewById(R.id.toggle_weather);
        boolean darkTheme = prefs.getBoolean(KEY_DARK_THEME, false);
        weatherToggle.check(darkTheme ? R.id.button_mode_rainy : R.id.button_mode_sunny);
        showWeather(darkTheme);
        weatherToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                boolean rainy = checkedId == R.id.button_mode_rainy;
                prefs.edit().putBoolean(KEY_DARK_THEME, rainy).apply();
                showWeather(rainy);
            }
        });

        startKenBurns();

        findViewById(R.id.button_get_started).setOnClickListener(v -> {
            prefs.edit().putBoolean(KEY_WELCOME_SEEN, true).apply();
            applySavedTheme(prefs);
            openHome();
        });
    }

    /** Swaps the scenery photo and starts/stops the rain shower. */
    private void showWeather(boolean rainy) {
        sceneryImage.setImageResource(rainy
                ? R.drawable.welcome_rainy : R.drawable.welcome_sunny);
        rainView.setVisibility(rainy ? View.VISIBLE : View.GONE);
    }

    /**
     * Slow, endless zoom-and-drift on the scenery so the screen feels
     * alive even without touching anything.
     */
    private void startKenBurns() {
        ObjectAnimator drift = ObjectAnimator.ofPropertyValuesHolder(sceneryImage,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1.08f, 1.22f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.08f, 1.22f),
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0f, -30f));
        drift.setDuration(18000);
        drift.setInterpolator(new LinearInterpolator());
        drift.setRepeatCount(ValueAnimator.INFINITE);
        drift.setRepeatMode(ValueAnimator.REVERSE);
        drift.start();
    }

    /** Applies the stored light/dark preference to the whole app. */
    public static void applySavedTheme(SharedPreferences prefs) {
        AppCompatDelegate.setDefaultNightMode(prefs.getBoolean(KEY_DARK_THEME, false)
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void openHome() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
