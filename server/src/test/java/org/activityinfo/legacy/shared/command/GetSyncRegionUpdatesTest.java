package org.activityinfo.legacy.shared.command;

import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;

public class GetSyncRegionUpdatesTest {

    @Test
    public void paths() {
        GetSyncRegionUpdates request = new GetSyncRegionUpdates("db/4", "16901");
        
        assertThat(request.getRegionPath(), equalTo("db/4"));
        assertThat(request.getRegionType(), equalTo("db"));
        assertThat(request.getRegionId(), equalTo(4));
        
        assertThat(request.getLocalVersion(), equalTo("16901"));
        assertThat(request.getLocalVersionNumber(), equalTo(16901L));
    }
    
    @Test
    public void singleton() {
        GetSyncRegionUpdates request = new GetSyncRegionUpdates("tables", "3");

        assertThat(request.getRegionPath(), equalTo("tables"));
        assertThat(request.getRegionType(), equalTo("tables"));
    }
    
}