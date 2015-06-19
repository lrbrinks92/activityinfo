package org.activityinfo.test.capacity.scripts;

import org.activityinfo.test.capacity.model.Scenario;
import org.activityinfo.test.capacity.scenario.coordination.CoordinationScenario;

import java.util.Arrays;
import java.util.Collection;

/**
 * Runs scenarios which specifically stress on the number of form submissions
 */
public class DatabaseStress implements CapacityTestScript {
    @Override
    public Collection<Scenario> get() {
        return Arrays.<Scenario>asList(
                countryCoordination("Lebanon"));
    }

    private CoordinationScenario countryCoordination(String name) {
        int databaseCount = 11;
        int partnerCount = 100;
        int usersPerPartner = 10;
        return new CoordinationScenario(name, databaseCount, partnerCount, usersPerPartner);
    }
}
