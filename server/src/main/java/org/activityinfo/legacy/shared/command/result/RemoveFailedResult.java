package org.activityinfo.legacy.shared.command.result;

import javax.ws.rs.core.Response;

// unspecific result class, can be subclassed later if needed 
@HttpStatusCode(Response.Status.CONFLICT)
public class RemoveFailedResult extends RemoveResult {
    private static final long serialVersionUID = 4876227847475729146L;

    public RemoveFailedResult() {
    }
}
