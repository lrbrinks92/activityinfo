package org.activityinfo.server.endpoint.ona;

import com.google.api.client.util.Maps;
import com.google.inject.Inject;
import org.activityinfo.legacy.shared.command.CreateLocation;
import org.activityinfo.model.auth.AuthenticatedUser;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.server.authentication.ServerSideAuthProvider;
import org.activityinfo.server.command.DispatcherSync;
import org.activityinfo.server.command.ResourceLocatorSync;
import org.activityinfo.server.command.ResourceLocatorSyncImpl;
import org.activityinfo.server.endpoint.odk.InstanceIdService;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static org.activityinfo.model.legacy.CuidAdapter.LOCATION_FIELD;
import static org.activityinfo.model.legacy.CuidAdapter.field;
import static org.activityinfo.model.resource.Resources.fromJson;

@Path("/tasks")
public class OnaTaskResource {
    private final ServerSideAuthProvider authProvider;
    private final DispatcherSync dispatcher;
    private final ResourceLocatorSync locator;
    private final InstanceIdService idService;

    @Inject
    public OnaTaskResource(ServerSideAuthProvider serverSideAuthProvider,
                           DispatcherSync dispatcherSync,
                           InstanceIdService instanceIdService) {
        authProvider = serverSideAuthProvider;
        dispatcher = dispatcherSync;
        locator = new ResourceLocatorSyncImpl(dispatcherSync);
        idService = instanceIdService;
    }

    @POST
    @Path("/persist")
    @Consumes(APPLICATION_FORM_URLENCODED)
    public void persist(@FormParam("formInstance") String formInstanceString,
                        @FormParam("instanceId") String instanceId,
                        @FormParam("userId") Integer userId,
                        @FormParam("locationId") Integer locationId,
                        @FormParam("locationTypeId") Integer locationTypeId,
                        @FormParam("latitude") Double latitude,
                        @FormParam("longitude") Double longitude) {
        authProvider.set(new AuthenticatedUser("", userId, "@"));

        final FormInstance formInstance = FormInstance.fromResource(fromJson(formInstanceString));
        final ResourceId formClassId = formInstance.getClassId();
        final CreateLocation createLocation = createLocation(locationId, locationTypeId, latitude, longitude);

        if (!idService.exists(instanceId)) {
            locator.persist(formInstance);

            if (createLocation == null) {
                if (formInstance.get(field(formClassId, LOCATION_FIELD)) != null) {
                    throw new IllegalStateException("No location created, but field was set");
                }
            } else {
                dispatcher.execute(createLocation);
            }

            idService.submit(instanceId);
        }
    }

    private CreateLocation createLocation(Integer id, Integer locationTypeId, Double latitude, Double longitude) {
        if (id != null && locationTypeId != null && latitude != null && longitude != null) {
            Map<String, Object> properties = Maps.newHashMap();

            properties.put("id", id);
            properties.put("locationTypeId", locationTypeId);
            properties.put("name", "Custom location");
            properties.put("latitude", latitude);
            properties.put("longitude", longitude);

            return new CreateLocation(properties);
        } else {
            return null;
        }
    }
}
