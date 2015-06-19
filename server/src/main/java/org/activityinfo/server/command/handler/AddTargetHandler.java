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
import org.activityinfo.legacy.shared.command.AddTarget;
import org.activityinfo.legacy.shared.command.result.CommandResult;
import org.activityinfo.legacy.shared.command.result.CreateResult;
import org.activityinfo.legacy.shared.exception.CommandException;
import org.activityinfo.legacy.shared.model.TargetDTO;
import org.activityinfo.server.database.hibernate.entity.*;

import javax.persistence.EntityManager;
import java.util.Date;

public class AddTargetHandler implements CommandHandler<AddTarget> {

    private final EntityManager em;
    private final PermissionOracle permissionOracle;

    @Inject
    public AddTargetHandler(EntityManager em, PermissionOracle permissionOracle) {
        this.em = em;
        this.permissionOracle = permissionOracle;
    }

    @Override
    public CommandResult execute(AddTarget cmd, User user) throws CommandException {

        TargetDTO form = cmd.getTarget();
        UserDatabase db = em.find(UserDatabase.class, cmd.getDatabaseId());

        permissionOracle.assertDesignPrivileges(db, user);
        
        Partner partner = null;
        if (form.get("partnerId") != null) {
            partner = em.find(Partner.class, form.get("partnerId"));
        } else if (form.getPartner() != null) {
            partner = em.find(Partner.class, form.getPartner().getId());
        }

        Project project = null;
        if (form.get("projectId") != null) {
            project = em.find(Project.class, form.get("projectId"));
        } else if (form.getProject() != null) {
            project = em.find(Project.class, form.getProject().getId());
        }
        
        AdminEntity adminEntity = null;
        // if(form.getAdminEntity() != null){
        // adminEntity = em.find(AdminEntity.class,
        // form.getAdminEntity().getId());
        // }

        Target target = new Target();
        target.setName(form.getName());
        target.setUserDatabase(db);
        target.setAdminEntity(adminEntity);
        target.setPartner(partner);
        target.setProject(project);
        target.setDate1(form.getDate1());
        target.setDate2(form.getDate2());

        db.setLastSchemaUpdate(new Date());

        em.persist(target);
        em.persist(db);

        db.getTargets().add(target);

        // if(adminEntity!=null){
        // adminEntity.getTargets().add(target);
        // }
        if (project != null) {
            project.getTargets().add(target);
        }
        if (partner != null) {
            partner.getTargets().add(target);
        }

        return new CreateResult(target.getId());
    }
}