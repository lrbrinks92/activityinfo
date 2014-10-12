package org.activityinfo.io.odk;

import org.activityinfo.model.type.FieldValue;
import org.activityinfo.model.type.primitive.BooleanFieldValue;
import org.w3c.dom.Element;

class BooleanFieldValueParser implements OdkFieldValueParser {
    @Override
    public FieldValue parse(Element element) {
        String text = OdkHelper.extractText(element);

        if (text == null) throw new IllegalArgumentException("Malformed Element passed to OdkFieldValueParser.parse()");

        return BooleanFieldValue.valueOf(text);
    }
}
