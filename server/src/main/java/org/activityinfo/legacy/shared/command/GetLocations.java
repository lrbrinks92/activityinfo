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

import java.util.ArrayList;
import java.util.List;

public class GetLocations implements Command<LocationResult> {
    private static final long serialVersionUID = 6998517531187983518L;

    private List<Integer> locationIds;
    private Filter filter;
    private Integer locationTypeId;

    public GetLocations() {
        locationIds = new ArrayList<Integer>();
    }

    public GetLocations(Integer id) {
        locationIds = new ArrayList<Integer>();
        if (id != null) {
            locationIds.add(id);
        }
    }

    public GetLocations(List<Integer> ids) {
        locationIds = ids;
    }

    public GetLocations(Filter filter) {
        this.filter = filter;
    }

    public List<Integer> getLocationIds() {
        return locationIds;
    }

    public void setLocationIds(List<Integer> ids) {
        locationIds = ids;
    }

    public Integer getLocationTypeId() {
        return locationTypeId;
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public GetLocations setLocationTypeId(Integer locationTypeId) {
        this.locationTypeId = locationTypeId;
        return this;
    }

    public boolean hasLocationIds() {
        return (locationIds != null && locationIds.size() > 0);
    }

}
