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
