package org.activityinfo.server.endpoint.akvo.flow;

import com.google.inject.Inject;
import liquibase.util.csv.CSVReader;
import org.activityinfo.legacy.shared.command.CreateEntity;
import org.activityinfo.legacy.shared.command.GetSchema;
import org.activityinfo.legacy.shared.command.UpdateFormClass;
import org.activityinfo.legacy.shared.command.result.CreateResult;
import org.activityinfo.legacy.shared.model.ActivityFormDTO;
import org.activityinfo.legacy.shared.model.LocationTypeDTO;
import org.activityinfo.legacy.shared.model.UserDatabaseDTO;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormField;
import org.activityinfo.model.legacy.CuidAdapter;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.type.enumerated.EnumItem;
import org.activityinfo.model.type.enumerated.EnumType;
import org.activityinfo.server.command.DispatcherSync;
import org.activityinfo.server.command.ResourceLocatorSync;
import org.activityinfo.server.endpoint.rest.RootResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Logger;

import static com.google.appengine.api.taskqueue.QueueFactory.getQueue;
import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;
import static java.util.logging.Level.SEVERE;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.activityinfo.model.resource.Resources.toJson;

@Path("/AkvoFlow")
public class AkvoFlowResource {
    private static final Logger LOGGER = Logger.getLogger(AkvoFlowResource.class.getName());

    private final DispatcherSync dispatcher;
    private final ResourceLocatorSync locator;

    @Inject
    public AkvoFlowResource(ResourceLocatorSync resourceLocatorSync, DispatcherSync dispatcherSync) {
        locator = resourceLocatorSync;
        dispatcher = dispatcherSync;
    }

    @POST
    @Path("/formClass")
    @Consumes(TEXT_PLAIN)
    public Response importFormClass(@Context UriInfo uri, String input) {
        final int databaseId;
        final UserDatabaseDTO schema;
        final String table[][], label, description;
        CSVReader csvReader = new CSVReader(new StringReader(input));

        try {
            table = (String[][]) csvReader.readAll().toArray(new String[0][0]);
        } catch (IOException e) {
            LOGGER.log(SEVERE, "IO exception reading CSV table", e);
            throw new WebApplicationException(e);
        } finally {
            try {
                csvReader.close();
            } catch (IOException e) {
                LOGGER.log(SEVERE, "IO exception closing CSV table", e);
            }
        }

        if (table.length < 2) {
            throw new WebApplicationException(BAD_REQUEST);
        }

        for (String[] row : table) {
            if (row.length != 3) throw new WebApplicationException(BAD_REQUEST);

            for (String field : row) {
                if (field == null) throw new WebApplicationException(BAD_REQUEST);
            }
        }

        try {
            databaseId = Integer.parseInt(table[0][0], 10);
            schema = dispatcher.execute(new GetSchema()).getDatabaseById(databaseId);
        } catch (RuntimeException runtimeException) {
            throw new WebApplicationException(runtimeException, BAD_REQUEST);
        }

        if (schema == null) {
            throw new WebApplicationException(UNAUTHORIZED);
        }

        label = table[0][1];
        description = table[0][2];

        LocationTypeDTO locationType = schema.getCountry().getNullLocationType();

        ActivityFormDTO activityFormDTO = new ActivityFormDTO();
        activityFormDTO.setName(label);
        activityFormDTO.set("databaseId", databaseId);
        activityFormDTO.set("locationTypeId", locationType.getId());

        CreateResult createResult = dispatcher.execute(new CreateEntity(activityFormDTO));
        int activityFormId = createResult.getNewId();

        FormClass formClass = new FormClass(CuidAdapter.activityFormClass(activityFormId));
        formClass.setOwnerId(CuidAdapter.databaseId(databaseId));
        formClass.setLabel(label);
        formClass.setDescription(description);

        for (int i = 1; i < table.length; i++) {
            FormField formField = formClass.addField(ResourceId.generateFieldId(EnumType.TYPE_CLASS));

            formField.setCode(table[i][0]);
            formField.setLabel(table[i][1]);

            EnumType enumType = new EnumType();
            for (String item : table[i][2].split("\n")) {
                enumType.getValues().add(new EnumItem(EnumItem.generateId(), item));
            }
            formField.setType(enumType);
        }

        dispatcher.execute(new UpdateFormClass(formClass));

        return Response.created(uri.getBaseUriBuilder()
                .path(RootResource.class).path("form").path(formClass.getId().asString())
                .build())
                .build();
    }

    @POST
    @Path("/feed")
    @Consumes(APPLICATION_FORM_URLENCODED)
    public void importFormClass(@FormParam("formClassId") ResourceId formClassId,
                                @FormParam("parameterId") ResourceId parameterId) {
        getQueue("akvo-index").add(withUrl("/tasks/index")
                .param("formClass", toJson(locator.getFormClass(formClassId)))
                .param("parameters", toJson(locator.getFormInstance(parameterId))));
    }
}
