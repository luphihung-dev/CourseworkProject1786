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
    private static final int DATABASE_VERSION = 1;

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

    public HikeDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_HIKES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // No schema migrations yet; future versions will add them here.
    }
}
