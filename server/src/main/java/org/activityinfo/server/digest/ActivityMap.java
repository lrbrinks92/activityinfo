package org.activityinfo.server.digest;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.activityinfo.server.database.hibernate.entity.SiteHistory;
import org.activityinfo.server.database.hibernate.entity.User;

public class ActivityMap {
    private final User user;
    private final Map<Integer, Integer> map = new HashMap<Integer, Integer>();

    /**
     * @param user
     * @param date
     * @param days
     *            amount of days, base 1, so if we're only looking for today, the days parameter has a value of 1. Today
     *            is calculated from 24 hours before this moment, to accomodate the period between the last digest and
     *            midnight of that day.
     * @param histories
     */
    public ActivityMap(User user, Date date, int days, List<SiteHistory> histories) {
        this.user = user;

        for (int i = 0; i < days; i++) {
            map.put(i, 0);
        }

        if (histories != null && !histories.isEmpty()) {
            for (SiteHistory history : histories) {
                int daysBetween = DigestDateUtil.absoluteDaysBetween(date, history.getTimeCreated());
                map.put(daysBetween, map.get(daysBetween) + 1);
            }
        }
    }

    public User getUser() {
        return user;
    }

    public Map<Integer, Integer> getMap() {
        return map;
    }

    public boolean hasActivity() {
        for (Integer value : map.values()) {
            if (value > 0) {
                return true;
            }
        }
        return false;
    }

    public static Map<Integer, Integer> getTotalActivityMap(List<ActivityMap> activities, int defaultDays) {
        Map<Integer, Integer> totals = new HashMap<Integer, Integer>();

        int size = defaultDays;
        if (activities != null && !activities.isEmpty()) {
            size = activities.get(0).map.keySet().size();
        }

        for (int i = 0; i < size; i++) {
            totals.put(i, 0);
        }

        for (ActivityMap activity : activities) {
            for (Entry<Integer, Integer> act : activity.map.entrySet()) {
                totals.put(act.getKey(), totals.get(act.getKey()) + act.getValue());
            }
        }

        return totals;
    }
}