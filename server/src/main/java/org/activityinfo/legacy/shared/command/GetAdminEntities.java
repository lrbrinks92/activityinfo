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

import com.google.common.collect.Sets;
import org.activityinfo.legacy.shared.command.result.AdminEntityResult;

import java.util.Collection;
import java.util.Set;

/**
 * Retrieves a list of admin entities from the server.
 *
 * @author alexander
 */
public class GetAdminEntities extends GetListCommand<AdminEntityResult> {

    public static final int ROOT = -1;

    private Collection<Integer> countryIds;
    private Integer levelId;
    private Integer parentId;
    private Set<Integer> entityIds;
    private Filter filter;

    public GetAdminEntities() {
        super();
    }

    public GetAdminEntities(int levelId) {
        super();
        this.levelId = levelId;
    }

    public GetAdminEntities(int levelId, Integer parentId) {
        super();
        this.levelId = levelId;
        this.parentId = parentId;
    }

    public Integer getLevelId() {
        return levelId;
    }

    public void setLevelId(Integer levelId) {
        this.levelId = levelId;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public Set<Integer> getEntityIds() {
        return entityIds;
    }

    public void setEntityIds(Iterable<Integer> entityIds) {
        this.entityIds = Sets.newHashSet(entityIds);
    }

    public GetAdminEntities setEntityId(Integer entityId) {
        this.entityIds = Sets.newHashSet(entityId);
        return this;
    }

    public void setCountryIds(Collection<Integer> countryIds) {
        this.countryIds = countryIds;
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GetAdminEntities that = (GetAdminEntities) o;

        if (countryIds != null ? !countryIds.equals(that.countryIds) : that.countryIds != null) {
            return false;
        }
        if (entityIds != null ? !entityIds.equals(that.entityIds) : that.entityIds != null) {
            return false;
        }
        if (filter != null ? !filter.equals(that.filter) : that.filter != null) {
            return false;
        }
        if (levelId != null ? !levelId.equals(that.levelId) : that.levelId != null) {
            return false;
        }
        if (parentId != null ? !parentId.equals(that.parentId) : that.parentId != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = countryIds != null ? countryIds.hashCode() : 0;
        result = 31 * result + (levelId != null ? levelId.hashCode() : 0);
        result = 31 * result + (parentId != null ? parentId.hashCode() : 0);
        result = 31 * result + (entityIds != null ? entityIds.hashCode() : 0);
        result = 31 * result + (filter != null ? filter.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "GetAdminEntities [levelId=" + levelId + ", parentId=" + parentId + ", filter=" + filter + ", country=" +
               countryIds + ", entityIds=" + entityIds + "]";
    }

}