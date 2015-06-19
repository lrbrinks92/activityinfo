package org.activityinfo.server.generated;

import com.google.appengine.api.datastore.*;
import org.activityinfo.model.auth.AuthenticatedUser;

import java.util.Date;

/**
 * Records metadata about a generated user.
 */
class GcsGeneratedMetadata {
    private String id;
    private long owner;
    private String filename;
    private String contentType;
    private boolean completed;
    private Double percentageComplete;
    private Date creationTime;
    
    public GcsGeneratedMetadata(String id) {
        this.id = id;
        this.creationTime = new Date();
    }

    public static GcsGeneratedMetadata load(String id) throws EntityNotFoundException {
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
        Entity entity = datastoreService.get(entityKey(id));
        GcsGeneratedMetadata metadata = new GcsGeneratedMetadata(id);
        metadata.filename = (String) entity.getProperty("filename");
        metadata.contentType = (String) entity.getProperty("contentType");
        metadata.owner = (Long)entity.getProperty("owner");
        metadata.completed = (entity.getProperty("completed") == Boolean.TRUE);
        metadata.creationTime = (Date) entity.getProperty("creationTime");
        metadata.percentageComplete = (Double)entity.getProperty("percentageComplete");
        return metadata;
    }
    
    public void save() {
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
        Entity entity = new Entity(entityKey(this.id));
        entity.setUnindexedProperty("filename", filename);
        entity.setUnindexedProperty("contentType", contentType);
        entity.setProperty("creationTime", creationTime);
        entity.setUnindexedProperty("completed", completed);
        entity.setProperty("owner", owner);
        entity.setProperty("percentageComplete", percentageComplete);
        datastoreService.put(entity);
    }
    
    
    public void markComplete() {
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
        Transaction tx = datastoreService.beginTransaction();
        Entity entity = null;
        try {
            entity = datastoreService.get(tx, entityKey(id));
        } catch (EntityNotFoundException e) {
            tx.rollback();
            throw new IllegalStateException("Metadata entity " + entityKey(id) + " has not been saved.");
        }
        entity.setProperty("completed", true);
        datastoreService.put(tx, entity);
        tx.commit();
    }
    
    private static Key entityKey(String id) {
        return KeyFactory.createKey("Generated", id);
    }

    public long getOwner() {
        return owner;
    }

    public void setOwner(long owner) {
        this.owner = owner;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public boolean isCompleted() {
        return completed;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public double getPercentageComplete() {
        if(completed) {
            return 1.0;
        } else if(percentageComplete != null) {
            return percentageComplete;
        } else {
            return 0d;
        }
    }

    public void setPercentageComplete(Double percentageComplete) {
        this.percentageComplete = percentageComplete;
    }

    public String getId() {
        return id;
    }

    public void setOwner(AuthenticatedUser authenticatedUser) {
        this.owner = authenticatedUser.getUserId();
    }
    
    public String getGcsPath() {
        return "generated/" + id;
    }
}
