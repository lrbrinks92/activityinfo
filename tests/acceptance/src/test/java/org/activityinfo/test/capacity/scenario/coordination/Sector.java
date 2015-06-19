package org.activityinfo.test.capacity.scenario.coordination;


import com.google.common.collect.Lists;
import org.activityinfo.test.capacity.model.UserRole;

import java.util.List;

public class Sector {
    private CoordinationScenario response;
    private String sectorName;
    
    private int activityCount = 35;
    private int indicatorCount = 40;
    
    private SectorLead sectorLead;

    public Sector(CoordinationScenario response, String sectorName) {
        this.response = response;
        this.sectorName = sectorName;
        this.sectorLead = new SectorLead(this);
    }
    
    public String getDatabaseName() {
        return sectorName;
    }
    
    public String getSectorName() {
        return sectorName;
    }

    public int getActivityCount() {
        return activityCount;
    }

    public String getActivityFormName(int i) {
        return String.format("%s Activity %d", sectorName, i);
    }
    
    public List<String> getActivityForms() {
        List<String> forms = Lists.newArrayList();
        for(int i=0;i<activityCount;++i) {
            forms.add(getActivityFormName(i));
        }
        return forms;
    }
    
    public int getIndicatorCount() {
        return indicatorCount;
    }


    public String getIndicatorName(String form, int indicatorIndex) {
        return String.format("%s Indicator %d", form, indicatorIndex);
    }

    public UserRole getSectorLead() {
        return sectorLead;
    }

    public CoordinationScenario getScenario() {
        return response;
    }

    @Override
    public String toString() {
        return sectorName;
    }

}
