package com.suarakita.model;

public class ActivityLog {

    private int id;
    private String adminName;
    private String action;
    private String targetType;
    private Integer targetId;
    private String description;
    private String createdAt;

    public int getId() {
        return id;
    }

    public String getAdminName() {
        return adminName;
    }

    public String getAction() {
        return action;
    }

    public String getTargetType() {
        return targetType;
    }

    public Integer getTargetId() {
        return targetId;
    }

    public String getDescription() {
        return description;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
