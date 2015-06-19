
package org.activityinfo.store.mysql


/**
 * Describes a MySql database within a {@code MySqlServer}
 */
class LiquibaseLogging {
    


    static def get(project) {

        switch (project.logging.level) {
            case org.gradle.api.logging.LogLevel.DEBUG:
                return liquibase.logging.LogLevel.DEBUG
            case org.gradle.api.logging.LogLevel.INFO:
                return liquibase.logging.LogLevel.INFO
            default:
                return liquibase.logging.LogLevel.SEVERE
        }
    }
    
}
