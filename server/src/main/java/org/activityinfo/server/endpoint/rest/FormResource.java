package org.activityinfo.server.endpoint.rest;


import com.google.common.collect.Iterables;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.type.FieldType;
import org.activityinfo.model.type.ReferenceType;
import org.activityinfo.server.authentication.ServerSideAuthProvider;
import org.activityinfo.server.command.ResourceLocatorSync;
import org.activityinfo.server.endpoint.odk.XFormInstanceReader;
import org.codehaus.jackson.map.ObjectMapper;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.LinkedHashMap;

import static org.activityinfo.model.legacy.CuidAdapter.LOCATION_FIELD;
import static org.activityinfo.model.legacy.CuidAdapter.activityFormClass;
import static org.activityinfo.model.legacy.CuidAdapter.field;
import static org.activityinfo.model.legacy.CuidAdapter.getLegacyIdFromCuid;

public class FormResource {
    private final FormClass formClass;
    private final int typeId;
    private final int userId;

    public FormResource(ServerSideAuthProvider serverSideAuthProvider, ResourceLocatorSync locator, int id) {
        formClass = locator.getFormClass(activityFormClass(id));

        FieldType fieldType = formClass.getField(field(formClass.getId(), LOCATION_FIELD)).getType();
        ResourceId locationFormClassId = Iterables.getOnlyElement(((ReferenceType) fieldType).getRange());

        typeId = getLegacyIdFromCuid(locationFormClassId);
        userId = serverSideAuthProvider.get().getUserId();
    }

    @POST
    @Path("/instances")
    @Consumes("application/json")
    public void createFormInstanceFromJson(@Context UriInfo uri, String json) throws IOException {
        XFormInstanceReader.build(new ObjectMapper().readValue(json, LinkedHashMap[].class), formClass, typeId, userId);
    }
}
