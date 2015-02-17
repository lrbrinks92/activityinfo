package org.activityinfo.server.endpoint.odk;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

public class InstanceIdServiceImpl implements InstanceIdService {
    final public static String KIND = "XFormInstanceId";

    final private DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

    private Key key(String instanceId) {
        return KeyFactory.createKey(KIND, instanceId);
    }

    @Override
    public boolean exists(String instanceId) {
        try {
            datastoreService.get(key(instanceId));
            return true;
        } catch (EntityNotFoundException e) {
            return false;
        }
    }

    @Override
    public void submit(String instanceId) {
        datastoreService.put(null, new Entity(key(instanceId)));
    }
}
