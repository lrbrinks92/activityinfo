package org.activityinfo.server.command.handler.crud;

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

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import org.activityinfo.core.shared.workflow.Workflow;
import org.activityinfo.server.command.handler.PermissionOracle;
import org.activityinfo.server.database.hibernate.entity.Activity;
import org.activityinfo.server.database.hibernate.entity.LocationType;
import org.activityinfo.server.database.hibernate.entity.User;
import org.activityinfo.server.database.hibernate.entity.UserDatabase;

import javax.persistence.EntityManager;
import java.util.Date;

public class LocationTypePolicy implements EntityPolicy<Activity> {

    private final EntityManager em;

    @Inject
    public LocationTypePolicy(EntityManager em) {
        this.em = em;
    }

    @Override
    public Integer create(User user, PropertyMap properties) {
        int databaseId = properties.get("databaseId");
        UserDatabase database = em.find(UserDatabase.class, databaseId);

        PermissionOracle.using(em).assertDesignPrivileges(database, user);

        // create the entity
        LocationType locationType = new LocationType();
        locationType.setVersion(1L);
        locationType.setName(properties.<String>get("name"));
        locationType.setCountry(database.getCountry());
        locationType.setWorkflowId(properties.getOptionalString("workflowId", Workflow.CLOSED_WORKFLOW_ID));
        locationType.setDatabase(database);

        em.persist(locationType);
        
        
        return locationType.getId();
    }

    @Override
    public void update(User user, Object entityId, PropertyMap changes) {
        LocationType locationType = em.find(LocationType.class, entityId);

        PermissionOracle.using(em).assertDesignPrivileges(locationType.getDatabase(), user);

        applyProperties(locationType, changes);
        
        locationType.incrementVersion();
    }

    private void applyProperties(LocationType locationType, PropertyMap changes) {
        if (changes.containsKey("name")) {
            locationType.setName((String) changes.get("name"));
        }
        if (changes.containsKey("workflowId")) {
            String workflowId = changes.get("workflowId");
            Preconditions.checkArgument(Workflow.isValidId(workflowId), "invalid workflow id %s", workflowId);
            locationType.setWorkflowId(workflowId);
        }
        locationType.getDatabase().setLastSchemaUpdate(new Date());
    }
}
