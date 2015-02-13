package org.activityinfo.server.endpoint.ona;

import org.activityinfo.server.util.jaxrs.AbstractRestModule;

public class OnaModule extends AbstractRestModule {
    @Override
    protected void configureResources() {
        bindResource(OnaTaskResource.class, "/tasks/*");
    }
}
