package org.activityinfo.legacy.shared.command;

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

import org.activityinfo.legacy.shared.command.result.LocationResult;
import org.activityinfo.legacy.shared.exception.CommandException;
import org.activityinfo.legacy.shared.model.LocationDTO;
import org.activityinfo.fixtures.InjectionSupport;
import org.activityinfo.server.command.CommandTestCase2;
import org.activityinfo.server.command.LocationDTOs;
import org.activityinfo.server.database.OnDataSet;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(InjectionSupport.class)
@OnDataSet("/dbunit/sites-simple1.db.xml")
public class CreateLocationHandlerTest extends CommandTestCase2 {
    @Test
    public void test() throws CommandException {
        LocationDTO location = LocationDTOs.newLocation();
        execute(new CreateLocation(location));

        SearchLocations getLocations = new SearchLocations().setName(location
                .getName());
        LocationResult locations = execute(getLocations);

        LocationDTO newLocation = locations.getData().get(0);
        assertEquals(location.getName(), newLocation.getName());
        assertEquals(location.getAxe(), newLocation.getAxe());
        assertEquals(location.getLongitude(), newLocation.getLongitude());
        assertEquals(location.getLatitude(), newLocation.getLatitude());
    }
}
