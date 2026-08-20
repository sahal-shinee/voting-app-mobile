package com.suarakita.model;

public class User {

    private int id;
    private String name;
    private String role;
    private boolean mustChangePassword;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public boolean isAdmin() {
        return "admin".equals(role);
    }
}
