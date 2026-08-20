package com.suarakita.model;

public class AdminCreateRequest {

    private final String name;
    private final String username;

    public AdminCreateRequest(String name, String username) {
        this.name = name;
        this.username = username;
    }
}
