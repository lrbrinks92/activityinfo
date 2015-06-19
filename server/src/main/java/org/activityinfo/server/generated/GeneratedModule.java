package org.activityinfo.server.generated;

import org.activityinfo.server.util.jaxrs.AbstractRestModule;

/**
 * Provides services and storage for generated resources
 */
public class GeneratedModule extends AbstractRestModule {
    @Override
    protected void configureResources() {
        bindResource(GeneratedResources.class);
        bind(StorageProvider.class).to(GcsStorageProvider.class);
    }
}
