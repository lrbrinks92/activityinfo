package org.activityinfo.server.database;

import com.bedatadriven.rebar.sql.client.fn.TxAsyncFunction;
import com.bedatadriven.rebar.sql.server.jdbc.SqliteStubDatabase;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Created by yuriy on 12/23/2014.
 */
public class TestSqliteDatabase extends SqliteStubDatabase {

    public TestSqliteDatabase(String databaseName) {
        super(databaseName);
    }

    @Override
    public void executeUpdates(String json, AsyncCallback<Integer> callback) {
        super.executeUpdates(adjustExecuteUpdates(json), callback);
    }

    public String adjustExecuteUpdates(String json) {
        return json;
    }

    @Override
    public <T> void execute(TxAsyncFunction<Void, T> f, AsyncCallback<T> callback) {
        super.execute(f, callback);
    }
}
