package org.activityinfo.store.mysql

import com.mysql.jdbc.Driver
import java.util.logging.Logger
import java.sql.Connection

/**
 * Describes parameters needed to connect to a MySQL Server instance
 */
class MySqlServer {
    String host
    int port = 3306
    String username
    String password
    File keyStore
    
    MySqlDatabase database(String name) {
        new MySqlDatabase(server: this, name: name)
    }

    String getUrl(String databaseName) {
        def url = "jdbc:mysql://${host}:${port}/${databaseName}?useUnicode=true&characterEncoding=UTF-8"
        if(keyStore) {
            url += "&useSSL=true"
        }
        url
    }
    
    MySqlDatabase getAt(String name) {
        database(name)
    }
    
    Connection connect(String databaseName) {
        Logger logger = Logger.getLogger(MySqlServer.class.getName())
        logger.info("Connecting to database:" + databaseName + ", host: " + host + ", port: " + port + ", user:" + username)

        def url = "jdbc:mysql://${host}:${port}/${databaseName}?useUnicode=true&characterEncoding=UTF-8"
        def driver = new Driver()
        return driver.connect(url, connectionProperties)
    }
    
    Connection connect() {
        return connect("mysql")
    }
    
    Properties getConnectionProperties() {
        Properties properties = new Properties()
        properties.put('user', username)
        properties.put("password", password)

        if(keyStore) {
            properties.put("verifyServerCertificate", 'true')
            properties.put('requireSSL', 'true')
            properties.put('useSSL', 'true')
            properties.put('trustCertificateKeyStoreUrl', keyStore.toURI().toURL().toString())
            properties.put('trustCertificateKeyStorePassword', 'notasecret')
            properties.put('clientCertificateKeyStoreUrl', keyStore.toURI().toURL().toString())
            properties.put('clientCertificateKeyStorePassword', 'notasecret')
        }
        return properties
    }
}
