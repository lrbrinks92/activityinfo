package org.activityinfo.server.endpoint.rest;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import org.activityinfo.server.DeploymentEnvironment;
import org.activityinfo.server.database.hibernate.entity.User;
import org.activityinfo.service.DeploymentConfiguration;

import javax.inject.Provider;
import javax.persistence.EntityManager;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;

/**
 * Allows creation of user accounts through the REST API
 *
 * <p>Currently only supported for debugging purposes, but could also be used to
 * sync external directory systems.</p>
 */
public class UsersResource {

    public static final String USER_API_ENABLED = "user.api.enabled";

    public static class NewUser {
        private String email;
        private String name;
        private String password;
        private String locale;
    }

    private DeploymentConfiguration deploymentConfiguration;
    private Provider<EntityManager> entityManager;

    public UsersResource(DeploymentConfiguration deploymentConfiguration, Provider<EntityManager> entityManager) {
        this.deploymentConfiguration = deploymentConfiguration;
        this.entityManager = entityManager;

    }

    private boolean isApiEnabled() {
        return "true".equals(deploymentConfiguration.getProperty(USER_API_ENABLED));
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createUser(NewUser newUser) {

        if(DeploymentEnvironment.isAppEngineDevelopment() && !isApiEnabled()) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("User API is disabled.").build();
        }

        if(Strings.isNullOrEmpty(newUser.email) ||
            Strings.isNullOrEmpty(newUser.name) ||
            Strings.isNullOrEmpty(newUser.password)) {
           
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("The email, name, and password properties are required.")
                    .build();
        }

        User user = new User();
        user.setDateCreated(new Date());
        user.setEmail(newUser.email);
        user.setEmailNotification(false);
        user.changePassword(newUser.password);
        
        if(Strings.isNullOrEmpty(newUser.locale)) {
            user.setLocale("en");
        } else if("en".equals(newUser.locale) || "fr".equals(newUser.locale)) {
            user.setLocale(newUser.locale);
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid locale").build();
        }
        
        entityManager.get().persist(user);
        
        return Response.status(Response.Status.CREATED).build();
    }
}
