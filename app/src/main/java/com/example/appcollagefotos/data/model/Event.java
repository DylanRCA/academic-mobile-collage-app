package com.example.appcollagefotos.data.model;

import com.google.firebase.database.IgnoreExtraProperties;
import java.io.Serializable;

@IgnoreExtraProperties
public class Event implements Serializable {
    private String id;
    private String name;
    private String eventType;
    private String logoBase64;
    private String eventCode;
    private boolean active;
    private Object createdAt;

    // Required empty constructor for Firebase Realtime Database
    public Event() {
        this.id = "";
        this.name = "";
        this.eventType = "";
        this.logoBase64 = "";
        this.eventCode = "";
        this.active = true;
        this.createdAt = null;
    }

    public Event(String id, String name, String eventType, String logoBase64, String eventCode, boolean active, Object createdAt) {
        this.id = id;
        this.name = name;
        this.eventType = eventType;
        this.logoBase64 = logoBase64;
        this.eventCode = eventCode;
        this.active = active;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getLogoBase64() {
        return logoBase64;
    }

    public void setLogoBase64(String logoBase64) {
        this.logoBase64 = logoBase64;
    }

    public String getEventCode() {
        return eventCode;
    }

    public void setEventCode(String eventCode) {
        this.eventCode = eventCode;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Object getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Object createdAt) {
        this.createdAt = createdAt;
    }
}
