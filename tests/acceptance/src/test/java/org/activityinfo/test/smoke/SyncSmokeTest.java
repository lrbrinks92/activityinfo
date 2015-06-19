package org.activityinfo.test.smoke;

import org.activityinfo.test.pageobject.web.ApplicationPage;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * Tests synchronization against a set of live accounts prior to going
 * live with a deployment
 */
public class SyncSmokeTest {
    
    @Rule
    public SmokeTestContext context = new SmokeTestContext();
    
    @Test
    public void initialSync() {
        ApplicationPage applicationPage = context.login();
        applicationPage.openSettingsMenu().enableOfflineMode();
        applicationPage.assertOfflineModeLoads(15, TimeUnit.MINUTES);
    }
}
