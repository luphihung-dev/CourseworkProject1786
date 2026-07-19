package com.luphihung.mhike;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;

import com.luphihung.mhike.adapter.HikeAdapter;
import com.luphihung.mhike.database.HikeDao;
import com.luphihung.mhike.model.Hike;
import com.luphihung.mhike.util.Formats;
import com.luphihung.mhike.util.InsetsHelper;

import java.util.Date;
import java.util.List;

/**
 * Search screen. Typing in the name field filters hikes live by name
 * prefix; the advanced filters narrow the results further by location,
 * length range and date.
 */
public class SearchActivity extends AppCompatActivity {

    private HikeDao hikeDao;
    private HikeAdapter resultsAdapter;
    private TextView resultCountText;

    private TextInputEditText nameInput;
    private TextInputEditText locationInput;
    private TextInputEditText minLengthInput;
    private TextInputEditText maxLengthInput;
    private TextInputEditText dateInput;
    private View filtersLayout;
    private MaterialButton toggleFiltersButton;

    /** Date filter in storage format (yyyy-MM-dd), or null when not set. */
    private String filterDateIso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        InsetsHelper.applySystemBarPadding(findViewById(R.id.root_layout));

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        hikeDao = new HikeDao(this);
        resultsAdapter = new HikeAdapter(this::openHikeDetail);
        RecyclerView resultsRecyclerView = findViewById(R.id.recycler_search_results);
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        resultsRecyclerView.setAdapter(resultsAdapter);
        resultCountText = findViewById(R.id.text_result_count);

        nameInput = findViewById(R.id.edit_search_name);
        locationInput = findViewById(R.id.edit_filter_location);
        minLengthInput = findViewById(R.id.edit_filter_min_length);
        maxLengthInput = findViewById(R.id.edit_filter_max_length);
        dateInput = findViewById(R.id.edit_filter_date);
        filtersLayout = findViewById(R.id.layout_filters);
        toggleFiltersButton = findViewById(R.id.button_toggle_filters);

        toggleFiltersButton.setOnClickListener(v -> toggleFilters());
        setUpDateFilter();

        // Re-run the search whenever any criterion changes.
        TextWatcher searchWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                runSearch();
            }
        };
        nameInput.addTextChangedListener(searchWatcher);
        locationInput.addTextChangedListener(searchWatcher);
        minLengthInput.addTextChangedListener(searchWatcher);
        maxLengthInput.addTextChangedListener(searchWatcher);
        dateInput.addTextChangedListener(searchWatcher);

        runSearch();
    }

    private void toggleFilters() {
        boolean showing = filtersLayout.getVisibility() == View.VISIBLE;
        filtersLayout.setVisibility(showing ? View.GONE : View.VISIBLE);
        toggleFiltersButton.setText(showing
                ? R.string.action_show_filters : R.string.action_hide_filters);
    }

    private void setUpDateFilter() {
        dateInput.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(R.string.hint_filter_date)
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();
            picker.addOnPositiveButtonClickListener(utcMillis -> {
                Date picked = new Date(utcMillis);
                filterDateIso = Formats.STORAGE_DATE.format(picked);
                dateInput.setText(Formats.DISPLAY_DATE.format(picked));
            });
            picker.show(getSupportFragmentManager(), "search_date_picker");
        });
    }

    private void runSearch() {
        // The clear-text icon empties the field, so drop the stored date too.
        if (dateInput.getText() == null || dateInput.getText().length() == 0) {
            filterDateIso = null;
        }

        List<Hike> results = hikeDao.search(
                textOf(nameInput),
                textOf(locationInput),
                parseLengthOrZero(minLengthInput),
                parseLengthOrZero(maxLengthInput),
                filterDateIso);

        resultsAdapter.submitList(results);
        resultCountText.setText(results.isEmpty()
                ? getString(R.string.search_no_results)
                : getResources().getQuantityString(
                        R.plurals.search_result_count, results.size(), results.size()));
    }

    private void openHikeDetail(Hike hike) {
        Intent intent = new Intent(this, HikeDetailActivity.class);
        intent.putExtra(HikeDetailActivity.EXTRA_HIKE_ID, hike.getId());
        startActivity(intent);
    }

    private String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    /** Reads a length filter, treating blanks and invalid numbers as "not set". */
    private double parseLengthOrZero(TextInputEditText input) {
        try {
            return Double.parseDouble(textOf(input));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
