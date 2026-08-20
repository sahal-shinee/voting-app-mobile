package com.suarakita.model;

public class MyVote {

    private int categoryId;
    private int candidateId;
    private String candidateName;
    private String votedAt;

    public int getCategoryId() {
        return categoryId;
    }

    public int getCandidateId() {
        return candidateId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public String getVotedAt() {
        return votedAt;
    }
}
