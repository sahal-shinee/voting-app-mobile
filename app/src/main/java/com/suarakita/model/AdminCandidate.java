package com.suarakita.model;

public class AdminCandidate {

    private int id;
    private int categoryId;
    private String name;
    private String photoUrl;
    private String description;
    private boolean isActive;
    private String createdAt;

    public int getId() {
        return id;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return isActive;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
