package org.activityinfo.server.endpoint.akvo.flow;

import com.google.inject.Inject;
import org.activityinfo.model.auth.AuthenticatedUser;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.model.legacy.CuidAdapter;
import org.activityinfo.model.resource.Resources;
import org.activityinfo.server.authentication.ServerSideAuthProvider;
import org.activityinfo.server.command.ResourceLocatorSync;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

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
    public void persist(String resource) {
        FormInstance formInstance = FormInstance.fromResource(Resources.fromJson(resource));
        int userId = CuidAdapter.getLegacyIdFromCuid(locator.getFormClass(formInstance.getClassId()).getOwnerId());
        AuthenticatedUser authenticatedUser = authProvider.get();
        authProvider.set(new AuthenticatedUser(authenticatedUser.getAuthToken(), userId, authenticatedUser.getEmail()));
        locator.persist(formInstance);
    }
}
