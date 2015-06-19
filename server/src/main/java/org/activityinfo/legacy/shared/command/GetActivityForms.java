package org.activityinfo.legacy.shared.command;

import org.activityinfo.legacy.shared.command.result.ActivityFormResults;

import java.util.Set;

/**
 * Fetches a list of forms based on the selected indicators
 * 
 */
public class GetActivityForms implements Command<ActivityFormResults> {
    
    private Filter filter;

    public GetActivityForms() {
    }

    public GetActivityForms(Filter filter) {
        this.filter = filter;
    }

    public GetActivityForms(Set<Integer> indicatorIds) {
        filter = new Filter();
        filter.addRestriction(DimensionType.Indicator, indicatorIds);
    }

    public Filter getFilter() {
        return filter;
    }

    public GetActivityForms setFilter(Filter filter) {
        this.filter = filter;
        return this;
    }
}
