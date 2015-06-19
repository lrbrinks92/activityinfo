package org.activityinfo.store.mysql

import liquibase.Liquibase
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.FileSystemResourceAccessor
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction

/**
 * Sets up a MySQL Database using Liquibase and dump scripts
 */
class MySqlSetupTask extends DefaultTask {

    boolean dropDatabase = false
    
    MySqlDatabase database
    String changeLog
    FileCollection scripts

    @TaskAction
    def setup() {
        logger.info("Creating database ${database.name}...")
        createDatabase()

        logger.info("Connecting to ${database.server}")
        def connection = database.connect()
        try {
            logger.info("Migrating schema...")
            migrateSchema(connection)

            logger.info("Populating database...")
            populateData(connection)
            
        } finally {
            connection.close()
        }
    }


    def createDatabase() {
        // First create the database itself, dropping first if requested
        def connection = database.server.connect()
        try {
            def stmt = connection.createStatement()
            if(dropDatabase) {
                stmt.execute("DROP DATABASE IF EXISTS `${database.name}`")
            }
            stmt.execute("CREATE DATABASE IF NOT EXISTS `${database.name}`")
            
        } finally {
            connection.close()
        }
    }

    def migrateSchema(connection) {
        def liquibase = new Liquibase(changeLog,
                new FileSystemResourceAccessor(project.file('src/main/resources').absolutePath),
                new JdbcConnection(connection));

        liquibase.log.logLevel = LiquibaseLogging.get(project)
        liquibase.update(null)
    }

    def populateData(connection) {
        ScriptRunner runner = new ScriptRunner(connection, false, true)
        if(!project.logger.isEnabled(LogLevel.DEBUG)) {
            runner.setLogWriter(null)
        }
        scripts.each {
            logger.info("Executing ${it}...")
            it.withReader { reader ->
                runner.execute(reader)
            }
        }
    }
}
