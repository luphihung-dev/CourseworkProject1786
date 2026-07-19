package com.luphihung.mhike.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Shared formatting helpers so every screen presents dates and
 * numbers the same way.
 */
public final class Formats {

    /** Format used to store dates in the database (sorts chronologically as text). */
    public static final SimpleDateFormat STORAGE_DATE =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    /** Format used to show dates to the user. */
    public static final SimpleDateFormat DISPLAY_DATE =
            new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());

    /** Format used to store observation timestamps in the database. */
    public static final SimpleDateFormat STORAGE_DATE_TIME =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
    /** Format used to show observation timestamps to the user. */
    public static final SimpleDateFormat DISPLAY_DATE_TIME =
            new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    private Formats() {
        // Utility class; not meant to be instantiated.
    }

    /** Converts a stored yyyy-MM-dd date into the user-facing format. */
    public static String displayDate(String isoDate) {
        if (isoDate == null) {
            return "";
        }
        try {
            Date parsed = STORAGE_DATE.parse(isoDate);
            return parsed == null ? isoDate : DISPLAY_DATE.format(parsed);
        } catch (ParseException e) {
            return isoDate;
        }
    }

    /** Converts a stored yyyy-MM-dd HH:mm timestamp into the user-facing format. */
    public static String displayDateTime(String storedTimestamp) {
        if (storedTimestamp == null) {
            return "";
        }
        try {
            Date parsed = STORAGE_DATE_TIME.parse(storedTimestamp);
            return parsed == null ? storedTimestamp : DISPLAY_DATE_TIME.format(parsed);
        } catch (ParseException e) {
            return storedTimestamp;
        }
    }

    /** Formats a number without a trailing ".0" for whole values (e.g. "5" not "5.0"). */
    public static String compactNumber(double value) {
        return value == Math.floor(value)
                ? String.valueOf((long) value) : String.valueOf(value);
    }
}
