package org.activityinfo.ui.client.component.formula;

import org.activityinfo.model.type.FieldType;
import org.activityinfo.model.type.enumerated.EnumType;
import org.activityinfo.model.type.number.QuantityType;
import org.activityinfo.model.type.primitive.TextType;
import org.activityinfo.model.type.time.LocalDateType;

class PredicateEditors {

    public static FieldPredicateEditor create(FieldType type, FieldPredicate predicate) {
        switch (predicate) {
            case EQUAL_TO:
            case NOT_EQUAL_TO:
            case GREATER_THAN:
            case GREATER_THAN_EQUAL_TO:
            case LESS_THAN:
            case LESS_THAN_EQUAL_TO:
                return createComparisonEditor(type, predicate);

            default:
                return new NullPredicateEditor();
        }
    }

    private static FieldPredicateEditor createComparisonEditor(FieldType type, FieldPredicate predicate) {
        if(type instanceof QuantityType) {
            return new QuantityComparisonEditor();
        } else if(type instanceof TextType) {
            return new TextComparisonEditor();
        } else if(type instanceof LocalDateType) {
            return new DateComparisonEditor();
        } else if(type instanceof EnumType) {
            return new EnumComparisonEditor(((EnumType) type));
        } else {
            return new NullPredicateEditor();
        }
    }

}
