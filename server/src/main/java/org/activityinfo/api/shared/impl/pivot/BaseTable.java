package org.activityinfo.api.shared.impl.pivot;

/*
 * #%L
 * ActivityInfo Server
 * %%
 * Copyright (C) 2009 - 2013 UNICEF
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.bedatadriven.rebar.sql.client.query.SqlQuery;
import org.activityinfo.reports.shared.content.TargetCategory;
import org.activityinfo.reports.shared.model.DimensionType;
import org.activityinfo.api.shared.command.PivotSites;

/**
 * Defines a base table of values to be aggregated and collected by the
 * PivotQuery
 */
public abstract class BaseTable {

    /**
     * @param dimensions
     * @return true if this base table is applicable for the given set of
     * dimensions, false if not
     */
    public abstract boolean accept(PivotSites command);

    public abstract void setupQuery(PivotSites command, SqlQuery query);

    /**
     * @param type
     * @return the fully-qualified table and column containing the id for this
     * dimension
     */
    public abstract String getDimensionIdColumn(DimensionType type);

    public abstract String getDateCompleteColumn();

    public abstract TargetCategory getTargetCategory();

    public SqlQuery createSqlQuery() {
        return new SqlQuery();
    }

    public boolean groupDimColumns() {
        return true;
    }
}
