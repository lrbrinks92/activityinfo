package org.activityinfo.store.query.shared;

import org.activityinfo.model.expr.functions.StatFunction;

import java.util.Arrays;

/**
 * Applies an aggregation function to a grouped list of values.
 */
public final class Aggregation {

    private Aggregation() {}

    /**
     * @param statistic the statistic to apply
     * @param groupId an array of at least length {@code numValues} containing group ids
     * @param values an array of at least length {@code numValues} containing the values to be aggregated
     * @param numValues number of values to aggregate.
     * @param numGroups number of groups present. The maximum value in the groupId values should be {@code numGroups-1}
     * @return an array of length {@code numGroups} containing the aggregated values.
     */
    public static double[] aggregate(StatFunction statistic, int[] groupId, double[] values,
                                     int numValues, int numGroups) {


        if(numValues == 0) {
            return new double[] { statistic.compute(values, 0, 0) };
        }

        HeapsortTandem.heapsortDescending(groupId, values, numValues);

        // Allocate the output for the results
        double result[] = new double[numGroups];
        Arrays.fill(result, Double.NaN);

        // Start at the first group
        int groupStart = 0;

        do {
            int masterRow = groupId[groupStart];

            // Find where this group ends
            int groupEnd = groupStart + 1;
            while (groupEnd < numValues && masterRow == groupId[groupEnd]) {
                groupEnd++;
            }

            // Compute the statistic over this group
            if(masterRow != -1) {
                result[masterRow] = statistic.compute(values, groupStart, groupEnd);
            }
            // Move to the next group
            groupStart = groupEnd;

        } while(groupStart < numValues);
        return result;
    }
}
