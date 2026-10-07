package com.example.appcollagefotos.data.model;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {
    private String id;
    private String phoneNumber;
    private String email;
    private Object createdAt;

    // Required empty constructor for Firebase Realtime Database
    public User() {
        this.id = "";
        this.phoneNumber = "";
        this.email = "";
        this.createdAt = null;
    }

    public User(String id, String phoneNumber, String email, Object createdAt) {
        this.id = id;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Object getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Object createdAt) {
        this.createdAt = createdAt;
    }
}
