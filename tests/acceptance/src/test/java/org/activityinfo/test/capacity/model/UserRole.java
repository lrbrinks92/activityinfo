package org.activityinfo.test.capacity.model;

import com.google.common.base.Optional;
import org.activityinfo.test.capacity.action.UserAction;

import java.util.Iterator;

/**
 * Provides a "script" for agents within a scenario to follow
 */
public interface UserRole {
    
    String getNickName();

    /**
     * 
     * Returns the list of tasks that the user must accomplish on
     * the given the day of the simulation
     * 
     * @param dayNumber the day number 
     * @return
     */
    Optional<UserAction> getTask(int dayNumber);

}
