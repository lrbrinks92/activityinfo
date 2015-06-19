package org.activityinfo.legacy.shared.command;

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

import org.activityinfo.legacy.shared.command.result.SyncRegionUpdate;

public class GetSyncRegionUpdates implements Command<SyncRegionUpdate> {
    private String regionPath;
    private String localVersion;

    public GetSyncRegionUpdates() {
    }

    public GetSyncRegionUpdates(String regionId, String localVersion) {
        this.regionPath = regionId;
        this.localVersion = localVersion;
    }

    public String getRegionPath() {
        return regionPath;
    }

    public void setRegionPath(String regionPath) {
        this.regionPath = regionPath;
    }

    public String getLocalVersion() {
        return localVersion;
    }

    public long getLocalVersionNumber() {
        if (localVersion == null) {
            return 0;
        }
        int fractionStart = localVersion.indexOf('.');
        if(fractionStart != -1) {
            return Long.parseLong(localVersion.substring(0, fractionStart));
        } else {
            return Long.parseLong(localVersion);
        }
    }
    
    public String getRegionType() {
        int separator = regionPath.indexOf('/');
        if(separator == -1) {
            return regionPath;
        } else {
            return regionPath.substring(0, separator);
        }
    }
    
    public int getRegionId() {
        int separator = regionPath.indexOf('/');
        if(separator == -1) {
            throw new UnsupportedOperationException("Region " + regionPath + " has no id component");
        }

        return Integer.parseInt(regionPath.substring(separator+1));
    }
    
    public void setLocalVersion(String localVersion) {
        this.localVersion = localVersion;
    }

    @Override
    public String toString() {
        return "GetSyncRegionUpdates{" +
                "regionPath='" + regionPath + '\'' +
                ", localVersion='" + localVersion + '\'' +
                '}';
    }
}
