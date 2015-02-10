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

import com.bedatadriven.rebar.sql.client.query.SqlQuery;
import com.google.inject.Inject;
import org.activityinfo.legacy.shared.command.GetSyncRegionUpdates;
import org.activityinfo.legacy.shared.command.result.SyncRegionUpdate;
import org.activityinfo.legacy.shared.impl.Tables;
import org.activityinfo.server.database.hibernate.entity.Location;
import org.activityinfo.server.database.hibernate.entity.User;
import org.json.JSONException;

import javax.persistence.EntityManager;
import java.util.Collections;
import java.util.List;

public class LocationUpdateBuilder implements UpdateBuilder {

    private static final int DEFAULT_CHUNK_SIZE = 50; // items in chunk
    private static final String REGION_PREFIX = "location/";

    private final EntityManager em;
    private LocalState localState;
    private SqliteBatchBuilder batch;

    private int typeId;
    private int chunkSize;

    @Inject
    public LocationUpdateBuilder(EntityManager em) {
        this(em, DEFAULT_CHUNK_SIZE);
    }

    public LocationUpdateBuilder(EntityManager em, int chunkSize) {
        this.em = em;
        this.chunkSize = chunkSize;
    }

    @Override
    public SyncRegionUpdate build(User user, GetSyncRegionUpdates request) throws Exception {

        typeId = parseTypeId(request);
        localState = new LocalState(request.getLocalVersion());
        batch = new SqliteBatchBuilder();

        SyncRegionUpdate update = new SyncRegionUpdate();
        long latestVersion = queryLatestVersion(update);
        if (latestVersion > localState.lastDate) {
            queryChanged();
            linkAdminEntities();
        }

        update.setVersion(Long.toString(latestVersion));
        update.setSql(batch.build());
        return update;
    }

    private int parseTypeId(GetSyncRegionUpdates request) {
        if (!request.getRegionId().startsWith(REGION_PREFIX)) {
            throw new AssertionError("Expected region prefixed by '" + REGION_PREFIX +
                                     "', got '" + request.getRegionId() + "'");
        }
        return Integer.parseInt(request.getRegionId().substring(REGION_PREFIX.length()));
    }

    private void queryChanged() {

        SqlQuery query = SqlQuery.select()
                                 .appendColumn("LocationId")
                                 .appendColumn("Name")
                                 .appendColumn("Axe")
                                 .appendColumn("X")
                                 .appendColumn("Y")
                                 .appendColumn("LocationTypeId")
                                 .appendColumn("workflowStatusId")
                                 .from(Tables.LOCATION)
                                 .where("locationTypeId")
                                 .equalTo(typeId)
                                 .where("timeEdited")
                                 .greaterThan(localState.lastDate);

        batch.insert().into(Tables.LOCATION).from(query).execute(em);
    }

    private void linkAdminEntities() throws JSONException {

        SqlQuery query = SqlQuery.select()
                                 .appendColumn("K.AdminEntityId")
                                 .appendColumn("K.LocationId")
                                 .from(Tables.LOCATION, "L")
                                 .innerJoin(Tables.LOCATION_ADMIN_LINK, "K")
                                 .on("L.LocationId=K.LocationId")
                                 .where("L.locationTypeId")
                                 .equalTo(typeId)
                                 .where("L.timeEdited")
                                 .greaterThan(localState.lastDate);

        batch.insert().into(Tables.LOCATION_ADMIN_LINK).from(query).execute(em);
    }

    private long queryLatestVersion(SyncRegionUpdate update) throws JSONException {
        SqlQuery query = SqlQuery.select()
                                 .appendColumn("timeEdited", "latest")
                                 .from(Tables.LOCATION)
                                 .where("locationTypeId")
                                 .equalTo(typeId)
                                 .where("timeEdited")
                                 .greaterThan(localState.lastDate);

        List<Long> longs = SqlQueryUtil.queryLongList(em, query);

        if (longs.isEmpty()) {
            update.setComplete(true);
            return localState.lastDate;
        }

        // our intention is to reduce batch, so we cut versions into chunks
        if (longs.size() > chunkSize) {
            longs = longs.subList(0, chunkSize);
            update.setComplete(false);
        } else {
            update.setComplete(true);
        }
        return Collections.max(longs);
    }


    private class LocalState {
        private long lastDate;

        public LocalState(Location lastLocation) {
            lastDate = lastLocation.getTimeEdited();
        }

        public LocalState(String cookie) {
            if (cookie == null) {
                lastDate = 0;
            } else {
                lastDate = TimestampHelper.fromString(cookie);
            }
        }

        public String toVersionString() {
            return TimestampHelper.toString(lastDate);
        }
    }
}
