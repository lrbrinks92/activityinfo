package org.activityinfo.store.query.impl.join;

import com.google.common.collect.Iterables;
import org.activityinfo.model.expr.functions.MaxFunction;
import org.activityinfo.model.expr.functions.StatFunction;
import org.activityinfo.model.query.ColumnView;
import org.activityinfo.model.query.DoubleArrayColumnView;
import org.activityinfo.store.query.impl.Slot;
import org.activityinfo.store.query.shared.Aggregation;

import java.util.List;


public class JoinedSubFormColumnViewSlot implements Slot<ColumnView> {

    private List<SubFormJoin> links;
    private Slot<ColumnView> nestedColumn;

    private StatFunction statistic = MaxFunction.INSTANCE;
    
    private ColumnView result;

    public JoinedSubFormColumnViewSlot(List<SubFormJoin> links, Slot<ColumnView> nestedColumn) {
        this.links = links;
        this.nestedColumn = nestedColumn;
    }

    @Override
    public ColumnView get() {
        if(result == null) {
            result = join();
        }
        return result;
    }

    private ColumnView join() {

        // The nested column may contain multiple rows 
        // for each row on the left
        ColumnView subColumn = nestedColumn.get();
        int numSubRows = subColumn.numRows();

        // In order to produce a column with one summarized entry per 
        // output row, we need to assign "groupIds" going from
        // parentId -> master row index via the primary key

        int masterRowId[] = new int[numSubRows];
        double subColumnValues[] = new double[numSubRows];

        SubFormJoin join = Iterables.getOnlyElement(links);
        PrimaryKeyMap parentLookup = join.getMasterPrimaryKey().get();
        ColumnView parentColumn = join.getParentColumn().get();

        for (int i = 0; i < numSubRows; ++i) {
            // Get the parent id of this row
            String parentId = parentColumn.getString(i);
            masterRowId[i] = parentLookup.getRowIndex(parentId);

            // Store the value
            subColumnValues[i] = subColumn.getDouble(i);
        }
        
        int numMasterRows = parentLookup.getNumRows();

        // Sort the data values by master row index
        double[] result = Aggregation.aggregate(statistic, masterRowId, subColumnValues, numSubRows, numMasterRows);
        
        return new DoubleArrayColumnView(result);
    }

}
