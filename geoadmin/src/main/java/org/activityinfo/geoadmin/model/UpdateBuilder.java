package org.activityinfo.geoadmin.model;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.type.FieldValue;

/**
 * Constructs a series of updates to a FormClass
 */
public class UpdateBuilder {
    
    private JsonObject update = new JsonObject();
    
    public UpdateBuilder setId(ResourceId id) {
        update.addProperty("@id", id.asString());
        return this;
    }
    
    public UpdateBuilder setClass(ResourceId id) {
        update.addProperty("@class", id.asString());
        return this;
    }
    
    public UpdateBuilder delete() {
        update.addProperty("@deleted", true);
        return this;
    }

    public void setProperty(ResourceId fieldId, FieldValue value) {
        if(value == null) {
            update.add(fieldId.asString(), JsonNull.INSTANCE);
        } else {
            update.add(fieldId.asString(), value.toJsonElement());
        }
    }

    public JsonObject build() {
        return update;
    }
}
