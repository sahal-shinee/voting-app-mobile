package com.suarakita.model;

public class CategoryToggleRequest {

    private final boolean isVotingOpen;
    private final boolean showLiveResults;

    public CategoryToggleRequest(boolean isVotingOpen, boolean showLiveResults) {
        this.isVotingOpen = isVotingOpen;
        this.showLiveResults = showLiveResults;
    }
}
