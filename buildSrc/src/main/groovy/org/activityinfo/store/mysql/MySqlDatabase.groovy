package org.activityinfo.store.mysql

import java.sql.Connection

/**
 * Describes a MySql database within a {@code MySqlServer}
 */
class MySqlDatabase {
    
    MySqlServer server
    String name

    String getUrl() {
        return server.getUrl(name)
    }

    Connection connect() {
        server.connect(name)
    }
    
    Map<String, String> getProperties() {
        Map<String, String> properties = new HashMap<>()
        properties.put("databaseUrl", url)
        server.connectionProperties.each { String name, String value ->
            properties.put("mysql." + name, value);
        }
        return properties
    }
}
