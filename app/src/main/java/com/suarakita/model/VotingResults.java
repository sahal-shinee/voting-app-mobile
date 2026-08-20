package com.suarakita.model;

import java.util.List;

public class VotingResults {

    private int categoryId;
    private String categoryName;
    private boolean isVotingOpen;
    private boolean showLiveResults;
    private int totalVotes;
    private List<CandidateResult> candidates;

    // Hanya terisi dari endpoint admin (GET /admin/categories/{id}/results).
    private List<FastestVoter> fastestVoters;
    private Integer votedCount;
    private Integer totalStudents;

    public int getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public boolean isVotingOpen() {
        return isVotingOpen;
    }

    public boolean isShowLiveResults() {
        return showLiveResults;
    }

    public int getTotalVotes() {
        return totalVotes;
    }

    public List<CandidateResult> getCandidates() {
        return candidates;
    }

    public List<FastestVoter> getFastestVoters() {
        return fastestVoters;
    }

    public Integer getVotedCount() {
        return votedCount;
    }

    public Integer getTotalStudents() {
        return totalStudents;
    }
}
