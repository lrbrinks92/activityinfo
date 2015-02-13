package org.activityinfo.legacy.shared.impl;

import com.bedatadriven.rebar.sql.client.SqlResultSetRow;
import com.bedatadriven.rebar.sql.client.query.SqlQuery;
import com.bedatadriven.rebar.sql.client.util.SingleRowHandler;
import com.google.common.base.Strings;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.activityinfo.legacy.shared.adapter.ActivityFormClassBuilder;
import org.activityinfo.legacy.shared.command.GetActivityForm;
import org.activityinfo.legacy.shared.command.GetFormClass;
import org.activityinfo.legacy.shared.command.result.FormClassResult;
import org.activityinfo.legacy.shared.model.ActivityFormDTO;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.legacy.CuidAdapter;
import org.activityinfo.model.resource.Resources;

public class GetFormClassHandlerAsync implements CommandHandlerAsync<GetFormClass, FormClassResult> {

    @Override
    public void execute(GetFormClass command, final ExecutionContext context, final AsyncCallback<FormClassResult> callback) {

        final int activityId = CuidAdapter.getLegacyIdFromCuid(command.getResourceId());

        SqlQuery.select("formClass")
                .from(Tables.ACTIVITY, "a")
                .where("a.activityId").equalTo(activityId)
                .execute(context.getTransaction(), new SingleRowHandler() {
                    @Override
                    public void handleRow(SqlResultSetRow row) {
                        String json = row.getString("formClass");
                        if(Strings.isNullOrEmpty(json)) {
                            constructFromLegacy(activityId, context, callback);
                        } else {
                            callback.onSuccess(new FormClassResult(json));
                        }
                    }
                });
    }

    private void constructFromLegacy(final int activityId, ExecutionContext context, final AsyncCallback<FormClassResult> callback) {
        context.execute(new GetActivityForm(activityId), new AsyncCallback<ActivityFormDTO>() {
            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }

            @Override
            public void onSuccess(ActivityFormDTO result) {
                String json;
                try {
                    ActivityFormClassBuilder builder = new ActivityFormClassBuilder(result);
                    FormClass formClass = builder.build();
                    json = Resources.toJson(formClass);
                } catch (Exception e) {
                    callback.onFailure(e);
                    return;
                }
                callback.onSuccess(new FormClassResult(json));
            }
        });

    }
}
