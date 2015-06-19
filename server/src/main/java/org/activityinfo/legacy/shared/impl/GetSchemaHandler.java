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
import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.activityinfo.legacy.shared.Log;
import org.activityinfo.legacy.shared.command.GetSchema;
import org.activityinfo.legacy.shared.model.*;
import org.activityinfo.legacy.shared.reports.util.mapping.Extents;
import org.activityinfo.promise.Promise;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetSchemaHandler implements CommandHandlerAsync<GetSchema, SchemaDTO> {

    @Override
    public void execute(GetSchema command, ExecutionContext context, AsyncCallback<SchemaDTO> callback) {

        new SchemaBuilder().build(context, callback);
    }

    private class SchemaBuilder {
        private final List<UserDatabaseDTO> databaseList = new ArrayList<UserDatabaseDTO>();
        private final List<CountryDTO> countryList = new ArrayList<CountryDTO>();

        private final Map<Integer, UserDatabaseDTO> databaseMap = new HashMap<Integer, UserDatabaseDTO>();

        private final Map<Integer, CountryDTO> countries = new HashMap<Integer, CountryDTO>();
        private final Map<Integer, PartnerDTO> partners = new HashMap<Integer, PartnerDTO>();
        private final Map<Integer, ActivityDTO> activities = new HashMap<>();
        private final Map<Integer, AttributeGroupDTO> attributeGroups = new HashMap<Integer, AttributeGroupDTO>();
        private final Map<Integer, ProjectDTO> projects = new HashMap<Integer, ProjectDTO>();
        private final Map<Integer, LocationTypeDTO> locationTypes = new HashMap<>();

        private SqlTransaction tx;
        private ExecutionContext context;

        public Promise<Void> loadCountries() {
            return execute(SqlQuery.select()
                    .appendColumn("CountryId", "id")
                    .appendColumn("Name", "name")
                    .appendColumn("X1", "x1")
                    .appendColumn("y1", "y1")
                    .appendColumn("x2", "x2")
                    .appendColumn("y2", "y2")
                    .from("country"),
                    new RowHandler() {
                        @Override
                        public void handleRow(SqlResultSetRow rs) {
                            CountryDTO country = new CountryDTO();
                            country.setId(rs.getInt("id"));
                            country.setName(rs.getString("name"));

                            Extents bounds = new Extents(rs.getDouble("y1"),
                                    rs.getDouble("y2"),
                                    rs.getDouble("x1"),
                                    rs.getDouble("x2"));

                            country.setBounds(bounds);

                            countries.put(country.getId(), country);
                            countryList.add(country);
                        }
                    });
        }

        public Promise<Void> loadLocationTypes() {
            SqlQuery query = SqlQuery.select("locationTypeId",
                    "name",
                    "boundAdminLevelId",
                    "DateDeleted",
                    "countryId",
                    "workflowId",
                    "databaseId").from("locationtype");

            return execute(query, new RowHandler() {

                @Override
                public void handleRow(SqlResultSetRow row) {
                    LocationTypeDTO type = new LocationTypeDTO();
                    type.setId(row.getInt("locationTypeId"));
                    type.setName(row.getString("name"));
                    type.setWorkflowId(row.getString("workflowId"));

                    if (!row.isNull("databaseId")) {
                        type.setDatabaseId(row.getInt("databaseId"));
                    }

                    if (!row.isNull("boundAdminLevelId")) {
                        type.setBoundAdminLevelId(row.getInt("boundAdminLevelId"));
                    }

                    type.setDeleted(!row.isNull("DateDeleted"));

                    int countryId = row.getInt("countryId");
                    CountryDTO country = countries.get(countryId);
                    country.getLocationTypes().add(type);

                    type.setAdminLevels(levelsForLocationType(country, type));
                    type.setCountryBounds(country.getBounds());

                    locationTypes.put(type.getId(), type);
                }
            });
        }

        private List<AdminLevelDTO> levelsForLocationType(CountryDTO country, LocationTypeDTO type) {

            if (type.isAdminLevel()) {
                // if this activity is bound to an administrative
                // level, then we need only as far down as this goes
                return country.getAdminLevelAncestors(type.getBoundAdminLevelId());

            } else if (type.isNationwide()) {
                return Lists.newArrayList();

            } else {
                // all admin levels
                return country.getAdminLevels();
            }
        }

        public Promise<Void> loadAdminLevels() {
            return execute(SqlQuery.select("adminLevelId", "name", "parentId", "countryId")
                    .from("adminlevel")
                    .whereTrue("deleted=0"),
                    new RowHandler() {

                        @Override
                        public void handleRow(SqlResultSetRow row) {
                            AdminLevelDTO level = new AdminLevelDTO();
                            level.setId(row.getInt("adminLevelId"));
                            level.setName(row.getString("name"));
                            level.setCountryId(row.getInt("countryId"));

                            if (!row.isNull("parentId")) {
                                level.setParentLevelId(row.getInt("parentId"));
                            }

                            countries.get(level.getCountryId()).getAdminLevels().add(level);
                        }
                    });
        }

        public Promise<Void> loadDatabases() {
            final Promise<Void> promise = new Promise<>();
            SqlQuery query = SqlQuery.select("d.DatabaseId")
                                     .appendColumn("d.Name")
                                     .appendColumn("d.FullName")
                                     .appendColumn("d.OwnerUserId")
                                     .appendColumn("d.CountryId")
                                     .appendColumn("o.Name", "OwnerName")
                                     .appendColumn("o.Email", "OwnerEmail")
                                     .appendColumn("p.AllowViewAll", "allowViewAll")
                                     .appendColumn("p.AllowEdit", "allowEdit")
                                     .appendColumn("p.AllowEditAll", "allowEditAll")
                                     .appendColumn("p.AllowManageUsers", "allowManageUsers")
                                     .appendColumn("p.AllowManageAllUsers", "allowManageAllUsers")
                                     .appendColumn("p.AllowDesign", "allowDesign")
                                     .appendColumn("p.PartnerId", "partnerId")
                                     .from("userdatabase d")
                                     .leftJoin(SqlQuery.selectAll()
                                                       .from("userpermission")
                                                       .where("userpermission.UserId")
                                                       .equalTo(context.getUser().getId()), "p")
                                     .on("p.DatabaseId = d.DatabaseId")
                                     .leftJoin("userlogin o")
                                     .on("d.OwnerUserId = o.UserId")
                                     .where("d.DateDeleted")
                                     .isNull()
                                     .orderBy("d.Name");

            // this is quite hackesh. we ultimately need to split up GetSchema()
            // into
            // GetDatabases() and GetDatabaseSchema() so that the client has
            // more fine-grained
            // control over which databases are visible, which will be important
            // as the number
            // of public databases grow
            if (context.getUser().isAnonymous()) {
                query.whereTrue("(d.DatabaseId in (select pa.DatabaseId from activity pa where pa.published>0))");
            } else {
                query.whereTrue("(o.userId = ? or p.AllowView = 1)").appendParameter(context.getUser().getId());
            }

            query.execute(tx, new SqlResultCallback() {

                @Override
                public void onSuccess(SqlTransaction tx, SqlResultSet results) {

                    for (SqlResultSetRow row : results.getRows()) {
                        UserDatabaseDTO db = new UserDatabaseDTO();
                        db.setId(row.getInt("DatabaseId"));
                        db.setName(row.getString("Name"));
                        db.setFullName(row.getString("FullName"));
                        db.setAmOwner(row.getInt("OwnerUserId") == context.getUser().getId());
                        db.setCountry(countries.get(row.getInt("CountryId")));
                        db.setOwnerName(row.getString("OwnerName"));
                        db.setOwnerEmail(row.getString("OwnerEmail"));

                        if (db.getAmOwner()) {
                            db.setViewAllAllowed(true);
                            db.setEditAllowed(true);
                            db.setEditAllAllowed(true);
                            db.setManageUsersAllowed(true);
                            db.setManageAllUsersAllowed(true);
                            db.setDesignAllowed(true);

                        } else if (row.isNull("allowViewAll")) {

                            // when other users see public databases
                            // they will not have a UserPermission record
                            db.setViewAllAllowed(true);
                            db.setEditAllowed(false);
                            db.setEditAllAllowed(false);
                            db.setManageUsersAllowed(false);
                            db.setManageAllUsersAllowed(false);
                            db.setDesignAllowed(false);

                        } else {
                            db.setViewAllAllowed(row.getBoolean("allowViewAll"));
                            db.setEditAllowed(row.getBoolean("allowEdit"));
                            db.setEditAllAllowed(row.getBoolean("allowEditAll"));
                            db.setManageUsersAllowed(row.getBoolean("allowManageUsers"));
                            db.setManageAllUsersAllowed(row.getBoolean("allowManageAllUsers"));
                            db.setDesignAllowed(row.getBoolean("allowDesign"));
                            db.setMyPartnerId(row.getInt("partnerId"));
                        }

                        // todo fix query !!! sometimes it returns duplicates
                        if (!databaseMap.containsKey(db.getId())) {
                            databaseMap.put(db.getId(), db);
                            databaseList.add(db);
                        } else {
                            continue;
                        }
                    }


                    if (databaseMap.isEmpty()) {
                        promise.resolve(null);
                    } else {
                        Promise.waitAll(
                                joinPartnersToDatabases(),
                                loadProjects(),
                                loadActivities(),
//                                loadIndicators(),
//                                loadAttributeGroups(),
//                                loadAttributes(),
//                                joinAttributesToActivities(),
                                loadLockedPeriods())
                               .then(promise);
                    }
                }
            });
            return promise;
        }

        protected Promise<Void> loadProjects() {
            final Promise<Void> promise = new Promise<>();
            SqlQuery.select("name", "projectId", "description", "databaseId")
                    .from("project")
                    .where("databaseId").in(databaseMap.keySet())
                    .where("dateDeleted").isNull()
                    .execute(tx, new SqlResultCallback() {
                        @Override
                        public void onSuccess(SqlTransaction tx, SqlResultSet results) {
                            for (SqlResultSetRow row : results.getRows()) {
                                ProjectDTO project = new ProjectDTO();
                                project.setName(row.getString("name"));
                                project.setId(row.getInt("projectId"));
                                project.setDescription(row.getString("description"));

                                int databaseId = row.getInt("databaseId");
                                UserDatabaseDTO database = databaseMap.get(databaseId);
                                database.getProjects().add(project);
                                project.setUserDatabase(database);
                                projects.put(project.getId(), project);
                            }
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
                    "projectId").from("lockedperiod")

                    .execute(tx, new SqlResultCallback() {

                        @Override
                        public void onSuccess(SqlTransaction tx, SqlResultSet results) {
                            for (SqlResultSetRow row : results.getRows()) {
                                LockedPeriodDTO lockedPeriod = new LockedPeriodDTO();

                                lockedPeriod.setFromDate(row.getDate("fromDate"));
                                lockedPeriod.setToDate(row.getDate("toDate"));
                                lockedPeriod.setEnabled(row.getBoolean("enabled"));
                                lockedPeriod.setName(row.getString("name"));
                                lockedPeriod.setId(row.getInt("lockedPeriodId"));

                                boolean parentFound = false;

                                if (!row.isNull("activityId")) {
                                    Integer activityId = row.getInt("activityId");
                                    ActivityDTO activity = activities.get(activityId);
                                    if (activity != null) { // activities can be
                                        // deleted...
                                        activity.getLockedPeriods().add(lockedPeriod);
                                        lockedPeriod.setParent(activity);
                                    }
                                    parentFound = true;
                                }
                                if (!row.isNull("userDatabaseId")) {
                                    Integer databaseId = row.getInt("userDatabaseId");
                                    UserDatabaseDTO database = databaseMap.get(databaseId);
                                    if (database != null) { // databases can be
                                        // deleted
                                        database.getLockedPeriods().add(lockedPeriod);
                                        lockedPeriod.setParent(database);
                                    }
                                    parentFound = true;
                                }
                                if (!row.isNull("projectId")) {
                                    Integer projectId = row.getInt("projectId");
                                    ProjectDTO project = projects.get(projectId);
                                    if (project != null) {
                                        project.getLockedPeriods().add(lockedPeriod);
                                        lockedPeriod.setParent(project);
                                        parentFound = true;
                                    }
                                }

                                if (!parentFound) {
                                    Log.debug(
                                            "Orphan lockedPeriod: No parent (UserDatabase/Activity/Project) found for" +
                                            " LockedPeriod with Id=" +
                                            lockedPeriod.getId());
                                }
                            }
                            promise.resolve(null);
                        }
                    });
            return promise;
        }

        private Promise<Void> joinPartnersToDatabases() {
            SqlQuery query = SqlQuery.select("d.databaseId", "d.partnerId", "p.name", "p.fullName")
                                     .from(Tables.PARTNER_IN_DATABASE, "d")
                                     .leftJoin(Tables.PARTNER, "p")
                                     .on("d.PartnerId = p.PartnerId")
                                     .orderBy("p.name");

            // Only allow results that are visible to this user if we are on the
            // server,
            // otherwise permissions have already been taken into account during
            // synchronization
            if (context.isRemote()) {
                query.where("d.databaseId").in(databaseMap.keySet());
            }

            return execute(query, new RowHandler() {

                @Override
                public void handleRow(SqlResultSetRow row) {

                    int partnerId = row.getInt("partnerId");
                    PartnerDTO partner = partners.get(partnerId);
                    if (partner == null) {
                        partner = new PartnerDTO();
                        partner.setId(partnerId);
                        partner.setName(row.getString("name"));
                        partner.setFullName(row.getString("fullName"));
                        partners.put(partnerId, partner);
                    }

                    UserDatabaseDTO db = databaseMap.get(row.getInt("databaseId"));
                    if (db != null) { // databases can be deleted
                        db.getPartners().add(partner);
                    }
                }
            });
        }

        public Promise<Void> loadActivities() {
            SqlQuery query = SqlQuery.select("activityId",
                    "name",
                    "category",
                    "locationTypeId",
                    "reportingFrequency",
                    "databaseId",
                    "classicView",
                    "published").from("activity").orderBy("SortOrder");

            if (context.isRemote()) {
                query.where("DateDeleted IS NULL");
                query.where("DatabaseId").in(databaseMap.keySet());
            }

            return execute(query, new RowHandler() {

                @Override
                public void handleRow(SqlResultSetRow row) {
                    ActivityDTO activity = new ActivityDTO();
                    activity.setId(row.getInt("activityId"));
                    activity.setName(row.getString("name"));
                    activity.setCategory(row.getString("category"));
                    activity.setReportingFrequency(row.getInt("reportingFrequency"));
                    activity.setPublished(row.getInt("published"));
                    activity.setClassicView(row.getBoolean("classicView"));

                    int databaseId = row.getInt("databaseId");
                    UserDatabaseDTO database = databaseMap.get(databaseId);
                    activity.setDatabase(database);
                    database.getActivities().add(activity);

                    int locationTypeId = row.getInt("locationTypeId");
                    LocationTypeDTO locationType = locationTypes.get(locationTypeId);

                    if(locationType == null) {
                        throw new IllegalStateException("No location type for " + locationTypeId);
                    }
                    activity.setLocationType(locationType);
                    activity.set("locationTypeId", locationType.getId());

                    activities.put(activity.getId(), activity);
                }
            });
        }
//
//        public Promise<Void> loadIndicators() {
//            SqlQuery query = SqlQuery.select("indicatorId",
//                    "name",
//                    "type",
//                    "expression",
//                    "skipExpression",
//                    "nameInExpression",
//                    "calculatedAutomatically",
//                    "category",
//                    "listHeader",
//                    "description",
//                    "aggregation",
//                    "units",
//                    "activityId",
//                    "sortOrder",
//                    "mandatory").from("indicator").orderBy("SortOrder");
//
//            if (context.isRemote()) {
//                query.where("DateDeleted IS NULL");
//                query.where("activityId")
//                     .in(SqlQuery.select("ActivityId").from("activity").where("databaseId").in(databaseMap.keySet()));
//
//            }
//
//            return execute(query, new RowHandler() {
//
//                @Override
//                public void handleRow(SqlResultSetRow rs) {
//                    IndicatorDTO indicator = new IndicatorDTO();
//                    indicator.setId(rs.getInt("indicatorId"));
//                    indicator.setName(rs.getString("name"));
//                    indicator.setTypeId(rs.getString("type"));
//                    indicator.setExpression(rs.getString("expression"));
//                    indicator.setSkipExpression(rs.getString("skipExpression"));
//                    indicator.setNameInExpression(rs.getString("nameInExpression"));
//                    indicator.setCalculatedAutomatically(rs.getBoolean("calculatedAutomatically"));
//                    indicator.setCategory(rs.getString("category"));
//                    indicator.setListHeader(rs.getString("listHeader"));
//                    indicator.setDescription(rs.getString("description"));
//                    indicator.setAggregation(rs.getInt("aggregation"));
//                    indicator.setUnits(rs.getString("units"));
//                    indicator.setMandatory(rs.getBoolean("mandatory"));
//                    indicator.setSortOrder(rs.getInt("sortOrder"));
//
//                    int activityId = rs.getInt("activityId");
//                    ActivityFormDTO activity = activities.get(activityId);
//                    if (activity != null) { // it may have been deleted
//                        activity.getIndicators().add(indicator);
//                    }
//                }
//            });
//        }
//
//        public Promise<Void> loadAttributeGroups() {
//            SqlQuery query = SqlQuery.select()
//                                     .appendColumn("AttributeGroupId", "id")
//                                     .appendColumn("Name", "name")
//                                     .appendColumn("multipleAllowed")
//                                     .appendColumn("mandatory")
//                                     .appendColumn("defaultValue")
//                                     .appendColumn("workflow")
//                                     .appendColumn("sortOrder")
//                                     .from("attributegroup")
//                                     .orderBy("SortOrder");
//
//            if (context.isRemote()) {
//                query.where("DateDeleted IS NULL");
//                query.where("AttributeGroupId")
//                     .in(SqlQuery.select("AttributeGroupId")
//                                 .from("attributegroupinactivity")
//                                 .where("ActivityId")
//                                 .in(SqlQuery.select("ActivityId")
//                                             .from("activity")
//                                             .where("databaseId")
//                                             .in(databaseMap.keySet())));
//
//            }
//
//            return execute(query, new RowHandler() {
//
//                @Override
//                public void handleRow(SqlResultSetRow rs) {
//
//                    AttributeGroupDTO group = new AttributeGroupDTO();
//                    group.setId(rs.getInt("id"));
//                    group.setName(rs.getString("name"));
//                    group.setMultipleAllowed(rs.getBoolean("multipleAllowed"));
//                    group.setMandatory(rs.getBoolean("mandatory"));
//                    group.setSortOrder(rs.getInt("sortOrder"));
//                    if (!rs.isNull("defaultValue")) { // if null it throws NPE
//                        group.setDefaultValue(rs.getInt("defaultValue"));
//                    }
//                    group.setWorkflow(rs.getBoolean("workflow"));
//
//                    attributeGroups.put(group.getId(), group);
//                }
//            });
//        }
//
//        public Promise<Void> loadAttributes() {
//            SqlQuery query = SqlQuery.select("attributeId", "name", "attributeGroupId")
//                                     .from("attribute")
//                                     .orderBy("SortOrder");
//
//            if (context.isRemote()) {
//                query.where("DateDeleted IS NULL");
//                query.where("AttributeGroupId")
//                     .in(SqlQuery.select("AttributeGroupId")
//                                 .from("attributegroupinactivity")
//                                 .where("ActivityId")
//                                 .in(SqlQuery.select("ActivityId")
//                                             .from("activity")
//                                             .where("databaseId")
//                                             .in(databaseMap.keySet())));
//
//            }
//
//            return execute(query, new RowHandler() {
//
//                @Override
//                public void handleRow(SqlResultSetRow row) {
//
//                    AttributeDTO attribute = new AttributeDTO();
//                    attribute.setId(row.getInt("attributeId"));
//                    attribute.setName(row.getString("name"));
//
//                    int groupId = row.getInt("attributeGroupId");
//                    AttributeGroupDTO group = attributeGroups.get(groupId);
//                    if (group != null) {
//                        group.getAttributes().add(attribute);
//                    }
//                }
//            });
//        }
//
//        public Promise<Void> joinAttributesToActivities() {
//            SqlQuery query = SqlQuery.select("J.activityId", "J.attributeGroupId")
//                                     .from("attributegroupinactivity J " +
//                                           "INNER JOIN attributegroup G ON (J.attributeGroupId = G.attributeGroupId)")
//                                     .orderBy("G.SortOrder")
//                                     .where("G.dateDeleted")
//                                     .isNull();
//
//            if (context.isRemote()) {
//                query.where("ActivityId")
//                     .in(SqlQuery.select("ActivityId").from("activity").where("databaseId").in(databaseMap.keySet()));
//
//            }
//
//            return execute(query, new RowHandler() {
//                @Override
//                public void handleRow(SqlResultSetRow row) {
//
//                    int groupId = row.getInt("attributeGroupId");
//
//                    ActivityFormDTO activity = activities.get(row.getInt("activityId"));
//                    if (activity != null) { // it may have been deleted
//                        activity.getAttributeGroups().add(attributeGroups.get(groupId));
//                    }
//
//                }
//            });
//        }

        private Promise<Void> execute(SqlQuery query, final RowHandler rowHandler) {
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

        public void build(ExecutionContext context, final AsyncCallback<SchemaDTO> callback) {
            this.context = context;
            this.tx = context.getTransaction();

            List<Promise<Void>> tasks = Lists.newArrayList();

            tasks.add(loadCountries());
            tasks.add(loadAdminLevels());
            tasks.add(loadLocationTypes());

            tasks.add(loadDatabases());

            SchemaDTO schemaDTO = new SchemaDTO();
            schemaDTO.setCountries(countryList);
            schemaDTO.setDatabases(databaseList);

            Promise.waitAll(tasks)
                   .then(Functions.constant(schemaDTO))
                   .then(callback);
        }
    }
}
