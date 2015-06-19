package org.activityinfo.server.endpoint.jsonrpc;

import org.activityinfo.server.util.jaxrs.AbstractRestModule;

public class JsonRpcModule extends AbstractRestModule {

    @Override
    protected void configureResources() {
        bindResource(JsonRpcServlet.class);
    }
}
