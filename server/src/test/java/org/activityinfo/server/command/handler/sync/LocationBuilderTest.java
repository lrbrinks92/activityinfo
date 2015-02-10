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

import com.google.inject.Inject;
import org.activityinfo.fixtures.InjectionSupport;
import org.activityinfo.fixtures.MockHibernateModule;
import org.activityinfo.fixtures.Modules;
import org.activityinfo.legacy.shared.command.GetSyncRegionUpdates;
import org.activityinfo.legacy.shared.command.result.SyncRegionUpdate;
import org.activityinfo.server.database.OnDataSet;
import org.activityinfo.server.database.hibernate.entity.User;
import org.activityinfo.server.util.logging.LoggingModule;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(InjectionSupport.class)
@Modules({
        MockHibernateModule.class,
        LoggingModule.class
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
        assertThat(update.getVersion(), equalTo("500")); // second chunk
        assertThat(update.getSql(), containsString("Boga"));

    }

}
