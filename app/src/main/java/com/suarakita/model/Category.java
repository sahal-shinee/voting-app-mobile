package com.suarakita.model;

public class Category {

    private int id;
    private String name;
    private String description;
    private boolean isVotingOpen;
    private boolean showLiveResults;
    private boolean hasVoted;
    private String deletedAt;
    private String votingStartAt;
    private String votingEndAt;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isVotingOpen() {
        return isVotingOpen;
    }

    public boolean isShowLiveResults() {
        return showLiveResults;
    }

    public boolean isHasVoted() {
        return hasVoted;
    }

    public String getDeletedAt() {
        return deletedAt;
    }

    public String getVotingStartAt() {
        return votingStartAt;
    }

    public String getVotingEndAt() {
        return votingEndAt;
    }
}
