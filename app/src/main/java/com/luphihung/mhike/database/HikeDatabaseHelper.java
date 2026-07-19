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
    private static final int DATABASE_VERSION = 2;

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
    }
}
