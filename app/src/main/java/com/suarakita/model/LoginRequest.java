package com.suarakita.model;

public class LoginRequest {

    private final String identifier;
    private final String password;

    public LoginRequest(String identifier, String password) {
        this.identifier = identifier;
        this.password = password;
    }
}
