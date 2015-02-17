package org.activityinfo.server.command;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import org.activityinfo.legacy.client.Dispatcher;
import org.activityinfo.legacy.client.remote.AbstractDispatcher;
import org.activityinfo.legacy.shared.adapter.ResourceLocatorAdaptor;
import org.activityinfo.legacy.shared.command.Command;
import org.activityinfo.legacy.shared.command.result.CommandResult;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.model.form.FormInstanceLabeler;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.promise.Promise;
import org.activityinfo.service.lookup.ReferenceChoice;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ResourceLocatorSyncImpl implements ResourceLocatorSync {

    private static final Logger LOGGER = Logger.getLogger(ResourceLocatorSyncImpl.class.getName());

    private final DispatcherSync dispatcherSync;
    private final Dispatcher dispatcherAsync;
    private final ResourceLocatorAdaptor locatorAsync;

    @Inject
    public ResourceLocatorSyncImpl(DispatcherSync dispatcher) {
        this.dispatcherSync = dispatcher;
        this.dispatcherAsync = new AsyncDispatchAdapter();
        this.locatorAsync = new ResourceLocatorAdaptor(dispatcherAsync);
    }

    @Override
    public FormClass getFormClass(ResourceId resourceId) {
        Promise<FormClass> formClass = locatorAsync.getFormClass(resourceId);
        return assertResolved(formClass, resourceId.asString());
    }

    @Override
    public FormInstance getFormInstance(ResourceId resourceId) {
        Promise<FormInstance> formClass = locatorAsync.getFormInstance(resourceId);
        return assertResolved(formClass, resourceId.asString());
    }

    @Override
    public List<ReferenceChoice> getReferenceChoices(Set<ResourceId> range) {
        List<FormInstance> instances = assertResolved(locatorAsync.queryInstances(range),
                Joiner.on(", ").join(range));

        List<ReferenceChoice> choices = Lists.newArrayList();
        for(FormInstance instance : instances) {
            choices.add(new ReferenceChoice(instance.getId(), FormInstanceLabeler.getLabel(instance)));
        }

        return choices;
    }

    @Override
    public void persist(FormInstance formInstance) {
        Promise<Void> result = locatorAsync.persist(formInstance);
        assertResolved(result, formInstance.toString());
    }

    @Override
    public void persist(FormInstance formInstance,
                        int locationId, int locationTypeId,
                        double latitude, double longitude) {
        Promise<Void> result = locatorAsync.persist(formInstance, locationId, locationTypeId, latitude, longitude);
        assertResolved(result, String.format("%s [%f,%f]", formInstance, latitude, longitude));
    }

    private <T> T assertResolved(Promise<T> result, String message) {
        if(!result.isSettled()) {
            throw new IllegalStateException("Promise is not resolved");
        }
        if(result.getState() == Promise.State.REJECTED) {
            LOGGER.log(Level.SEVERE, "Failed to persist " + message, result.getException());
            throw new RuntimeException(result.getException());
        }
        return result.get();
    }

    private class AsyncDispatchAdapter extends AbstractDispatcher {

        @Override
        public <T extends CommandResult> void execute(Command<T> command, AsyncCallback<T> callback) {
            T result;
            try {
                result = dispatcherSync.execute(command);
            } catch(Exception e) {
                callback.onFailure(e);
                return;
            }
            callback.onSuccess(result);
        }
    }
}
