package com.suarakita.model;

public class Student {

    private int id;
    private String name;
    private String nis;
    private boolean mustChangePassword;
    private String createdAt;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNis() {
        return nis;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
