package org.activityinfo.server.command.handler.sync;

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

import com.extjs.gxt.ui.client.data.RpcMap;
import com.google.inject.Inject;
import org.activityinfo.fixtures.InjectionSupport;
import org.activityinfo.fixtures.Modules;
import org.activityinfo.fixtures.TestHibernateModule;
import org.activityinfo.legacy.shared.command.GetSyncRegionUpdates;
import org.activityinfo.legacy.shared.command.GetSyncRegions;
import org.activityinfo.legacy.shared.command.result.SyncRegion;
import org.activityinfo.legacy.shared.command.result.SyncRegionUpdate;
import org.activityinfo.legacy.shared.command.result.SyncRegions;
import org.activityinfo.server.command.handler.GetSyncRegionsHandler;
import org.activityinfo.server.command.handler.UpdateEntityHandler;
import org.activityinfo.server.command.handler.crud.LocationTypePolicy;
import org.activityinfo.server.command.handler.crud.PropertyMap;
import org.activityinfo.server.database.OnDataSet;
import org.activityinfo.server.database.hibernate.entity.User;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

@RunWith(InjectionSupport.class)
@Modules({
        TestHibernateModule.class,
})
public class LocationBuilderTest {

    @Inject
    private EntityManagerFactory emf;

    @Test
    @OnDataSet("/dbunit/sites-simple1.db.xml")
    public void sqlBuilding() throws Exception {

        EntityManager em = emf.createEntityManager();

        int locationType = 3;
        GetSyncRegionUpdates request = new GetSyncRegionUpdates("location/" + locationType, null);

        LocationUpdateBuilder builder = new LocationUpdateBuilder(em);
        SyncRegionUpdate update = builder.build(new User(), request);

        System.out.println("sql: " + update.getSql());
        System.out.println("size: " + update.getSql().length());

        assertThat(update.getSql(), containsString("location"));
        assertThat(update.getSql(), containsString("locationadminlink"));
        assertThat(update.getSql(), containsString("Shabunda"));
        assertThat(update.getSql(), containsString("12,7")); // admin level for Shabunda

    }

    @Test
    @OnDataSet("/dbunit/sites-simple1.db.xml")
    public void cutting() throws Exception {

        EntityManager em = emf.createEntityManager();

        int chunkSize = 2;
        LocationUpdateBuilder builder = new LocationUpdateBuilder(em, chunkSize);

        GetSyncRegionUpdates request = new GetSyncRegionUpdates("location/" + 1, null);
        SyncRegionUpdate update = builder.build(new User(), request);

        assertThat(update.isComplete(), equalTo(false));
        assertThat(update.getVersion(), equalTo("2")); // first chunk
        assertThat(update.getSql(), containsString("Ngshwe"));

        request.setLocalVersion(update.getVersion());
        update = builder.build(new User(), request);

        assertThat(update.isComplete(), equalTo(true));
        assertThat(update.getVersion(), equalTo("3")); // second chunk
        assertThat(update.getSql(), containsString("Boga"));

    }


    @Test
    @OnDataSet("/dbunit/sites-simple1.db.xml")
    public void locationTypeChange() throws Exception {

        EntityManager em = emf.createEntityManager();
        
        User user = em.find(User.class, 1);
        
        // Update the location type 1
        Map<String, Object> changes = new HashMap<>();
        changes.put("name", "Ishamael");
        
        em.getTransaction().begin();
        LocationTypePolicy locationTypePolicy = new LocationTypePolicy(em);
        locationTypePolicy.update(user, 1, new PropertyMap(changes)); 
        em.getTransaction().commit();


        // First update should include this change
        String regionId = "location/" + 1;
        LocationUpdateBuilder builder = new LocationUpdateBuilder(em);
        GetSyncRegionUpdates request = new GetSyncRegionUpdates(regionId, null);
        SyncRegionUpdate update = builder.build(user, request);
        assertThat(update.isComplete(), equalTo(true));
        assertThat(update.getSql(), containsString("Ishamael"));
        
        // We should be up to date now...
        GetSyncRegionsHandler getSyncRegionsHandler = new GetSyncRegionsHandler(em);
        SyncRegions syncRegions = getSyncRegionsHandler.execute(new GetSyncRegions(), user);
        
        System.out.println(syncRegions.getList());
        
        assertThat(syncRegions, hasItem(new SyncRegion(regionId, update.getVersion())));
    }

}
