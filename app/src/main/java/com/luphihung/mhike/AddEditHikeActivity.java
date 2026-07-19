package com.luphihung.mhike;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.luphihung.mhike.model.Hike;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Screen for entering a new hike or editing an existing one.
 * All required fields are validated before the user is shown a
 * confirmation dialog summarising what will be saved.
 */
public class AddEditHikeActivity extends AppCompatActivity {

    /** Intent extra carrying the hike to edit; absent when adding a new hike. */
    public static final String EXTRA_HIKE = "com.luphihung.mhike.EXTRA_HIKE";

    /** Format used to store dates in the database (sorts chronologically as text). */
    private static final SimpleDateFormat STORAGE_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    /** Format used to show dates to the user. */
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT =
            new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());

    private TextInputLayout nameLayout;
    private TextInputLayout locationLayout;
    private TextInputLayout dateLayout;
    private TextInputLayout lengthLayout;
    private TextInputLayout difficultyLayout;
    private TextInputLayout durationLayout;
    private TextInputEditText nameInput;
    private TextInputEditText locationInput;
    private TextInputEditText dateInput;
    private TextInputEditText lengthInput;
    private TextInputEditText durationInput;
    private TextInputEditText descriptionInput;
    private AutoCompleteTextView difficultyDropdown;
    private AutoCompleteTextView terrainDropdown;
    private MaterialButtonToggleGroup parkingToggle;
    private TextView parkingErrorText;

    /** Date chosen with the picker, kept in storage format (yyyy-MM-dd). */
    private String selectedDateIso;
    /** Hike being edited, or null when creating a new one. */
    private Hike hikeBeingEdited;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_hike);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        bindViews();
        setUpDropdowns();
        setUpDatePicker();
        setUpValidationReset();

        hikeBeingEdited = (Hike) getIntent().getSerializableExtra(EXTRA_HIKE);
        if (hikeBeingEdited != null) {
            toolbar.setTitle(R.string.title_edit_hike);
            populateForm(hikeBeingEdited);
        }

        findViewById(R.id.button_save).setOnClickListener(v -> onSaveClicked());
    }

    private void bindViews() {
        nameLayout = findViewById(R.id.input_layout_name);
        locationLayout = findViewById(R.id.input_layout_location);
        dateLayout = findViewById(R.id.input_layout_date);
        lengthLayout = findViewById(R.id.input_layout_length);
        difficultyLayout = findViewById(R.id.input_layout_difficulty);
        durationLayout = findViewById(R.id.input_layout_duration);
        nameInput = findViewById(R.id.edit_name);
        locationInput = findViewById(R.id.edit_location);
        dateInput = findViewById(R.id.edit_date);
        lengthInput = findViewById(R.id.edit_length);
        durationInput = findViewById(R.id.edit_duration);
        descriptionInput = findViewById(R.id.edit_description);
        difficultyDropdown = findViewById(R.id.dropdown_difficulty);
        terrainDropdown = findViewById(R.id.dropdown_terrain);
        parkingToggle = findViewById(R.id.toggle_parking);
        parkingErrorText = findViewById(R.id.text_parking_error);
    }

    private void setUpDropdowns() {
        difficultyDropdown.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                getResources().getStringArray(R.array.difficulty_levels)));
        terrainDropdown.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                getResources().getStringArray(R.array.terrain_types)));
    }

    /** The date field is read-only; tapping it opens a Material date picker. */
    private void setUpDatePicker() {
        dateInput.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(R.string.hint_date)
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();
            picker.addOnPositiveButtonClickListener(utcMillis -> {
                Date picked = new Date(utcMillis);
                selectedDateIso = STORAGE_DATE_FORMAT.format(picked);
                dateInput.setText(DISPLAY_DATE_FORMAT.format(picked));
                dateLayout.setError(null);
            });
            picker.show(getSupportFragmentManager(), "hike_date_picker");
        });
    }

    /** Clears field errors as soon as the user corrects the input. */
    private void setUpValidationReset() {
        nameInput.setOnFocusChangeListener((v, focus) -> { if (focus) nameLayout.setError(null); });
        locationInput.setOnFocusChangeListener((v, focus) -> { if (focus) locationLayout.setError(null); });
        lengthInput.setOnFocusChangeListener((v, focus) -> { if (focus) lengthLayout.setError(null); });
        difficultyDropdown.setOnItemClickListener((parent, view, position, id) ->
                difficultyLayout.setError(null));
        parkingToggle.addOnButtonCheckedListener((group, checkedId, isChecked) ->
                parkingErrorText.setVisibility(View.GONE));
    }

    /** Fills the form with an existing hike's values when editing. */
    private void populateForm(Hike hike) {
        nameInput.setText(hike.getName());
        locationInput.setText(hike.getLocation());
        selectedDateIso = hike.getDate();
        dateInput.setText(formatDateForDisplay(hike.getDate()));
        parkingToggle.check(hike.isParkingAvailable()
                ? R.id.button_parking_yes : R.id.button_parking_no);
        lengthInput.setText(formatNumber(hike.getLengthKm()));
        difficultyDropdown.setText(hike.getDifficulty(), false);
        descriptionInput.setText(hike.getDescription());
        if (hike.getEstimatedDurationHours() > 0) {
            durationInput.setText(formatNumber(hike.getEstimatedDurationHours()));
        }
        terrainDropdown.setText(hike.getTerrainType(), false);
    }

    private void onSaveClicked() {
        Hike hike = validateForm();
        if (hike != null) {
            showConfirmationDialog(hike);
        }
    }

    /**
     * Checks every required field and shows an inline error message on each
     * one that is missing or invalid.
     *
     * @return a populated {@link Hike} when the form is valid, otherwise null
     */
    private Hike validateForm() {
        boolean valid = true;

        String name = textOf(nameInput);
        if (name.isEmpty()) {
            nameLayout.setError(getString(R.string.error_name_required));
            valid = false;
        }

        String location = textOf(locationInput);
        if (location.isEmpty()) {
            locationLayout.setError(getString(R.string.error_location_required));
            valid = false;
        }

        if (selectedDateIso == null) {
            dateLayout.setError(getString(R.string.error_date_required));
            valid = false;
        }

        int checkedParkingId = parkingToggle.getCheckedButtonId();
        if (checkedParkingId == View.NO_ID) {
            parkingErrorText.setVisibility(View.VISIBLE);
            valid = false;
        }

        double lengthKm = 0;
        String lengthText = textOf(lengthInput);
        if (lengthText.isEmpty()) {
            lengthLayout.setError(getString(R.string.error_length_required));
            valid = false;
        } else {
            try {
                lengthKm = Double.parseDouble(lengthText);
                if (lengthKm <= 0) {
                    lengthLayout.setError(getString(R.string.error_length_invalid));
                    valid = false;
                }
            } catch (NumberFormatException e) {
                lengthLayout.setError(getString(R.string.error_length_invalid));
                valid = false;
            }
        }

        String difficulty = difficultyDropdown.getText().toString().trim();
        if (difficulty.isEmpty()) {
            difficultyLayout.setError(getString(R.string.error_difficulty_required));
            valid = false;
        }

        // Optional field: only validated when the user typed something.
        double durationHours = 0;
        String durationText = textOf(durationInput);
        if (!durationText.isEmpty()) {
            try {
                durationHours = Double.parseDouble(durationText);
                if (durationHours <= 0) {
                    durationLayout.setError(getString(R.string.error_duration_invalid));
                    valid = false;
                }
            } catch (NumberFormatException e) {
                durationLayout.setError(getString(R.string.error_duration_invalid));
                valid = false;
            }
        }

        if (!valid) {
            return null;
        }

        Hike hike = hikeBeingEdited != null ? hikeBeingEdited : new Hike();
        hike.setName(name);
        hike.setLocation(location);
        hike.setDate(selectedDateIso);
        hike.setParkingAvailable(checkedParkingId == R.id.button_parking_yes);
        hike.setLengthKm(lengthKm);
        hike.setDifficulty(difficulty);
        hike.setDescription(textOf(descriptionInput));
        hike.setEstimatedDurationHours(durationHours);
        hike.setTerrainType(terrainDropdown.getText().toString().trim());
        return hike;
    }

    /**
     * Shows all entered details back to the user so they can double-check
     * them before saving, as required by the specification.
     */
    private void showConfirmationDialog(Hike hike) {
        View summaryView = getLayoutInflater().inflate(R.layout.dialog_hike_summary, null);
        setSummaryText(summaryView, R.id.summary_name, hike.getName());
        setSummaryText(summaryView, R.id.summary_location, hike.getLocation());
        setSummaryText(summaryView, R.id.summary_date, formatDateForDisplay(hike.getDate()));
        setSummaryText(summaryView, R.id.summary_parking,
                getString(hike.isParkingAvailable() ? R.string.option_yes : R.string.option_no));
        setSummaryText(summaryView, R.id.summary_length,
                formatNumber(hike.getLengthKm()) + " " + getString(R.string.suffix_km));
        setSummaryText(summaryView, R.id.summary_difficulty, hike.getDifficulty());
        setSummaryText(summaryView, R.id.summary_duration,
                hike.getEstimatedDurationHours() > 0
                        ? formatNumber(hike.getEstimatedDurationHours()) + " "
                                + getString(R.string.suffix_hours)
                        : getString(R.string.value_not_provided));
        setSummaryText(summaryView, R.id.summary_terrain,
                hike.getTerrainType().isEmpty()
                        ? getString(R.string.value_not_provided) : hike.getTerrainType());
        setSummaryText(summaryView, R.id.summary_description,
                hike.getDescription().isEmpty()
                        ? getString(R.string.value_not_provided) : hike.getDescription());

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_title)
                .setView(summaryView)
                .setPositiveButton(R.string.confirm_save, (dialog, which) -> saveHike(hike))
                .setNegativeButton(R.string.confirm_edit, null)
                .show();
    }

    private void saveHike(Hike hike) {
        // Persistence is added with the database layer; for now just confirm and close.
        Toast.makeText(this, R.string.message_hike_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void setSummaryText(View root, int viewId, String value) {
        ((TextView) root.findViewById(viewId)).setText(value);
    }

    private String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    /** Converts a stored yyyy-MM-dd date into the user-facing format. */
    private String formatDateForDisplay(String isoDate) {
        try {
            Date parsed = STORAGE_DATE_FORMAT.parse(isoDate);
            return parsed == null ? isoDate : DISPLAY_DATE_FORMAT.format(parsed);
        } catch (ParseException e) {
            return isoDate;
        }
    }

    /** Formats a number without a trailing ".0" for whole values (e.g. "5" not "5.0"). */
    private String formatNumber(double value) {
        return value == Math.floor(value)
                ? String.valueOf((long) value) : String.valueOf(value);
    }
}
