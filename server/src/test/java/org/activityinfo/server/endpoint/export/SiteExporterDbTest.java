package org.activityinfo.server.endpoint.export;
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

import org.activityinfo.fixtures.InjectionSupport;
import org.activityinfo.legacy.shared.command.DimensionType;
import org.activityinfo.legacy.shared.command.Filter;
import org.activityinfo.server.command.CommandTestCase2;
import org.activityinfo.server.database.OnDataSet;
import org.activityinfo.server.report.NullStorageProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author yuriyz on 10/23/2014.
 */
@RunWith(InjectionSupport.class)
@OnDataSet("/dbunit/sites-simple1.db.xml")
public class SiteExporterDbTest extends CommandTestCase2 {

    public static final int PEAR_DB = 1;

    /**
     * Exporter obey following rules:
     * - only activities of selected DB
     * - number of sheets must be the same as number of activities
     */
    @Test
    public void numberOfSheets() {

        Filter filter = new Filter();
        filter.addRestriction(DimensionType.Database, PEAR_DB);

        SiteExporter exporter = new SiteExporter(new TaskContext(getDispatcherSync(), new NullStorageProvider(), "XY"));
        exporter.buildExcelWorkbook(filter);

        assertEquals(exporter.getBook().getNumberOfSheets(), 2);
        assertNotNull(exporter.getBook().getSheet("PEAR - NFI"));
    }
}
