package org.activityinfo.server.endpoint.odk;

import org.activityinfo.model.type.FieldType;
import org.activityinfo.model.type.NarrativeType;
import org.activityinfo.model.type.ReferenceType;
import org.activityinfo.model.type.attachment.AttachmentType;
import org.activityinfo.model.type.barcode.BarcodeType;
import org.activityinfo.model.type.enumerated.EnumType;
import org.activityinfo.model.type.geo.GeoPointType;
import org.activityinfo.model.type.number.QuantityType;
import org.activityinfo.model.type.primitive.BooleanType;
import org.activityinfo.model.type.primitive.TextType;
import org.activityinfo.model.type.time.LocalDateType;

public class FieldValueParserFactory {
    static public FieldValueParser fromFieldType(FieldType fieldType, boolean odk) {
        if (fieldType instanceof BarcodeType) {
            return new BarcodeFieldValueParser();
        }
        if (fieldType instanceof BooleanType) {
            return new BooleanFieldValueParser();
        }
        if (fieldType instanceof EnumType) {
            return new IdEnumFieldValueParser();
        }
        if (fieldType instanceof GeoPointType) {
            return new GeoPointFieldValueParser();
        }
        if (fieldType instanceof AttachmentType) {
            return new AttachmentFieldValueParser();
        }
        if (fieldType instanceof LocalDateType) {
            return new LocalDateFieldValueParser();
        }
        if (fieldType instanceof NarrativeType) {
            return new NarrativeFieldValueParser();
        }
        if (fieldType instanceof QuantityType) {
            return new QuantityFieldValueParser((QuantityType) fieldType);
        }
        if (fieldType instanceof ReferenceType) {
            return new ReferenceFieldValueParser();
        }
        if (fieldType instanceof TextType) {
            return new TextFieldValueParser();
        }

        // If this happens, it means this class needs to be expanded to support the new FieldType class.
        throw new IllegalArgumentException("Unknown FieldType passed to FieldValueParserFactory.get()");
    }
}
