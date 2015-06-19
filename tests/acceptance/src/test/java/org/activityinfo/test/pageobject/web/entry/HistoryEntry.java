package org.activityinfo.test.pageobject.web.entry;

import java.util.ArrayList;
import java.util.List;


public class HistoryEntry {
    private String summary;
    private List<String> changes;


    public HistoryEntry(String summary) {
        this.summary = summary;
        this.changes = new ArrayList<>();
    }

    public String getSummary() {
        return summary;
    }

    public void addChange(String text) {
        changes.add(text);
    }

    @Override
    public String toString() {
        return "HistoryEntry{" + summary + "}";
    }
    
    public void appendTo(StringBuilder s) {
        s.append("* ").append(summary).append("\n");
        for(String change : changes) {
            s.append("  - ").append(change).append("\n");
        }
    }

    public List<String> getChanges() {
        return changes;
    }
}
