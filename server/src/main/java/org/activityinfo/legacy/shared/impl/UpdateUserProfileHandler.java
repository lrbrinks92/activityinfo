package org.activityinfo.legacy.shared.impl;

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

import com.bedatadriven.rebar.sql.client.query.SqlUpdate;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.activityinfo.legacy.shared.command.UpdateUserProfile;
import org.activityinfo.legacy.shared.command.result.VoidResult;
import org.activityinfo.legacy.shared.model.UserProfileDTO;

public class UpdateUserProfileHandler implements CommandHandlerAsync<UpdateUserProfile, VoidResult> {

    @Override
    public void execute(final UpdateUserProfile command,
                        ExecutionContext context,
                        final AsyncCallback<VoidResult> callback) {

        UserProfileDTO model = command.getModel();

        SqlUpdate.update("userlogin")
                 .where("userId", model.getUserId())
                 .value("name", model.getName())
                 .value("organization", model.getOrganization()).value("jobtitle", model.getJobtitle())
                // .value("locale", model.toString())
                .value("emailNotification", model.isEmailNotification()).execute(context.getTransaction());

        callback.onSuccess(new VoidResult());
    }
}
