package org.activityinfo.model.type;

import org.activityinfo.model.resource.PropertyBag;
import org.activityinfo.model.resource.Record;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.type.number.Quantity;
import org.activityinfo.model.type.primitive.BooleanFieldValue;
import org.activityinfo.model.type.primitive.TextValue;

public class Types {

    public static FieldValue read(PropertyBag bag, String name) {
        Object value = bag.get(name);
        if (value == null) {
            return null;
        } else if (value instanceof String) {
            return TextValue.valueOf((String) value);
        } else if (value instanceof Boolean) {
            Boolean booleanValue = (Boolean) value;
            return BooleanFieldValue.valueOf(booleanValue);
        } else if (value instanceof Record) {
            Record record = (Record) value;
            try {
                return TypeRegistry.get().deserializeFieldValue(record);
            } catch (Exception e) {
                throw new RuntimeException("Exception thrown while reading property '" + name + "' from " + value);
            }
        } else if (value instanceof Double) {
            return new Quantity((Double) value);
        } else {
            throw new UnsupportedOperationException(name + " = " + value);
        }
    }

    public static <V extends FieldValue> V read(PropertyBag bag, String name, RecordFieldTypeClass<V> typeClass) {
        Record record = bag.isRecord(name);
        if(record != null) {
            String typeName = record.getString(FieldValue.TYPE_CLASS_FIELD_NAME);
            if(typeClass.getId().equals(typeName)) {
                return typeClass.deserialize(record);
            }
        }
        return null;
    }

    public static ResourceId readReference(PropertyBag bag, String name) {
        ReferenceValue value = read(bag, name, ReferenceType.TYPE_CLASS);
        return value.getResourceId();
    }

    public static ResourceId parameterFormClassId(FieldTypeClass typeClass) {
        return ResourceId.valueOf("_type:" + typeClass.getId());
    }

}