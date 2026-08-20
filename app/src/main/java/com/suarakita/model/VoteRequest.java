package com.suarakita.model;

public class VoteRequest {

    private final int categoryId;
    private final int candidateId;

    public VoteRequest(int categoryId, int candidateId) {
        this.categoryId = categoryId;
        this.candidateId = candidateId;
    }
}
