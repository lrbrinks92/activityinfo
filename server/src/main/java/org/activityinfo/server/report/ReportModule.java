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

import com.google.inject.Singleton;
import org.activityinfo.server.endpoint.gwtrpc.MapIconServlet;
import org.activityinfo.server.generated.GcsStorageProvider;
import org.activityinfo.server.generated.GeneratedResources;
import org.activityinfo.server.generated.StorageProvider;
import org.activityinfo.server.report.generator.MapIconPath;
import org.activityinfo.server.schedule.ReportMailerServlet;
import org.activityinfo.server.util.jaxrs.AbstractRestModule;

public class ReportModule extends AbstractRestModule {

    public ReportModule() {
        super();
    }

    @Override
    protected void configureResources() {
        bind(StorageProvider.class).to(GcsStorageProvider.class);
        bind(String.class).annotatedWith(MapIconPath.class).toProvider(MapIconPathProvider.class).in(Singleton.class);
        
        bindResource(GeneratedResources.class);

        serve("/icon*").with(MapIconServlet.class);
        serve("/tasks/mailSubscriptions").with(ReportMailerServlet.class);
    }
}
