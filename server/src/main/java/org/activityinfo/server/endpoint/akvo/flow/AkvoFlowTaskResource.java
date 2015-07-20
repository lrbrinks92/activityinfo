package org.activityinfo.server.endpoint.akvo.flow;

import com.google.inject.Inject;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.model.legacy.CuidAdapter;
import org.activityinfo.server.authentication.ServerSideAuthProvider;
import org.activityinfo.server.command.ResourceLocatorSync;
import org.activityinfo.server.database.hibernate.entity.User;
import org.activityinfo.service.feed.FeedService;

import javax.persistence.EntityManager;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import java.util.logging.Logger;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static org.activityinfo.model.legacy.CuidAdapter.getLegacyIdFromCuid;
import static org.activityinfo.model.resource.Resources.fromJson;

@Path("/tasks")
public class AkvoFlowTaskResource {
    
    private static final Logger LOGGER = Logger.getLogger(AkvoFlowTaskResource.class.getName());
    
    private static final int CALCULATOR_USER_ID = 8230;
    
    private final EntityManager manager;
    private final ServerSideAuthProvider authProvider;
    private final FeedService feedService;

    @Inject
    public AkvoFlowTaskResource(EntityManager entityManager,
                                ServerSideAuthProvider serverSideAuthProvider,
                                ResourceLocatorSync resourceLocatorSync) {
        manager = entityManager;
        authProvider = serverSideAuthProvider;
        feedService = new AkvoFlowFeed(CuidAdapter.activityFormClass(13365), resourceLocatorSync);
    }

    @POST
    @Path("/index")
    @Consumes(APPLICATION_FORM_URLENCODED)
    public void index(@FormParam("formClass") String formClassString, @FormParam("parameters") String parameterString) {

        LOGGER.info("Starting indexing task...");
        
        FormClass formClass = FormClass.fromResource(fromJson(formClassString));
        FormInstance parameters = FormInstance.fromResource(fromJson(parameterString));

        authenticate(formClass);

        if (parameters.getOwnerId().equals(feedService.getParameterFormClass().getId())) {
            feedService.updateFeed(formClass, parameters);
        }
    }

    @POST
    @Path("/fetch")
    @Consumes(APPLICATION_FORM_URLENCODED)
    public void fetch(@FormParam("formClass") String formClassString,
                      @FormParam("parameters") String parameterString,
                      @FormParam("id") String id,
                      @FormParam("startDate") long startDate,
                      @FormParam("endDate") long endDate) {
        FormClass formClass = FormClass.fromResource(fromJson(formClassString));
        FormInstance parameters = FormInstance.fromResource(fromJson(parameterString));

        authenticate(formClass);

        if (parameters.getOwnerId().equals(feedService.getParameterFormClass().getId())) {
            feedService.fetchInstance(formClass, parameters, id, startDate, endDate);
        }
    }

    private void authenticate(FormClass formClass) {
        User parameterClassOwner = manager.find(User.class, CALCULATOR_USER_ID);
        
        LOGGER.info("Parameter Form class owner = " + parameterClassOwner.getEmail());

        authProvider.set(parameterClassOwner);
    }
}
