package org.activityinfo.server.command;

/*
 * #%L
 * ActivityInfo Server
 * %%
 * Copyright (C) 2009 - 2013 UNICEF
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.activityinfo.fixtures.InjectionSupport;
import org.activityinfo.fixtures.Modules;
import org.activityinfo.fixtures.TestHibernateModule;
import org.activityinfo.legacy.shared.command.*;
import org.activityinfo.legacy.shared.command.result.CreateResult;
import org.activityinfo.legacy.shared.command.result.SiteResult;
import org.activityinfo.legacy.shared.exception.CommandException;
import org.activityinfo.legacy.shared.model.LocationDTO;
import org.activityinfo.legacy.shared.model.PartnerDTO;
import org.activityinfo.legacy.shared.model.SiteDTO;
import org.activityinfo.server.database.OnDataSet;
import org.activityinfo.server.endpoint.gwtrpc.GwtRpcModule;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(InjectionSupport.class)
@Modules({
        TestHibernateModule.class,
        GwtRpcModule.class,
})
public class LocalSiteCreateTest extends LocalHandlerTestCase {

    @Test
    @OnDataSet("/dbunit/sites-simple1.db.xml")
    public void createNew() throws CommandException {

        synchronize();

        // create a new detached, client model
        SiteDTO newSite = SiteDTOs.newSite();

        LocationDTO location = LocationDTOs.newLocation();
        executeLocally(new CreateLocation(location));

        newSite.setLocation(location);
        // create command

        CreateSite cmd = new CreateSite(newSite);

        // execute the command

        CreateResult result = executeLocally(cmd);

        // let the client know the command has succeeded
        newSite.setId(result.getNewId());

        // try to retrieve what we've created FROM OUR CLIENT SIDE DATABASE

        SiteResult loadResult = executeLocally(GetSites.byId(newSite.getId()));

        Assert.assertEquals(1, loadResult.getData().size());

        SiteDTO secondRead = loadResult.getData().get(0);

        // confirm that the changes are there
        SiteDTOs.validateNewSite(secondRead);

        newRequest();

        // now Sync with the server
        synchronize();

        // Confirm that paging works client side
        GetSites pagingRequest = new GetSites();
        pagingRequest.setLimit(1);

        loadResult = executeLocally(pagingRequest);

    }

    @Test
    @OnDataSet("/dbunit/sites-simple1.db.xml")
    public void delete() throws CommandException {

        synchronize();

        executeLocally(new DeleteSite(1));

        assertThat(executeLocally(GetSites.byId(1)).getTotalLength(),
                equalTo(0));

        synchronize();

        assertThat(executeRemotely(GetSites.byId(1)).getTotalLength(),
                equalTo(0));
        assertThat(executeLocally(GetSites.byId(1)).getTotalLength(),
                equalTo(0));

    }

    @Test
    @OnDataSet("/dbunit/sites-simple1.db.xml")
    public void siteRemovePartnerConflict() {

        // FIRST U1 adds a new partner

        int databaseId = 1;

        PartnerDTO iom = new PartnerDTO();
        iom.setName("IOM");

        CreateResult result = executeRemotely(new AddPartner(databaseId, iom));
        iom.setId(result.getNewId());

        // Now U2 synchronizes, and adds a new site with this partner

        synchronize();

        SiteDTO site = new SiteDTO();
        site.setId(3343234);
        site.setActivityId(1);
        site.setPartner(iom);
        site.setDate1(new Date());
        site.setDate2(new Date());
        site.setLocationId(1);

        executeLocally(new CreateSite(site));

        // At T+3, U2 thinks better, removes IOM

        executeRemotely(new RemovePartner(databaseId, iom.getId()));

        // At T+4, U1 synchronizes, and IOM is removed, but site remains

        synchronize();

        // Verify that there is still a label for this partner

        SiteResult sites = executeLocally(GetSites.byId(site.getId()));

        assertThat(sites.getTotalLength(), equalTo(1));
        assertThat(sites.getData().get(0).getName(), equalTo(site.getName()));

    }

}
