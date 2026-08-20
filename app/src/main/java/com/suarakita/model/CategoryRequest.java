package com.suarakita.model;

public class CategoryRequest {

    private final String name;
    private final String description;
    private final String votingStartAt;
    private final String votingEndAt;

    public CategoryRequest(String name, String description, String votingStartAt, String votingEndAt) {
        this.name = name;
        this.description = description;
        this.votingStartAt = votingStartAt;
        this.votingEndAt = votingEndAt;
    }
}
