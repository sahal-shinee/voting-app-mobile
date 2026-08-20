package com.suarakita.model;

public class CandidateResult {

    private int candidateId;
    private String name;
    private String photoUrl;
    private boolean isActive;
    private int voteCount;
    private double percentage;

    public int getCandidateId() {
        return candidateId;
    }

    public String getName() {
        return name;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public boolean isActive() {
        return isActive;
    }

    public int getVoteCount() {
        return voteCount;
    }

    public double getPercentage() {
        return percentage;
    }
}
