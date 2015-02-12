package org.activityinfo.server.endpoint.rest;

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

public class DatabaseResource {

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
        builder.setActivityId(activityId);
        builder.setDatabaseId(databaseId);
        FormClass formClass = builder.build();

        dispatcher.execute(new UpdateFormClass(formClass));

        return Response.created(uri.getAbsolutePathBuilder()
                .path(RootResource.class).path("forms").path(formClass.getId().asString())
                .build())
                .build();
    }
}
