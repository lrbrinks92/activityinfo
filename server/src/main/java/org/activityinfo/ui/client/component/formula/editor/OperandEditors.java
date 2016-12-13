package org.activityinfo.ui.client.component.formula.editor;

import org.activityinfo.model.expr.functions.*;
import org.activityinfo.model.type.FieldType;
import org.activityinfo.model.type.enumerated.EnumType;
import org.activityinfo.model.type.number.QuantityType;
import org.activityinfo.model.type.primitive.TextType;
import org.activityinfo.model.type.time.LocalDateType;
import org.activityinfo.ui.client.component.formula.model.SimpleOperator;

class OperandEditors {

    public static OperandEditor create(FieldType type, SimpleOperator predicate) {
        switch (predicate) {
            case EQUAL_TO:
            case NOT_EQUAL_TO:
            case GREATER_THAN:
            case GREATER_THAN_EQUAL_TO:
            case LESS_THAN:
            case LESS_THAN_EQUAL_TO:
                return createComparisonEditor(type);

            default:
                return new NullEditor();
        }
    }

    private static OperandEditor createComparisonEditor(FieldType type) {
        if(type instanceof QuantityType) {
            return new QuantityOperandEditor();
        } else if(type instanceof TextType) {
            return new TextOperandEditor();
        } else if(type instanceof LocalDateType) {
            return new DateEditor();
        } else if(type instanceof EnumType) {
            return new EnumOperandEditor(((EnumType) type));
        } else {
            return new NullEditor();
        }
    }

    public static ComparisonOperator comparisonOperator(SimpleOperator predicate) {
        switch (predicate) {
            case EQUAL_TO:
                return EqualFunction.INSTANCE;
            case NOT_EQUAL_TO:
                return NotEqualFunction.INSTANCE;
            case GREATER_THAN:
                return GreaterFunction.INSTANCE;
            case GREATER_THAN_EQUAL_TO:
                return GreaterOrEqualFunction.INSTANCE;
            case LESS_THAN:
                return LessFunction.INSTANCE;
            case LESS_THAN_EQUAL_TO:
                return LessOrEqualFunction.INSTANCE;
            default:
                throw new IllegalArgumentException();
        }
    }

}
