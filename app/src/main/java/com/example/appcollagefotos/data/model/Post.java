package com.example.appcollagefotos.data.model;

import com.google.firebase.database.IgnoreExtraProperties;
import java.io.Serializable;

@IgnoreExtraProperties
public class Post implements Serializable {
    private String id;
    private String eventId;
    private String userId;
    private String imageBase64;
    private String message;
    private Object createdAt;
    private String deviceInfo;

    // Required empty constructor for Firebase Realtime Database
    public Post() {
        this.id = "";
        this.eventId = "";
        this.userId = "";
        this.imageBase64 = "";
        this.message = null;
        this.createdAt = null;
        this.deviceInfo = null;
    }

    public Post(String id, String eventId, String userId, String imageBase64, String message, Object createdAt, String deviceInfo) {
        this.id = id;
        this.eventId = eventId;
        this.userId = userId;
        this.imageBase64 = imageBase64;
        this.message = message;
        this.createdAt = createdAt;
        this.deviceInfo = deviceInfo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Object createdAt) {
        this.createdAt = createdAt;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }
}
