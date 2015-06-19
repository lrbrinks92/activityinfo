package org.activityinfo.test.capacity.scripts;

import org.activityinfo.test.capacity.model.Scenario;
import org.activityinfo.test.capacity.scenario.coordination.CoordinationScenario;

import java.util.Arrays;
import java.util.Collection;

/**
 * Simple scenario for testing the test :-)
 */
public class SingleDatabase implements CapacityTestScript {
    @Override
    public Collection<Scenario> get() {
        int databaseCount = 1;
        int partnerCount = 1;
        int usersPerPartner = 1;
        return Arrays.<Scenario>asList(new CoordinationScenario("Test", databaseCount, partnerCount, usersPerPartner));
    }
}
