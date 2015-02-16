package org.activityinfo.server.endpoint.rest;

import com.google.common.collect.Iterables;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.type.FieldType;
import org.activityinfo.model.type.ReferenceType;
import org.activityinfo.server.authentication.ServerSideAuthProvider;
import org.activityinfo.server.command.ResourceLocatorSync;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.io.IOException;

import static com.google.appengine.api.taskqueue.QueueFactory.getQueue;
import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;
import static org.activityinfo.model.legacy.CuidAdapter.LOCATION_FIELD;
import static org.activityinfo.model.legacy.CuidAdapter.activityFormClass;
import static org.activityinfo.model.legacy.CuidAdapter.field;
import static org.activityinfo.model.legacy.CuidAdapter.getLegacyIdFromCuid;

public class FormResource {
    private final int formClassId;
    private final int typeId;
    private final int userId;

    public FormResource(ServerSideAuthProvider serverSideAuthProvider, ResourceLocatorSync locator, int formClassId) {
        FormClass formClass = locator.getFormClass(activityFormClass(formClassId));
        FieldType fieldType = formClass.getField(field(formClass.getId(), LOCATION_FIELD)).getType();
        ResourceId locationFormClassId = Iterables.getOnlyElement(((ReferenceType) fieldType).getRange());

        this.formClassId = formClassId;
        this.typeId = getLegacyIdFromCuid(locationFormClassId);
        this.userId = serverSideAuthProvider.get().getUserId();
    }

    @POST
    @Path("/instances")
    @Consumes("application/json")
    public void createFormInstanceFromJson(String json) throws IOException {
        for (JsonNode jsonNode : new ObjectMapper().readValue(json, JsonNode[].class)) {
            getQueue("ona").add(withUrl("/tasks/persist")
                    .param("formInstance", jsonNode.toString())
                    .param("formClassId", String.valueOf(formClassId))
                    .param("typeId", String.valueOf(typeId))
                    .param("userId", String.valueOf(userId)));
        }
    }
}
