package com.suarakita.model;

public class StudentCreateRequest {

    private final String name;
    private final String nis;

    public StudentCreateRequest(String name, String nis) {
        this.name = name;
        this.nis = nis;
    }
}
