package org.activityinfo.server.endpoint.odk;

import com.google.inject.ImplementedBy;

@ImplementedBy(InstanceIdServiceImpl.class)
public interface InstanceIdService {
    boolean exists(String instanceId);

    void submit(String instanceId);
}
