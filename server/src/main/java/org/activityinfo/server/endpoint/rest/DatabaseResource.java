package org.activityinfo.server.endpoint.rest;

import liquibase.util.csv.CSVReader;
import org.activityinfo.io.xform.XFormReader;
import org.activityinfo.io.xform.form.XForm;
import org.activityinfo.legacy.shared.command.CreateEntity;
import org.activityinfo.legacy.shared.command.GetSchema;
import org.activityinfo.legacy.shared.command.UpdateFormClass;
import org.activityinfo.legacy.shared.command.result.CreateResult;
import org.activityinfo.legacy.shared.model.ActivityFormDTO;
import org.activityinfo.legacy.shared.model.DTOViews;
import org.activityinfo.legacy.shared.model.LocationTypeDTO;
import org.activityinfo.legacy.shared.model.UserDatabaseDTO;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormField;
import org.activityinfo.model.legacy.CuidAdapter;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.type.enumerated.EnumItem;
import org.activityinfo.model.type.enumerated.EnumType;
import org.activityinfo.server.command.DispatcherSync;
import org.codehaus.jackson.map.annotate.JsonView;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Logger;

import static java.util.logging.Level.SEVERE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

public class DatabaseResource {
    private static final Logger LOGGER = Logger.getLogger(DatabaseResource.class.getName());

    private final DispatcherSync dispatcher;
    private final int databaseId;

    public DatabaseResource(DispatcherSync dispatcher, int databaseId) {
        this.dispatcher = dispatcher;
        this.databaseId = databaseId;
    }

    private UserDatabaseDTO getSchema() {
        UserDatabaseDTO db = dispatcher.execute(new GetSchema()).getDatabaseById(databaseId);
        if (db == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return db;
    }

    @GET
    @Path("schema")
    @JsonView(DTOViews.Schema.class)
    @Produces(MediaType.APPLICATION_JSON)
    public UserDatabaseDTO getDatabaseSchema() {
        return getSchema();
    }


    @GET
    @Path("schema.csv")
    public Response getDatabaseSchemaCsv() {
        SchemaCsvWriter writer = new SchemaCsvWriter(dispatcher);
        writer.write(databaseId);

        return Response.ok()
                .type("text/css")
                .header("Content-Disposition", "attachment; filename=schema_" + databaseId + ".csv")
                .entity(writer.toString())
                .build();
    }


    @POST
    @Path("/forms")
    @Consumes("application/xml")
    public Response createFormFromXForm(@Context UriInfo uri, XForm xForm) {

        UserDatabaseDTO schema = getSchema();
        LocationTypeDTO locationType = schema.getCountry().getNullLocationType();

        ActivityFormDTO activityDTO = new ActivityFormDTO();
        activityDTO.setName(xForm.getHead().getTitle());
        activityDTO.set("databaseId", databaseId);
        activityDTO.set("locationTypeId", locationType.getId());

        CreateResult createResult = dispatcher.execute(new CreateEntity(activityDTO));
        int activityId = createResult.getNewId();

        XFormReader builder = new XFormReader(xForm);
        FormClass formClass = builder.build();
        formClass.setId(CuidAdapter.activityFormClass(activityId));
        formClass.setOwnerId(CuidAdapter.databaseId(databaseId));

        dispatcher.execute(new UpdateFormClass(formClass));

        return Response.created(uri.getAbsolutePathBuilder()
                .path(RootResource.class).path("forms").path(formClass.getId().asString())
                .build())
                .build();
    }

    @POST
    @Path("/forms")
    @Consumes("text/csv")
    public Response importFormClass(@Context UriInfo uri, String input) {
        final int databaseId;
        final UserDatabaseDTO schema;
        final String table[][], label, description;
        final CSVReader csvReader = new CSVReader(new StringReader(input));

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
}
