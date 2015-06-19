package org.activityinfo.server.mail;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import org.activityinfo.service.DeploymentConfiguration;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.logging.Logger;

@Path("/bounceHook")
public class BounceHook {

    private static final Logger LOGGER = Logger.getLogger(BounceHook.class.getName());

    private final String bounceHookToken;

    @Inject
    public BounceHook(DeploymentConfiguration config) {
        bounceHookToken = config.getProperty("postmark.bouncehook.key");
    }

    @POST 
    @Path("/{token}") 
    @Consumes(MediaType.APPLICATION_JSON)
    public Response bounced(@PathParam("token") String token, Bounce bounce) {

        if (Strings.isNullOrEmpty(bounceHookToken) || !bounceHookToken.equals(token)) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }

        LOGGER.info("Subject = " + bounce.getSubject());
        LOGGER.info("Email = " + bounce.getEmail());
        LOGGER.info("Type = " + bounce.getType());
        LOGGER.info("Token = " + token);

        return Response.ok().build();
    }
}
