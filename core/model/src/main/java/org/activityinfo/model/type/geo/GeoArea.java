package org.activityinfo.model.type.geo;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.activityinfo.model.resource.IsRecord;
import org.activityinfo.model.resource.Record;
import org.activityinfo.model.type.FieldTypeClass;

/* A Field Value describing a geographic area on the Earth's surface
* in the WGS84 geographic reference system.
*/
public class GeoArea implements GeoFieldValue, IsRecord {

    private Extents envelope;
    private String blobId;

    public GeoArea(Extents envelope, String blobId) {
        this.envelope = envelope;
        this.blobId = blobId;
    }

    public GeoArea(Extents envelope) {
        this.envelope = envelope;
    }


    public Extents getEnvelope() {
        return envelope;
    }

    public String getBlobId() {
        return blobId;
    }

    @Override
    public FieldTypeClass getTypeClass() {
        return GeoAreaType.TYPE_CLASS;
    }

    @Override
    public Record asRecord() {
        Record record = new Record();
        record.set(TYPE_CLASS_FIELD_NAME, getTypeClass().getId());
        
        if(envelope != null) {
            record.set("bbox", envelope.asRecord());
        }
        if(!Strings.isNullOrEmpty(blobId)) {
            record.set("blobId", blobId);
        }
        return record;
    }


    @Override
    public JsonElement toJsonElement() {
        JsonObject object = new JsonObject();
        object.addProperty("blobId", blobId);
        object.add("bbox", envelope.toJsonElement());
        return object;
    }

}
