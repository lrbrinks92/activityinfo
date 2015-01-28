package org.activityinfo.server.endpoint.akvo.flow;

import com.google.inject.Inject;
import org.activityinfo.model.auth.AuthenticatedUser;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.model.resource.Resources;
import org.activityinfo.server.authentication.ServerSideAuthProvider;
import org.activityinfo.server.command.ResourceLocatorSync;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;

@Path("/tasks/persist")
public class PersistFormInstanceResource {
    private final ServerSideAuthProvider authProvider;
    private final ResourceLocatorSync locator;

    @Inject
    public PersistFormInstanceResource(ServerSideAuthProvider serverSideAuthProvider,
                                       ResourceLocatorSync resourceLocatorSync) {
        authProvider = serverSideAuthProvider;
        locator = resourceLocatorSync;
    }

    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    public void persist(@FormParam("resource") String resource, @FormParam("userId") int userId) {
        AuthenticatedUser authenticatedUser = authProvider.get();
        authProvider.set(new AuthenticatedUser(authenticatedUser.getAuthToken(), userId, authenticatedUser.getEmail()));
        locator.persist(FormInstance.fromResource(Resources.fromJson(resource)));
    }
}
