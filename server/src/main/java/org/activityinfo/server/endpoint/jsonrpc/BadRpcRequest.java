package org.activityinfo.server.endpoint.jsonrpc;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class BadRpcRequest extends WebApplicationException {
    
    public BadRpcRequest(String message, Object... args) {
        super(Response.status(Response.Status.BAD_REQUEST).entity(String.format(message, args)).build()); 
    }

    @Override
    public String toString() {
        return getResponse().getEntity().toString();
    }
}
