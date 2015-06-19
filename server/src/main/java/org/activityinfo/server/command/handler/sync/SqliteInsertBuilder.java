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

import javax.persistence.EntityManager;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

public class SqliteInsertBuilder {

    private static final Logger LOGGER = Logger.getLogger(SqliteInsertBuilder.class.getName());

    private String tableName;

    private StringBuilder insert;
    private int numColumns;

    private SqlQuery query;
    private SqliteBatchBuilder batch;

    private StringBuilder sql = new StringBuilder();
    private int rowCount = 0;
    private ColumnAppender[] appenders;


    public SqliteInsertBuilder(SqliteBatchBuilder batch) {
        this.batch = batch;
    }

    public SqliteInsertBuilder into(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public SqliteInsertBuilder from(SqlQuery query) {
        this.query = query;
        return this;
    }

    public void execute(EntityManager entityManager) {
        SqlQueryUtil.execute(entityManager, query, new ResultSetHandler() {
            @Override
            public void handle(ResultSet rs) throws Exception {
                begin(rs, rs.getMetaData().getColumnCount());
                while (rs.next()) {
                    appendRow(rs);
                }
                finish();
            }
        });
    }

    private void composeInsertStatement(ResultSet rs) throws SQLException {
        insert = new StringBuilder();
        insert.append("INSERT OR REPLACE INTO ").append(tableName).append(" (");
        for (int i = 0; i != numColumns; ++i) {
            if (i > 0) {
                insert.append(",");
            }
            insert.append(rs.getMetaData().getColumnName(i + 1));
        }
        insert.append(") ");
    }

    public void begin(ResultSet rs) throws SQLException {
        begin(rs, rs.getMetaData().getColumnCount());
    }
    
    public void begin(ResultSet rs, int numColumns) throws SQLException {
        this.numColumns = numColumns;
        composeInsertStatement(rs);
        setupAppenders(rs, numColumns);
    }

    private void setupAppenders(ResultSet rs, int numColumns) throws SQLException {
        appenders = new ColumnAppender[numColumns];
        for (int i = 0; i != numColumns; ++i) {
            appenders[i] = ColumnAppender.forType(rs.getMetaData().getColumnType(i + 1));
        }
    }
    
    public void appendRow(ResultSet rs) throws SQLException, IOException {
        if (rowCount == 0) {
            sql.append(insert);
        } else {
            sql.append(" UNION ");
        }
        sql.append("SELECT ");
        for (int i = 0; i != numColumns; ++i) {
            if (i > 0) {
                sql.append(',');
            }
            appenders[i].append(sql, rs, i + 1);
        }
        rowCount++;
        if (rowCount > 450) {
            batch.addStatement(sql.toString());
            sql.setLength(0);
            rowCount = 0;
        }
    }


    public void finish() throws IOException {
        if (sql.length() > 0) {
            batch.addStatement(sql.toString());
        }
    }

}
