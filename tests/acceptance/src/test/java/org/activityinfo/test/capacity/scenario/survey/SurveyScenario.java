package org.activityinfo.test.capacity.scenario.survey;

import org.activityinfo.test.capacity.model.Scenario;
import org.activityinfo.test.capacity.model.UserRole;

import java.util.List;

/**
 * Models a survey use case, where a few forms are setup and then 
 * completed by a large number of surveys in a short period of time.
 */
public class SurveyScenario implements Scenario {


    @Override
    public List<UserRole> getUsers() {
        return null;
    }

    @Override
    public int getDayCount() {
        return 0;
    }
}
