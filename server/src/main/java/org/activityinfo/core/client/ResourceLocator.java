package org.activityinfo.core.client;


import org.activityinfo.core.shared.Projection;
import org.activityinfo.core.shared.criteria.Criteria;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.model.resource.IsResource;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.promise.Promise;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ResourceLocator {

    /**
     * Fetches the user form.
     *
     * @param formId
     * @return
     */
    Promise<FormClass> getFormClass(ResourceId formId);

    Promise<FormInstance> getFormInstance(ResourceId formId);

    /**
     * Persists a resource to the server, creating or updating as necessary.
     *
     * @param resource the resource to persist.
     * @return a Promise that resolves when the persistance operation completes
     * successfully.
     */
    Promise<Void> persist(IsResource resource);

    Promise<Void> persist(FormInstance formInstance,
                          int locationId, int locationTypeId,
                          double latitude, double longitude);

    Promise<Void> persist(List<? extends IsResource> resources);

    /**
     * Retrieves the form instances that match the given criteria.
     * @param criteria
     */
    Promise<List<FormInstance>> queryInstances(Criteria criteria);

    Promise<QueryResult<Projection>> queryProjection(InstanceQuery query);

    Promise<List<Projection>> query(InstanceQuery query);

    Promise<Void> remove(Collection<ResourceId> resources);

    Promise<List<FormInstance>> queryInstances(Set<ResourceId> resourceIds);
}
