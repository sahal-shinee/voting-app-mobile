package com.suarakita.model;

import java.util.List;

public class ImportResult {

    private List<ImportedStudent> created;
    private int createdCount;
    private List<SkippedRow> skipped;
    private int skippedCount;

    public List<ImportedStudent> getCreated() {
        return created;
    }

    public int getCreatedCount() {
        return createdCount;
    }

    public List<SkippedRow> getSkipped() {
        return skipped;
    }

    public int getSkippedCount() {
        return skippedCount;
    }
}
