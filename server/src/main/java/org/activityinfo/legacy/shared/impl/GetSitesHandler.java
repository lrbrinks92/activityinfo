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
import com.bedatadriven.rebar.sql.client.query.SqlDialect;
import com.bedatadriven.rebar.sql.client.query.SqlQuery;
import com.bedatadriven.rebar.time.calendar.LocalDate;
import com.extjs.gxt.ui.client.Style.SortDir;
import com.extjs.gxt.ui.client.data.SortInfo;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.*;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import org.activityinfo.legacy.shared.Log;
import org.activityinfo.legacy.shared.command.DimensionType;
import org.activityinfo.legacy.shared.command.Filter;
import org.activityinfo.legacy.shared.command.GetSites;
import org.activityinfo.legacy.shared.command.result.SiteResult;
import org.activityinfo.legacy.shared.model.*;
import org.activityinfo.model.expr.eval.FieldReader;
import org.activityinfo.model.expr.eval.FormSymbolTable;
import org.activityinfo.model.expr.eval.PartialEvaluator;
import org.activityinfo.model.form.FormField;
import org.activityinfo.model.form.FormFieldType;
import org.activityinfo.model.legacy.CuidAdapter;
import org.activityinfo.model.type.FieldTypeClass;
import org.activityinfo.model.type.FieldValue;
import org.activityinfo.model.type.expr.CalculatedFieldType;
import org.activityinfo.model.type.number.Quantity;
import org.activityinfo.model.type.number.QuantityType;
import org.activityinfo.promise.Promise;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GetSitesHandler implements CommandHandlerAsync<GetSites, SiteResult> {

    private final SqlDialect dialect;


    @Inject
    public GetSitesHandler(SqlDialect dialect) {
        super();
        this.dialect = dialect;
    }

    @Override
    public void execute(final GetSites command,
                        final ExecutionContext context,
                        final AsyncCallback<SiteResult> callback) {

        Log.trace("Entering execute()");
        doQuery(command, context, callback);
    }

    private void doQuery(final GetSites command,
                         final ExecutionContext context,
                         final AsyncCallback<SiteResult> callback) {

        // in order to pull in the linked queries, we want to
        // to create two queries that we union together.

        // for performance reasons, we want to apply all of the joins
        // and filters on both parts of the union query

        SqlQuery unioned;
        if(command.isFetchLinks()) {
            unioned = unionedQuery(context, command);
            unioned.appendAllColumns();
        } else {
            unioned = primaryQuery(context, command);
        }

        if (isMySql() && command.getLimit() >= 0) {
            // with this feature, MySQL will keep track of the total
            // number of rows regardless of our limit statement.
            // This way we don't have to execute the query twice to
            // get the total count
            //
            // unfortunately, this is not available on sqlite
            unioned.appendKeyword("SQL_CALC_FOUND_ROWS");
        }

        applySort(unioned, command.getSortInfo());
        applyPaging(unioned, command);

        final Multimap<Integer, SiteDTO> siteMap = HashMultimap.create();
        final List<SiteDTO> sites = new ArrayList<SiteDTO>();

        final Map<Integer, SiteDTO> reportingPeriods = Maps.newHashMap();


        final SiteResult result = new SiteResult(sites);
        result.setOffset(command.getOffset());

        Log.trace("About to execute primary query: " + unioned.sql());

        unioned.execute(context.getTransaction(), new SqlResultCallback() {
            @Override
            public void onSuccess(SqlTransaction tx, SqlResultSet results) {

                Log.trace("Primary query returned " + results.getRows().size() + ", rows, starting to add to map");

                for (SqlResultSetRow row : results.getRows()) {
                    SiteDTO site = toSite(command, row);
                    sites.add(site);
                    siteMap.put(site.getId(), site);

                    if(command.isFetchAllReportingPeriods()) {
                        reportingPeriods.put(row.getInt("PeriodId"), site);
                    }
                }

                Log.trace("Finished adding to map");

                List<Promise<Void>> queries = Lists.newArrayList();

                if (command.getLimit() <= 0) {
                    result.setTotalLength(results.getRows().size());
                } else {
                    queries.add(queryTotalLength(tx, command, context, result));
                }


                if (!sites.isEmpty()) {
                    if (command.isFetchAdminEntities()) {
                        queries.add(joinEntities(tx, siteMap));
                    }
                    if (command.isFetchAttributes()) {
                        queries.add(joinAttributeValues(command, tx, siteMap));
                    }
                    if (command.fetchAnyIndicators()) {
                        queries.add(joinIndicatorValues(command, tx, siteMap, reportingPeriods));
                    }
                }
                Promise.waitAll(queries).then(Functions.constant(result)).then(callback);
            }
        });
    }

    private SqlQuery unionedQuery(ExecutionContext context, GetSites command) {

        SqlQuery primaryQuery = primaryQuery(context, command);
        SqlQuery linkedQuery = linkedQuery(context, command);

        SqlQuery unioned = SqlQuery.select().from(unionAll(primaryQuery, linkedQuery), "u");
        for (Object param : primaryQuery.parameters()) {
            unioned.appendParameter(param);
        }
        for (Object param : linkedQuery.parameters()) {
            unioned.appendParameter(param);
        }
        return unioned;
    }

    private String unionAll(SqlQuery primaryQuery, SqlQuery linkedQuery) {
        StringBuilder union = new StringBuilder();
        union.append("(").append(primaryQuery.sql()).append(" UNION ALL ").append(linkedQuery.sql()).append(")");
        return union.toString();
    }

    private SqlQuery primaryQuery(ExecutionContext context, GetSites command) {
        SqlQuery query = SqlQuery.select()
                .appendColumn("site.SiteId")
                .appendColumn("(0)", "Linked")
                .appendColumn("activity.ActivityId")
                .appendColumn("activity.name", "ActivityName")
                .appendColumn("db.DatabaseId", "DatabaseId")
                .appendColumn("site.DateCreated", "DateCreated")
                .appendColumn("site.projectId", "ProjectId")
                .appendColumn("project.name", "ProjectName")
                .appendColumn("project.dateDeleted", "ProjectDateDeleted")
                .appendColumn("site.comments", "Comments")
                .appendColumn("site.DateEdited")
                .appendColumn("site.timeEdited", "TimeEdited");


        if(command.isFetchAllReportingPeriods()) {
            query.appendColumn("period.Date1", "Date1")
                 .appendColumn("period.Date2", "Date2")
                 .appendColumn("period.ReportingPeriodId", "PeriodId");

            query.from(Tables.REPORTING_PERIOD, "period")
                .leftJoin(Tables.SITE, "site").on("site.SiteId=period.SiteId");


        } else {
            query.from(Tables.SITE);
            query.appendColumn("site.Date1", "Date1")
                 .appendColumn("site.Date2", "Date2");
        }

        query
            .whereTrue("site.dateDeleted is null")
            .leftJoin(Tables.ACTIVITY)
            .on("site.ActivityId = activity.ActivityId")
            .leftJoin(Tables.USER_DATABASE, "db")
            .on("activity.DatabaseId = db.DatabaseId")
            .leftJoin(Tables.PARTNER)
            .on("site.PartnerId = partner.PartnerId")
            .leftJoin(Tables.PROJECT)
            .on("site.ProjectId = project.ProjectId");


        if(command.isFetchPartner()) {
            query.appendColumn("partner.PartnerId", "PartnerId")
                 .appendColumn("partner.name", "PartnerName");
        }

        if(command.isFetchLocation()) {
            query.appendColumn("location.locationId", "LocationId")
                .appendColumn("location.name", "LocationName")
                .appendColumn("location.axe", "LocationAxe")
                .appendColumn("locationType.name", "LocationTypeName")
                .appendColumn("location.x", "x")
                .appendColumn("location.y", "y");

        }
        if(locationJoinRequired(command)) {
            query
                .leftJoin(Tables.LOCATION).on("site.LocationId = location.LocationId")
                .leftJoin(Tables.LOCATION_TYPE, "locationType").on("location.LocationTypeId = locationType.LocationTypeId");
        }

        applyPermissions(query, context);
        applyFilter(query, command.getFilter());

        Optional<Integer> adminLevelId = adminLevelId(command.getSortInfo().getSortField());
         if (command.isFetchAdminEntities() && adminLevelId.isPresent() ) {
             query.appendColumn("derived.name", "adminName");
             query.leftJoin(locationToAdminTable(adminLevelId.get()), "derived")
                    .on("derived.locationId = location.LocationId");
            }

        if (command.getFilter().isRestricted(DimensionType.Indicator)) {
            applyPrimaryIndicatorFilter(query, command.getFilter());
        }

        System.out.println(query.sql());

        return query;
    }

    private SqlQuery locationToAdminTable(int adminLevelId) {
        return SqlQuery
                .select("k.locationId", "e.name")
                .from(Tables.LOCATION_ADMIN_LINK, "k")
                .leftJoin(Tables.ADMIN_ENTITY, "e")
                .on("k.adminentityid = e.adminentityid")
                .whereTrue("e.adminlevelid = " + adminLevelId);
    }

    private boolean locationJoinRequired(GetSites command) {
        return command.isFetchLocation() || command.getFilter().isRestricted(DimensionType.Location);
    }

    private SqlQuery linkedQuery(ExecutionContext context, GetSites command) {
        SqlQuery query = SqlQuery.select()
                .appendColumn("DISTINCT site.SiteId", "SiteId")
                .appendColumn("1", "Linked")
                .appendColumn("activity.ActivityId")
                .appendColumn("activity.name", "ActivityName")
                .appendColumn("db.DatabaseId", "DatabaseId")
                .appendColumn("site.DateCreated", "DateCreated")
                .appendColumn("site.projectId", "ProjectId")
                .appendColumn("project.name", "ProjectName")
                .appendColumn("project.dateDeleted", "ProjectDateDeleted")
                .appendColumn("site.comments", "Comments")
                .appendColumn("site.DateEdited")
                .appendColumn("site.timeEdited", "TimeEdited")
                .appendColumn("site.Date1", "Date1")
                .appendColumn("site.Date2", "Date2");

        if (command.isFetchPartner()) {
            query
            .appendColumn("partner.PartnerId", "PartnerId")
            .appendColumn("partner.name", "PartnerName");
        }

        if (command.isFetchLocation()) {
            query
            .appendColumn("location.locationId", "LocationId")
            .appendColumn("location.name", "LocationName")
            .appendColumn("location.axe", "LocationAxe")
            .appendColumn("locationType.name", "LocationTypeName")
            .appendColumn("location.x", "x")
            .appendColumn("location.y", "y");
        }

        if (command.getFilter().isRestricted(DimensionType.Indicator)) {
            /*
             * When filtering by indicators, restructure the query to fetch the
             * results more efficiently
             */
            query.from(Tables.INDICATOR_LINK, "link")
                    .innerJoin(Tables.INDICATOR_VALUE, "siv")
                    .on("link.SourceIndicatorId = siv.IndicatorId")
                    .innerJoin(Tables.REPORTING_PERIOD, "srp")
                    .on("siv.ReportingPeriodId = srp.ReportingPeriodId")
                    .innerJoin(Tables.SITE, "site")
                    .on("srp.SiteId=site.SiteId")
                    .innerJoin(Tables.INDICATOR, "di")
                    .on("link.DestinationIndicatorId=di.IndicatorId")
                    .innerJoin(Tables.ACTIVITY, "activity")
                    .on("di.ActivityId=activity.ActivityId")
                    .where("link.DestinationIndicatorId")
                    .in(command.getFilter().getRestrictions(DimensionType.Indicator));
        } else {
            query.from(Tables.SITE)
                    .innerJoin(Tables.INDICATOR, "si")
                    .on("si.activityid=site.activityid")
                    .innerJoin(Tables.INDICATOR_LINK, "link")
                    .on("si.indicatorId=link.sourceindicatorid")
                    .innerJoin(Tables.INDICATOR, "di")
                    .on("link.destinationIndicatorId=di.indicatorid")
                    .leftJoin(Tables.ACTIVITY)
                    .on("di.ActivityId = activity.ActivityId");
        }
        query.leftJoin(Tables.USER_DATABASE, "db")
                .on("activity.DatabaseId = db.DatabaseId")
                .leftJoin(Tables.PARTNER)
                .on("site.PartnerId = partner.PartnerId")
                .leftJoin(Tables.PROJECT)
                .on("site.ProjectId = project.ProjectId")
                .whereTrue("site.dateDeleted is null");

        if(locationJoinRequired(command)) {
            query
            .leftJoin(Tables.LOCATION)
                .on("site.LocationId = location.LocationId")
                .leftJoin(Tables.LOCATION_TYPE, "locationType")
                .on("location.LocationTypeId = locationType.LocationTypeId");
        }

        Optional<Integer> adminLevelId = adminLevelId(command.getSortInfo().getSortField());
        if (command.isFetchAdminEntities() && adminLevelId.isPresent()) {
            query.appendColumn("derived.name", "adminName");
            query.leftJoin(locationToAdminTable(adminLevelId.get()), "derived")
                    .on("derived.locationId = location.LocationId");
        }


        applyPermissions(query, context);
        applyFilter(query, command.getFilter());

        return query;
    }

    private void applyPaging(final SqlQuery query, GetSites command) {
        if (command.getOffset() > 0 || command.getLimit() > 0) {
            query.setLimitClause(dialect.limitClause(command.getOffset(), command.getLimit()));
        }
    }

    private void applyPermissions(final SqlQuery query, ExecutionContext context) {
        // Apply permissions if we are on the server, otherwise permissions have
        // already been taken into account during synchronization

        if (context.isRemote()) {
            query.whereTrue("activity.DateDeleted IS NULL").and("db.DateDeleted IS NULL");
            query.whereTrue("(db.OwnerUserId = ? OR " +
                            "db.DatabaseId in " +
                            "(SELECT p.DatabaseId from userpermission p where p.UserId = ? and p.AllowViewAll) or " +
                            "db.DatabaseId in " +
                            "(select p.DatabaseId from userpermission p where (p.UserId = ?) and p.AllowView and p" +
                            ".PartnerId = site.PartnerId) " +
                            " OR (select count(*) from activity pa where pa.published>0 and pa.ActivityId = site" +
                            ".ActivityId) > 0 )");

            query.appendParameter(context.getUser().getId());
            query.appendParameter(context.getUser().getId());
            query.appendParameter(context.getUser().getId());
        }
    }

    private void applySort(SqlQuery query, SortInfo sortInfo) {
        if (sortInfo.getSortDir() != SortDir.NONE) {
            String field = sortInfo.getSortField();
            boolean ascending = sortInfo.getSortDir() == SortDir.ASC;

            if (field.equals("date1")) {
                query.orderBy("Date1", ascending);
            } else if (field.equals("date2")) {
                query.orderBy("Date2", ascending);
            } else if (field.equals("locationName")) {
                query.orderBy("LocationName", ascending);
            } else if (field.equals("partner")) {
                query.orderBy("PartnerName", ascending);
            } else if (field.equals("locationAxe")) {
                query.orderBy("LocationAxe", ascending);
            } else if (field.startsWith(IndicatorDTO.PROPERTY_PREFIX)) {
                int indicatorId = IndicatorDTO.indicatorIdForPropertyName(field);
                query.orderBy(SqlQuery.selectSingle("SUM(v.Value)")
                        .from(Tables.INDICATOR_VALUE, "v")
                        .leftJoin(Tables.REPORTING_PERIOD, "r")
                        .on("v.ReportingPeriodId=r.ReportingPeriodId")
                        .whereTrue("v.IndicatorId=" + indicatorId)
                        .and("r.SiteId=u.SiteId"), ascending);
            } else if (adminLevelId(field).isPresent()) {
                query.orderBy("adminName", ascending);
            }  else if (field.equals("DateEdited")) {
                query.orderBy("DateEdited", ascending);
            } else {
                Log.error("Unimplemented sort on GetSites: '" + field + "");
            }
        }
    }

    public static Optional<Integer> adminLevelId(String sortField) {
        if (sortField != null && sortField.startsWith("E") && sortField.length() > 1) {
            return Optional.of(Integer.parseInt(sortField.substring("E".length())));
        }
        return Optional.absent();
    }

    private void applyFilter(SqlQuery query, Filter filter) {
        if (filter != null) {
            if (filter.getRestrictedDimensions() != null && filter.getRestrictedDimensions().size() > 0) {
                query.onlyWhere(" AND (");

                boolean isFirst = true;
                boolean isRestricted = false;
                for (DimensionType type : filter.getRestrictedDimensions()) {
                    if (isQueryableType(type)) {
                        addJoint(query, filter.isLenient(), isFirst);
                        isRestricted = true;
                    }

                    if (type == DimensionType.Activity) {
                        query.onlyWhere("activity.ActivityId").in(filter.getRestrictions(type));

                    } else if (type == DimensionType.Database) {
                        query.onlyWhere("activity.DatabaseId").in(filter.getRestrictions(type));

                    } else if (type == DimensionType.Partner) {
                        query.onlyWhere("site.PartnerId").in(filter.getRestrictions(type));

                    } else if (type == DimensionType.Project) {
                        query.onlyWhere("site.ProjectId").in(filter.getRestrictions(type));

                    } else if (type == DimensionType.AdminLevel) {
                        query.onlyWhere("site.LocationId")
                                .in(SqlQuery.select("Link.LocationId")
                                        .from(Tables.LOCATION_ADMIN_LINK, "Link")
                                        .where("Link.AdminEntityId")
                                        .in(filter.getRestrictions(type)));

                    } else if (type == DimensionType.Site) {
                        query.onlyWhere("site.SiteId").in(filter.getRestrictions(type));

                    } else if (type == DimensionType.Attribute) {
                        Set<Integer> attributes = filter.getRestrictions(DimensionType.Attribute);
                        boolean isFirstAttr = true;
                        for (Integer attribute : attributes) {
                            SqlQuery attributefilter = SqlQuery.select()
                                    .appendColumn("1", "__VAL_EXISTS")
                                    .from("attributevalue", "av")
                                    .whereTrue("av.value=1")
                                    .and("av.SiteId = site.SiteId")
                                    .where("av.AttributeId")
                                    .equalTo(attribute);

                            addJoint(query, filter.isLenient(), isFirstAttr);
                            if (isFirstAttr) {
                                isFirstAttr = false;
                            }
                            query.onlyWhere("EXISTS (" + attributefilter.sql() + ") ");
                            query.appendParameter(attribute);
                        }

                    } else if (type == DimensionType.Location) {
                        query.onlyWhere("location.locationId").in(filter.getRestrictions(type));
                    }

                    if (isQueryableType(type) && isFirst) {
                        isFirst = false;
                    }
                }
                if (!isRestricted) {
                    query.onlyWhere(" 1=1 ");
                }
                query.onlyWhere(")");
            }

            LocalDate filterMinDate = filter.getDateRange().getMinLocalDate();
            if (filterMinDate != null) {
                query.where("site.Date2").greaterThanOrEqualTo(filterMinDate);
            }
            LocalDate filterMaxDate = filter.getDateRange().getMaxLocalDate();
            if (filterMaxDate != null) {
                query.where("site.Date2").lessThanOrEqualTo(filterMaxDate);
            }
        }
    }

    private boolean isQueryableType(DimensionType type) {
        return (type == DimensionType.Activity ||
                type == DimensionType.Database ||
                type == DimensionType.Partner ||
                type == DimensionType.Project ||
                type == DimensionType.AdminLevel ||
                type == DimensionType.Attribute ||
                type == DimensionType.Site ||
                type == DimensionType.Location);
    }

    private void addJoint(SqlQuery query, boolean lenient, boolean first) {
        if (!first) {
            if (lenient) {
                query.onlyWhere(" OR ");
            } else {
                query.onlyWhere(" AND ");
            }
        }
    }

    private void applyPrimaryIndicatorFilter(SqlQuery query, Filter filter) {
        SqlQuery subQuery = new SqlQuery().appendColumn("period.SiteId")
                .from(Tables.INDICATOR_VALUE, "iv")
                .leftJoin(Tables.REPORTING_PERIOD, "period")
                .on("iv.ReportingPeriodId=period.ReportingPeriodId")
                .where("iv.IndicatorId")
                .in(filter.getRestrictions(DimensionType.Indicator))
                .whereTrue("iv.Value IS NOT NULL");

        query.where("site.SiteId").in(subQuery);
    }

    private Promise<Void> queryTotalLength(SqlTransaction tx,
                                  GetSites command,
                                  ExecutionContext context,
                                  final SiteResult result) {

        final Promise<Void> promise = new Promise<>();
        if (isMySql()) {
            tx.executeSql("SELECT FOUND_ROWS() site_count", new SqlResultCallback() {

                @Override
                public void onSuccess(SqlTransaction tx, SqlResultSet results) {
                    result.setTotalLength(results.getRow(0).getInt("site_count"));
                    promise.resolve(null);
                }
            });
        } else {
            // otherwise we have to execute the whole thing again
            SqlQuery query = countQuery(command, context);
            query.execute(tx, new SqlResultCallback() {

                @Override
                public void onSuccess(SqlTransaction tx, SqlResultSet results) {
                    result.setTotalLength(results.getRow(0).getInt("site_count"));
                    promise.resolve(null);
                }
            });
        }
        return promise;
    }

    private SqlQuery countQuery(GetSites command, ExecutionContext context) {
        SqlQuery unioned = unionedQuery(context, command);
        unioned.appendColumn("count(*)", "site_count");
        return unioned;
    }

    private Promise<Void> joinEntities(SqlTransaction tx, final Multimap<Integer, SiteDTO> siteMap) {

        final Promise<Void> complete = new Promise<>();

        Log.trace("Starting joinEntities()");

        SqlQuery.select("site.SiteId",
                "Link.adminEntityId",
                "e.name",
                "e.adminLevelId",
                "e.adminEntityParentId",
                "x1",
                "y1",
                "x2",
                "y2")
                .from(Tables.SITE)
                .innerJoin(Tables.LOCATION)
                .on("location.LocationId = site.LocationId")
                .innerJoin(Tables.LOCATION_ADMIN_LINK, "Link")
                .on("Link.LocationId = location.LocationId")
                .innerJoin(Tables.ADMIN_ENTITY, "e")
                .on("Link.AdminEntityId = e.AdminEntityId")
                .where("site.SiteId")
                .in(siteMap.keySet())
                .execute(tx, new SqlResultCallback() {

                    @Override
                    public void onSuccess(SqlTransaction tx, SqlResultSet results) {

                        Log.trace("Received results for joinEntities()");

                        Map<Integer, AdminEntityDTO> entities = Maps.newHashMap();

                        for (SqlResultSetRow row : results.getRows()) {

                            int adminEntityId = row.getInt("adminEntityId");
                            AdminEntityDTO entity = entities.get(adminEntityId);
                            if (entity == null) {
                                entity = GetAdminEntitiesHandler.toEntity(row);
                                entities.put(adminEntityId, entity);
                            }

                            for (SiteDTO site : siteMap.get(row.getInt("SiteId"))) {
                                site.setAdminEntity(entity.getLevelId(), entity);
                            }
                        }

                        Log.trace("Done populating results for joinEntities");
                        complete.onSuccess(null);
                    }
                });
        return complete;
    }


    private boolean weAreFetchingAllSitesForAnActivityAndThereAreNoLinkedSites(
            GetSites command, Multimap<Integer,SiteDTO> siteMap) {

        // are we limiting the number of rows to return?
        if(command.getLimit() >= 0) {
            return false;
        }

        // are we filtering on a SINGLE dimension??
        Filter filter = command.getFilter();
        if( filter.getRestrictedDimensions().size() != 1 ) {
            return false;
        }

        // is that dimension the Activity dimension?
        if( !filter.getRestrictedDimensions().contains(DimensionType.Activity)) {
            return false;
        }

        // are there any linked sites?
        if(command.isFetchLinks()) {
            for (SiteDTO site : siteMap.values()) {
                if(site.isLinked()) {
                    return false;
                }
            }
        }

        // RETURN ALL SITES for filtered Activity
        return true;
    }

    private Promise<Void> joinIndicatorValues(final GetSites command, SqlTransaction tx,
                                              final Multimap<Integer, SiteDTO> siteMap,
                                              final Map<Integer, SiteDTO> periodMap) {

        final Promise<Void> complete = new Promise<>();

        Log.trace("Starting joinIndicatorValues()");

        SqlQuery query = SqlQuery.select()
                .appendColumn("P.SiteId", "SiteId")
                .appendColumn("V.IndicatorId", "SourceIndicatorId")
                .appendColumn("I.ActivityId", "SourceActivityId")
                .appendColumn("D.IndicatorId", "DestIndicatorId")
                .appendColumn("D.ActivityId", "DestActivityId")
                .appendColumn("I.Type")
                .appendColumn("I.Expression")
                .appendColumn("V.Value")
                .appendColumn("V.TextValue")
                .appendColumn("V.DateValue")
                .appendColumn("P.ReportingPeriodId", "PeriodId")
                .from(Tables.REPORTING_PERIOD, "P")
                .innerJoin(Tables.INDICATOR_VALUE, "V")
                .on("P.ReportingPeriodId = V.ReportingPeriodId")
                .innerJoin(Tables.INDICATOR, "I")
                .on("I.IndicatorId = V.IndicatorId")
                .leftJoin(Tables.INDICATOR_LINK, "L")
                .on("L.SourceIndicatorId=I.IndicatorId")
                .leftJoin(Tables.INDICATOR, "D")
                .on("L.DestinationIndicatorId=D.IndicatorId")
                .whereTrue("I.dateDeleted IS NULL");

        if(weAreFetchingAllSitesForAnActivityAndThereAreNoLinkedSites(command, siteMap)) {
            query.where("I.ActivityId").in(command.getFilter().getRestrictions(DimensionType.Activity));
        } else {
            query.where("P.SiteId").in(siteMap.keySet());
        }

        query.execute(tx, new SqlResultCallback() {

            @Override
            public void onSuccess(SqlTransaction tx, SqlResultSet results) {
                Log.trace("Received results for join indicators");

                for (final SqlResultSetRow row : results.getRows()) {
                    FieldTypeClass indicatorType = FormFieldType.valueOf(row.getString("Type"));
                    String expression = row.getString("Expression");
                    boolean isCalculatedIndicator = !Strings.isNullOrEmpty(expression);
                    Object indicatorValue = null;
                    if (isCalculatedIndicator) {
                        // ignore -> see joinCalculatedIndicatorValues
                    } else { // if indicator is no calculated then assign value directly
                        if (indicatorType == FieldTypeClass.QUANTITY) {
                            if(!row.isNull("Value")) {
                                indicatorValue = row.getDouble("Value");
                            }
                        } else if (indicatorType == FieldTypeClass.FREE_TEXT || indicatorType == FieldTypeClass.NARRATIVE) {
                            if(!row.isNull("TextValue")) {
                                indicatorValue = row.getString("TextValue");
                            }
                        } else if (indicatorType == FieldTypeClass.LOCAL_DATE) {
                            indicatorValue = row.getDate("DateValue");
                        } else if (indicatorType == FieldTypeClass.BOOLEAN) {
                            if (!row.isNull("BooleanValue")) {
                                indicatorValue = row.getBoolean("BooleanValue");
                            }
                        }
                    }

                    int sourceActivityId = row.getInt("SourceActivityId");

                    if (command.isFetchAllReportingPeriods()) {
                        SiteDTO site = periodMap.get(row.getInt("PeriodId"));
                        if(site != null) {
                            site.setIndicatorValue(row.getInt("SourceIndicatorId"), indicatorValue);
                        }
                    } else {

                        for (SiteDTO site : siteMap.get(row.getInt("SiteId"))) {
                            if (sourceActivityId == site.getActivityId()) {
                                int indicatorId = row.getInt("SourceIndicatorId");
                                site.setIndicatorValue(indicatorId, indicatorValue);
                            } else if (!row.isNull("DestActivityId")) {
                                int destActivityId = row.getInt("DestActivityId");
                                if (site.getActivityId() == destActivityId) {
                                    int indicatorId = row.getInt("DestIndicatorId");
                                    site.setIndicatorValue(indicatorId, indicatorValue);
                                }
                            }
                        }
                    }
                }
                Log.trace("Done populating dtos for join indicators");

                // after normal indicators are evaluated try to calculate indicators with expression
                joinCalculatedIndicatorValues(complete, tx, siteMap);
            }
        });
        return complete;
    }

    private void joinCalculatedIndicatorValues(final Promise<Void> complete, SqlTransaction tx, final Multimap<Integer, SiteDTO> siteMap) {
        Log.trace("Starting joinIndicatorValues()");

        final Set<Integer> activityIds = Sets.newHashSet();
        for (SiteDTO siteDTO : siteMap.values()) {
            activityIds.add(siteDTO.getActivityId());
        }

        SqlQuery query = SqlQuery.select()
                .appendColumn("I.IndicatorId", "indicatorId")
                .appendColumn("I.Name", "indicatorName")
                .appendColumn("I.ActivityId", "activityId")
                .appendColumn("I.Type", "type")
                .appendColumn("I.Expression", "expression")
                .appendColumn("I.nameInExpression", "code")
                .appendColumn("I.calculatedAutomatically", "calculatedAutomatically")
                .from(Tables.INDICATOR, "I")
                .where("I.ActivityId")
                .in(activityIds)
                .and("I.dateDeleted IS NULL")
                .orderBy("I.SortOrder");

        Log.info(query.toString());

        query.execute(tx, new SqlResultCallback() {
            @Override
            public void onSuccess(SqlTransaction tx, final SqlResultSet results) {
                List<FormField> fields = Lists.newArrayList();
                for(SqlResultSetRow row : results.getRows()) {
                    fields.add(createField(row));
                }

                FormSymbolTable symbolTable = new FormSymbolTable(fields);
                PartialEvaluator<SiteDTO> evaluator = new PartialEvaluator<>(symbolTable, new SiteFieldReaderFactory());

                List<CalculatedIndicatorReader> readers = Lists.newArrayList();
                for(FormField field : fields) {
                    if(field.getType() instanceof CalculatedFieldType) {
                        FieldReader<SiteDTO> reader = evaluator.partiallyEvaluate(field);
                        if(reader.getType() instanceof QuantityType) {
                            readers.add(new CalculatedIndicatorReader(field, reader));
                        }
                    }
                }

                for(SiteDTO site : siteMap.values()) {
                    for(CalculatedIndicatorReader reader : readers) {
                        reader.read(site);
                    }
                }
                complete.onSuccess(null);
            }
        });
    }

    private FormField createField(SqlResultSetRow rs) {
        IndicatorDTO indicator = new IndicatorDTO();
        indicator.setId(rs.getInt("indicatorId"));
        indicator.setName("indicatorName");
        indicator.setTypeId(rs.getString("type"));
        indicator.setExpression(rs.getString("expression"));
        indicator.setSkipExpression(rs.getString("skipExpression"));
        indicator.setNameInExpression(rs.getString("code"));
        indicator.setCalculatedAutomatically(rs.getBoolean("calculatedAutomatically"));
        indicator.setUnits(rs.getString("units"));
        return indicator.asFormField();
    }

    private static class CalculatedIndicatorReader {
        private String propertyName;
        private FieldReader<SiteDTO> reader;

        private CalculatedIndicatorReader(FormField field, FieldReader<SiteDTO> reader) {
            this.propertyName = IndicatorDTO.getPropertyName(CuidAdapter.getLegacyIdFromCuid(field.getId()));
            this.reader = reader;
        }

        public void read(SiteDTO site) {
            FieldValue value = reader.readField(site);
            if(value instanceof Quantity) {
                site.set(propertyName, ((Quantity) value).getValue());
            }
        }
    }

    private Promise<Void> joinAttributeValues(GetSites command, SqlTransaction tx, final Multimap<Integer, SiteDTO> siteMap) {

        Log.trace("Starting joinAttributeValues() ");
        final Promise<Void> complete = new Promise<>();

        SqlQuery sqlQuery = SqlQuery.select()
                .appendColumn("v.AttributeId", "attributeId")
                .appendColumn("a.Name", "attributeName")
                .appendColumn("v.Value", "value")
                .appendColumn("v.SiteId", "siteId")
                .appendColumn("g.name", "groupName")
                .from(Tables.ATTRIBUTE_VALUE, "v")
                .leftJoin(Tables.ATTRIBUTE, "a")
                .on("v.AttributeId = a.AttributeId")
                .leftJoin(Tables.ATTRIBUTE_GROUP, "g")
                .on("a.AttributeGroupId=g.AttributeGroupId")
                .whereTrue("v.Value=1")
                .orderBy("groupName, attributeName");

        if(weAreFetchingAllSitesForAnActivityAndThereAreNoLinkedSites(command, siteMap)) {
            sqlQuery.leftJoin(Tables.ATTRIBUTE_GROUP_IN_ACTIVITY, "ag")
                    .on("ag.attributeGroupId=g.attributeGroupId")
                    .where("ag.ActivityId").in(command.getFilter().getRestrictions(DimensionType.Activity));
        } else {
            sqlQuery.where("v.SiteId").in(siteMap.keySet());
        }

        sqlQuery.execute(tx, new SqlResultCallback() {
            @Override
            public void onSuccess(SqlTransaction tx, SqlResultSet results) {
                Log.trace("Received results for joinAttributeValues() ");

                for (SqlResultSetRow row : results.getRows()) {
                    int attributeId = row.getInt("attributeId");
                    boolean value = row.getBoolean("value");
                    String groupName = row.getString("groupName");
                    String attributeName = row.getString("attributeName");

                    for (SiteDTO site : siteMap.get(row.getInt("siteId"))) {
                        site.setAttributeValue(attributeId, value);

                        if (value) {
                            site.addDisplayAttribute(groupName, attributeName);
                        }
                    }
                }

                Log.trace("Done populating results for joinAttributeValues()");
                complete.onSuccess(null);
            }
        });
        return complete;
    }

    private SiteDTO toSite(GetSites query, SqlResultSetRow row) {
        SiteDTO model = new SiteDTO();
        model.setId(row.getInt("SiteId"));
        model.setLinked(row.getBoolean("Linked"));
        model.setActivityId(row.getInt("ActivityId"));
        model.setDate1(row.getDate("Date1"));
        model.setDate2(row.getDate("Date2"));
        model.setDateCreated(row.getDate("DateCreated"));
        model.setTimeEdited(row.getDouble("TimeEdited"));

        if(query.isFetchLocation()) {
            model.setLocationId(row.getInt("LocationId"));
            model.setLocationName(row.getString("LocationName"));
            model.setLocationAxe(row.getString("LocationAxe"));

            if (!row.isNull("x") && !row.isNull("y")) {
                model.setX(row.getDouble("x"));
                model.setY(row.getDouble("y"));
            }
        }

        if(query.isFetchPartner()) {
            PartnerDTO partner = new PartnerDTO();
            partner.setId(row.getInt("PartnerId"));
            partner.setName(row.getString("PartnerName"));
            model.setPartner(partner);
        }

        if (!row.isNull("ProjectId") && row.isNull("ProjectDateDeleted")) {
            ProjectDTO project = new ProjectDTO();
            project.setId(row.getInt("ProjectId"));
            project.setName(row.getString("ProjectName"));
            model.setProject(project);
        }

        if (query.isFetchAllReportingPeriods()) {
            model.set("reportingPeriodId", row.get("PeriodId"));
        }


        model.setComments(row.getString("Comments"));

        return model;
    }

    private boolean isMySql() {
        return dialect.isMySql();
    }

}
