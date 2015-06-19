package org.activityinfo.server.endpoint.rest;

import com.google.common.collect.Sets;
import org.activityinfo.legacy.shared.command.DimensionType;
import org.activityinfo.legacy.shared.command.Filter;
import org.activityinfo.legacy.shared.command.Month;
import org.activityinfo.legacy.shared.command.PivotSites;
import org.activityinfo.legacy.shared.command.result.Bucket;
import org.activityinfo.legacy.shared.reports.model.DateDimension;
import org.activityinfo.legacy.shared.reports.model.DateUnit;
import org.activityinfo.legacy.shared.reports.model.Dimension;
import org.activityinfo.server.command.DispatcherSync;
import org.activityinfo.server.report.util.DateUtilCalendarImpl;
import org.activityinfo.server.util.monitoring.Timed;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Set;

public class CubeResource {

    private final DispatcherSync dispatcherSync;

    public CubeResource(DispatcherSync dispatcherSync) {
        this.dispatcherSync = dispatcherSync;
    }

    @GET
    @Timed(name = "api.rest.sites.pivot")
    @Produces("application/json")
    public List<Bucket> pivot(@QueryParam("dimension") List<String> dimensions, @QueryParam("form") List<Integer> forms,
                              @QueryParam("month") String monthName) {

        Filter filter = new Filter();
        if(forms.size() == 0) {
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("Must specify at least one ?form={formId}").build());
        }
        filter.addRestriction(DimensionType.Activity, forms);


        if(monthName != null) {
            Month month = Month.parseMonth(monthName);
            filter.setDateRange(new DateUtilCalendarImpl().monthRange(month));
        }

        Set<Dimension> pivotDimensions = Sets.newHashSet();

        if(forms.size() == 0) {
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("Must specify at least one ?dimension={indicator|form|database|...}").build());
        }

        for(String dimension : dimensions) {
            switch(dimension) {
                case "indicator":
                    pivotDimensions.add(new Dimension(DimensionType.Indicator));
                    break;
                case "site":
                    pivotDimensions.add(new Dimension(DimensionType.Site));
                    break;
                case "month":
                    pivotDimensions.add(new DateDimension(DateUnit.MONTH));
                    break;
                default:
                    throw new WebApplicationException(Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity("Invalid dimension '" + dimension + "'").build());
            }
        }


        PivotSites query = new PivotSites();
        query.setFilter(filter);
        query.setDimensions(pivotDimensions);

        PivotSites.PivotResult result = dispatcherSync.execute(query);

        return result.getBuckets();

    }

}
