package org.activityinfo.model.resource;

import org.activityinfo.model.type.FieldValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Describes an update to a Resource
 */
public class RecordUpdate {
    
    private long userId;
    private ResourceId recordId;
    private ResourceId parentId;
    private boolean deleted = false;
    private Map<ResourceId, FieldValue> changedFieldValues = new HashMap<>();

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public void setRecordId(ResourceId recordId) {
        this.recordId = recordId;
    }
    
    public ResourceId getRecordId() {
        return recordId;
    }

    public ResourceId getParentId() {
        return parentId;
    }

    public void setParentId(ResourceId parentId) {
        this.parentId = parentId;
    }

    public void set(ResourceId fieldId, FieldValue value) {
        changedFieldValues.put(fieldId, value);
    }

    public Map<ResourceId, FieldValue> getChangedFieldValues() {
        return changedFieldValues;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
