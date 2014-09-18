package org.activityinfo.ui.app.client.store;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.resource.Resource;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.system.ApplicationClassProvider;
import org.activityinfo.ui.app.client.action.RemoteUpdateHandler;
import org.activityinfo.ui.app.client.request.Request;
import org.activityinfo.ui.flux.dispatcher.Dispatcher;
import org.activityinfo.ui.flux.store.AbstractStore;
import org.activityinfo.ui.flux.store.Status;

import java.util.Map;

/**
 * The ResourceCache manages the caching and retrieval of individual
 * resource objects.
 *
 */
public class ResourceStore extends AbstractStore implements RemoteUpdateHandler {

    private ApplicationClassProvider classProvider;
    private Map<ResourceId, Status<Resource>> resources = Maps.newHashMap();


    public ResourceStore(Dispatcher dispatcher) {
        super(dispatcher);
        classProvider = new ApplicationClassProvider();
    }


    @Override
    public void requestStarted(Request request) {

    }

    @Override
    public void requestFailed(Request request, Exception e) {

    }

    @Override
    public <R> void processUpdate(Request<R> request, R response) {
        if(response instanceof Resource) {
            cache((Resource)response);
        }
    }

    private void cache(Resource response) {
        resources.put(response.getId(), Status.cache(response.copy()));
        fireChange();
    }

    public Status<Resource> get(ResourceId id) {
        Status<Resource> resource = resources.get(id);
        if(resource == null) {
            return Status.unavailable();
        } else {
            return resource;
        }
    }

    public Status<FormClass> getFormClass(ResourceId id) {

        if(classProvider.isApplicationFormClass(id)) {
            return Status.cache(classProvider.get(id));
        }

        return get(id).join(new Function<Resource, FormClass>() {
            @Override
            public FormClass apply(Resource input) {
                return FormClass.fromResource(input);
            }
        });
    }
}