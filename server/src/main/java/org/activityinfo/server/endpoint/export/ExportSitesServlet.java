package org.activityinfo.server.endpoint.export;

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

import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.activityinfo.model.auth.AuthenticatedUser;
import org.activityinfo.server.generated.GeneratedResource;
import org.activityinfo.server.generated.StorageProvider;

import javax.inject.Provider;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Exports complete data to an Excel file
 *
 * @author Alex Bertram
 */
@Singleton
public class ExportSitesServlet extends HttpServlet {
    private final Provider<AuthenticatedUser> authenticatedUserProvider;
    private final StorageProvider storageProvider;

    private static final Logger LOGGER = Logger.getLogger(ExportSitesServlet.class.getName());
    
    @Inject
    public ExportSitesServlet(Provider<AuthenticatedUser> authenticatedUserProvider, StorageProvider storageProvider) {
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.storageProvider = storageProvider;
    }

    /**
     * Initiates an export to Excel task. A token is send back to the client as plain text
     * that can be use to poll the status of the export and retrieve the result.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // Create a unique key from which the user can retrieve the file from GCS
        GeneratedResource export = storageProvider.create("application/vnd.ms-excel", fileName());

        TaskOptions options = TaskOptions.Builder.withUrl(ExportSitesTask.END_POINT);
        for(Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
            options.param(entry.getKey(), entry.getValue()[0]);
        }
        options.param("userId", Integer.toString(authenticatedUserProvider.get().getId()));
        options.param("userEmail", authenticatedUserProvider.get().getEmail());
        options.param("exportId", export.getId());
        options.param("filename", fileName());
        options.retryOptions(RetryOptions.Builder.withTaskRetryLimit(3));

        QueueFactory.getDefaultQueue().add(options);

        LOGGER.info("Enqueued export with id " + export.getId() + " on behalf of " +
            authenticatedUserProvider.get().getEmail());
        
        resp.setStatus(HttpServletResponse.SC_ACCEPTED);
        resp.getOutputStream().print(export.getId());
    }

    private String fileName() {
        String date = new SimpleDateFormat("YYYY-MM-dd_HHmmss").format(new Date());
        return ("ActivityInfo_Export_" + date + ".xls").replace(" ", "_");
    }

}