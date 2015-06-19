package org.activityinfo.store.mysql

import liquibase.Liquibase
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.FileSystemResourceAccessor
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Checks for unapplied liquibase changesets to the production
 * server and writes MySQL migration scripts if neccessary.
 */
class MySqlReleaseTask extends DefaultTask {
    
    String changeLog

    @TaskAction
    def migrate() {
  
        MySqlServer server = new MySqlServer()
        server.host = project.property('productionMySqlHost')
        server.username = project.property('productionMySqlUsername')
        server.password = project.property('productionMySqlPassword')
        
        MySqlDatabase database = server.database('activityinfo')

        logger.info("Connecting to production database...")
        def connection = database.connect()
        def liquibase = new Liquibase(changeLog,
                new FileSystemResourceAccessor(project.file('src/main/resources').absolutePath),
                new JdbcConnection(connection));
        liquibase.log.logLevel = LiquibaseLogging.get(project)

        try {
            logger.info("Validating Changesets...")
            liquibase.validate()

            logger.info("Checking for unapplied changesets...")
            def toApply = liquibase.listUnrunChangeSets(null)

            if(toApply.isEmpty()) {
                logger.lifecycle('Database is up to date.')
                
            } else {
                toApply.each {
                    logger.info("Unapplied changeset: ${it.id}")
                }
                
                if(!project.buildDir.exists()) {
                    project.buildDir.mkdirs()
                }
                def sqlFile = project.file("${project.buildDir}/migration-b${project.buildNumber}.sql")
                sqlFile.withWriter { writer ->
                    liquibase.update(null, writer)
                }
                throw new RuntimeException("Database requires migrations, apply migrations in" +
                        " ${project.rootProject.relativePath(sqlFile)} before continuing.")
            }

        } finally {
            connection.close()
        }
    }

}
