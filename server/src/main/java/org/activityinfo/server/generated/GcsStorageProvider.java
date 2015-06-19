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

import com.google.appengine.api.appidentity.AppIdentityService;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.activityinfo.model.auth.AuthenticatedUser;
import org.activityinfo.server.database.hibernate.entity.Domain;
import org.activityinfo.service.DeploymentConfiguration;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.logging.Logger;

public class GcsStorageProvider implements StorageProvider {

    private static final Logger LOGGER = Logger.getLogger(GcsStorageProvider.class.getName());
    
    public static final String BUCKET_PROPERTY = "generated.resources.bucket";
    
    private final SecureRandom random = new SecureRandom();
    private Provider<Domain> domainProvider;
    private final Provider<AuthenticatedUser> authProvider;

    private final String bucket;


    @Inject
    public GcsStorageProvider(Provider<Domain> domainProvider,
                              Provider<AuthenticatedUser> authProvider,
                              DeploymentConfiguration deploymentConfiguration) {
        this.domainProvider = domainProvider;
        this.authProvider = authProvider;

        if (deploymentConfiguration.hasProperty(BUCKET_PROPERTY)) {
            this.bucket = deploymentConfiguration.getProperty(BUCKET_PROPERTY);
        } else {
            AppIdentityService appIdentityService = AppIdentityServiceFactory.getAppIdentityService();
            this.bucket = appIdentityService.getDefaultGcsBucketName();
        }
    }

    @Override
    public GeneratedResource create(String mimeType, String filename) throws IOException {

        String id = Long.toString(Math.abs(random.nextLong()), 16);
        GcsGeneratedMetadata metadata = new GcsGeneratedMetadata(id);
        metadata.setContentType(mimeType);
        metadata.setFilename(filename);
        metadata.setOwner(authProvider.get());
        metadata.save();
        
        LOGGER.info("Created GeneratedResource " + id + " for user " + authProvider.get().getEmail());

        return new GcsGeneratedResource(domainProvider.get(), bucket, metadata);
    }

    @Override
    public GeneratedResource get(String exportId) {
        GcsGeneratedMetadata metadata;
        try {
            metadata = GcsGeneratedMetadata.load(exportId);
        } catch (EntityNotFoundException e) {
            LOGGER.info("Generated Resource " + exportId + " not found at " + e.getKey());

            throw new IllegalArgumentException("GeneratedResource not found: " + e.getKey());
        }

        return new GcsGeneratedResource(domainProvider.get(), bucket, metadata);
    }
}
    
    