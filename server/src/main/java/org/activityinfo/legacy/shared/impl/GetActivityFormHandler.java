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
import com.bedatadriven.rebar.sql.client.SqlResultSetRow;
import com.bedatadriven.rebar.sql.client.SqlTransaction;
import com.bedatadriven.rebar.sql.client.query.SqlQuery;
import com.bedatadriven.rebar.sql.client.util.RowHandler;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.activityinfo.legacy.shared.command.GetActivityForm;
import org.activityinfo.legacy.shared.exception.IllegalAccessCommandException;
import org.activityinfo.legacy.shared.model.*;
import org.activityinfo.legacy.shared.reports.util.mapping.Extents;
import org.activityinfo.promise.Promise;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetActivityFormHandler implements CommandHandlerAsync<GetActivityForm, ActivityFormDTO> {

    private ActivityFormCache cache;

    public GetActivityFormHandler() {
        this.cache = new ActivityFormCache();
    }

    public GetActivityFormHandler(ActivityFormCache cache) {
        this.cache = cache;
    }

    @Override
    public void execute(GetActivityForm command, final ExecutionContext context, AsyncCallback<ActivityFormDTO> callback) {

        Promise<ActivityFormDTO> form = fetchForm(context, command.getActivityId());

        // Apply permissions independently of caching so that we can safely cache the form for all users.
        form.join(new Function<ActivityFormDTO, Promise<ActivityFormDTO>>() {
            @Nullable
            @Override
            public Promise<ActivityFormDTO> apply(ActivityFormDTO form) {
                if (form.getOwnerUserId() == context.getUser().getId()) {
                    form.setEditAllowed(true);
                    form.setEditAllAllowed(true);
                    form.setDesignAllowed(true);
                    return Promise.resolved(form);

                } else {
                    return applyPermissions(context, form);
                }
            }
        }).then(callback);

    }

    private Promise<ActivityFormDTO> applyPermissions(ExecutionContext context, final ActivityFormDTO form) {
        final Promise<ActivityFormDTO> result = new Promise<>();
        SqlQuery.selectAll()
                .appendColumn("allowView")
                .appendColumn("allowViewAll")
                .appendColumn("allowEdit")
                .appendColumn("allowEditAll")
                .appendColumn("allowDesign")
                .appendColumn("partnerId")
                .from(Tables.USER_PERMISSION, "p")
                .where("p.UserId").equalTo(context.getUser().getId())
                .where("p.DatabaseId").equalTo(form.getDatabaseId())
                .execute(context.getTransaction(), new SqlResultCallback() {
                    @Override
                    public void onSuccess(SqlTransaction tx, SqlResultSet results) {
                        if(results.getRows().isEmpty()) {
                            result.reject(new IllegalAccessCommandException());
                            return;
                        }
                        SqlResultSetRow row = results.getRow(0);
                        if(!row.getBoolean("allowView")) {
                            result.reject(new IllegalAccessCommandException());
                            return;
                        }
                        form.setEditAllowed(row.getBoolean("allowEdit"));
                        form.setEditAllAllowed(row.getBoolean("allowEditAll"));
                        form.setDesignAllowed(row.getBoolean("allowDesign"));
                        form.setCurrentPartnerId(row.getInt("partnerId"));
                        result.resolve(form);
                    }
                });
        return result;
    }

    private Promise<ActivityFormDTO> fetchForm(ExecutionContext context, final int activityId) {
        ActivityFormDTO form = cache.getCopyIfPresent(activityId);
        if(form != null) {
            return Promise.resolved(form);
        }
        return new FormBuilder(activityId).build(context).then(new Function<ActivityFormDTO, ActivityFormDTO>() {
            @Override
            public ActivityFormDTO apply(ActivityFormDTO input) {
                cache.put(input);
                return input;
            }
        });
    }

    private class FormBuilder {

        private int countryId;
        private int databaseId;
        private int activityId;

        private ActivityFormDTO activity;
        private final Map<Integer, AttributeGroupDTO> attributeGroups = new HashMap<Integer, AttributeGroupDTO>();

        private SqlTransaction tx;

        private FormBuilder(int activityId) {
            this.activityId = activityId;
            this.activity = new ActivityFormDTO();
            this.activity.setId(activityId);
        }

        public Promise<ActivityFormDTO> build(ExecutionContext context) {
            this.tx = context.getTransaction();

            List<Promise<Void>> tasks = Lists.newArrayList();

            tasks.add(loadActivity());
            tasks.add(loadIndicators());
            tasks.add(loadLockedPeriods());
            tasks.add(loadAttributeGroups());
            tasks.add(loadAttributes());

            return Promise.waitAll(tasks).then(Functions.constant(activity));
        }

        public Promise<Void> loadActivity() {

            SqlQuery query = SqlQuery.select()
                    .appendColumn("a.activityId", "activityId")
                    .appendColumn("a.name", "name")
                    .appendColumn("a.category", "category")
                    .appendColumn("a.locationTypeId", "locationTypeId")
                    .appendColumn("t.name", "locationTypeName")
                    .appendColumn("t.boundAdminLevelId", "locationTypeLevelId")
                    .appendColumn("t.databaseId", "locationTypeDatabaseId")
                    .appendColumn("t.workflowId", "locationTypeWorkflowId")
                    .appendColumn("a.reportingFrequency", "reportingFrequency")
                    .appendColumn("a.databaseId", "databaseId")
                    .appendColumn("a.classicView", "classicView")
                    .appendColumn("a.published", "published")
                    .appendColumn("db.name", "databaseName")
                    .appendColumn("db.ownerUserId", "ownerUserId")
                    .appendColumn("c.countryId", "countryId")
                    .appendColumn("c.x1", "x1")
                    .appendColumn("c.y1", "y1")
                    .appendColumn("c.x2", "x2")
                    .appendColumn("c.y2", "y2")
                    .from("activity", "a")
                    .leftJoin("userdatabase", "db").on("db.databaseId=a.databaseId")
                    .leftJoin("country", "c").on("db.countryId=c.countryId")
                    .leftJoin("locationtype", "t").on("t.locationTypeId=a.locationTypeId")
                    .where("a.activityId").equalTo(activityId);

            return execute(query, new RowHandler() {

                @Override
                public void handleRow(SqlResultSetRow row) {

                    countryId = row.getInt("countryId");
                    databaseId = row.getInt("databaseId");
                    Extents countryBounds = new Extents(
                            row.getDouble("x1"),
                            row.getDouble("y1"),
                            row.getDouble("x2"),
                            row.getDouble("y2"));

                    LocationTypeDTO locationType = new LocationTypeDTO();
                    locationType.setId(row.getInt("locationTypeId"));
                    locationType.setName(row.getString("locationTypeName"));
                    locationType.setCountryBounds(countryBounds);
                    locationType.setWorkflowId(row.getString("locationTypeWorkflowId"));

                    if(!row.isNull("locationTypeLevelId")) {
                        locationType.setBoundAdminLevelId(row.getInt("locationTypeLevelId"));
                    }
                    if(!row.isNull("locationTypeDatabaseId")) {
                        locationType.setDatabaseId(row.getInt("locationTypeDatabaseId"));
                    }

                    activity.setId(row.getInt("activityId"));
                    activity.setDatabaseId(databaseId);
                    activity.setDatabaseName(row.getString("databaseName"));
                    activity.setOwnerUserId(row.getInt("ownerUserId"));
                    activity.setName(row.getString("name"));
                    activity.setCategory(row.getString("category"));
                    activity.setReportingFrequency(row.getInt("reportingFrequency"));
                    activity.setPublished(row.getInt("published"));
                    activity.setClassicView(row.getBoolean("classicView"));
                    activity.setLocationType(locationType);

                }
            }).join(new Function<Void, Promise<Void>>() {
                @Nullable
                @Override
                public Promise<Void> apply(Void input) {
                    return Promise.waitAll(
                            loadAdminLevels(),
                            loadPartners(),
                            loadProjects());
                }
            });
        }


        public Promise<Void> loadAdminLevels() {
            return execute(SqlQuery.select("adminLevelId", "name", "parentId", "countryId")
                            .from("adminlevel")
                            .whereTrue("deleted=0")
                            .where("countryId").equalTo(countryId),
                    new SqlResultCallback() {

                        @Override
                        public void onSuccess(SqlTransaction tx, SqlResultSet results) {
                            Map<Integer, AdminLevelDTO> levels = new HashMap<Integer, AdminLevelDTO>();
                            for(SqlResultSetRow row : results.getRows()) {
                                AdminLevelDTO level = new AdminLevelDTO();
                                level.setId(row.getInt("adminLevelId"));
                                level.setName(row.getString("name"));
                                level.setCountryId(row.getInt("countryId"));

                                if (!row.isNull("parentId")) {
                                    level.setParentLevelId(row.getInt("parentId"));
                                }
                                levels.put(level.getId(), level);
                            }
                            activity.getLocationType().setAdminLevels(levelsForLocationType(levels, activity.getLocationType()));
                        }

                    });
        }


        private List<AdminLevelDTO> levelsForLocationType(Map<Integer, AdminLevelDTO> adminLevels, LocationTypeDTO type) {

            if (type.isAdminLevel()) {
                // if this activity is bound to an administrative
                // level, then we need only as far down as this goes
                return getRootAdminLevel(adminLevels, type);

            } else if(type.isNationwide()) {
                return Lists.newArrayList();

            } else {
                return new ArrayList<>(adminLevels.values());
            }
        }

        /**
         * Must be called only by location type that has admin level bound!
         * @param type location type
         * @return root admin level
         */
        private List<AdminLevelDTO> getRootAdminLevel(Map<Integer, AdminLevelDTO> adminLevels, LocationTypeDTO type) {
            List<AdminLevelDTO> ancestors = new ArrayList<AdminLevelDTO>();
            AdminLevelDTO level = adminLevels.get(type.getBoundAdminLevelId());

            if (level == null) {
                throw new IllegalStateException("Unable to find any admin level however in locationtype is marked to bound to admin level.");
            }

            while (true) {
                ancestors.add(0, level);

                if (level.isRoot()) {
                    return ancestors;
                } else {
                    level = adminLevels.get(level.getParentLevelId());
                }
            }
        }

        protected Promise<Void> loadProjects() {
            final Promise<Void> promise = new Promise<>();
            SqlQuery.select("name", "projectId", "description", "databaseId")
                    .from("project")
                    .where("databaseId").equalTo(databaseId)
                    .where("dateDeleted").isNull()
                    .orderBy("name")
                    .execute(tx, new SqlResultCallback() {
                        @Override
                        public void onSuccess(SqlTransaction tx, SqlResultSet results) {
                            List<ProjectDTO> projects = Lists.newArrayList();
                            for (SqlResultSetRow row : results.getRows()) {
                                ProjectDTO project = new ProjectDTO();
                                project.setName(row.getString("name"));
                                project.setId(row.getInt("projectId"));
                                project.setDescription(row.getString("description"));

                                projects.add(project);
                            }
                            activity.setProjects(projects);
                            promise.resolve(null);
                        }
                    });
            return promise;
        }

        protected Promise<Void> loadLockedPeriods() {
            final Promise<Void> promise = new Promise<>();
            SqlQuery.select("fromDate",
                    "toDate",
                    "enabled",
                    "name",
                    "lockedPeriodId",
                    "userDatabaseId",
                    "activityId",
                    "projectId")
                    .from("lockedperiod")
                    .whereTrue("(userDatabaseId=" + databaseId + ") OR " +
                            "(activityId=" + activity.getId() + ") OR " +
                            "(projectId in (select projectId from project where databaseId=" + databaseId + "))")

                    .execute(tx, new SqlResultCallback() {

                        @Override
                        public void onSuccess(SqlTransaction tx, SqlResultSet results) {
                            for (SqlResultSetRow row : results.getRows()) {
                                LockedPeriodDTO lockedPeriod = new LockedPeriodDTO();
                                lockedPeriod.setId(row.getInt("lockedPeriodId"));
                                lockedPeriod.setFromDate(row.getDate("fromDate"));
                                lockedPeriod.setToDate(row.getDate("toDate"));
                                lockedPeriod.setEnabled(row.getBoolean("enabled"));
                                lockedPeriod.setName(row.getString("name"));

                                if(!row.isNull("userDatabaseId")) {
                                    lockedPeriod.setParentId(row.getInt("userDatabaseId"));
                                    lockedPeriod.setParentType(UserDatabaseDTO.ENTITY_NAME);
                                    activity.getLockedPeriods().add(lockedPeriod);

                                } else if(!row.isNull("activityId")) {
                                    lockedPeriod.setParentId(row.getInt("activityId"));
                                    lockedPeriod.setParentType(ActivityDTO.ENTITY_NAME);
                                    activity.getLockedPeriods().add(lockedPeriod);

                                } else if(!row.isNull("projectId")) {
                                    lockedPeriod.setParentId(row.getInt("projectId"));
                                    lockedPeriod.setParentType(ProjectDTO.ENTITY_NAME);
                                    activity.getLockedPeriods().add(lockedPeriod);
                                }
                            }
                            promise.resolve(null);
                        }
                    });
            return promise;
        }

        private Promise<Void> loadPartners() {
            SqlQuery query = SqlQuery.select("d.databaseId", "d.partnerId", "p.name", "p.fullName")
                    .from(Tables.PARTNER_IN_DATABASE, "d")
                    .leftJoin(Tables.PARTNER, "p")
                    .on("d.PartnerId = p.PartnerId")
                    .where("d.databaseid").equalTo(databaseId)
                    .orderBy("p.name");

            return execute(query, new SqlResultCallback() {

                @Override
                public void onSuccess(SqlTransaction tx, SqlResultSet results) {
                    List<PartnerDTO> partners = Lists.newArrayList();

                    for(SqlResultSetRow row : results.getRows()) {
                        PartnerDTO partner = new PartnerDTO();
                        partner.setId(row.getInt("partnerId"));
                        partner.setName(row.getString("name"));
                        partner.setFullName(row.getString("fullName"));
                        partners.add(partner);
                    }
                    activity.setPartnerRange(partners);
                }
            });
        }

        public Promise<Void> loadIndicators() {
            SqlQuery query = SqlQuery.select("indicatorId",
                    "name",
                    "type",
                    "expression",
                    "skipExpression",
                    "nameInExpression",
                    "calculatedAutomatically",
                    "category",
                    "listHeader",
                    "description",
                    "aggregation",
                    "units",
                    "activityId",
                    "sortOrder",
                    "mandatory")
                    .from("indicator")
                    .where("activityId").equalTo(activity.getId())
                    .whereTrue("dateDeleted is null")
                    .orderBy("SortOrder");

            return execute(query, new RowHandler() {

                @Override
                public void handleRow(SqlResultSetRow rs) {
                    IndicatorDTO indicator = new IndicatorDTO();
                    indicator.setId(rs.getInt("indicatorId"));
                    indicator.setName(rs.getString("name"));
                    indicator.setTypeId(rs.getString("type"));
                    indicator.setExpression(rs.getString("expression"));
                    indicator.setSkipExpression(rs.getString("skipExpression"));
                    indicator.setNameInExpression(rs.getString("nameInExpression"));
                    indicator.setCalculatedAutomatically(rs.getBoolean("calculatedAutomatically"));
                    indicator.setCategory(rs.getString("category"));
                    indicator.setListHeader(rs.getString("listHeader"));
                    indicator.setDescription(rs.getString("description"));
                    indicator.setAggregation(rs.getInt("aggregation"));
                    indicator.setUnits(rs.getString("units"));
                    indicator.setMandatory(rs.getBoolean("mandatory"));
                    indicator.setSortOrder(rs.getInt("sortOrder"));


                    activity.getIndicators().add(indicator);
                }
            });
        }

        public Promise<Void> loadAttributeGroups() {

            SqlQuery query = SqlQuery.select()
                    .appendColumn("AttributeGroupId", "id")
                    .appendColumn("Name", "name")
                    .appendColumn("multipleAllowed")
                    .appendColumn("mandatory")
                    .appendColumn("defaultValue")
                    .appendColumn("workflow")
                    .appendColumn("sortOrder")
                    .from("attributegroup")
                    .where("attributeGroupId").in(attributeGroups())
                    .whereTrue("dateDeleted is NULL")
                    .orderBy("SortOrder");

            return execute(query, new RowHandler() {

                @Override
                public void handleRow(SqlResultSetRow rs) {

                    AttributeGroupDTO group = new AttributeGroupDTO();
                    group.setId(rs.getInt("id"));
                    group.setName(rs.getString("name"));
                    group.setMultipleAllowed(rs.getBoolean("multipleAllowed"));
                    group.setMandatory(rs.getBoolean("mandatory"));
                    group.setSortOrder(rs.getInt("sortOrder"));
                    if (!rs.isNull("defaultValue")) { // if null it throws NPE
                        group.setDefaultValue(rs.getInt("defaultValue"));
                    }
                    group.setWorkflow(rs.getBoolean("workflow"));
                    attributeGroups.put(group.getId(), group);

                    activity.getAttributeGroups().add(group);
                }
            });
        }


        public Promise<Void> loadAttributes() {
            SqlQuery query = SqlQuery.select("attributeId", "name", "attributeGroupId")
                    .from("attribute")
                    .orderBy("SortOrder")
                    .where("attributeGroupId").in(attributeGroups())
                    .whereTrue("dateDeleted is null");

            return execute(query, new RowHandler() {

                @Override
                public void handleRow(SqlResultSetRow row) {

                    AttributeDTO attribute = new AttributeDTO();
                    attribute.setId(row.getInt("attributeId"));
                    attribute.setName(row.getString("name"));

                    int groupId = row.getInt("attributeGroupId");
                    AttributeGroupDTO group = attributeGroups.get(groupId);
                    if (group != null) {
                        group.getAttributes().add(attribute);
                    }
                }
            });
        }

        private SqlQuery attributeGroups() {
            return SqlQuery.select("AttributeGroupId")
                    .from("attributegroupinactivity")
                    .where("ActivityId").equalTo(activity.getId());
        }


        private Promise<Void> execute(SqlQuery query, final SqlResultCallback rowHandler) {
            final Promise<Void> promise = new Promise<>();
            query.execute(tx, new SqlResultCallback() {
                @Override
                public void onSuccess(SqlTransaction tx, SqlResultSet results) {
                    rowHandler.onSuccess(tx, results);
                    promise.resolve(null);
                }
            });
            return promise;
        }

    }
}
