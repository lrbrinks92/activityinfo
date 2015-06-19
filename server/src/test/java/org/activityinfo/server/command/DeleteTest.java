package org.activityinfo.server.command;

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

import com.extjs.gxt.ui.client.data.ModelData;
import org.activityinfo.fixtures.InjectionSupport;
import org.activityinfo.legacy.shared.command.Delete;
import org.activityinfo.legacy.shared.command.GetActivityForm;
import org.activityinfo.legacy.shared.command.GetSchema;
import org.activityinfo.legacy.shared.command.GetSites;
import org.activityinfo.legacy.shared.command.result.PagingResult;
import org.activityinfo.legacy.shared.exception.CommandException;
import org.activityinfo.legacy.shared.model.ActivityFormDTO;
import org.activityinfo.legacy.shared.model.SchemaDTO;
import org.activityinfo.legacy.shared.model.SiteDTO;
import org.activityinfo.server.database.OnDataSet;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

@RunWith(InjectionSupport.class)
@OnDataSet("/dbunit/sites-simple1.db.xml")
public class DeleteTest extends CommandTestCase {

    public <T extends ModelData> T getById(Collection<T> list, Integer id) {
        for (T element : list) {
            if (id.equals(element.get("id"))) {
                return element;
            }
        }
        return null;
    }

    @Test
    public void testDeleteSite() throws CommandException {

        PagingResult<SiteDTO> sites = execute(GetSites.byId(3));
        execute(new Delete(sites.getData().get(0)));

        sites = execute(GetSites.byId(3));
        Assert.assertEquals(0, sites.getData().size());

        sites = execute(new GetSites());
        assertNull(getById(sites.getData(), 3));
    }

    @Test
    public void testDeleteIndicator() throws CommandException {

        int activityId = 1;
        int indicatorId = 1;

        execute(new Delete("Indicator", indicatorId));

        ActivityFormDTO form = execute(new GetActivityForm(activityId));
        assertNull(form.getIndicatorById(indicatorId));

        PagingResult<SiteDTO> sites = execute(GetSites.byId(1));
        assertNull(sites.getData().get(0).getIndicatorValue(1));
    }

    @Test
    public void testDeleteAttribute() throws CommandException {

        ActivityFormDTO form = execute(new GetActivityForm(1));
        execute(new Delete(form.getAttributeById(1)));

        form = execute(new GetActivityForm(1));
        assertNull(form.getAttributeById(1));
       
    }

    @Test
    public void testDeleteActivity() throws CommandException {

        ActivityFormDTO form = execute(new GetActivityForm(1));
        execute(new Delete(form));
        execute(new Delete("Activity", 4));

        SchemaDTO schema = execute(new GetSchema());
        assertNull("delete by entity reference", schema.getActivityById(1));
        assertNull("delete by id", schema.getActivityById(4));
    }

    @Test
    public void testDeleteLocationType() throws CommandException {
        int locationTypeId = 4;

        SchemaDTO schema = execute(new GetSchema());
        assertNotNull(schema.getLocationTypeById(locationTypeId)); // assert location type exists

        execute(new Delete("LocationType", locationTypeId));

        schema = execute(new GetSchema());
        assertTrue(schema.getLocationTypeById(locationTypeId).isDeleted()); // assert "delete" flag is set
    }
}
