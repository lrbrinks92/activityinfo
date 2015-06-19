package org.activityinfo.server.generated;

/*
 * #%L
 * ActivityInfo Server
 * %%
 * Copyright (C) 2009 - 2013 UNICEF
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.google.gson.JsonObject;
import com.google.inject.Inject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Serves up temporary files created during the rendering process. The actual
 * blob is identified by a secure hexadecimal id, but a friendly filename can be
 * appended to the uri, which is the best cross-browser way to indicate the file
 * name to be saved as. For example:
 * <p/>
 * <blockquote>
 * http://www.activityinfo.org/generated/1b391a99c2b49a2c/Untitled%20
 * Report%2020130426_0446.rtf </blockquote>
 * <p/>
 * In this URL, the text following the 1b391a99c2b49a2c/ is ignored, but used by
 * all browsers to suggest the file name to save.
 */
@Path("/generated")
public class GeneratedResources {
    
    private static final Logger LOGGER = Logger.getLogger(GeneratedResources.class.getName());

    private final StorageProvider storageProvider;

    @Inject
    public GeneratedResources(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
    }


    @GET
    @Path("status/{id}")
    public Response getStatus(@Context UriInfo uriInfo, @PathParam("id") String id) {
        try {
            GeneratedResource generatedResource = storageProvider.get(id);

            LOGGER.info("Generated Resource " + id + " completed: " + generatedResource.isComplete());

            JsonObject status = new JsonObject();
            status.addProperty("progress", generatedResource.getProgress());
            
            if(generatedResource.isComplete()) {
                status.addProperty("downloadUri", generatedResource.getDownloadUri());
            }
            
            return Response.ok(status.toString(), MediaType.APPLICATION_JSON_TYPE).build();
            
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("{id:[a-f0-9]+}/{filename}")
    public Response download(@PathParam("id") String id, @PathParam("filename") String filename) throws IOException {
        return storageProvider.get(id).serve();
    }

}
