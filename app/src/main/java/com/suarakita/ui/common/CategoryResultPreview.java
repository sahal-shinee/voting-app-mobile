package com.suarakita.ui.common;

import com.suarakita.model.Category;
import com.suarakita.model.VotingResults;

public class CategoryResultPreview {

    public enum State { LOADING, LOADED, LOCKED, ERROR }

    public final Category category;
    public VotingResults results;
    public State state = State.LOADING;

    public CategoryResultPreview(Category category) {
        this.category = category;
    }
}
