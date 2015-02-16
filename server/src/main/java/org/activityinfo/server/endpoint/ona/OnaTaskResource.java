package org.activityinfo.server.endpoint.ona;

import com.google.inject.Inject;
import org.activityinfo.model.auth.AuthenticatedUser;
import org.activityinfo.server.authentication.ServerSideAuthProvider;
import org.activityinfo.server.command.DispatcherSync;
import org.activityinfo.server.command.ResourceLocatorSync;
import org.activityinfo.service.guid.SiteIdGuidService;
import org.codehaus.jackson.map.ObjectMapper;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.io.IOException;
import java.util.LinkedHashMap;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;

@Path("/tasks")
public class OnaTaskResource {
    private final ServerSideAuthProvider authProvider;
    private final XFormInstanceReader xFormInstanceReader;

    @Inject
    public OnaTaskResource(ServerSideAuthProvider serverSideAuthProvider,
                           DispatcherSync dispatcher,
                           ResourceLocatorSync locator,
                           SiteIdGuidService siteIdGuidService) {
        authProvider = serverSideAuthProvider;
        xFormInstanceReader = new XFormInstanceReader(dispatcher, locator, siteIdGuidService);
    }

    @POST
    @Path("/persist")
    @Consumes(APPLICATION_FORM_URLENCODED)
    public void persist(@FormParam("formInstance") String formInstance,
                        @FormParam("formClassId") Integer formClassId,
                        @FormParam("typeId") Integer typeId,
                        @FormParam("userId") Integer userId) throws IOException {
        authProvider.set(new AuthenticatedUser("", userId, "@"));
        xFormInstanceReader.build(new ObjectMapper().readValue(formInstance, LinkedHashMap.class), formClassId, typeId);
    }
}
