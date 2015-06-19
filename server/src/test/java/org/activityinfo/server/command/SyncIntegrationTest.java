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

import com.bedatadriven.rebar.sql.server.jdbc.JdbcScheduler;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.activityinfo.fixtures.InjectionSupport;
import org.activityinfo.fixtures.Modules;
import org.activityinfo.fixtures.TestHibernateModule;
import org.activityinfo.legacy.client.Dispatcher;
import org.activityinfo.legacy.shared.command.*;
import org.activityinfo.legacy.shared.command.result.MonthlyReportResult;
import org.activityinfo.legacy.shared.command.result.SiteResult;
import org.activityinfo.legacy.shared.model.AttributeDTO;
import org.activityinfo.legacy.shared.model.IndicatorRowDTO;
import org.activityinfo.legacy.shared.model.SiteDTO;
import org.activityinfo.legacy.shared.util.Collector;
import org.activityinfo.model.legacy.KeyGenerator;
import org.activityinfo.server.authentication.AuthenticationModuleStub;
import org.activityinfo.server.database.OnDataSet;
import org.activityinfo.server.database.TestSqliteDatabase;
import org.activityinfo.server.database.hibernate.entity.AdminEntity;
import org.activityinfo.server.database.hibernate.entity.Location;
import org.activityinfo.server.database.hibernate.entity.LocationType;
import org.activityinfo.server.endpoint.gwtrpc.GwtRpcModule;
import org.activityinfo.ui.client.local.LocalModuleStub;
import org.activityinfo.ui.client.local.sync.pipeline.InstallPipeline;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.EntityManager;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.activityinfo.legacy.shared.command.UpdateMonthlyReports.Change;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(InjectionSupport.class)
@Modules({
        TestHibernateModule.class,
        GwtRpcModule.class,
})
public class SyncIntegrationTest extends LocalHandlerTestCase {
    @Inject
    private KeyGenerator keyGenerator;

    @Before
    public void setupLogging() {
        Logger.getLogger("org.hibernate").setLevel(Level.ALL);
    }

    @Test
    @OnDataSet("/dbunit/sites-simple1.db.xml")
    public void run() throws SQLException, InterruptedException {
        synchronize();

        Collector<Date> lastUpdate = Collector.newCollector();
        syncHistoryTable.get(lastUpdate);

        assertThat(lastUpdate.getResult(), is(not(nullValue())));

        assertThat(
                queryString("select Name from Indicator where IndicatorId=103"),
                equalTo("Nb. of distributions"));
        assertThat(
                queryString("select Name from AdminLevel where AdminLevelId=1"),
                equalTo("Province"));
        assertThat(
                queryString("select Name from AdminEntity where AdminEntityId=21"),
                equalTo("Irumu"));
        assertThat(queryString("select Name from Location where LocationId=7"),
                equalTo("Shabunda"));
        assertThat(
                queryInt("select value from IndicatorValue where ReportingPeriodId=601 and IndicatorId=6"),
                equalTo(35));

        assertThat(
                queryInt("select PartnerId from partnerInDatabase where databaseid=2"),
                equalTo(1));

        assertThat(
                queryInt("select AttributeGroupId  from AttributeGroupInActivity where ActivityId=2"),
                equalTo(1));

        assertThat(queryInt("select count(*) from LockedPeriod"), equalTo(4));
        assertThat(
                queryInt("select count(*) from LockedPeriod where ProjectId is not null"),
                equalTo(1));
        assertThat(
                queryInt("select count(*) from LockedPeriod where ActivityId is not null"),
                equalTo(1));
        assertThat(
                queryInt("select count(*) from LockedPeriod where UserDatabaseId is not null"),
                equalTo(2));

        // / now try updating a site remotely (from another client)
        // and veryify that we get the update after we synchronized

        Thread.sleep(1000);

        SiteDTO s1 = executeLocally(GetSites.byId(1)).getData().get(0);
        assertThat((Double) s1.getIndicatorValue(1), equalTo(1500d));

        Map<String, Object> changes = Maps.newHashMap();
        changes.put(AttributeDTO.getPropertyName(1), true);
        changes.put("comments", "newComments");
        executeRemotely(new UpdateSite(1, changes));

        synchronize();

        s1 = executeLocally(GetSites.byId(1)).getData().get(0);

        assertThat(s1.getAttributeValue(1), equalTo(true));
        assertThat(s1.getAttributeValue(2), equalTo(false));
        assertThat(s1.getComments(), equalTo("newComments"));

        // old values are preserved...
        assertThat(s1.getIndicatorDoubleValue(1), equalTo(1500d));

        // Try deleting a site

        executeRemotely(new Delete("Site", 1));

        synchronize();

        SiteResult siteResult = executeLocally(GetSites.byId(1));

        assertThat(siteResult.getData().size(), equalTo(0));

        // Verify that we haven't toasted the other data

        SiteDTO site = executeLocally(GetSites.byId(3)).getData().get(0);
        assertThat(site.getIndicatorDoubleValue(1), equalTo(10000d));

    }

    @Test
    @OnDataSet("/dbunit/locations.db.xml")
    public void locationsAreChunked() throws SQLException, InterruptedException {
        addLocationsToServerDatabase(220);
        synchronize();

        assertThat(Integer.valueOf(queryString("select count(*) from Location")),
                equalTo(221));

        // update a location on the server
        serverEm.getTransaction().begin();
        Location location = (Location) serverEm.createQuery(
                "select l from Location l where l.name = 'Penekusu 26'")
                .getSingleResult();
        location.setAxe("Motown");
        location.setVersion(location.getLocationType().incrementVersion());
        serverEm.getTransaction().commit();

        newRequest();
        synchronize();

        assertThat(
                queryInt("select count(*) from Location where Name='Penekusu 26'"),
                equalTo(1));
        assertThat(
                queryString("select axe from Location where Name='Penekusu 26'"),
                equalTo("Motown"));

        // now create a new location
        Location newLocation = new Location();
        int locationId = keyGenerator.generateInt();
        newLocation.setName("Bukavu");
        newLocation.setId(123456789);
        newLocation.setLocationType(serverEm.find(LocationType.class, 1));
        newLocation.setVersion(newLocation.getLocationType().incrementVersion());
        newLocation.setId(locationId);
        serverEm.getTransaction().begin();
        serverEm.persist(newLocation);
        serverEm.getTransaction().commit();

        newRequest();

        synchronize();

        assertThat(queryString("select name from Location where LocationId = "
                        + locationId),
                equalTo("Bukavu"));
    }

    @Test
    @OnDataSet("/dbunit/sites-simple1.db.xml")
    public void testGetAdminEntities() throws SQLException, InterruptedException {
        synchronize();
        executeLocally(new GetAdminEntities(1));
    }

    @Test
    @OnDataSet("/dbunit/monthly-calc-indicators.db.xml")
    public void updateMonthlyReports() throws SQLException, InterruptedException {
        synchronize();

        int siteId = 1;

        MonthlyReportResult result = executeLocally(new GetMonthlyReports(siteId, new Month(2009, 1), 5));


        IndicatorRowDTO women = result.getData().get(0);
        IndicatorRowDTO men = result.getData().get(1);

        assertThat(women.getIndicatorName(), equalTo("women"));
        assertThat(women.getIndicatorId(), equalTo(7002));

        assertThat(men.getIndicatorName(), equalTo("men"));
        assertThat(men.getActivityName(), equalTo("NFI"));
        assertThat(men.getActivityId(), equalTo(901));
        assertThat(men.getIndicatorId(), equalTo(7001));

        assertThat(men.getValue(2009, 1), CoreMatchers.equalTo(200d));
        assertThat(women.getValue(2009, 1), equalTo(300d));

        assertThat(men.getValue(2009, 2), equalTo(150d));
        assertThat(women.getValue(2009, 2), equalTo(330d));

        // Update locally

        executeLocally(new UpdateMonthlyReports(siteId, Lists.newArrayList(
            new Change(men.getIndicatorId(), new Month(2009, 1), 221d),
            new Change(men.getIndicatorId(), new Month(2009, 3), 444d),
            new Change(women.getIndicatorId(), new Month(2009, 5), 200d),
            new Change(men.getIndicatorId(), new Month(2009, 5), 522d))));

        result = executeLocally(new GetMonthlyReports(siteId, new Month(2009, 1), 12));

        women = result.getData().get(0);
        men = result.getData().get(1);

        assertThat(men.getValue(2009, 1), equalTo(221d));
        assertThat(women.getValue(2009, 1), equalTo(300d));

        // same - no change
        assertThat(men.getValue(2009, 2), equalTo(150d));
        assertThat(women.getValue(2009, 2), equalTo(330d));

        // new periods
        assertThat(men.getValue(2009, 3), equalTo(444d));
        assertThat(women.getValue(2009, 5), equalTo(200d));
        assertThat(men.getValue(2009, 5), equalTo(522d));

        // Synchronize
        synchronize();

        newRequest();

        MonthlyReportResult remoteResult = executeRemotely(new GetMonthlyReports(siteId, new Month(2009, 1), 12));
        women = remoteResult.getData().get(0);
        men = remoteResult.getData().get(1);

        assertThat(men.getValue(2009, 1), equalTo(221d));
        assertThat(women.getValue(2009, 1), equalTo(300d));

        // same - no change
        assertThat(men.getValue(2009, 2), equalTo(150d));
        assertThat(women.getValue(2009, 2), equalTo(330d));

        // new periods
        assertThat(men.getValue(2009, 3), equalTo(444d));
        assertThat(women.getValue(2009, 5), equalTo(200d));
        assertThat(men.getValue(2009, 5), equalTo(522d));

        newRequest();

        // REmote update
        executeRemotely(new UpdateMonthlyReports(siteId, Lists.newArrayList(
                new Change(men.getIndicatorId(), new Month(2009, 1), 40d),
                new Change(women.getIndicatorId(), new Month(2009, 3), 6000d))));

        newRequest();

        synchronize();

        result = executeLocally(new GetMonthlyReports(siteId, new Month(2009, 1), 5));
        women = result.getData().get(0);
        men = result.getData().get(1);

        assertThat(men.getValue(2009, 1), CoreMatchers.equalTo(40d));
        assertThat(women.getValue(2009, 1), CoreMatchers.equalTo(300d));  // unchanged

        assertThat(women.getValue(2009, 3), equalTo(6000d));



    }
    

    // AI-864 : we know that
    // 1) on customer side location is present but locationadminlink entry is absent.
    // 2) location and locationadminlink are updated with single SyncRegion
    // Conclusion: the only possible bug is that location was updated but locationadminlink failed to update
    // due to some weird problem (network connection failure)
    // Test: in this test we will try to emulate connection failure
    @Test
    @OnDataSet("/dbunit/sites-simple-with-unicode.db.xml")
    public void failResume() throws SQLException, InterruptedException {
        String databaseName = "target/localdbtest"
                + new java.util.Date().getTime();

        final AtomicBoolean forceFail = new AtomicBoolean(true);

        final TestSqliteDatabase localDatabase = new TestSqliteDatabase(databaseName) {
            @Override
            public String adjustExecuteUpdates(String json) {
                JsonParser parser = new JsonParser();
                JsonArray list = parser.parse(json).getAsJsonArray();

                // ugly : better way to identify when to fail ?
                if (list.size() == 2 && json.contains("location") && json.contains("locationadminlink") && forceFail.get()) {
                    forceFail.set(false);
                    throw new RuntimeException("Forced to fail locationadminlink update");
                }
                return json;
            }
        };

        Dispatcher remoteDispatcher = new RemoteDispatcherStub(servlet);

        Injector clientSideInjector = Guice.createInjector(new LocalModuleStub(
                AuthenticationModuleStub.getCurrentUser(),
                localDatabase,
                remoteDispatcher));

        final InstallPipeline installer = clientSideInjector.getInstance(InstallPipeline.class);

        // sync with failure
        newRequest();
        installer.start();
        localDatabase.processEventQueue();

        // try again (now without failure)
        JdbcScheduler.get().forceCleanup();
        newRequest();
        installer.start();
        localDatabase.processEventQueue();

        assertThat(localDatabase.selectString("select Name from Location where LocationId=7"),
                equalTo("Shabunda"));

        assertThat(localDatabase.selectString(adminEntityBy(7, 1)),
                equalTo("3"));

        assertThat(localDatabase.selectString(adminEntityBy(7, 2)),
                equalTo("12"));
    }

    // AI-864, create 50k locations and try to sync them
    // Check response time (must be less than 5seconds)
    @Test
    @Ignore // we don't want to kill our build time, please run it manually
    @OnDataSet("/dbunit/sites-simple-with-unicode.db.xml")
    public void syncWithHugeLocationsCount() throws SQLException, InterruptedException {

        final TestSqliteDatabase localDatabase = new TestSqliteDatabase("target/localdbtest"
                + new java.util.Date().getTime());

        // before sync, fill in db with locations
        int generatedLocationCount = 50000;
        final List<Integer> locationIds = addLocationsToServerDatabase(generatedLocationCount);

        Dispatcher remoteDispatcher = new RemoteDispatcherStub(servlet);

        Injector clientSideInjector = Guice.createInjector(new LocalModuleStub(
                AuthenticationModuleStub.getCurrentUser(),
                localDatabase,
                remoteDispatcher));

        final InstallPipeline installer = clientSideInjector.getInstance(InstallPipeline.class);

        // sync
        newRequest();
        installer.start();
        localDatabase.processEventQueue();

        int locationCountInDataSet = 7;
        assertThat(localDatabase.selectInt("select count(*) from Location"),
                equalTo(generatedLocationCount + locationCountInDataSet));

        assertThat(localDatabase.selectString("select Name from Location where LocationId=7"),
                equalTo("Shabunda"));

        assertThat(localDatabase.selectString(adminEntityBy(7, 1)),
                equalTo("3"));

        assertThat(localDatabase.selectString(adminEntityBy(7, 2)),
                equalTo("12"));

        // assert all locations are persisted
        for (Integer id : locationIds) {
            assertThat(localDatabase.selectInt("select LocationId from Location where LocationId=" + id),
                    equalTo(id));
        }
    }

    private String adminEntityBy(int locationId, int adminLevel) {
        return "SELECT e.AdminEntityId " +
                "FROM locationadminlink k " +
                "left join adminentity e on k.AdminEntityId = e.AdminEntityId " +
                "where e.AdminLevelId=" + adminLevel +
                " and k.locationId=" + locationId;
    }

    // AI-864 : check maybe sql fails because of unicode in names (must be properly escaped)
    @Test
    @OnDataSet("/dbunit/sites-simple-with-unicode.db.xml")
    public void syncWithUnicodeInNames() throws SQLException, InterruptedException {
        synchronize();

        Collector<Date> lastUpdate = Collector.newCollector();
        syncHistoryTable.get(lastUpdate);

        assertThat(queryString("select Name from Location where LocationId=8"),
                equalTo("Shabunda_é_'_"));

        assertThat(queryInt("select adminEntityId from locationAdminLink where LocationId=8"),
                equalTo(3));

        assertThat(lastUpdate.getResult(), is(not(nullValue())));

        assertThat(
                queryString("select Name from Indicator where IndicatorId=103"),
                equalTo("Nb. of distributions"));
        assertThat(
                queryString("select Name from AdminLevel where AdminLevelId=1"),
                equalTo("Province"));
        assertThat(
                queryString("select Name from AdminEntity where AdminEntityId=21"),
                equalTo("Irumu"));
        assertThat(queryString("select Name from Location where LocationId=7"),
                equalTo("Shabunda"));
        assertThat(queryInt("select value from IndicatorValue where ReportingPeriodId=601 and IndicatorId=6"),
                equalTo(35));
    }

    private List<Integer> addLocationsToServerDatabase(int count) {

        final List<Integer> locationIds = Lists.newArrayList();

        EntityManager entityManager = serverEntityManagerFactory.createEntityManager();
        LocationType locationType = entityManager.find(LocationType.class, 1);

        entityManager.getTransaction().begin();
        for (int i = 1; i <= count; ++i) {

            Location loc = new Location();
            loc.setId(i + 10); // first 10 ids are used by data set
            loc.setVersion(locationType.incrementVersion());
            loc.setName("Penekusu " + i);
            loc.getAdminEntities().add(entityManager.getReference(AdminEntity.class, 2));
            loc.getAdminEntities().add(entityManager.getReference(AdminEntity.class, 12));
            loc.setLocationType(locationType);
            entityManager.persist(loc);
            entityManager.flush();

            locationIds.add(loc.getId());
            assertTrue(loc.getId() != 0);
            locationIds.add(loc.getId());
        }
        entityManager.getTransaction().commit();
        entityManager.close();
        return locationIds;
    }

    private String queryString(String sql) throws SQLException {
        return localDatabase.selectString(sql);
    }

    private Integer queryInt(String sql) throws SQLException {
        return localDatabase.selectInt(sql);
    }

}
