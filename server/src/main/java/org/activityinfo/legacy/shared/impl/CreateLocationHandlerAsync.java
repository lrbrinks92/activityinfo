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

import com.bedatadriven.rebar.sql.client.SqlResultCallback;
import com.bedatadriven.rebar.sql.client.SqlResultSet;
import com.bedatadriven.rebar.sql.client.SqlTransaction;
import com.bedatadriven.rebar.sql.client.query.SqlInsert;
import com.bedatadriven.rebar.sql.client.query.SqlQuery;
import com.bedatadriven.rebar.sql.client.query.SqlUpdate;
import com.extjs.gxt.ui.client.data.RpcMap;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.activityinfo.legacy.shared.command.CreateLocation;
import org.activityinfo.legacy.shared.command.result.VoidResult;
import org.activityinfo.legacy.shared.model.AdminLevelDTO;

import java.util.Date;

public class CreateLocationHandlerAsync implements CommandHandlerAsync<CreateLocation, VoidResult> {

    @Override
    public void execute(final CreateLocation command,
                        ExecutionContext context,
                        final AsyncCallback<VoidResult> callback) {

        SqlQuery.select("LocationTypeId")
                .from(Tables.LOCATION)
                .where("LocationId")
                .equalTo(command.getLocationId())
                .execute(context.getTransaction(), new SqlResultCallback() {
                    @Override
                    public void onSuccess(SqlTransaction tx, SqlResultSet results) {
                        if (results.getRows().isEmpty()) {
                            // New Location
                            createLocation(tx, command);
                            callback.onSuccess(null);

                        } else {
                            updateLocation(tx, command.getProperties());
                            callback.onSuccess(null);
                        }
                    }
                });
    }

    private void createLocation(SqlTransaction tx, CreateLocation command) {

        RpcMap properties = command.getProperties();

        SqlInsert.insertInto("location")
                 .value("LocationId", properties.get("id"))
                 .value("LocationTypeId", properties.get("locationTypeId"))
                 .value("Name", properties.get("name"))
                 .value("Axe", properties.get("axe"))
                 .value("X", properties.get("longitude"))
                 .value("Y", properties.get("latitude"))
                 .value("timeEdited", new Date().getTime())
                 .execute(tx);

        insertAdminLinks(tx, properties);
    }


    private void updateLocation(SqlTransaction tx, RpcMap properties) {
        SqlUpdate.update("location")
                 .valueIfNotNull("Name", properties.get("name"))
                 .valueIfNotNull("Axe", properties.get("axe"))
                 .valueIfNotNull("X", properties.get("longitude"))
                 .valueIfNotNull("Y", properties.get("latitude"))
                 .valueIfNotNull("workflowstatusid", properties.get("workflowstatusid"))
                 .value("timeEdited", new Date().getTime())
                 .where("locationId", properties.get("id"))
                 .execute(tx);

        SqlUpdate.delete(Tables.LOCATION_ADMIN_LINK).where("LocationId", properties.get("id")).execute(tx);

        insertAdminLinks(tx, properties);
    }

    private void insertAdminLinks(SqlTransaction tx, RpcMap properties) {

        for (String property : properties.keySet()) {
            if (property.startsWith(AdminLevelDTO.PROPERTY_PREFIX)) {
                SqlInsert.insertInto("locationadminlink")
                         .value("LocationId", properties.get("id"))
                         .value("AdminEntityId", properties.get(property))
                         .execute(tx);
            }
        }
    }

}
