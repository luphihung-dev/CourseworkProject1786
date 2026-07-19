package com.luphihung.mhike;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

/**
 * Home screen of M-Hike. Shows the list of hikes the user has planned
 * and provides the entry point for adding a new hike.
 */
public class MainActivity extends AppCompatActivity {

    private RecyclerView hikeRecyclerView;
    private LinearLayout emptyStateLayout;
    private ExtendedFloatingActionButton addHikeFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));

        hikeRecyclerView = findViewById(R.id.recycler_hikes);
        emptyStateLayout = findViewById(R.id.layout_empty_state);
        addHikeFab = findViewById(R.id.fab_add_hike);

        hikeRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        addHikeFab.setOnClickListener(v ->
                startActivity(new Intent(this, AddEditHikeActivity.class)));
        updateEmptyState(true);
    }

    /** Toggles between the hike list and the empty-state placeholder. */
    private void updateEmptyState(boolean isEmpty) {
        emptyStateLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        hikeRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
}
