package org.activityinfo.server;

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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import org.activityinfo.server.attachment.AttachmentModule;
import org.activityinfo.server.authentication.AuthenticationModule;
import org.activityinfo.server.branding.BrandingModule;
import org.activityinfo.server.database.ServerDatabaseModule;
import org.activityinfo.server.database.hibernate.HibernateModule;
import org.activityinfo.server.digest.DigestModule;
import org.activityinfo.server.endpoint.akvo.flow.AkvoFlowModule;
import org.activityinfo.server.endpoint.content.ContentModule;
import org.activityinfo.server.endpoint.export.ExportModule;
import org.activityinfo.server.endpoint.gwtrpc.GwtRpcModule;
import org.activityinfo.server.endpoint.jsonrpc.JsonRpcModule;
import org.activityinfo.server.endpoint.kml.KmlModule;
import org.activityinfo.server.endpoint.odk.OdkModule;
import org.activityinfo.server.endpoint.rest.RestApiModule;
import org.activityinfo.server.event.EventModule;
import org.activityinfo.server.geo.GeometryModule;
import org.activityinfo.server.login.LoginModule;
import org.activityinfo.server.mail.MailModule;
import org.activityinfo.server.report.ReportModule;
import org.activityinfo.server.util.TemplateModule;
import org.activityinfo.server.util.blob.BlobServiceModule;
import org.activityinfo.server.util.config.ConfigModule;
import org.activityinfo.server.util.jaxrs.JaxRsModule;
import org.activityinfo.server.util.locale.LocaleModule;
import org.activityinfo.server.util.logging.LoggingModule;
import org.activityinfo.service.blob.GcsBlobFieldStorageServiceModule;

import javax.servlet.ServletContextEvent;
import java.util.logging.Logger;

/**
 * A Servlet context listener that initializes the Dependency Injection
 * Framework (Guice) upon startup.
 */
public class StartupListener extends GuiceServletContextListener {

    private static Logger logger = Logger.getLogger(StartupListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        logger.info("ActivityInfo servlet context is initializing");
        super.contextInitialized(servletContextEvent);

    }

    @Override
    protected Injector getInjector() {

        return Guice.createInjector(new HibernateModule(),
                new ConfigModule(),
                new LoggingModule(),
                new TemplateModule(),
                new MailModule(),
                new ServerDatabaseModule(),
                new ContentModule(),
                new GeometryModule(),
                new AuthenticationModule(),
                new AttachmentModule(),
                new ReportModule(),
                new EventModule(),
                new DigestModule(),
                new LoginModule(),
                new GwtRpcModule(),
                new JsonRpcModule(),
                new ExportModule(),
                new KmlModule(),
                new BrandingModule(),
                new BlobServiceModule(),
                new LocaleModule(),
                new JaxRsModule(),
                new RestApiModule(),
                new OdkModule(),
                new AkvoFlowModule(),
                new GcsBlobFieldStorageServiceModule());
    }

}