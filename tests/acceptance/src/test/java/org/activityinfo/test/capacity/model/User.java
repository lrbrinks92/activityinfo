package org.activityinfo.test.capacity.model;

import org.activityinfo.test.capacity.action.ActionExecution;
import org.activityinfo.test.capacity.action.UserAction;

import javax.annotation.Nonnull;

/**
 * Describes a user within a scenario
 */
public class User {

    private final ScenarioContext scenarioContext;
    private final UserRole role;


    public User(ScenarioContext scenarioContext, @Nonnull UserRole role) {
        this.scenarioContext = scenarioContext;
        this.role = role;
    }

    public String getNickname() {
        return role.getNickName();
    }

    public UserRole getRole() {
        return role;
    }

    @Override
    public String toString() {
        return getNickname();
    }


    public Runnable execution(UserAction action) {
        return new ActionExecution(scenarioContext, role, action);
    }

}
