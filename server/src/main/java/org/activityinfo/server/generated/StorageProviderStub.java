package org.activityinfo.server.generated;

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

import com.google.common.base.Preconditions;

import javax.ws.rs.core.Response;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class StorageProviderStub implements StorageProvider {

    private final String folder;
    private final Map<String, LocalGeneratedResource> tempStorageMap = new HashMap<>();

    public StorageProviderStub(String folder) {
        this.folder = folder.replace('\\', '/');
    }

    @Override
    public GeneratedResource create(String mimeType, String suffix) throws IOException {
        String id = Long.toString((new Date()).getTime());
        String path = folder + "/img" + suffix;

        LocalGeneratedResource storage = new LocalGeneratedResource(id, path);
        tempStorageMap.put(id, storage);
        return storage;
    }

    @Override
    public GeneratedResource get(String exportId) {
        return Preconditions.checkNotNull(tempStorageMap.get(exportId));
    }


    private class LocalGeneratedResource implements GeneratedResource {
        private final String id;
        private final String path;
        
        private double progress;
        public LocalGeneratedResource(String id, String path) {
            this.id = id;
            this.path = path;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public boolean isComplete() {
            return false;
        }

        @Override
        public String getDownloadUri() {
            return "file://" + path;
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            return new FileOutputStream(path);
        }

        @Override
        public void updateProgress(double percentageComplete) {
            progress = percentageComplete;
        }

        @Override
        public double getProgress() {
            return progress;
        }

        @Override
        public Response serve() throws IOException {
            throw new UnsupportedOperationException();
        }
    }
}
