package org.activityinfo.server.endpoint.rest;


import com.google.api.client.util.Maps;
import com.google.common.collect.Iterables;
import org.activityinfo.legacy.shared.command.CreateLocation;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.type.FieldType;
import org.activityinfo.model.type.ReferenceType;
import org.activityinfo.server.command.DispatcherSync;
import org.activityinfo.server.command.ResourceLocatorSync;
import org.activityinfo.server.command.ResourceLocatorSyncImpl;
import org.activityinfo.server.endpoint.odk.InstanceIdService;
import org.activityinfo.server.endpoint.odk.XFormInstanceReader;
import org.activityinfo.server.endpoint.odk.XFormInstanceReader.MetadataPersister;
import org.activityinfo.server.endpoint.odk.XFormInstanceReader.MetadataPersister.Metadata;
import org.codehaus.jackson.map.ObjectMapper;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.CREATED;
import static org.activityinfo.model.legacy.CuidAdapter.LOCATION_FIELD;
import static org.activityinfo.model.legacy.CuidAdapter.activityFormClass;
import static org.activityinfo.model.legacy.CuidAdapter.field;
import static org.activityinfo.model.legacy.CuidAdapter.getLegacyIdFromCuid;

public class FormResource {
    private final DispatcherSync dispatcherSync;
    private final ResourceLocatorSync locator;
    private final FormClass formClass;
    private final int typeId;
    private final InstanceIdService instanceIdService;

    public FormResource(DispatcherSync dispatcher, int id, InstanceIdService idService) {
        dispatcherSync = dispatcher;
        locator = new ResourceLocatorSyncImpl(dispatcherSync);
        formClass = locator.getFormClass(activityFormClass(id));

        FieldType fieldType = formClass.getField(field(formClass.getId(), LOCATION_FIELD)).getType();
        ResourceId locationFormClassId = Iterables.getOnlyElement(((ReferenceType) fieldType).getRange());

        typeId = getLegacyIdFromCuid(locationFormClassId);
        instanceIdService = idService;
    }

    @POST
    @Path("/instances")
    @Consumes("application/json")
    public Response createFormInstanceFromJson(@Context UriInfo uri, String json) throws IOException {
        final Map<ResourceId, Metadata> map = Maps.newHashMap();
        final MetadataPersister metadataPersister = new MetadataPersister() {
            @Override
            public void persist(ResourceId formInstanceId, Metadata metadata) {
                map.put(formInstanceId, metadata);
            }
        };
        final LinkedHashMap<String, Object> array[] = new ObjectMapper().readValue(json, LinkedHashMap[].class);

        for (FormInstance formInstance : new XFormInstanceReader(array, formClass, metadataPersister, typeId).build()) {
            final Metadata metadata = map.get(formInstance.getId());
            final String instanceId = metadata.instanceId;
            final CreateLocation createLocation = metadata.createLocation;

            if (!instanceIdService.exists(instanceId)) {
                locator.persist(formInstance);

                if (createLocation == null) {
                    if (formInstance.get(field(formClass.getId(), LOCATION_FIELD)) != null) {
                        throw new IllegalStateException("No location created, but field was set");
                    }
                } else {
                    dispatcherSync.execute(createLocation);
                }

                instanceIdService.submit(instanceId);
            }
        }

        return Response.status(CREATED).build();
    }
}
