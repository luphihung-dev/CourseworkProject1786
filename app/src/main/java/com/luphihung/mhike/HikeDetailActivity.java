package com.luphihung.mhike;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.luphihung.mhike.database.HikeDao;
import com.luphihung.mhike.model.Hike;
import com.luphihung.mhike.util.Formats;
import com.luphihung.mhike.util.InsetsHelper;

/**
 * Shows the full details of one hike, with actions to edit or delete it.
 */
public class HikeDetailActivity extends AppCompatActivity {

    /** Intent extra carrying the id of the hike to display. */
    public static final String EXTRA_HIKE_ID = "com.luphihung.mhike.EXTRA_HIKE_ID";

    private HikeDao hikeDao;
    private Hike hike;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hike_detail);
        InsetsHelper.applySystemBarPadding(findViewById(R.id.root_layout));

        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.inflateMenu(R.menu.menu_hike_detail);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_edit_hike) {
                openEditor();
                return true;
            } else if (item.getItemId() == R.id.action_delete_hike) {
                confirmDelete();
                return true;
            }
            return false;
        });

        hikeDao = new HikeDao(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload in case the hike was just edited.
        long hikeId = getIntent().getLongExtra(EXTRA_HIKE_ID, Hike.UNSAVED_ID);
        hike = hikeDao.getById(hikeId);
        if (hike == null) {
            // The hike was deleted while this screen was in the background.
            finish();
            return;
        }
        showHike(hike);
    }

    private void showHike(Hike hike) {
        toolbar.setTitle(hike.getName());
        setText(R.id.detail_location, hike.getLocation());
        setText(R.id.detail_date, Formats.displayDate(hike.getDate()));
        setText(R.id.detail_parking,
                getString(hike.isParkingAvailable() ? R.string.option_yes : R.string.option_no));
        setText(R.id.detail_length,
                Formats.compactNumber(hike.getLengthKm()) + " " + getString(R.string.suffix_km));
        setText(R.id.detail_difficulty, hike.getDifficulty());
        setText(R.id.detail_duration, hike.getEstimatedDurationHours() > 0
                ? Formats.compactNumber(hike.getEstimatedDurationHours()) + " "
                        + getString(R.string.suffix_hours)
                : getString(R.string.value_not_provided));
        setText(R.id.detail_terrain, isBlank(hike.getTerrainType())
                ? getString(R.string.value_not_provided) : hike.getTerrainType());
        setText(R.id.detail_description, isBlank(hike.getDescription())
                ? getString(R.string.value_not_provided) : hike.getDescription());
    }

    private void openEditor() {
        Intent intent = new Intent(this, AddEditHikeActivity.class);
        intent.putExtra(AddEditHikeActivity.EXTRA_HIKE, hike);
        startActivity(intent);
    }

    /** Asks for confirmation first because deleting a hike cannot be undone. */
    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_hike_title)
                .setMessage(getString(R.string.delete_hike_message, hike.getName()))
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    hikeDao.delete(hike.getId());
                    Toast.makeText(this, R.string.message_hike_deleted, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void setText(int viewId, String value) {
        ((TextView) findViewById(viewId)).setText(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
