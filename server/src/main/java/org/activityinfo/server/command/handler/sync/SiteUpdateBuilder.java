package org.activityinfo.server.command.handler.sync;

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

import com.bedatadriven.rebar.sql.client.query.SqlQuery;
import com.google.inject.Inject;
import org.activityinfo.legacy.shared.command.GetSyncRegionUpdates;
import org.activityinfo.legacy.shared.command.result.SyncRegionUpdate;
import org.activityinfo.legacy.shared.impl.Tables;
import org.activityinfo.server.database.hibernate.entity.Activity;
import org.activityinfo.server.database.hibernate.entity.User;

import javax.persistence.EntityManager;
import java.io.IOException;

public class SiteUpdateBuilder implements UpdateBuilder {

    public static final String REGION_TYPE = "form-submissions";
    
    private final EntityManager entityManager;

    private Activity activity;
    private JpaBatchBuilder batch;
    private long localVersion;

    @Inject
    public SiteUpdateBuilder(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public SyncRegionUpdate build(User user, GetSyncRegionUpdates request) throws IOException {
        batch = new JpaBatchBuilder(entityManager, request.getRegionPath());
        activity = entityManager.find(Activity.class, request.getRegionId());
        localVersion = request.getLocalVersionNumber();

        if (activity.getSiteVersion() > localVersion) {
            if(localVersion > 0) {
                deleteUpdated();
            }

            insert(Tables.SITE, updatedSitesQuery());
            insert(Tables.ATTRIBUTE_VALUE, updateAttributeValues());
            insert(Tables.REPORTING_PERIOD, updatedReportingPeriods());
            insert(Tables.INDICATOR_VALUE, updateIndicatorValues());

        }
        batch.setComplete(true);
        batch.setVersion(activity.getVersion());
        return batch.buildUpdate();
    }

    private void insert(String table, SqlQuery query) {
        batch.insert().into(table).from(query).execute(entityManager);
    }

    
    private void deleteUpdated() throws IOException {
        String updatedIds = SqlQueryUtil.queryIdSet(entityManager, updatedSites());

        batch.addStatement("DELETE FROM attributevalue WHERE siteId IN " + updatedIds);
        batch.addStatement("DELETE FROM indicatorvalue WHERE reportingperiodid IN " +
                           "(SELECT reportingperiodid FROM reportingperiod WHERE siteId IN " + updatedIds + ")");
        batch.addStatement("DELETE FROM reportingperiod WHERE siteId IN " + updatedIds);
        batch.addStatement("DELETE FROM site WHERE siteId IN " + updatedIds);

        // there seem to be some clients left in an inconsistent state, probably
        // due to errors on the server side earlier. So we clean up.
        batch.addStatement("DELETE FROM indicatorvalue WHERE indicatorvalue.reportingperiodid NOT IN " +
                           "(select reportingperiodid from reportingperiod)");
    }

    private SqlQuery updatedSites() {
        return SqlQuery.select()
                       .from(Tables.SITE, "s")
                       .appendColumn("s.SiteId")
                       .where("s.ActivityId").equalTo(activity.getId())
                       .where("s.version").greaterThan(localVersion);
    }

    private SqlQuery updateIndicatorValues() {
        return SqlQuery.select()
                       .from(Tables.INDICATOR_VALUE, "iv")
                       .leftJoin(Tables.REPORTING_PERIOD, "rp").on("rp.ReportingPeriodId=iv.ReportingPeriodId")
                       .leftJoin(Tables.SITE, "s").on("rp.SiteId = s.SiteId")
                       .leftJoin(Tables.ACTIVITY, "a").on("s.ActivityId = a.ActivityId")
                       .appendColumn("iv.IndicatorId")
                       .appendColumn("iv.ReportingPeriodId")
                       .appendColumn("iv.Value")
                       .appendColumn("iv.TextValue")
                       .appendColumn("iv.DateValue")
                       .appendColumn("iv.BooleanValue")
                       .where("a.ActivityId").equalTo(activity.getId())
                       .where("s.version").greaterThan(localVersion)
                       .whereTrue("s.dateDeleted IS NULL");
    }

    private SqlQuery updatedReportingPeriods() {
        return SqlQuery.select()
                       .from(Tables.REPORTING_PERIOD, "rp")
                       .leftJoin(Tables.SITE, "s").on("rp.SiteId = s.SiteId")
                       .appendColumn("rp.ReportingPeriodId")
                       .appendColumn("rp.SiteId")
                       .appendColumn("rp.Date1")
                       .appendColumn("rp.Date2")
                       .where("s.activityId").equalTo(activity.getId())
                       .where("s.version").greaterThan(localVersion)
                       .whereTrue("s.dateDeleted IS NULL");
    }

    private SqlQuery updateAttributeValues() {
        return SqlQuery.select()
                       .from(Tables.ATTRIBUTE_VALUE, "av")
                       .leftJoin(Tables.SITE, "s").on("av.SiteId = s.SiteId")
                       .appendColumn("av.AttributeId")
                       .appendColumn("av.SiteId")
                       .appendColumn("av.Value")
                       .where("s.ActivityId").equalTo(activity.getId())
                       .where("s.version").greaterThan(localVersion)
                       .whereTrue("av.Value=1")
                       .whereTrue("s.dateDeleted IS NULL");
    }

    private SqlQuery updatedSitesQuery() {
        return SqlQuery.select()
                       .from(Tables.SITE, "s")
                       .appendColumn("s.SiteId")
                       .appendColumn("s.Date1")
                       .appendColumn("s.Date2")
                       .appendColumn("s.ActivityId")
                       .appendColumn("s.LocationId")
                       .appendColumn("s.PartnerId")
                       .appendColumn("s.ProjectId")
                       .appendColumn("s.Comments")
                       .appendColumn("s.timeEdited")
                       .where("s.version").greaterThan(localVersion)
                       .where("s.activityId").equalTo(activity.getId())
                       .whereTrue("s.dateDeleted IS NULL");
    }

}

