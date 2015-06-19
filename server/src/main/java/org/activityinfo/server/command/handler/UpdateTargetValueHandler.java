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
import org.activityinfo.legacy.shared.command.UpdateTargetValue;
import org.activityinfo.legacy.shared.command.result.CommandResult;
import org.activityinfo.legacy.shared.command.result.VoidResult;
import org.activityinfo.legacy.shared.exception.CommandException;
import org.activityinfo.server.database.hibernate.entity.*;

import javax.persistence.EntityManager;
import java.util.logging.Logger;

public class UpdateTargetValueHandler extends BaseEntityHandler implements CommandHandler<UpdateTargetValue> {

    private final static Logger LOG = Logger.getLogger(UpdateTargetValueHandler.class.getName());

    @Inject
    public UpdateTargetValueHandler(EntityManager em) {
        super(em);
    }

    @Override
    public CommandResult execute(UpdateTargetValue cmd, User user) throws CommandException {

        Double newValue = cmd.getChanges().get("value");

        TargetValue targetValue = entityManager().find(TargetValue.class,
                new TargetValueId(cmd.getTargetId(), cmd.getIndicatorId()));
        
        if(targetValue == null) {
        
            if(newValue != null) {
                // Need a new record
                Target target = entityManager().find(Target.class, cmd.getTargetId());
                Indicator indicator = entityManager().find(Indicator.class, cmd.getIndicatorId());

                targetValue = new TargetValue();
                targetValue.setId(new TargetValueId(cmd.getTargetId(), cmd.getIndicatorId()));
                targetValue.setValue(cmd.getChanges().get("value"));
                targetValue.setTarget(target);
                targetValue.setIndicator(indicator);

                entityManager().persist(targetValue);
            }

        } else {
            targetValue.setValue(newValue);

        }
        
        return new VoidResult();
    }
}
