package com.luphihung.mhike;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import com.luphihung.mhike.adapter.HikeAdapter;
import com.luphihung.mhike.database.HikeDao;
import com.luphihung.mhike.model.Hike;
import com.luphihung.mhike.util.InsetsHelper;

import java.util.List;

/**
 * Home screen of M-Hike. Shows the list of hikes the user has planned
 * and provides the entry point for adding a new hike.
 */
public class MainActivity extends AppCompatActivity {

    private RecyclerView hikeRecyclerView;
    private LinearLayout emptyStateLayout;
    private HikeAdapter hikeAdapter;
    private HikeDao hikeDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        InsetsHelper.applySystemBarPadding(findViewById(R.id.root_layout));

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_main);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_search) {
                startActivity(new Intent(this, SearchActivity.class));
                return true;
            } else if (item.getItemId() == R.id.action_switch_theme) {
                switchTheme();
                return true;
            } else if (item.getItemId() == R.id.action_delete_all) {
                confirmDeleteAll();
                return true;
            }
            return false;
        });

        hikeRecyclerView = findViewById(R.id.recycler_hikes);
        emptyStateLayout = findViewById(R.id.layout_empty_state);
        ExtendedFloatingActionButton addHikeFab = findViewById(R.id.fab_add_hike);

        hikeDao = new HikeDao(this);
        hikeAdapter = new HikeAdapter(this::openHikeDetail);
        hikeRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        hikeRecyclerView.setAdapter(hikeAdapter);

        addHikeFab.setOnClickListener(v ->
                startActivity(new Intent(this, AddEditHikeActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh whenever the user returns from adding/editing/deleting a hike.
        loadHikes();
    }

    private void loadHikes() {
        List<Hike> hikes = hikeDao.getAll();
        hikeAdapter.submitList(hikes);
        updateEmptyState(hikes.isEmpty());
    }

    private void openHikeDetail(Hike hike) {
        Intent intent = new Intent(this, HikeDetailActivity.class);
        intent.putExtra(HikeDetailActivity.EXTRA_HIKE_ID, hike.getId());
        startActivity(intent);
    }

    /** Flips the saved light/dark preference and re-themes the app instantly. */
    private void switchTheme() {
        android.content.SharedPreferences prefs =
                getSharedPreferences(WelcomeActivity.PREFS_NAME, MODE_PRIVATE);
        boolean nowDark = !prefs.getBoolean(WelcomeActivity.KEY_DARK_THEME, false);
        prefs.edit().putBoolean(WelcomeActivity.KEY_DARK_THEME, nowDark).apply();
        WelcomeActivity.applySavedTheme(prefs);
    }

    /** Resetting the database is destructive, so the user must confirm first. */
    private void confirmDeleteAll() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_all_title)
                .setMessage(R.string.delete_all_message)
                .setPositiveButton(R.string.action_delete_all, (dialog, which) -> {
                    hikeDao.deleteAll();
                    Toast.makeText(this, R.string.message_all_deleted, Toast.LENGTH_SHORT).show();
                    loadHikes();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** Toggles between the hike list and the empty-state placeholder. */
    private void updateEmptyState(boolean isEmpty) {
        emptyStateLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        hikeRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
}
