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
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.hibernate.Session;
import org.hibernate.ejb.HibernateEntityManager;
import org.hibernate.jdbc.AbstractWork;
import org.hibernate.jdbc.Work;

import javax.persistence.EntityManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SqlQueryUtil {

    public static ResultSet query(Connection connection, final SqlQuery query) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(query.sql());
        Object[] params = query.parameters();
        for (int i = 0; i != params.length; ++i) {
            statement.setObject(i + 1, params[i]);
        }
        ResultSet rs = statement.executeQuery();
        return rs;
    }

    public static String queryIdSet(EntityManager entityManager, final SqlQuery query) {
        final StringBuilder set = new StringBuilder();
        ((HibernateEntityManager) entityManager).getSession().doWork(new Work() {

            @Override
            public void execute(Connection connection) throws SQLException {
                set.append("(");
                boolean needsComma = false;
                ResultSet rs = query(connection, query);
                ColumnAppender appender = ColumnAppender.forType(rs.getMetaData().getColumnType(1));
                while (rs.next()) {
                    if (needsComma) {
                        set.append(',');
                    }
                    appender.append(set, rs, 1);
                    needsComma = true;
                }
                set.append(")");
            }
        });
        return set.toString();
    }

    public static String idSet(List<Integer> ids) {
        String s = "(";
        int size = ids.size();
        for (int i = 0; i < size; i++) {
            Integer id = ids.get(i);
            s = s + id;
            boolean isLast = (i + 1) == size;
            if (!isLast && size != 1) {
                s+= ",";
            }
        }
        s = s + ")";
        return s;
    }

    public static List<Long> queryLongList(EntityManager entityManager, final SqlQuery query) {
        final List<Long> result = Lists.newArrayList();
        ((HibernateEntityManager) entityManager).getSession().doWork(new Work() {

            @Override
            public void execute(Connection connection) throws SQLException {
                ResultSet rs = query(connection, query);
                while (rs.next()) {
                    result.add(rs.getLong(1));
                }
            }
        });
        return result;
    }

    public static void execute(EntityManager entityManager, final SqlQuery query, final ResultSetHandler handler) {
        entityManager.unwrap(Session.class).doWork(new AbstractWork() {
            @Override
            public void execute(Connection connection) throws SQLException {
                try(PreparedStatement statement = connection.prepareStatement(query.sql())) {
                    Object[] params = query.parameters();
                    for (int i = 0; i != params.length; ++i) {
                        statement.setObject(i + 1, params[i]);
                    }
                    try(ResultSet rs = statement.executeQuery()) {
                        try {
                            handler.handle(rs);
                        } catch (Exception e) {
                            throw new SQLException(e);
                        }
                    }
                }
            }
        });
    }

}
