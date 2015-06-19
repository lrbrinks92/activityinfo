package org.activityinfo.model.type;

import com.google.common.collect.Maps;
import org.activityinfo.model.resource.Record;
import org.activityinfo.model.type.barcode.BarcodeType;
import org.activityinfo.model.type.enumerated.EnumType;
import org.activityinfo.model.type.expr.CalculatedFieldType;
import org.activityinfo.model.type.expr.ExprFieldType;
import org.activityinfo.model.type.geo.GeoPointType;
import org.activityinfo.model.type.image.ImageType;
import org.activityinfo.model.type.number.QuantityType;
import org.activityinfo.model.type.primitive.BooleanType;
import org.activityinfo.model.type.primitive.TextType;
import org.activityinfo.model.type.time.LocalDateIntervalType;
import org.activityinfo.model.type.time.LocalDateType;

import java.util.Map;

/**
 * Global registry of {@code FieldTypeClass}es.
 */
public class TypeRegistry {

    private static TypeRegistry INSTANCE;

    public static TypeRegistry get() {
        if(INSTANCE == null) {
            INSTANCE = new TypeRegistry();
        }
        return INSTANCE;
    }

    private Map<String, FieldTypeClass> typeMap = Maps.newHashMap();

    private TypeRegistry() {
        register(EnumType.TYPE_CLASS);
        register(ReferenceType.TYPE_CLASS);
        register(TextType.TYPE_CLASS);
        register(QuantityType.TYPE_CLASS);
        register(NarrativeType.TYPE_CLASS);
        register(CalculatedFieldType.TYPE_CLASS);
        register(ExprFieldType.TYPE_CLASS);
        register(LocalDateType.TYPE_CLASS);
        register(LocalDateIntervalType.TYPE_CLASS);
        register(GeoPointType.TYPE_CLASS);
        register(BooleanType.TYPE_CLASS);
        register(BarcodeType.TYPE_CLASS);
        register(ImageType.TYPE_CLASS);
    }

    private void register(FieldTypeClass typeClass) {
        typeMap.put(typeClass.getId().toUpperCase(), typeClass);
    }

    public FieldTypeClass getTypeClass(String typeId) {
        FieldTypeClass typeClass = typeMap.get(typeId.toUpperCase());
        if(typeClass == null) {
            throw new RuntimeException("Unknown type: " + typeId);
        }
        return typeClass;
    }

    public Iterable<FieldTypeClass> getTypeClasses() {
        return typeMap.values();
    }

    public FieldValue deserializeFieldValue(Record record) {
        String typeClassId = record.getString(FieldValue.TYPE_CLASS_FIELD_NAME);
        FieldTypeClass typeClass = getTypeClass(typeClassId);
        if(typeClass instanceof RecordFieldTypeClass) {
            return ((RecordFieldTypeClass) typeClass).deserialize(record);
        } else {
            throw new UnsupportedOperationException(typeClassId + " cannot be deserialized from a Record");
        }
    }


    public static FieldValue readField(Record record, String name, FieldTypeClass typeClass) {
        Record fieldValue = record.isRecord(name);
        if(fieldValue == null) {
            return null;
        }
        String typeClassId = fieldValue.isString(FieldValue.TYPE_CLASS_FIELD_NAME);
        if(!typeClass.getId().equals(typeClassId)) {
            return null;
        }
        return ((RecordFieldTypeClass) typeClass).deserialize(record);
    }
}
