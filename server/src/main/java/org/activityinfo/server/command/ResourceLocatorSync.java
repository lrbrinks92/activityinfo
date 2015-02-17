package org.activityinfo.server.command;

import com.google.inject.ImplementedBy;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.service.lookup.ReferenceChoice;

import java.util.List;
import java.util.Set;

@ImplementedBy(ResourceLocatorSyncImpl.class)
public interface ResourceLocatorSync {
    void persist(FormInstance formInstance);

    void persist(FormInstance formInstance, int locationId, int locationTypeId, double latitude, double longitude);

    FormClass getFormClass(ResourceId resourceId);

    FormInstance getFormInstance(ResourceId resourceId);

    List<ReferenceChoice> getReferenceChoices(Set<ResourceId> range);
}
