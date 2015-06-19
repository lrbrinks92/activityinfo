package org.activityinfo.server.util.monitoring;

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

import com.bedatadriven.appengine.metrics.MetricsRequestFilter;
import com.bedatadriven.appengine.metrics.MetricsServlet;
import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;

import java.util.logging.Logger;

public class MonitoringModule extends ServletModule {

    private static final Logger LOGGER = Logger.getLogger(MonitoringModule.class.getName());
    

    public MonitoringModule() {
    }

    @Override
    protected void configureServlets() {

        bind(MetricsRequestFilter.class).in(Singleton.class);
        bind(MetricsServlet.class).in(Singleton.class);
        
        filter("/*").through(MetricsRequestFilter.class);
        serve("/tasks/metrics").with(MetricsServlet.class);
    }
}
