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
import com.bedatadriven.rebar.time.calendar.LocalDate;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.activityinfo.legacy.shared.command.UpdateSite;
import org.activityinfo.legacy.shared.command.result.VoidResult;
import org.activityinfo.legacy.shared.model.AttributeDTO;
import org.activityinfo.legacy.shared.model.IndicatorDTO;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

public class UpdateSiteHandlerAsync implements CommandHandlerAsync<UpdateSite, VoidResult> {

    @Override
    public void execute(final UpdateSite command, ExecutionContext context, final AsyncCallback<VoidResult> callback) {

        final Map<String, Object> changes = command.getChanges().getTransientMap();
        SqlTransaction tx = context.getTransaction();
        updateSiteProperties(tx, command, changes);
        updateAttributeValues(tx, command.getSiteId(), changes);
        updateReportingPeriod(tx, command.getSiteId(), changes);

        callback.onSuccess(new VoidResult());
    }

    private void updateSiteProperties(SqlTransaction tx, UpdateSite command, Map<String, Object> changes) {
        Date now = new Date();
        SqlUpdate.update("site")
                 .where("SiteId", command.getSiteId())
                 .value("date1", changes)
                 .value("date2", changes)
                 .value("comments", changes)
                 .value("projectId", changes)
                 .value("partnerId", changes)
                 .value("locationId", changes)
                 .value("dateEdited", now)
                 .value("timeEdited", now.getTime())
                 .execute(tx);
    }

    private void updateAttributeValues(SqlTransaction tx, int siteId, Map<String, Object> changes) {
        for (Entry<String, Object> change : changes.entrySet()) {
            if (change.getKey().startsWith(AttributeDTO.PROPERTY_PREFIX)) {
                int attributeId = AttributeDTO.idForPropertyName(change.getKey());
                Boolean value = (Boolean) change.getValue();

                SqlUpdate.delete(Tables.ATTRIBUTE_VALUE)
                         .where("attributeId", attributeId)
                         .where("siteId", siteId)
                         .execute(tx);

                if (value != null) {
                    SqlInsert.insertInto(Tables.ATTRIBUTE_VALUE)
                             .value("attributeId", attributeId)
                             .value("siteId", siteId)
                             .value("value", value)
                             .execute(tx);
                }
            }
        }
    }

    private void updateReportingPeriod(SqlTransaction tx, final int siteId, final Map<String, Object> changes) {
        SqlQuery.select("reportingPeriodId")
                .from(Tables.REPORTING_PERIOD)
                .where("siteId")
                .equalTo(siteId)
                .execute(tx, new SqlResultCallback() {

                    @Override
                    public void onSuccess(SqlTransaction tx, SqlResultSet results) {
                        if (results.getRows().size() == 1) {
                            updateReportingPeriod(tx, siteId, results.getRow(0).getInt("reportingPeriodId"), changes);
                        }
                    }
                });
    }

    private void updateReportingPeriod(SqlTransaction tx,
                                       int siteId,
                                       int reportingPeriodId,
                                       Map<String, Object> changes) {

        SqlUpdate.update(Tables.REPORTING_PERIOD)
                 .where("reportingPeriodId", reportingPeriodId)
                 .value("date1", changes)
                 .value("date2", changes)
                 .execute(tx);

        for (Map.Entry<String, Object> change : changes.entrySet()) {
            if (change.getKey().startsWith(IndicatorDTO.PROPERTY_PREFIX)) {
                int indicatorId = IndicatorDTO.indicatorIdForPropertyName(change.getKey());
                Object value = change.getValue();

                SqlUpdate.delete(Tables.INDICATOR_VALUE)
                         .where("reportingPeriodId", reportingPeriodId)
                         .where("indicatorId", indicatorId)
                         .execute(tx);

                if (value != null) {
                    SqlInsert sqlInsert = SqlInsert.insertInto(Tables.INDICATOR_VALUE)
                            .value("reportingPeriodId", reportingPeriodId)
                            .value("indicatorId", indicatorId);

                    if (value instanceof Double) {
                        if (((Double)value).isNaN()) {
                            throw new RuntimeException("It's not allowed to send Double.NaN values for update, indicatorId: " + indicatorId);
                        }

                        sqlInsert.value("value", value).execute(tx);
                    } else if (value instanceof String) {
                        sqlInsert.value("TextValue", value).execute(tx);
                    } else if (value instanceof LocalDate) {
                        sqlInsert.value("DateValue", value).execute(tx);
                    }
                }
            }
        }
    }

}
