package org.activityinfo.server.command.handler;

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

import com.google.inject.Inject;
import org.activityinfo.legacy.shared.command.Delete;
import org.activityinfo.legacy.shared.command.result.CommandResult;
import org.activityinfo.legacy.shared.exception.CommandException;
import org.activityinfo.server.database.hibernate.entity.Deleteable;
import org.activityinfo.server.database.hibernate.entity.ReallyDeleteable;
import org.activityinfo.server.database.hibernate.entity.User;
import org.activityinfo.server.database.hibernate.entity.UserDatabase;

import javax.persistence.EntityManager;
import java.util.logging.Logger;

public class DeleteHandler implements CommandHandler<Delete> {
    
    private static final Logger LOGGER = Logger.getLogger(DeleteHandler.class.getName());
    
    private EntityManager em;
    private PermissionOracle permissionOracle;

    @Inject
    public DeleteHandler(EntityManager em, PermissionOracle permissionOracle) {
        this.em = em;
        this.permissionOracle = permissionOracle;
    }

    @Override
    public CommandResult execute(Delete cmd, User user) {

        
        Class entityClass = entityClassForEntityName(cmd.getEntityName());
        Object entity = em.find(entityClass, cmd.getId());
        
        if(entity == null) {
            throw new CommandException(String.format("%s with id %d does not exist.", 
                    cmd.getEntityName(), cmd.getId()));
        }

        // Ensure that the user is authorized to perform deletion
        permissionOracle.assertDeletionAuthorized(entity, user);

        if (entity instanceof Deleteable) {
            Deleteable deleteable = (Deleteable) entity;
            deleteable.delete();
        }

        if (entity instanceof ReallyDeleteable) {
            ReallyDeleteable reallyDeleteable = (ReallyDeleteable) entity;
            reallyDeleteable.deleteReferences();
            em.remove(reallyDeleteable);
        }

        return null;
    }


    private Class<Deleteable> entityClassForEntityName(String entityName) {
        try {
            return (Class<Deleteable>) Class.forName(UserDatabase.class.getPackage().getName() + "." + entityName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Invalid entity name '" + entityName + "'", e);
        } catch (ClassCastException e) {
            throw new RuntimeException("Entity type '" + entityName + "' not Deletable", e);
        }
    }
}
