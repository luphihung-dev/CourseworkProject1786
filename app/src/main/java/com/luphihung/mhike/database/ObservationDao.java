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
        return values;
    }

    private Observation fromCursor(Cursor cursor) {
        Observation observation = new Observation();
        observation.setId(cursor.getLong(cursor.getColumnIndexOrThrow(HikeDatabaseHelper.COLUMN_OBS_ID)));
        observation.setHikeId(cursor.getLong(cursor.getColumnIndexOrThrow(HikeDatabaseHelper.COLUMN_OBS_HIKE_ID)));
        observation.setText(cursor.getString(cursor.getColumnIndexOrThrow(HikeDatabaseHelper.COLUMN_OBS_TEXT)));
        observation.setObservedAt(cursor.getString(cursor.getColumnIndexOrThrow(HikeDatabaseHelper.COLUMN_OBS_TIME)));
        observation.setComments(cursor.getString(cursor.getColumnIndexOrThrow(HikeDatabaseHelper.COLUMN_OBS_COMMENTS)));
        return observation;
    }
}
