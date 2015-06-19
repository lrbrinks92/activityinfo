package org.activityinfo.test.capacity.action;


import org.activityinfo.test.driver.ApiApplicationDriver;

/**
 * An individual user action that can be executed with the {@code ApiApplicationDriver}
 */
public interface UserAction {
    
    void execute(ApiApplicationDriver driver) throws Exception;
}
