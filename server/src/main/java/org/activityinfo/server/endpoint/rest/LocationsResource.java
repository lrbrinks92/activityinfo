package org.activityinfo.server.endpoint.rest;

import com.sun.jersey.api.core.InjectParam;
import org.activityinfo.model.legacy.KeyGenerator;
import org.activityinfo.legacy.shared.command.GetLocations;
import org.activityinfo.legacy.shared.command.result.LocationResult;
import org.activityinfo.legacy.shared.model.AdminEntityDTO;
import org.activityinfo.legacy.shared.model.LocationDTO;
import org.activityinfo.server.command.DispatcherSync;
import org.activityinfo.server.database.hibernate.entity.AdminEntity;
import org.activityinfo.server.database.hibernate.entity.Location;
import org.activityinfo.server.database.hibernate.entity.LocationType;
import org.activityinfo.server.endpoint.rest.model.NewLocation;
import org.activityinfo.server.util.monitoring.Timed;
import org.codehaus.jackson.JsonGenerator;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

public class LocationsResource {

    private DispatcherSync dispatcher;

    public LocationsResource(DispatcherSync dispatcher) {
        this.dispatcher = dispatcher;
    }

    @GET 
    @Timed(name = "api.rest.locations.get")
    @Produces(MediaType.APPLICATION_JSON)
    public Response query(@QueryParam("type") int typeId) throws IOException {

        GetLocations query = new GetLocations();
        query.setLocationTypeId(typeId);

        LocationResult result = dispatcher.execute(query);


        StringWriter writer = new StringWriter();
        JsonGenerator json = Jackson.createJsonFactory(writer);

        json.writeStartArray();
        for (LocationDTO location : result.getData()) {
            json.writeStartObject();
            json.writeNumberField("id", location.getId());
            json.writeStringField("name", location.getName());
            if (location.hasAxe()) {
                json.writeStringField("code", location.getAxe());
            }
            if (location.hasCoordinates()) {
                json.writeNumberField("latitude", location.getLatitude());
                json.writeNumberField("longitude", location.getLongitude());
            }
            if(!location.getAdminEntities().isEmpty()) {
                json.writeObjectFieldStart("adminEntities");
                for (AdminEntityDTO entity : location.getAdminEntities()) {
                    json.writeFieldName(Integer.toString(entity.getLevelId()));
                    json.writeStartObject();
                    json.writeNumberField("id", entity.getId());
                    json.writeStringField("name", entity.getName());
                    json.writeEndObject();
                }
                json.writeEndObject();
            }
            json.writeEndObject();
        }
        json.writeEndArray();
        json.close();

        return Response.ok(writer.toString()).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    @POST 
    @Path("/{typeId}")
    @Timed(name = "api.rest.locations.post")
    public Response postNewLocations(@InjectParam EntityManager entityManager,
                                     @PathParam("typeId") int locationTypeId,
                                     List<NewLocation> locations) {

        KeyGenerator generator = new KeyGenerator();

        entityManager.getTransaction().begin();

        LocationType locationType = entityManager.getReference(LocationType.class, locationTypeId);
        for (NewLocation newLocation : locations) {

            Location location = new Location();
            location.setId(generator.generateInt());

            System.out.println(location.getId());

            location.setName(newLocation.getName());
            location.setLocationType(locationType);
            location.setX(newLocation.getLongitude());
            location.setY(newLocation.getLatitude());
            location.setTimeEdited(new Date());
            location.setAdminEntities(new HashSet<AdminEntity>());
            for (int entityId : newLocation.getAdminEntityIds()) {
                location.getAdminEntities().add(entityManager.getReference(AdminEntity.class, entityId));
            }

            entityManager.persist(location);
        }

        entityManager.getTransaction().commit();

        return Response.ok().build();
    }
}
    
