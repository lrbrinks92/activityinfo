package org.activityinfo

import org.activityinfo.store.mysql.MySqlDatabase

/**
 * Defines a deployment environment
 */
class DeploymentEnvironment {

    /**
     * Google Cloud ProjectId
     */
    String projectId
    
    MySqlDatabase database

    String url
    
    boolean isLocal() {
        return projectId == null
    }
    
    public Map<String, String> getSystemProperties() {
        def properties = new HashMap<String, String>()
        if(database == null) {
            throw new IllegalStateException("Database has not been configured yet")
        }
        if(url == null) {
            throw new IllegalStateException("Environment URL has not been configured yet")
        }

        properties.putAll(database.properties)
        properties.put("test.url", url)
        
        if(properties == null) {
            throw new NullPointerException();
        }
        
        return properties
    }
}
