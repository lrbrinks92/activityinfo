package org.activityinfo.server.command.handler.sync;


import java.sql.ResultSet;

interface ResultSetHandler {
    
    void handle(ResultSet resultSet) throws Exception;
    
}
