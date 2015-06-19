package org.activityinfo.server.report;

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

import com.google.common.io.ByteStreams;
import org.activityinfo.server.generated.GeneratedResource;
import org.activityinfo.server.generated.StorageProvider;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.OutputStream;

public class NullStorageProvider implements StorageProvider {

    @Override
    public GeneratedResource create(String mimeType, String suffix)  throws IOException {
        return newNullExport();
    }

    @Override
    public GeneratedResource get(String exportId) {
        return newNullExport();
    }
    

    private GeneratedResource newNullExport() {
        return new GeneratedResource() {
            
            private double progress = 0;

            @Override
            public String getId() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isComplete() {
                return false;
            }

            @Override
            public String getDownloadUri() {
                return "http://";
            }

            @Override
            public OutputStream openOutputStream() throws IOException {
                return ByteStreams.nullOutputStream();
            }

            @Override
            public void updateProgress(double percentageComplete) {
                progress = 0;   
            }

            @Override
            public double getProgress() {
                return progress;
            }

            @Override
            public Response serve() throws IOException {
                throw new UnsupportedOperationException();
            }
        };
    }

}
