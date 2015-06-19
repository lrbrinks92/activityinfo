package org.activityinfo.server.command.handler;

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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import org.activityinfo.legacy.shared.command.GetSyncRegions;
import org.activityinfo.legacy.shared.command.result.CommandResult;
import org.activityinfo.legacy.shared.command.result.SyncRegion;
import org.activityinfo.legacy.shared.command.result.SyncRegions;
import org.activityinfo.legacy.shared.exception.CommandException;
import org.activityinfo.server.command.handler.sync.AdminUpdateBuilder;
import org.activityinfo.server.command.handler.sync.TableDefinitionUpdateBuilder;
import org.activityinfo.server.database.hibernate.entity.User;
import org.activityinfo.server.database.hibernate.entity.UserDatabase;
import org.activityinfo.server.database.hibernate.entity.UserPermission;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class GetSyncRegionsHandler implements CommandHandler<GetSyncRegions> {

    private EntityManager entityManager;

    @Inject
    public GetSyncRegionsHandler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public SyncRegions execute(GetSyncRegions cmd, User user) throws CommandException {

        Set<Integer> countryIds = Sets.newHashSet();
        Set<Integer> visibleDatabaseIds = Sets.newHashSet();
        List<SyncRegion> databaseRegions = new ArrayList<>();
        
        List<UserDatabase> ownedDatabases = entityManager
            .createQuery("SELECT db FROM UserDatabase db WHERE db.owner = :user", UserDatabase.class)
            .setParameter("user", user)
            .getResultList();

        for (UserDatabase database : ownedDatabases) {
            if(!database.isDeleted()) {
                visibleDatabaseIds.add(database.getId());
                countryIds.add(database.getCountry().getId());
            }
            databaseRegions.add(new SyncRegion("db/" + database.getId(), database.getVersion()));
        }
        
        List<UserPermission> sharedDatabases = entityManager
            .createQuery("SELECT p FROM UserPermission p LEFT JOIN FETCH p.database WHERE p.user = :user",
                    UserPermission.class)
            .setParameter("user", user)
            .getResultList();
        
        for(UserPermission permission : sharedDatabases) {
            if(permission.isAllowView() && !permission.getDatabase().isDeleted()) {
                visibleDatabaseIds.add(permission.getDatabase().getId());
                countryIds.add(permission.getDatabase().getCountry().getId());
            }
            long version = Math.max(permission.getVersion(), permission.getDatabase().getVersion());
            databaseRegions.add(new SyncRegion("db/" + permission.getDatabase().getId(), version));
        }

        List<SyncRegion> regions = Lists.newArrayList();
        regions.add(new SyncRegion("site-tables", TableDefinitionUpdateBuilder.CURRENT_VERSION));

        for(Integer countryId : countryIds) {
            regions.add(new SyncRegion("country/" + countryId, "1"));
        }
        regions.addAll(databaseRegions);
        regions.addAll(listAdminRegions(countryIds));
        regions.addAll(listLocations(countryIds));
        regions.addAll(listSiteRegions(visibleDatabaseIds));
        return new SyncRegions(regions);
    }


    @SuppressWarnings("unchecked")
    private Collection<SyncRegion> listLocations(Set<Integer> countryIds) {

        List<SyncRegion> locationRegions = Lists.newArrayList();
        if (!countryIds.isEmpty()) {
            List<Tuple> locationTypes = entityManager
            .createQuery("SELECT L.id, L.version FROM LocationType L WHERE L.country.id in :countryIds",
                    Tuple.class)
            .setParameter("countryIds", countryIds)
            .getResultList();
            
            for(Tuple locationType : locationTypes) {
                locationRegions.add(new SyncRegion("location/" + locationType.get(0), locationType.get(1)));
            }
        }
        return locationRegions;
    }

    private Collection<SyncRegion> listAdminRegions(Set<Integer> countryIds) {

        List<SyncRegion> adminRegions = Lists.newArrayList();

        if (!countryIds.isEmpty()) {
            List<Integer> levels = entityManager.createQuery(
                    "SELECT level.id " +
                     "FROM AdminLevel level " +
                     "WHERE level.country.id in (:countries)", Integer.class)
                .setParameter("countries", countryIds)
                .getResultList();

            for (Integer level : levels) {
                adminRegions.add(new SyncRegion("admin/" + level, AdminUpdateBuilder.LAST_VERSION_NUMBER));
            }
        }
        return adminRegions;
    }

    /**
     * We need a separate sync region for each Partner/UserDatabase combination
     * because we may be given permission to view data at different times.
     */
    private Collection<SyncRegion> listSiteRegions(Collection<Integer> databases) {
        List<SyncRegion> siteRegions = Lists.newArrayList();
        
        if (!databases.isEmpty()) {
            List<Tuple> activities = entityManager
                    .createQuery("SELECT A.id, A.version FROM Activity A WHERE A.database.id in :databaseIds",
                            Tuple.class)
                    .setParameter("databaseIds", databases)
                    .getResultList();


            for (Tuple activity : activities) {
                siteRegions.add(new SyncRegion("form-submissions/" +  activity.get(0), activity.get(1)));
            }
        }
        return siteRegions;
    }
}
