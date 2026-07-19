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
