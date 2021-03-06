package org.activityinfo.model.type.barcode;

import com.google.gson.JsonElement;
import org.activityinfo.model.type.FieldType;
import org.activityinfo.model.type.FieldTypeClass;
import org.activityinfo.model.type.FieldValue;
import org.activityinfo.model.type.RecordFieldTypeClass;

/**
 * A value types that describes a real-valued barcode and its units.
 */
public class BarcodeType implements FieldType {


    public static final FieldTypeClass TYPE_CLASS = new RecordFieldTypeClass() {

        public static final String TYPE_ID = "barcode";

        @Override
        public String getId() {
            return TYPE_ID;
        }

        @Override
        public FieldType createType() {
            return INSTANCE;
        }

    };

    public static final BarcodeType INSTANCE = new BarcodeType();

    private BarcodeType() {
    }

    @Override
    public FieldTypeClass getTypeClass() {
        return TYPE_CLASS;
    }

    @Override
    public FieldValue parseJsonValue(JsonElement value) {
        return BarcodeValue.valueOf(value.getAsString());
    }

    @Override
    public String toString() {
        return "BarcodeType";
    }

    /**
     *
     * @return the singleton instance for this type
     */
    private Object readResolve() {
        return INSTANCE;
    }

}
