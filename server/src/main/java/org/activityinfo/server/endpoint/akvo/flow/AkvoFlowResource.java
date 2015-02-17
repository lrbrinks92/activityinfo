package org.activityinfo.server.endpoint.akvo.flow;

import com.google.inject.Inject;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.server.command.ResourceLocatorSync;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import static com.google.appengine.api.taskqueue.QueueFactory.getQueue;
import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static org.activityinfo.model.resource.Resources.toJson;

@Path("/AkvoFlow")
public class AkvoFlowResource {
    private final ResourceLocatorSync locator;

    @Inject
    public AkvoFlowResource(ResourceLocatorSync resourceLocatorSync) {
        locator = resourceLocatorSync;
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
