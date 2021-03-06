package org.activityinfo.ui.client.http;

import org.activityinfo.api.client.ActivityInfoClientAsync;
import org.activityinfo.model.query.ColumnSet;
import org.activityinfo.model.query.QueryModel;
import org.activityinfo.promise.Promise;

/**
 * Created by alex on 27-1-17.
 */
public class QueryRequest implements HttpRequest<ColumnSet> {
    private final QueryModel queryModel;

    public QueryRequest(QueryModel queryModel) {
        this.queryModel = queryModel;
    }

    @Override
    public Promise<ColumnSet> execute(ActivityInfoClientAsync client) {
        return client.queryTableColumns(queryModel);
    }
}
