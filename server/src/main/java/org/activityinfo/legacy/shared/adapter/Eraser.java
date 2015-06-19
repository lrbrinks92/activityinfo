package org.activityinfo.legacy.shared.adapter;
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

import com.google.common.collect.Lists;
import org.activityinfo.model.legacy.CuidAdapter;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.promise.Promise;
import org.activityinfo.core.shared.workflow.Workflow;
import org.activityinfo.legacy.client.Dispatcher;
import org.activityinfo.legacy.shared.command.BatchCommand;
import org.activityinfo.legacy.shared.command.Command;
import org.activityinfo.legacy.shared.command.CreateLocation;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.promise.Promise;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yuriyz on 3/27/14.
 */
public class Eraser {

    private final Dispatcher dispatcher;
    private final Collection<ResourceId> instanceIds;

    public Eraser(Dispatcher dispatcher, Collection<ResourceId> instanceIds) {
        this.dispatcher = dispatcher;
        this.instanceIds = instanceIds;
    }

    public Promise<Void> execute() {

        List<Command> commands = Lists.newArrayList();
        for (ResourceId instanceId : instanceIds) {
            if (instanceId.getDomain() == CuidAdapter.LOCATION_DOMAIN) {


                Map<String, Object> properties = new HashMap<>();
                properties.put("id", CuidAdapter.getLegacyIdFromCuid(instanceId));
                properties.put("workflowstatusid", Workflow.REJECTED);

                commands.add(new CreateLocation(properties));
            }
        }

        return dispatcher.execute(new BatchCommand(commands)).thenDiscardResult();
    }
}
