package org.activityinfo.legacy.shared.impl.pivot;

import com.bedatadriven.rebar.sql.client.SqlResultCallback;
import com.bedatadriven.rebar.sql.client.SqlResultSet;
import com.bedatadriven.rebar.sql.client.SqlResultSetRow;
import com.bedatadriven.rebar.sql.client.SqlTransaction;
import com.bedatadriven.rebar.sql.client.query.SqlQuery;
import com.google.common.collect.*;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.activityinfo.legacy.shared.command.DimensionType;
import org.activityinfo.legacy.shared.command.Filter;
import org.activityinfo.legacy.shared.command.GetSites;
import org.activityinfo.legacy.shared.command.PivotSites;
import org.activityinfo.legacy.shared.command.result.Bucket;
import org.activityinfo.legacy.shared.command.result.SiteResult;
import org.activityinfo.legacy.shared.impl.Tables;
import org.activityinfo.legacy.shared.impl.pivot.calc.*;
import org.activityinfo.legacy.shared.model.SiteDTO;
import org.activityinfo.legacy.shared.reports.content.DimensionCategory;
import org.activityinfo.legacy.shared.reports.content.EntityCategory;
import org.activityinfo.legacy.shared.reports.content.SimpleCategory;
import org.activityinfo.legacy.shared.reports.content.TargetCategory;
import org.activityinfo.legacy.shared.reports.model.AdminDimension;
import org.activityinfo.legacy.shared.reports.model.AttributeGroupDimension;
import org.activityinfo.legacy.shared.reports.model.DateDimension;
import org.activityinfo.legacy.shared.reports.model.Dimension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Aggregates calculated indicators using the GetSites command.
 */
public class CalculatedIndicatorsQuery implements WorkItem {

    private static final Logger LOGGER = Logger.getLogger(CalculatedIndicatorsQuery.class.getName());

    private static final Dimension INDICATOR_DIM = new Dimension(DimensionType.Indicator);

    private final PivotSites query;
    private final PivotQueryContext queryContext;

    private Set<Integer> activityIds = Sets.newHashSet();
    private Set<Integer> indicatorIds = Sets.newHashSet();

    private Map<Integer, DimensionCategory> activityMap = Maps.newHashMap();
    private Map<Integer, DimensionCategory> activityCategoryMap = Maps.newHashMap();
    private Map<Integer, DimensionCategory> activityToDatabaseMap = Maps.newHashMap();
    private Map<Integer, EntityCategory> indicatorMap = Maps.newHashMap();
    private Map<Integer, Integer> indicatorAggregationMap = Maps.newHashMap();

    private Multimap<Integer, EntityCategory> attributes = HashMultimap.create();

    private List<DimAccessor> dimAccessors = Lists.newArrayList();

    private AsyncCallback<Void> callback;

    public CalculatedIndicatorsQuery(PivotQueryContext queryContext) {
        this.query = queryContext.getCommand();
        this.queryContext = queryContext;
    }

    @Override
    public void execute(final AsyncCallback<Void> callback) {
        this.callback = callback;
        final SqlQuery query = SqlQuery.selectDistinct()
                .appendColumn("i.indicatorId", "indicatorId")
                .appendColumn("i.name", "indicatorName")
                .appendColumn("i.activityId", "activityId")
                .appendColumn("i.sortOrder", "indicatorOrder")
                .appendColumn("i.aggregation", "aggregation")
                .appendColumn("a.name", "activityName")
                .appendColumn("a.category", "activityCategory")
                .appendColumn("a.sortOrder", "activityOrder")
                .appendColumn("db.DatabaseId", "databaseId")
                .appendColumn("db.name", "databaseName")
                .from(Tables.INDICATOR, "i")
                .leftJoin(Tables.ACTIVITY, "a").on("a.activityId=i.activityId")
                .leftJoin(Tables.USER_DATABASE, "db").on("a.databaseId=db.databaseId")
                .whereTrue("i.calculatedAutomatically=1 and i.Expression is not null");

        Filter filter = this.query.getFilter();
        if(filter.isRestricted(DimensionType.Indicator)) {
            query.where("i.indicatorId").in(filter.getRestrictions(DimensionType.Indicator));

        } else if(filter.isRestricted(DimensionType.Activity)) {
            query.where("i.activityId").in(filter.getRestrictions(DimensionType.Activity));

        } else if(filter.isRestricted(DimensionType.Database)) {
            query.where("a.databaseId").in(filter.getRestrictions(DimensionType.Database));

        } else {
            // too broad
            callback.onSuccess(null);
            return;
        }

        // enforce visibility rules

        query.whereTrue(visibilityRules());

        query.execute(queryContext.getExecutionContext().getTransaction(), new SqlResultCallback() {

            @Override
            public void onSuccess(SqlTransaction tx, SqlResultSet results) {

                if (results.getRows().isEmpty()) {
                    callback.onSuccess(null);

                } else {

                    boolean hasSumAggregation = false;
                    for (SqlResultSetRow row : results.getRows()) {

                        LOGGER.info("row = " + row);

                        int activityId = row.getInt("activityId");
                        int indicatorId = row.getInt("indicatorId");

                        activityIds.add(activityId);
                        indicatorIds.add(indicatorId);

                        activityMap.put(activityId, new EntityCategory(activityId,
                            row.getString("activityName"),
                            row.getInt("activityOrder")));

                        activityCategoryMap.put(activityId, new SimpleCategory(row.getString("activityCategory")));

                        activityToDatabaseMap.put(activityId,
                                new EntityCategory(row.getInt("databaseId"), row.getString("databaseName")));
                        indicatorMap.put(indicatorId,
                            new EntityCategory(indicatorId,
                                row.getString("indicatorName"),
                                row.getInt("indicatorOrder")));
                        indicatorAggregationMap.put(indicatorId, row.getInt("aggregation"));

                        if (!hasSumAggregation) {
                            hasSumAggregation = row.getInt("aggregation") == 0;
                        }
                    }

                    // set aggregation to Sum for all indicators if at least one indicator has different aggregation
                    if (hasSumAggregation) {
                        for (Map.Entry<Integer, Integer> entry : indicatorAggregationMap.entrySet()) {
                            entry.setValue(0); // set to Sum
                        }
                    }

                    if (queryContext.getCommand().isPivotedBy(DimensionType.AttributeGroup)) {
                        queryAttributeGroups();
                    } else {
                        querySites();
                    }
                }
            }
        });
    }

    private void queryAttributeGroups() {

        Set<Integer> groupIds = Sets.newHashSet();
        for(Dimension dim : query.getDimensions()) {
            if(dim instanceof AttributeGroupDimension) {
                AttributeGroupDimension groupDim = (AttributeGroupDimension) dim;
                groupIds.add(groupDim.getAttributeGroupId());
            }
        }

        SqlQuery.select()
                .appendColumn("g.attributeGroupId", "groupId")
                .appendColumn("g.name", "groupName")
                .appendColumn("a.attributeId")
                .appendColumn("a.name")
                .appendColumn("a.sortOrder")
                .from(Tables.ATTRIBUTE, "a")
                .leftJoin(Tables.ATTRIBUTE_GROUP, "g").on("a.attributeGroupId=g.attributeGroupId")
                .where("a.attributeGroupId").in(groupIds)
                .execute(queryContext.getExecutionContext().getTransaction(), new SqlResultCallback() {
                    @Override
                    public void onSuccess(SqlTransaction tx, SqlResultSet results) {
                        for (SqlResultSetRow row : results.getRows()) {
                            int groupId = row.getInt("groupId");
                            int attributeId = row.getInt("attributeId");
                            int sortOrder = row.getInt("sortOrder");
                            String attributeName = row.getString("name");
                            attributes.put(groupId, new EntityCategory(attributeId, attributeName, sortOrder));
                        }
                        querySites();
                    }
                });
    }

    private String visibilityRules() {
        int userId = queryContext.getExecutionContext().getUser().getId();

        return new StringBuilder()
        .append("(")
        // databases we own
        .append("db.OwnerUserId = ").append(userId).append(" ")

        // databases with allow view all
        .append("OR ")
        .append("db.DatabaseId IN (")
        .append(" SELECT ")
        .append("  p.DatabaseId ")
        .append(" FROM ")
        .append("  userpermission p ")
        .append(" WHERE ")
        .append("  p.UserId = ")
        .append(userId)
        .append("  AND p.AllowViewAll").append(") ")

       // or activities that are published

        .append(" OR ")
        .append(" (a.published > 0)")
        .append(")")
        .toString();

    }

    private void querySites() {

        GetSites sitesQuery = new GetSites(composeSiteFilter());
        sitesQuery.setFetchAdminEntities( query.isPivotedBy(DimensionType.AdminLevel) );
        sitesQuery.setFetchAttributes(query.isPivotedBy(DimensionType.AttributeGroup));
        sitesQuery.setFetchAllIndicators(true);
        sitesQuery.setFetchLocation(query.isPivotedBy(DimensionType.Location));
        sitesQuery.setFetchPartner(query.isPivotedBy(DimensionType.Partner));
        sitesQuery.setFetchComments(false);
        sitesQuery.setFetchDates(query.isPivotedBy(DimensionType.Date));
        sitesQuery.setFetchLinks(false);
        sitesQuery.setFetchAllReportingPeriods(true);

        sitesQuery.setLimit(-1);


        for (Dimension dim : query.getDimensions()) {
            if (dim.getType() != DimensionType.Indicator) {
                dimAccessors.add(createAccessor(dim));
            }
        }

        queryContext.getExecutionContext().execute(sitesQuery, new AsyncCallback<SiteResult>() {
            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }

            @Override
            public void onSuccess(SiteResult result) {
                try {
                    aggregateSites(result);
                    callback.onSuccess(null);
                } catch(Throwable caught) {
                    callback.onFailure(caught);
                }
            }
        });
    }


    private Filter composeSiteFilter() {
        Filter siteFilter = new Filter();
        siteFilter.addRestriction(DimensionType.Activity, activityIds);
        siteFilter.setDateRange(query.getFilter().getDateRange());

        for(DimensionType type : query.getFilter().getRestrictedDimensions()) {
            if(type != DimensionType.Activity && type != DimensionType.Database && type != DimensionType.Indicator) {
                siteFilter.addRestriction(type, query.getFilter().getRestrictions(type));
            }
        }

        return siteFilter;
    }


    private DimAccessor createAccessor(Dimension dim) {
        if(dim.getType() == DimensionType.Activity) {
            return new ActivityAccessor(dim, activityMap);

        } else if(dim.getType() == DimensionType.ActivityCategory) {
                return new ActivityAccessor(dim, activityCategoryMap);

        } else if(dim.getType() == DimensionType.Database) {
            return new ActivityAccessor(dim, activityToDatabaseMap);

        } else if(dim.getType() == DimensionType.AdminLevel) {
            return new AdminAccessor((AdminDimension) dim);

        } else if(dim.getType() == DimensionType.Date) {
            DateDimension dateDim = (DateDimension) dim;
            return new DateAccessor(dateDim);

        } else if(dim.getType() == DimensionType.AttributeGroup) {
            AttributeGroupDimension groupDim = (AttributeGroupDimension) dim;
            return new AttributeAccessor(groupDim, attributes.get(groupDim.getAttributeGroupId()));

        } else if(dim.getType() == DimensionType.Location) {
            return new LocationAccessor(dim);

        } else if (dim.getType() == DimensionType.Partner) {
            return new PartnerAccessor(dim);
        } else if (dim.getType() == DimensionType.Site) {
            return new SiteAccessor(dim);
        } else if (dim.getType() == DimensionType.Target) {
            return new TargetAccessor(dim);
        }
        throw new UnsupportedOperationException("dim: " + dim);
    }


    private void aggregateSites(SiteResult result) {

        for (int i = 0; i != result.getTotalLength(); ++i) {
            SiteDTO site = result.getData().get(i);

            // Now loop over each value
            for (EntityCategory indicator : indicatorMap.values()) {
                Double value = site.getIndicatorDoubleValue(indicator.getId());

                if (value != null) {

                    Bucket bucket = new Bucket(value);
                    bucket.setAggregationMethod(indicatorAggregationMap.get(indicator.getId()));

                    bucket.setCategory(new Dimension(DimensionType.Target), TargetCategory.REALIZED);
                    if (query.isPivotedBy(DimensionType.Indicator)) {
                        bucket.setCategory(new Dimension(DimensionType.Indicator), indicator);
                    }
                    for (int j = 0; j != dimAccessors.size(); ++j) {
                        bucket.setCategory(dimAccessors.get(j).getDimension(), dimAccessors.get(j).getCategory(site));
                    }
                    queryContext.addBucket(bucket);
                }
            }
        }
    }

}
