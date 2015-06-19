package org.activityinfo.test.driver;


import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import java.util.List;

public class TableData {
    
    public class Row {
        private List<String> cells;

        public Row(Iterable<String> cells) {
            this.cells = Lists.newArrayList(cells);
        }
        
        @Override
        public String toString() {
            return Joiner.on(",").join(cells);
        }
    }
    
    private List<Row> rows = Lists.newArrayList();
    
    public void addRow(Iterable<String> cells) {
        rows.add(new Row(cells));
    }
    
    public String toString() {
        return Joiner.on("\n").join(rows);
    }
    
}
