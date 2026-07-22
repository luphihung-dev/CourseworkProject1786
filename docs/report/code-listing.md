# Section 5 – Code

The listings below contain the main classes of both applications, each headed by its source file name and the language it is written in. Generated code, layout and resource XML, launcher icons and build scripts are omitted, and only the relevant part of the Android manifest is shown. Where a class is long, the listing is limited to the methods that carry the logic, and the line numbers of the extract are given. The complete project is available at https://github.com/luphihung-dev/M-Hike.

## 5.1 Configuration

### AndroidManifest.xml (extract, lines 1-12 and 39-47)  —  Android manifest, XML

The manifest declares only the two location permissions, which are needed for the optional autofill of the location field; there is deliberately no internet permission, so hike data cannot leave the device through the application. The queries element is required on Android 11 and above before the application may look for a camera application to take observation photographs with. The provider element registers a FileProvider under the authority com.luphihung.mhike.fileprovider, which issues a temporary content URI for a single photograph instead of exposing a file path. Every activity except the launcher is declared with exported set to false, so no other application can start them.

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Used to pre-fill the hike location from the device's position. -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

    <!-- Lets the app find a camera app to take observation photos with. -->
    <queries>
        <intent>
            <action android:name="android.media.action.IMAGE_CAPTURE" />
        </intent>
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="com.luphihung.mhike.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
```

## 5.2 Native Android application - data layer (Java)

### model/Hike.java  —  Java

Hike is a plain Java object that carries one row of the hikes table between the database and the screens. It holds the six compulsory fields of the specification together with the two additional fields, an estimated duration and a terrain type, and an optional description. Keeping it free of Android classes means the same object can be built from a Cursor, from the entry form, or in a test. The identifier is zero until the row has been inserted, which is how the code distinguishes a new hike from an existing one.

```java
package com.luphihung.mhike.model;

import java.io.Serializable;

/**
 * Represents a planned hike. Instances are passed between activities
 * (hence Serializable) and persisted in the local SQLite database.
 */
public class Hike implements Serializable {

    /** Value used for hikes that have not been saved to the database yet. */
    public static final long UNSAVED_ID = -1;

    private long id = UNSAVED_ID;
    private String name;
    private String location;
    /** Stored in ISO format (yyyy-MM-dd) so dates sort and compare correctly. */
    private String date;
    private boolean parkingAvailable;
    private double lengthKm;
    private String difficulty;
    private String description;
    // Custom fields required by the specification ("fields of your own invention")
    private double estimatedDurationHours;
    private String terrainType;

    public Hike() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public boolean isParkingAvailable() {
        return parkingAvailable;
    }

    public void setParkingAvailable(boolean parkingAvailable) {
        this.parkingAvailable = parkingAvailable;
    }

    public double getLengthKm() {
        return lengthKm;
    }

    public void setLengthKm(double lengthKm) {
        this.lengthKm = lengthKm;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getEstimatedDurationHours() {
        return estimatedDurationHours;
    }

    public void setEstimatedDurationHours(double estimatedDurationHours) {
        this.estimatedDurationHours = estimatedDurationHours;
    }

    public String getTerrainType() {
        return terrainType;
    }

    public void setTerrainType(String terrainType) {
        this.terrainType = terrainType;
    }
}
```

### model/Observation.java  —  Java

Observation carries one row of the observations table. Besides the observation text and the time it was made, it holds the identifier of the hike it belongs to, an optional comment and an optional path to a photograph. Both optional values may be null, which is what allows an observation to be saved with no comment and no picture. The class mirrors Hike in shape so that the two data-access classes can be written the same way.

```java
package com.luphihung.mhike.model;

import java.io.Serializable;

/**
 * A single observation recorded during a hike, e.g. an animal sighting,
 * trail condition or weather note. Each observation belongs to one hike.
 */
public class Observation implements Serializable {

    /** Value used for observations that have not been saved to the database yet. */
    public static final long UNSAVED_ID = -1;

    private long id = UNSAVED_ID;
    private long hikeId;
    private String text;
    /** Stored as yyyy-MM-dd HH:mm so timestamps sort correctly as text. */
    private String observedAt;
    private String comments;
    /** Absolute path of a photo attached to this observation, or null. */
    private String photoPath;

    public Observation() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getHikeId() {
        return hikeId;
    }

    public void setHikeId(long hikeId) {
        this.hikeId = hikeId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(String observedAt) {
        this.observedAt = observedAt;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }
}
```

### database/HikeDatabaseHelper.java  —  Java

This class creates the SQLite database and owns the schema. Every table and column name is exposed as a constant so the helper and the data-access classes cannot drift apart. The observations table declares hike_id as a foreign key with ON DELETE CASCADE and foreign key enforcement is switched on in onConfigure, so deleting a hike also removes its observations and no orphan rows can exist. The database is at version three, and onUpgrade migrates an existing installation, adding the observations table at version two and the photograph column at version three rather than discarding stored data.

```java
package com.luphihung.mhike.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Creates and maintains the local SQLite database used by M-Hike.
 * Table/column names are exposed as constants so the DAO classes and
 * this helper always stay in sync.
 */
public class HikeDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "mhike.db";
    private static final int DATABASE_VERSION = 3;

    // Hikes table
    public static final String TABLE_HIKES = "hikes";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_LOCATION = "location";
    public static final String COLUMN_DATE = "hike_date";
    public static final String COLUMN_PARKING = "parking_available";
    public static final String COLUMN_LENGTH = "length_km";
    public static final String COLUMN_DIFFICULTY = "difficulty";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_DURATION = "duration_hours";
    public static final String COLUMN_TERRAIN = "terrain_type";

    // Observations table (one hike has many observations)
    public static final String TABLE_OBSERVATIONS = "observations";
    public static final String COLUMN_OBS_ID = "_id";
    public static final String COLUMN_OBS_HIKE_ID = "hike_id";
    public static final String COLUMN_OBS_TEXT = "observation";
    public static final String COLUMN_OBS_TIME = "observed_at";
    public static final String COLUMN_OBS_COMMENTS = "comments";
    public static final String COLUMN_OBS_PHOTO = "photo_path";

    private static final String SQL_CREATE_HIKES =
            "CREATE TABLE " + TABLE_HIKES + " ("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_NAME + " TEXT NOT NULL, "
                    + COLUMN_LOCATION + " TEXT NOT NULL, "
                    + COLUMN_DATE + " TEXT NOT NULL, "
                    + COLUMN_PARKING + " INTEGER NOT NULL, "
                    + COLUMN_LENGTH + " REAL NOT NULL, "
                    + COLUMN_DIFFICULTY + " TEXT NOT NULL, "
                    + COLUMN_DESCRIPTION + " TEXT, "
                    + COLUMN_DURATION + " REAL, "
                    + COLUMN_TERRAIN + " TEXT)";

    private static final String SQL_CREATE_OBSERVATIONS =
            "CREATE TABLE " + TABLE_OBSERVATIONS + " ("
                    + COLUMN_OBS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_OBS_HIKE_ID + " INTEGER NOT NULL, "
                    + COLUMN_OBS_TEXT + " TEXT NOT NULL, "
                    + COLUMN_OBS_TIME + " TEXT NOT NULL, "
                    + COLUMN_OBS_COMMENTS + " TEXT, "
                    + COLUMN_OBS_PHOTO + " TEXT, "
                    + "FOREIGN KEY (" + COLUMN_OBS_HIKE_ID + ") REFERENCES "
                    + TABLE_HIKES + "(" + COLUMN_ID + ") ON DELETE CASCADE)";

    public HikeDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        // Needed so deleting a hike also deletes its observations (ON DELETE CASCADE).
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_HIKES);
        db.execSQL(SQL_CREATE_OBSERVATIONS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Version 2 introduced observations recorded during a hike.
            db.execSQL(SQL_CREATE_OBSERVATIONS);
        }
        if (oldVersion == 2) {
            // Version 3 added an optional photo to each observation.
            db.execSQL("ALTER TABLE " + TABLE_OBSERVATIONS
                    + " ADD COLUMN " + COLUMN_OBS_PHOTO + " TEXT");
        }
    }
}
```

### database/HikeDao.java  —  Java

HikeDao wraps every SQL statement that touches the hikes table, so no activity in the application contains SQL. Inserts and updates are built with ContentValues and every query passes its values as arguments to placeholders, which means a name containing an apostrophe or a deliberate SQL fragment is stored as text and never executed. The search method builds its WHERE clause piece by piece from whichever criteria the user supplied, adding one placeholder for each, so a single method serves both the simple and the advanced search. Each cursor is read inside a try-with-resources block so it is always closed.

```java
package com.luphihung.mhike.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.luphihung.mhike.model.Hike;

import java.util.ArrayList;
import java.util.List;

/**
 * Data access object for the hikes table. Keeps all SQL for hikes in
 * one place so activities never touch the database directly.
 */
public class HikeDao {

    private final HikeDatabaseHelper databaseHelper;

    public HikeDao(Context context) {
        databaseHelper = new HikeDatabaseHelper(context.getApplicationContext());
    }

    /** Inserts a new hike and returns it with its generated id set. */
    public Hike insert(Hike hike) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        long id = db.insert(HikeDatabaseHelper.TABLE_HIKES, null, toContentValues(hike));
        hike.setId(id);
        return hike;
    }

    /** Updates an existing hike, matching on its id. */
    public void update(Hike hike) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.update(HikeDatabaseHelper.TABLE_HIKES, toContentValues(hike),
                HikeDatabaseHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(hike.getId())});
    }

    /** Deletes a single hike by id. */
    public void delete(long hikeId) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.delete(HikeDatabaseHelper.TABLE_HIKES,
                HikeDatabaseHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(hikeId)});
    }

    /** Removes every hike from the database (the "reset" option). */
    public void deleteAll() {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.delete(HikeDatabaseHelper.TABLE_HIKES, null, null);
    }

    /** Returns all hikes ordered by date, soonest first. */
    public List<Hike> getAll() {
        List<Hike> hikes = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        try (Cursor cursor = db.query(HikeDatabaseHelper.TABLE_HIKES, null,
                null, null, null, null,
                HikeDatabaseHelper.COLUMN_DATE + " ASC")) {
            while (cursor.moveToNext()) {
                hikes.add(fromCursor(cursor));
            }
        }
        return hikes;
    }

    /** Looks up one hike by id, or returns null if it no longer exists. */
    public Hike getById(long hikeId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        try (Cursor cursor = db.query(HikeDatabaseHelper.TABLE_HIKES, null,
                HikeDatabaseHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(hikeId)}, null, null, null)) {
            return cursor.moveToFirst() ? fromCursor(cursor) : null;
        }
    }

    /**
     * Searches hikes against any combination of criteria. Criteria that are
     * null (or 0 for lengths) are ignored, so a name-only search and the
     * advanced multi-field search share this one query builder.
     *
     * @param namePrefix     matches names starting with this text
     * @param locationText   matches locations containing this text
     * @param minLengthKm    minimum hike length, or 0 to ignore
     * @param maxLengthKm    maximum hike length, or 0 to ignore
     * @param dateIso        exact hike date in yyyy-MM-dd, or null to ignore
     */
    public List<Hike> search(String namePrefix, String locationText,
                             double minLengthKm, double maxLengthKm, String dateIso) {
        StringBuilder selection = new StringBuilder("1=1");
        List<String> args = new ArrayList<>();

        if (namePrefix != null && !namePrefix.isEmpty()) {
            selection.append(" AND ").append(HikeDatabaseHelper.COLUMN_NAME).append(" LIKE ?");
            args.add(namePrefix + "%");
        }
        if (locationText != null && !locationText.isEmpty()) {
            selection.append(" AND ").append(HikeDatabaseHelper.COLUMN_LOCATION).append(" LIKE ?");
            args.add("%" + locationText + "%");
        }
        if (minLengthKm > 0) {
            selection.append(" AND ").append(HikeDatabaseHelper.COLUMN_LENGTH).append(" >= ?");
            args.add(String.valueOf(minLengthKm));
        }
        if (maxLengthKm > 0) {
            selection.append(" AND ").append(HikeDatabaseHelper.COLUMN_LENGTH).append(" <= ?");
            args.add(String.valueOf(maxLengthKm));
        }
        if (dateIso != null && !dateIso.isEmpty()) {
            selection.append(" AND ").append(HikeDatabaseHelper.COLUMN_DATE).append(" = ?");
            args.add(dateIso);
        }

        List<Hike> hikes = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        try (Cursor cursor = db.query(HikeDatabaseHelper.TABLE_HIKES, null,
                selection.toString(), args.toArray(new String[0]), null, null,
                HikeDatabaseHelper.COLUMN_NAME + " COLLATE NOCASE ASC")) {
            while (cursor.moveToNext()) {
                hikes.add(fromCursor(cursor));
            }
        }
        return hikes;
    }

    private ContentValues toContentValues(Hike hike) {
        ContentValues values = new ContentValues();
        values.put(HikeDatabaseHelper.COLUMN_NAME, hike.getName());
        values.put(HikeDatabaseHelper.COLUMN_LOCATION, hike.getLocation());
        values.put(HikeDatabaseHelper.COLUMN_DATE, hike.getDate());
        values.put(HikeDatabaseHelper.COLUMN_PARKING, hike.isParkingAvailable() ? 1 : 0);
        values.put(HikeDatabaseHelper.COLUMN_LENGTH, hike.getLengthKm());
        values.put(HikeDatabaseHelper.COLUMN_DIFFICULTY, hike.getDifficulty());
        values.put(HikeDatabaseHelper.COLUMN_DESCRIPTION, hike.getDescription());
        values.put(HikeDatabaseHelper.COLUMN_DURATION, hike.getEstimatedDurationHours());
        values.put(HikeDatabaseHelper.COLUMN_TERRAIN, hike.getTerrainType());
        return values;
    }

    private Hike fromCursor(Cursor cursor) {
        Hike hike = new Hike();
        hike.setId(cursor.getLong(cursor.getColumnIndexOrThrow(HikeDatabaseHelper.COLUMN_ID)));
        hike.setName(cursor.getString(cursor.getColumnIndexOrThrow(HikeDatabaseHelper.COLUMN_NAME)));
        hike.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(HikeDatabaseHelper.COLUMN_LOCATION)));
        hike.setDate(cursor.getString(cursor.getColumnIndexOrThrow(HikeDatabaseHelper.COLUMN_DATE)));
        hike.setParkingAvailable(cursor.getInt(cursor.getColumnIndexOrThrow(HikeDatabaseHelper.COLUMN_PARKING)) == 1);
        hike.setLengthKm(cursor.getDouble(cursor.getColumnIndexOrThrow(HikeDatabaseHelper.COLUMN_LENGTH)));
        hike.setDifficulty(cursor.getString(cursor.getColumnIndexOrThrow(HikeDatabaseHelper.COLUMN_DIFFICULTY)));
        hike.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(HikeDatabaseHelper.COLUMN_DESCRIPTION)));
        hike.setEstimatedDurationHours(cursor.getDouble(cursor.getColumnIndexOrThrow(HikeDatabaseHelper.COLUMN_DURATION)));
        hike.setTerrainType(cursor.getString(cursor.getColumnIndexOrThrow(HikeDatabaseHelper.COLUMN_TERRAIN)));
        return hike;
    }
}
```

### database/ObservationDao.java  —  Java

ObservationDao is the equivalent class for the observations table and follows the same rules: parameterised statements, ContentValues for writes and try-with-resources for reads. Observations are always fetched for one hike and returned newest first, which is the order the detail screen shows them in. Deleting a single observation is done here by identifier; deleting all observations of a hike is left to the cascade declared in the schema.

```java
package com.luphihung.mhike.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.luphihung.mhike.model.Observation;

import java.util.ArrayList;
import java.util.List;

/**
 * Data access object for the observations table.
 */
public class ObservationDao {

    private final HikeDatabaseHelper databaseHelper;

    public ObservationDao(Context context) {
        databaseHelper = new HikeDatabaseHelper(context.getApplicationContext());
    }

    /** Inserts a new observation and returns it with its generated id set. */
    public Observation insert(Observation observation) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        long id = db.insert(HikeDatabaseHelper.TABLE_OBSERVATIONS, null,
                toContentValues(observation));
        observation.setId(id);
        return observation;
    }

    /** Updates an existing observation, matching on its id. */
    public void update(Observation observation) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.update(HikeDatabaseHelper.TABLE_OBSERVATIONS, toContentValues(observation),
                HikeDatabaseHelper.COLUMN_OBS_ID + " = ?",
                new String[]{String.valueOf(observation.getId())});
    }

    /** Deletes a single observation by id. */
    public void delete(long observationId) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.delete(HikeDatabaseHelper.TABLE_OBSERVATIONS,
                HikeDatabaseHelper.COLUMN_OBS_ID + " = ?",
                new String[]{String.valueOf(observationId)});
    }

    /** Returns all observations for one hike, newest first. */
    public List<Observation> getAllForHike(long hikeId) {
        List<Observation> observations = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        try (Cursor cursor = db.query(HikeDatabaseHelper.TABLE_OBSERVATIONS, null,
                HikeDatabaseHelper.COLUMN_OBS_HIKE_ID + " = ?",
                new String[]{String.valueOf(hikeId)}, null, null,
                HikeDatabaseHelper.COLUMN_OBS_TIME + " DESC")) {
            while (cursor.moveToNext()) {
                observations.add(fromCursor(cursor));
            }
        }
        return observations;
    }

    private ContentValues toContentValues(Observation observation) {
        ContentValues values = new ContentValues();
        values.put(HikeDatabaseHelper.COLUMN_OBS_HIKE_ID, observation.getHikeId());
        values.put(HikeDatabaseHelper.COLUMN_OBS_TEXT, observation.getText());
        values.put(HikeDatabaseHelper.COLUMN_OBS_TIME, observation.getObservedAt());
        values.put(HikeDatabaseHelper.COLUMN_OBS_COMMENTS, observation.getComments());
        values.put(HikeDatabaseHelper.COLUMN_OBS_PHOTO, observation.getPhotoPath());
        return values;
    }

    private Observation fromCursor(Cursor cursor) {
        Observation observation = new Observation();
        observation.setId(cursor.getLong(cursor.getColumnIndexOrThrow(HikeDatabaseHelper.COLUMN_OBS_ID)));
        observation.setHikeId(cursor.getLong(cursor.getColumnIndexOrThrow(HikeDatabaseHelper.COLUMN_OBS_HIKE_ID)));
        observation.setText(cursor.getString(cursor.getColumnIndexOrThrow(HikeDatabaseHelper.COLUMN_OBS_TEXT)));
        observation.setObservedAt(cursor.getString(cursor.getColumnIndexOrThrow(HikeDatabaseHelper.COLUMN_OBS_TIME)));
        observation.setComments(cursor.getString(cursor.getColumnIndexOrThrow(HikeDatabaseHelper.COLUMN_OBS_COMMENTS)));
        observation.setPhotoPath(cursor.getString(cursor.getColumnIndexOrThrow(HikeDatabaseHelper.COLUMN_OBS_PHOTO)));
        return observation;
    }
}
```

## 5.3 Native Android application - user interface (Java)

### MainActivity.java  —  Java

MainActivity is the home screen and lists the stored hikes in a RecyclerView. It reloads the list in onResume, so a hike added, edited or deleted on another screen is reflected as soon as the user returns. The overflow menu holds the two application-wide actions: switching between the light and dark themes, which is written to SharedPreferences and applied through AppCompatDelegate, and deleting every hike, which is guarded by a dialogue stating that the action cannot be undone. An empty-state message is shown instead of the list when the database contains no hikes.

```java
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
public class
MainActivity extends AppCompatActivity {

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
```

### AddEditHikeActivity.java (extract, lines 111-172: filling the location from the device)  —  Java

This part of the entry screen implements the GPS autofill. The location permission is requested at the moment the user taps the button rather than at start-up, and if it is refused the field simply stays editable, so the feature degrades instead of blocking the user. The application asks the LocationManager for the last known position from whichever provider is enabled, then turns the coordinates into a place name with Geocoder, falling back to the raw latitude and longitude if no name can be resolved.

```java
    private void requestCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fillLocationFromDevice();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Reads the device's position once and reverse-geocodes it into a
     * readable place name for the location field.
     */
    private void fillLocationFromDevice() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null) {
            Toast.makeText(this, R.string.message_location_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, R.string.message_locating, Toast.LENGTH_SHORT).show();
        try {
            String provider = pickBestProvider(locationManager);
            LocationManagerCompat.getCurrentLocation(locationManager, provider,
                    (android.os.CancellationSignal) null,
                    ContextCompat.getMainExecutor(this), this::onDeviceLocation);
        } catch (SecurityException | IllegalArgumentException e) {
            Toast.makeText(this, R.string.message_location_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Prefers the fused provider (fast and battery friendly, API 31+), then
     * the network provider, and only falls back to raw GPS, which can take
     * a long time to get a first fix indoors.
     */
    private String pickBestProvider(LocationManager locationManager) {
        if (android.os.Build.VERSION.SDK_INT >= 31
                && locationManager.getAllProviders().contains(LocationManager.FUSED_PROVIDER)) {
            return LocationManager.FUSED_PROVIDER;
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            return LocationManager.NETWORK_PROVIDER;
        }
        return LocationManager.GPS_PROVIDER;
    }

    private void onDeviceLocation(Location location) {
        if (location == null) {
            Toast.makeText(this, R.string.message_location_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        // Geocoder does network I/O, so resolve the place name off the UI thread.
        Executors.newSingleThreadExecutor().execute(() -> {
            String placeName = resolvePlaceName(location);
            runOnUiThread(() -> {
                locationInput.setText(placeName);
                locationLayout.setError(null);
            });
        });
    }

    /** Turns coordinates into "locality, country", falling back to raw coordinates. */
```

### AddEditHikeActivity.java (extract, lines 276-415: validation, confirmation and saving)  —  Java

validateForm collects every problem before returning, so all invalid fields are marked at once and the user is not sent back to the form repeatedly; it returns a fully built Hike only when nothing is wrong. showConfirmationDialog then displays every entered value and offers Go back and edit as its second choice, which is the confirmation step the specification asks for. Only when the user confirms does saveHike run, inserting a new row or updating the existing one depending on whether the screen was opened with a hike identifier. The same three methods serve both creating and editing, so the two paths cannot behave differently.

```java
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
        setSummaryText(summaryView, R.id.summary_date, Formats.displayDate(hike.getDate()));
        setSummaryText(summaryView, R.id.summary_parking,
                getString(hike.isParkingAvailable() ? R.string.option_yes : R.string.option_no));
        setSummaryText(summaryView, R.id.summary_length,
                Formats.compactNumber(hike.getLengthKm()) + " " + getString(R.string.suffix_km));
        setSummaryText(summaryView, R.id.summary_difficulty, hike.getDifficulty());
        setSummaryText(summaryView, R.id.summary_duration,
                hike.getEstimatedDurationHours() > 0
                        ? Formats.compactNumber(hike.getEstimatedDurationHours()) + " "
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
        if (hike.getId() == Hike.UNSAVED_ID) {
            hikeDao.insert(hike);
        } else {
            hikeDao.update(hike);
        }
        Toast.makeText(this, R.string.message_hike_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

```

### HikeDetailActivity.java (extract, lines 158-171: sharing a hike)  —  Java

Sharing builds a plain-text summary of the hike and hands it to an implicit ACTION_SEND intent, so the user can pass it to any messaging, mail or notes application installed on the device. Nothing is transmitted by the application itself, which is why no internet permission is required.

```java
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

```

### HikeDetailActivity.java (extract, lines 199-297: observations and photographs)  —  Java

One dialogue serves both adding and editing an observation: it is passed null for a new record and the existing observation when editing. The time is filled in automatically when the dialogue opens, and the comment field may be left empty because the specification treats it as optional. launchCamera writes the photograph into the private external files directory of the application and exposes it to the camera application through the FileProvider registered in the manifest, storing only the path in the database so the database itself stays small. When a photograph exists it is shown as a preview inside the dialogue.

```java
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
```

### SearchActivity.java  —  Java

SearchActivity implements both searches described in the brief. Typing in the name field filters as the user types and matches any part of the name, so a single letter is enough to narrow the list. The advanced panel adds a location, a minimum and a maximum length and a date; whichever criteria are filled in are passed to HikeDao.search and combined, and the number of matches is shown above the results so the effect of a filter is visible even when nothing matches.

```java
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
```

### adapter/HikeAdapter.java  —  Java

HikeAdapter binds Hike objects to the cards shown on the home screen and in the search results. It uses the view-holder pattern so that scrolling reuses views instead of inflating new ones, and it exposes a single click listener supplied by the hosting activity, which keeps navigation decisions out of the adapter.

```java
package com.luphihung.mhike.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import com.luphihung.mhike.R;
import com.luphihung.mhike.model.Hike;
import com.luphihung.mhike.util.Formats;

import java.util.ArrayList;
import java.util.List;

/**
 * Binds hikes to the cards shown on the home screen and in search results.
 */
public class HikeAdapter extends RecyclerView.Adapter<HikeAdapter.HikeViewHolder> {

    /** Callback fired when the user taps a hike card. */
    public interface OnHikeClickListener {
        void onHikeClick(Hike hike);
    }

    private final List<Hike> hikes = new ArrayList<>();
    private final OnHikeClickListener clickListener;

    public HikeAdapter(OnHikeClickListener clickListener) {
        this.clickListener = clickListener;
    }

    /** Replaces the whole list, e.g. after a database reload. */
    public void submitList(List<Hike> newHikes) {
        hikes.clear();
        hikes.addAll(newHikes);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HikeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hike, parent, false);
        return new HikeViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull HikeViewHolder holder, int position) {
        holder.bind(hikes.get(position));
    }

    @Override
    public int getItemCount() {
        return hikes.size();
    }

    class HikeViewHolder extends RecyclerView.ViewHolder {

        private final TextView nameText;
        private final TextView locationText;
        private final TextView dateLengthText;
        private final Chip difficultyChip;

        HikeViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.text_hike_name);
            locationText = itemView.findViewById(R.id.text_hike_location);
            dateLengthText = itemView.findViewById(R.id.text_hike_date_length);
            difficultyChip = itemView.findViewById(R.id.chip_difficulty);
        }

        void bind(Hike hike) {
            nameText.setText(hike.getName());
            locationText.setText(hike.getLocation());
            dateLengthText.setText(itemView.getContext().getString(
                    R.string.format_date_and_length,
                    Formats.displayDate(hike.getDate()),
                    Formats.compactNumber(hike.getLengthKm())));
            difficultyChip.setText(hike.getDifficulty());
            itemView.setOnClickListener(v -> clickListener.onHikeClick(hike));
        }
    }
}
```

### adapter/ObservationAdapter.java  —  Java

ObservationAdapter renders the observations of one hike. It hides the comment view when an observation was saved without a comment and the image view when there is no photograph, so an optional field costs no empty space on screen. Editing and deleting are raised to the activity through a small listener interface rather than handled here.

```java
package com.luphihung.mhike.adapter;

import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.luphihung.mhike.R;
import com.luphihung.mhike.model.Observation;
import com.luphihung.mhike.util.Formats;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Binds the observations of a hike to cards on the hike detail screen.
 */
public class ObservationAdapter extends RecyclerView.Adapter<ObservationAdapter.ObservationViewHolder> {

    /** Callbacks for the per-observation options menu. */
    public interface OnObservationActionListener {
        void onEditObservation(Observation observation);

        void onDeleteObservation(Observation observation);
    }

    private final List<Observation> observations = new ArrayList<>();
    private final OnObservationActionListener actionListener;

    public ObservationAdapter(OnObservationActionListener actionListener) {
        this.actionListener = actionListener;
    }

    /** Replaces the whole list, e.g. after a database reload. */
    public void submitList(List<Observation> newObservations) {
        observations.clear();
        observations.addAll(newObservations);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ObservationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_observation, parent, false);
        return new ObservationViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ObservationViewHolder holder, int position) {
        holder.bind(observations.get(position));
    }

    @Override
    public int getItemCount() {
        return observations.size();
    }

    class ObservationViewHolder extends RecyclerView.ViewHolder {

        private final TextView observationText;
        private final TextView timeText;
        private final TextView commentsText;
        private final ImageView photoView;
        private final ImageButton menuButton;

        ObservationViewHolder(@NonNull View itemView) {
            super(itemView);
            observationText = itemView.findViewById(R.id.text_observation);
            timeText = itemView.findViewById(R.id.text_observation_time);
            commentsText = itemView.findViewById(R.id.text_observation_comments);
            photoView = itemView.findViewById(R.id.image_observation);
            menuButton = itemView.findViewById(R.id.button_observation_menu);
        }

        void bind(Observation observation) {
            observationText.setText(observation.getText());
            timeText.setText(Formats.displayDateTime(observation.getObservedAt()));

            boolean hasComments = observation.getComments() != null
                    && !observation.getComments().trim().isEmpty();
            commentsText.setVisibility(hasComments ? View.VISIBLE : View.GONE);
            commentsText.setText(observation.getComments());

            String photoPath = observation.getPhotoPath();
            if (photoPath != null && new File(photoPath).exists()) {
                photoView.setImageBitmap(BitmapFactory.decodeFile(photoPath));
                photoView.setVisibility(View.VISIBLE);
            } else {
                photoView.setVisibility(View.GONE);
            }

            menuButton.setOnClickListener(v -> showOptionsMenu(observation));
        }

        private void showOptionsMenu(Observation observation) {
            android.widget.PopupMenu popup =
                    new android.widget.PopupMenu(itemView.getContext(), menuButton);
            popup.getMenu().add(R.string.action_edit);
            popup.getMenu().add(R.string.action_delete);
            popup.setOnMenuItemClickListener(item -> {
                String editLabel = itemView.getContext().getString(R.string.action_edit);
                if (editLabel.contentEquals(item.getTitle())) {
                    actionListener.onEditObservation(observation);
                } else {
                    actionListener.onDeleteObservation(observation);
                }
                return true;
            });
            popup.show();
        }
    }
}
```

## 5.4 Cross-platform application, .NET MAUI (C#)

### MHike.Maui/Models/Hike.cs  —  C#

This is the MAUI equivalent of the Hike model. The attributes above the class and the identifier tell sqlite-net how to map the object to a table, so the schema is derived from the class instead of being written as SQL. The properties are deliberately the same as those of the Java model so that both applications store the same information.

```csharp
using SQLite;

namespace MHike.Maui.Models;

/// <summary>
/// A planned hike stored in the local SQLite database.
/// Mirrors the schema used by the native Android version of M-Hike.
/// </summary>
[Table("hikes")]
public class Hike
{
    [PrimaryKey, AutoIncrement]
    [Column("id")]
    public int Id { get; set; }

    [Column("name")]
    public string Name { get; set; } = string.Empty;

    [Column("location")]
    public string Location { get; set; } = string.Empty;

    [Column("hike_date")]
    public DateTime Date { get; set; } = DateTime.Today;

    [Column("parking_available")]
    public bool ParkingAvailable { get; set; }

    [Column("length_km")]
    public double LengthKm { get; set; }

    [Column("difficulty")]
    public string Difficulty { get; set; } = string.Empty;

    [Column("description")]
    public string Description { get; set; } = string.Empty;

    // Custom fields required by the specification
    [Column("duration_hours")]
    public double EstimatedDurationHours { get; set; }

    [Column("terrain_type")]
    public string TerrainType { get; set; } = string.Empty;

    /// <summary>Summary line shown underneath the hike name in the list.</summary>
    [Ignore]
    public string Summary => $"{Location}  ·  {Date:ddd, dd MMM yyyy}  ·  {LengthKm} km";
}
```

### MHike.Maui/Data/HikeDatabase.cs  —  C#

HikeDatabase plays the part that the helper and the data-access classes play in the Java application. It opens the SQLite file in the application data directory, creates the table on first use and exposes asynchronous methods to list, fetch, save and delete hikes. Because sqlite-net builds the statements from the object, the values are bound rather than concatenated, so the same protection against SQL injection applies here.

```csharp
using MHike.Maui.Models;
using SQLite;

namespace MHike.Maui.Data;

/// <summary>
/// Data access layer for hikes. Opens the SQLite database lazily so the
/// connection is only created the first time it is needed.
/// </summary>
public class HikeDatabase
{
    private SQLiteAsyncConnection? _connection;

    private async Task<SQLiteAsyncConnection> GetConnectionAsync()
    {
        if (_connection is null)
        {
            string databasePath = Path.Combine(FileSystem.AppDataDirectory, "mhike.db3");
            _connection = new SQLiteAsyncConnection(databasePath,
                SQLiteOpenFlags.ReadWrite | SQLiteOpenFlags.Create | SQLiteOpenFlags.SharedCache);
            await _connection.CreateTableAsync<Hike>();
        }
        return _connection;
    }

    public async Task<List<Hike>> GetAllAsync()
    {
        var connection = await GetConnectionAsync();
        return await connection.Table<Hike>().OrderBy(h => h.Date).ToListAsync();
    }

    /// <summary>Inserts a new hike or updates an existing one based on its id.</summary>
    public async Task SaveAsync(Hike hike)
    {
        var connection = await GetConnectionAsync();
        if (hike.Id == 0)
        {
            await connection.InsertAsync(hike);
        }
        else
        {
            await connection.UpdateAsync(hike);
        }
    }

    public async Task DeleteAsync(Hike hike)
    {
        var connection = await GetConnectionAsync();
        await connection.DeleteAsync(hike);
    }

    /// <summary>Removes every hike from the database (the "reset" option).</summary>
    public async Task DeleteAllAsync()
    {
        var connection = await GetConnectionAsync();
        await connection.DeleteAllAsync<Hike>();
    }
}
```

### MHike.Maui/MainPage.xaml.cs  —  C#

MainPage is the list screen. It reloads the hikes each time the page appears so that a change made on the form page shows immediately, and it passes the identifier of a hike to the form page through a Shell route parameter, which is the MAUI counterpart of an intent extra. Deleting asks for confirmation first, and sharing uses the cross-platform Share API.

```csharp
using MHike.Maui.Data;
using MHike.Maui.Models;

namespace MHike.Maui;

/// <summary>
/// Home screen: lists all stored hikes and offers edit, delete and
/// reset-database actions (feature f).
/// </summary>
public partial class MainPage : ContentPage
{
    private readonly HikeDatabase _database;

    public MainPage(HikeDatabase database)
    {
        InitializeComponent();
        _database = database;
    }

    protected override async void OnAppearing()
    {
        base.OnAppearing();
        await LoadHikesAsync();
    }

    private async Task LoadHikesAsync()
    {
        List<Hike> hikes = await _database.GetAllAsync();
        HikeCollectionView.ItemsSource = hikes;
        bool isEmpty = hikes.Count == 0;
        EmptyStateLayout.IsVisible = isEmpty;
        HikeCollectionView.IsVisible = !isEmpty;
    }

    private async void OnAddHikeClicked(object? sender, EventArgs e)
    {
        await Navigation.PushAsync(new HikeFormPage(_database));
    }

    private async void OnEditHikeClicked(object? sender, EventArgs e)
    {
        if (sender is Button { CommandParameter: Hike hike })
        {
            await Navigation.PushAsync(new HikeFormPage(_database, hike));
        }
    }

    /// <summary>
    /// Additional feature: shares the hike details as plain text through
    /// any app the user picks (messaging, email, notes...).
    /// </summary>
    private async void OnShareHikeClicked(object? sender, EventArgs e)
    {
        if (sender is Button { CommandParameter: Hike hike })
        {
            string shareText = $"M-Hike – {hike.Name}\n"
                + $"Location: {hike.Location}\n"
                + $"Date: {hike.Date:ddd, dd MMM yyyy}\n"
                + $"Length: {hike.LengthKm} km\n"
                + $"Difficulty: {hike.Difficulty}";

            await Share.Default.RequestAsync(new ShareTextRequest
            {
                Title = "Share hike",
                Text = shareText
            });
        }
    }

    private async void OnDeleteHikeClicked(object? sender, EventArgs e)
    {
        if (sender is Button { CommandParameter: Hike hike })
        {
            bool confirmed = await DisplayAlert("Delete this hike?",
                $"\"{hike.Name}\" will be removed. This cannot be undone.",
                "Delete", "Cancel");
            if (confirmed)
            {
                await _database.DeleteAsync(hike);
                await LoadHikesAsync();
            }
        }
    }

    private async void OnDeleteAllClicked(object? sender, EventArgs e)
    {
        bool confirmed = await DisplayAlert("Delete all hikes?",
            "This removes every hike stored on this device and cannot be undone.",
            "Delete all", "Cancel");
        if (confirmed)
        {
            await _database.DeleteAllAsync();
            await LoadHikesAsync();
        }
    }
}
```

### MHike.Maui/HikeFormPage.xaml.cs  —  C#

The form page carries the same rules as the native entry screen. Validation collects every problem and shows a message under each field, the confirmation dialogue lists the entered values and offers to go back and edit, and only then is the hike written to the database. The page serves both creating and editing: when it is opened with an identifier it loads that hike and updates it, otherwise it inserts a new one.

```csharp
using System.Globalization;
using System.Text;

using MHike.Maui.Data;
using MHike.Maui.Models;

namespace MHike.Maui;

/// <summary>
/// Entry form for adding or editing a hike (feature e). All required
/// fields are validated and the user confirms a summary of the details
/// before anything is saved.
/// </summary>
public partial class HikeFormPage : ContentPage
{
    private readonly HikeDatabase _database;
    private readonly Hike? _hikeBeingEdited;

    public HikeFormPage(HikeDatabase database, Hike? hikeToEdit = null)
    {
        InitializeComponent();
        _database = database;
        _hikeBeingEdited = hikeToEdit;

        if (hikeToEdit is not null)
        {
            Title = "Edit hike";
            PopulateForm(hikeToEdit);
        }
    }

    private void PopulateForm(Hike hike)
    {
        NameEntry.Text = hike.Name;
        LocationEntry.Text = hike.Location;
        HikeDatePicker.Date = hike.Date;
        ParkingYesRadio.IsChecked = hike.ParkingAvailable;
        ParkingNoRadio.IsChecked = !hike.ParkingAvailable;
        LengthEntry.Text = hike.LengthKm.ToString(CultureInfo.InvariantCulture);
        DifficultyPicker.SelectedItem = hike.Difficulty;
        DurationEntry.Text = hike.EstimatedDurationHours > 0
            ? hike.EstimatedDurationHours.ToString(CultureInfo.InvariantCulture)
            : string.Empty;
        TerrainPicker.SelectedItem = string.IsNullOrEmpty(hike.TerrainType) ? null : hike.TerrainType;
        DescriptionEditor.Text = hike.Description;
    }

    /// <summary>
    /// Additional feature: reads the device's position once and
    /// reverse-geocodes it into a readable place name for the location field.
    /// </summary>
    private async void OnUseCurrentLocationClicked(object? sender, EventArgs e)
    {
        try
        {
            var permission = await Permissions.RequestAsync<Permissions.LocationWhenInUse>();
            if (permission != PermissionStatus.Granted)
            {
                await DisplayAlert("Permission needed",
                    "Location permission is needed to fill this in automatically.", "OK");
                return;
            }

            var location = await Geolocation.GetLocationAsync(new GeolocationRequest(
                GeolocationAccuracy.Medium, TimeSpan.FromSeconds(15)));
            if (location is null)
            {
                await DisplayAlert("Location unavailable",
                    "Could not get your location. Check that location is turned on.", "OK");
                return;
            }

            var placemarks = await Geocoding.GetPlacemarksAsync(location.Latitude, location.Longitude);
            var place = placemarks?.FirstOrDefault();
            LocationEntry.Text = place is null
                ? $"{location.Latitude:F5}, {location.Longitude:F5}"
                : string.Join(", ", new[] { place.Locality ?? place.SubAdminArea, place.CountryName }
                    .Where(part => !string.IsNullOrEmpty(part)));
        }
        catch (Exception)
        {
            await DisplayAlert("Location unavailable",
                "Could not get your location. Check that location is turned on.", "OK");
        }
    }

    private async void OnSaveClicked(object? sender, EventArgs e)
    {
        Hike? hike = ValidateForm();
        if (hike is null)
        {
            return;
        }

        // Show all the details back to the user before saving, as required.
        bool confirmed = await DisplayAlert("Confirm hike details",
            BuildSummary(hike), "Confirm & save", "Go back & edit");
        if (!confirmed)
        {
            return;
        }

        await _database.SaveAsync(hike);
        await DisplayAlert("Saved", "The hike has been saved.", "OK");
        await Navigation.PopAsync();
    }

    /// <summary>
    /// Checks every required field, showing an inline error under each one
    /// that is missing or invalid.
    /// </summary>
    private Hike? ValidateForm()
    {
        bool valid = true;

        string name = NameEntry.Text?.Trim() ?? string.Empty;
        NameError.IsVisible = name.Length == 0;
        valid &= name.Length > 0;

        string location = LocationEntry.Text?.Trim() ?? string.Empty;
        LocationError.IsVisible = location.Length == 0;
        valid &= location.Length > 0;

        bool parkingChosen = ParkingYesRadio.IsChecked || ParkingNoRadio.IsChecked;
        ParkingError.IsVisible = !parkingChosen;
        valid &= parkingChosen;

        bool lengthValid = double.TryParse(LengthEntry.Text, NumberStyles.Float,
            CultureInfo.InvariantCulture, out double lengthKm) && lengthKm > 0;
        LengthError.IsVisible = !lengthValid;
        valid &= lengthValid;

        bool difficultyChosen = DifficultyPicker.SelectedItem is not null;
        DifficultyError.IsVisible = !difficultyChosen;
        valid &= difficultyChosen;

        // Optional field: only validated when the user typed something.
        double durationHours = 0;
        string durationText = DurationEntry.Text?.Trim() ?? string.Empty;
        if (durationText.Length > 0)
        {
            bool durationValid = double.TryParse(durationText, NumberStyles.Float,
                CultureInfo.InvariantCulture, out durationHours) && durationHours > 0;
            DurationError.IsVisible = !durationValid;
            valid &= durationValid;
        }
        else
        {
            DurationError.IsVisible = false;
        }

        if (!valid)
        {
            return null;
        }

        Hike hike = _hikeBeingEdited ?? new Hike();
        hike.Name = name;
        hike.Location = location;
        hike.Date = HikeDatePicker.Date;
        hike.ParkingAvailable = ParkingYesRadio.IsChecked;
        hike.LengthKm = lengthKm;
        hike.Difficulty = (string)DifficultyPicker.SelectedItem!;
        hike.EstimatedDurationHours = durationHours;
        hike.TerrainType = SelectedTextOrEmpty(TerrainPicker.SelectedItem);
        hike.Description = DescriptionEditor.Text?.Trim() ?? string.Empty;
        return hike;
    }

    private static string SelectedTextOrEmpty(object? pickerSelection)
    {
        return pickerSelection as string ?? string.Empty;
    }

    private string BuildSummary(Hike hike)
    {
        var summary = new StringBuilder();
        summary.AppendLine($"Name: {hike.Name}");
        summary.AppendLine($"Location: {hike.Location}");
        summary.AppendLine($"Date: {hike.Date:ddd, dd MMM yyyy}");
        summary.AppendLine($"Parking: {(hike.ParkingAvailable ? "Yes" : "No")}");
        summary.AppendLine($"Length: {hike.LengthKm} km");
        summary.AppendLine($"Difficulty: {hike.Difficulty}");
        summary.AppendLine($"Est. duration: {(hike.EstimatedDurationHours > 0 ? $"{hike.EstimatedDurationHours} hours" : "Not provided")}");
        summary.AppendLine($"Terrain: {(hike.TerrainType.Length > 0 ? hike.TerrainType : "Not provided")}");
        summary.Append($"Description: {(hike.Description.Length > 0 ? hike.Description : "Not provided")}");
        return summary.ToString();
    }
}
```
