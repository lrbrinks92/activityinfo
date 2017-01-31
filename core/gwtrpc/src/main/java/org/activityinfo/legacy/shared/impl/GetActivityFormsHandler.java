package org.activityinfo.legacy.shared.impl;

import com.bedatadriven.rebar.sql.client.SqlResultCallback;
import com.bedatadriven.rebar.sql.client.SqlResultSet;
import com.bedatadriven.rebar.sql.client.SqlResultSetRow;
import com.bedatadriven.rebar.sql.client.SqlTransaction;
import com.bedatadriven.rebar.sql.client.query.SqlQuery;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.activityinfo.legacy.shared.command.*;
import org.activityinfo.legacy.shared.command.result.ActivityFormResults;
import org.activityinfo.legacy.shared.model.ActivityFormDTO;
import org.activityinfo.legacy.shared.model.SchemaDTO;
import org.activityinfo.promise.Promise;

import java.util.List;
import java.util.logging.Logger;

public class GetActivityFormsHandler implements CommandHandlerAsync<GetActivityForms, ActivityFormResults> {

    private static final Logger LOGGER = Logger.getLogger(GetActivityFormsHandler.class.getName());

    @Override
    public void execute(GetActivityForms command, final ExecutionContext context, final AsyncCallback<ActivityFormResults> callback) {

        composeQuery(command.getFilter())
        .execute(context.getTransaction(), new SqlResultCallback() {
            @Override
            public void onSuccess(SqlTransaction tx, final SqlResultSet results) {
                context.execute(new GetSchema(), new AsyncCallback<SchemaDTO>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        callback.onFailure(caught);
                    }

                    @Override
                    public void onSuccess(SchemaDTO schema) {
                        Promise.map(applyPermission(schema, results), new Function<SqlResultSetRow, Promise<ActivityFormDTO>>() {
                            @Override
                            public Promise<ActivityFormDTO> apply(SqlResultSetRow input) {
                                return fetchForm(context, input.getInt("activityId"));
                            }
                        }).then(new Function<List<ActivityFormDTO>, ActivityFormResults>() {
                            @Override
                            public ActivityFormResults apply(List<ActivityFormDTO> input) {
                                return new ActivityFormResults(input);
                            }
                        }).then(callback);
                    }
                });
            }
        });
    }

    private List<SqlResultSetRow> applyPermission(SchemaDTO schema, SqlResultSet results) {
        List<SqlResultSetRow> allowedActivities = Lists.newArrayList();
        for (SqlResultSetRow row : results.getRows()) {
            if (schema.getActivityById(row.getInt("activityId")) != null) {
                allowedActivities.add(row);
            }
        }
        return allowedActivities;
    }

    private SqlQuery composeQuery(Filter filter) {
        if(filter.isRestricted(DimensionType.Indicator)) {
            return SqlQuery.selectDistinct()
                    .appendColumn("i.activityId")
                    .from(Tables.INDICATOR, "i")
                    .leftJoin(Tables.ACTIVITY, "a").on("a.activityId=i.activityId")
                    .where("indicatorId").in(filter.getRestrictions(DimensionType.Indicator))
                    .whereTrue("i.dateDeleted is NULL")
                    .whereTrue("a.dateDeleted is NULL");

        } else if(filter.isRestricted(DimensionType.Activity)) {
            return SqlQuery.selectDistinct()
                    .appendColumn("activityId")
                    .from(Tables.ACTIVITY)
                    .whereTrue("dateDeleted is NULL")
                    .where("activityId").in(filter.getRestrictions(DimensionType.Activity));

        } else if(filter.isRestricted(DimensionType.Database)) {
            return SqlQuery.selectDistinct()
                    .appendColumn("activityId")
                    .from(Tables.ACTIVITY)
                    .whereTrue("dateDeleted is NULL")
                    .where("databaseId").in(filter.getRestrictions(DimensionType.Database));
        } else {

            LOGGER.warning("No restrictions specified, returning empty set. Filter = " + filter);

            return SqlQuery.select("activityId").from(Tables.ACTIVITY).whereTrue("0=1");
        }

    }

    private Promise<ActivityFormDTO> fetchForm(ExecutionContext context, int activityId) {
        final Promise<ActivityFormDTO> form = new Promise<>();
        context.execute(new GetActivityForm(activityId), new AsyncCallback<ActivityFormDTO>() {
            @Override
            public void onFailure(Throwable caught) {
                form.reject(caught);
            }

            @Override
            public void onSuccess(ActivityFormDTO result) {
                form.resolve(result);
            }
        });
        return form;
    }
}