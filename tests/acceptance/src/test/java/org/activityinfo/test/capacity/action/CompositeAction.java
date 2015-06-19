package org.activityinfo.test.capacity.action;

import org.activityinfo.test.driver.ApiApplicationDriver;

import java.util.Arrays;

public class CompositeAction implements UserAction {
    
    private final Iterable<UserAction> actions;

    public CompositeAction(UserAction... actions) {
        this.actions = Arrays.asList(actions);
    }

    public CompositeAction(Iterable<UserAction> actions) {
        this.actions = actions;
    }

    @Override
    public void execute(ApiApplicationDriver driver) throws Exception {
        for(UserAction action : actions) {
            action.execute(driver);
        }
    }

    public Iterable<UserAction> getActions() {
        return actions;
    }

    @Override
    public String toString() {
        return actions.toString();
    }
}
