package com.luphihung.mhike;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.appcompat.widget.Toolbar;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import com.luphihung.mhike.adapter.ObservationAdapter;
import com.luphihung.mhike.database.HikeDao;
import com.luphihung.mhike.database.ObservationDao;
import com.luphihung.mhike.model.Hike;
import com.luphihung.mhike.model.Observation;
import com.luphihung.mhike.util.Formats;
import com.luphihung.mhike.util.InsetsHelper;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Shows the full details of one hike, with actions to edit or delete it.
 */
public class HikeDetailActivity extends AppCompatActivity
        implements ObservationAdapter.OnObservationActionListener {

    /** Intent extra carrying the id of the hike to display. */
    public static final String EXTRA_HIKE_ID = "com.luphihung.mhike.EXTRA_HIKE_ID";

    private HikeDao hikeDao;
    private ObservationDao observationDao;
    private Hike hike;
    private Toolbar toolbar;
    private ObservationAdapter observationAdapter;
    private RecyclerView observationRecyclerView;
    private TextView noObservationsText;

    /** Launches the camera app; the result lands in the pending photo file. */
    private ActivityResultLauncher<Uri> takePictureLauncher;
    /** File the camera is currently writing to, before the user confirms. */
    private File pendingPhotoFile;
    /** Photo attached to the observation dialog that is currently open. */
    private String dialogPhotoPath;
    private ImageView dialogPhotoPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hike_detail);
        InsetsHelper.applySystemBarPadding(findViewById(R.id.root_layout));

        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.inflateMenu(R.menu.menu_hike_detail);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_share_hike) {
                shareHike();
                return true;
            } else if (item.getItemId() == R.id.action_edit_hike) {
                openEditor();
                return true;
            } else if (item.getItemId() == R.id.action_delete_hike) {
                confirmDelete();
                return true;
            }
            return false;
        });

        hikeDao = new HikeDao(this);
        observationDao = new ObservationDao(this);

        observationAdapter = new ObservationAdapter(this);
        observationRecyclerView = findViewById(R.id.recycler_observations);
        observationRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        observationRecyclerView.setAdapter(observationAdapter);
        noObservationsText = findViewById(R.id.text_no_observations);

        findViewById(R.id.button_add_observation).setOnClickListener(v ->
                showObservationDialog(null));

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(), photoTaken -> {
                    if (photoTaken && pendingPhotoFile != null) {
                        dialogPhotoPath = pendingPhotoFile.getAbsolutePath();
                        showPhotoPreview();
                    } else if (pendingPhotoFile != null) {
                        // The user backed out of the camera; discard the empty file.
                        pendingPhotoFile.delete();
                    }
                    pendingPhotoFile = null;
                });
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
        loadObservations();
    }

    private void loadObservations() {
        List<Observation> observations = observationDao.getAllForHike(hike.getId());
        observationAdapter.submitList(observations);
        boolean isEmpty = observations.isEmpty();
        noObservationsText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        observationRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
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

    /** Sends the hike details as plain text to any app the user picks. */
    private void shareHike() {
        String shareText = getString(R.string.app_name) + " – " + hike.getName() + "\n"
                + getString(R.string.label_location) + ": " + hike.getLocation() + "\n"
                + getString(R.string.label_date) + ": " + Formats.displayDate(hike.getDate()) + "\n"
                + getString(R.string.label_length) + ": "
                + Formats.compactNumber(hike.getLengthKm()) + " " + getString(R.string.suffix_km) + "\n"
                + getString(R.string.label_difficulty) + ": " + hike.getDifficulty();

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(sendIntent, getString(R.string.share_hike_title)));
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

    /**
     * Shows the dialog used both for adding a new observation and editing an
     * existing one. The time defaults to now for new observations, as the
     * specification recommends.
     *
     * @param observationToEdit the observation to edit, or null to add a new one
     */
    private void showObservationDialog(Observation observationToEdit) {
        View formView = getLayoutInflater().inflate(R.layout.dialog_observation_form, null);
        TextInputLayout observationLayout = formView.findViewById(R.id.input_layout_observation);
        TextInputEditText observationInput = formView.findViewById(R.id.edit_observation);
        TextInputEditText timeInput = formView.findViewById(R.id.edit_observation_time);
        TextInputEditText commentsInput = formView.findViewById(R.id.edit_observation_comments);

        // The chosen timestamp lives in a Calendar so the pickers can update
        // the date and the time parts independently.
        Calendar selectedTime = Calendar.getInstance();
        if (observationToEdit != null) {
            observationInput.setText(observationToEdit.getText());
            commentsInput.setText(observationToEdit.getComments());
            try {
                Date parsed = Formats.STORAGE_DATE_TIME.parse(observationToEdit.getObservedAt());
                if (parsed != null) {
                    selectedTime.setTime(parsed);
                }
            } catch (java.text.ParseException ignored) {
                // Fall back to the current time if the stored value is unreadable.
            }
        }
        timeInput.setText(Formats.DISPLAY_DATE_TIME.format(selectedTime.getTime()));
        timeInput.setOnClickListener(v -> pickDateAndTime(selectedTime, timeInput));

        // Optional photo attached to the observation.
        dialogPhotoPath = observationToEdit == null ? null : observationToEdit.getPhotoPath();
        dialogPhotoPreview = formView.findViewById(R.id.image_observation_photo);
        showPhotoPreview();
        formView.findViewById(R.id.button_take_photo).setOnClickListener(v -> launchCamera());

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(observationToEdit == null
                        ? R.string.title_new_observation : R.string.title_edit_observation)
                .setView(formView)
                .setPositiveButton(R.string.action_save, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.show();

        // Override the positive button so the dialog stays open when validation fails.
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String text = observationInput.getText() == null
                            ? "" : observationInput.getText().toString().trim();
                    if (text.isEmpty()) {
                        observationLayout.setError(getString(R.string.error_observation_required));
                        return;
                    }
                    Observation observation = observationToEdit == null
                            ? new Observation() : observationToEdit;
                    observation.setHikeId(hike.getId());
                    observation.setText(text);
                    observation.setObservedAt(
                            Formats.STORAGE_DATE_TIME.format(selectedTime.getTime()));
                    observation.setComments(commentsInput.getText() == null
                            ? "" : commentsInput.getText().toString().trim());
                    observation.setPhotoPath(dialogPhotoPath);

                    if (observation.getId() == Observation.UNSAVED_ID) {
                        observationDao.insert(observation);
                    } else {
                        observationDao.update(observation);
                    }
                    Toast.makeText(this, R.string.message_observation_saved,
                            Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadObservations();
                });
    }

    /** Creates a destination file and hands it to the device's camera app. */
    private void launchCamera() {
        try {
            File photoDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            pendingPhotoFile = new File(photoDir,
                    "observation_" + System.currentTimeMillis() + ".jpg");
            Uri photoUri = FileProvider.getUriForFile(this,
                    "com.luphihung.mhike.fileprovider", pendingPhotoFile);
            takePictureLauncher.launch(photoUri);
        } catch (Exception e) {
            Toast.makeText(this, R.string.message_photo_failed, Toast.LENGTH_SHORT).show();
        }
    }

    /** Shows the attached photo in the open dialog, or hides the preview. */
    private void showPhotoPreview() {
        if (dialogPhotoPreview == null) {
            return;
        }
        if (dialogPhotoPath != null && new File(dialogPhotoPath).exists()) {
            dialogPhotoPreview.setImageBitmap(BitmapFactory.decodeFile(dialogPhotoPath));
            dialogPhotoPreview.setVisibility(View.VISIBLE);
        } else {
            dialogPhotoPreview.setVisibility(View.GONE);
        }
    }

    /** Opens a date picker followed by a time picker to set the observation time. */
    private void pickDateAndTime(Calendar selectedTime, TextInputEditText timeInput) {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.hint_observation_time)
                .setSelection(selectedTime.getTimeInMillis())
                .build();
        datePicker.addOnPositiveButtonClickListener(utcMillis -> {
            Calendar picked = Calendar.getInstance();
            picked.setTimeInMillis(utcMillis);
            selectedTime.set(picked.get(Calendar.YEAR), picked.get(Calendar.MONTH),
                    picked.get(Calendar.DAY_OF_MONTH));

            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(selectedTime.get(Calendar.HOUR_OF_DAY))
                    .setMinute(selectedTime.get(Calendar.MINUTE))
                    .build();
            timePicker.addOnPositiveButtonClickListener(v -> {
                selectedTime.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                selectedTime.set(Calendar.MINUTE, timePicker.getMinute());
                timeInput.setText(Formats.DISPLAY_DATE_TIME.format(selectedTime.getTime()));
            });
            timePicker.show(getSupportFragmentManager(), "observation_time_picker");
        });
        datePicker.show(getSupportFragmentManager(), "observation_date_picker");
    }

    @Override
    public void onEditObservation(Observation observation) {
        showObservationDialog(observation);
    }

    @Override
    public void onDeleteObservation(Observation observation) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_observation_title)
                .setMessage(R.string.delete_observation_message)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    observationDao.delete(observation.getId());
                    Toast.makeText(this, R.string.message_observation_deleted,
                            Toast.LENGTH_SHORT).show();
                    loadObservations();
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
