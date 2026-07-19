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
