package org.activityinfo.ui.client.component.formula.model;


import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.model.type.Cardinality;
import org.activityinfo.model.type.FieldType;
import org.activityinfo.model.type.ReferenceType;
import org.activityinfo.model.type.enumerated.EnumType;
import org.activityinfo.model.type.number.QuantityType;
import org.activityinfo.model.type.primitive.TextType;
import org.activityinfo.model.type.time.LocalDateType;

/**
 * Choice of predicates available to users through the simplified interface.
 */
public enum SimpleOperator {

    EQUAL_TO {
        @Override
        public String getLabel() {
            return I18N.CONSTANTS.operatorEquals();
        }

        @Override
        public boolean accept(FieldType type) {
            if(SimpleOperator.isComparable(type) ||
                    type instanceof TextType ||
                    type instanceof ReferenceType) {
                return true;
            }
            if(type instanceof EnumType) {
                EnumType enumType = (EnumType) type;
                if(enumType.getCardinality() == Cardinality.SINGLE) {
                    return true;
                }
            }
            return false;
        }

    },
    NOT_EQUAL_TO {
        @Override
        public String getLabel() {
            return I18N.CONSTANTS.operatorNotEqual();
        }

        @Override
        public boolean accept(FieldType type) {
            return EQUAL_TO.accept(type);
        }
    },
    GREATER_THAN {
        @Override
        public String getLabel() {
            return I18N.CONSTANTS.operatorGreaterThan();
        }

        @Override
        public boolean accept(FieldType type) {
            return SimpleOperator.isComparable(type);
        }
    },
    GREATER_THAN_EQUAL_TO {
        @Override
        public String getLabel() {
            return I18N.CONSTANTS.operatorGreaterThanEqual();
        }

        @Override
        public boolean accept(FieldType type) {
            return SimpleOperator.isComparable(type);
        }
    },
    LESS_THAN {
        @Override
        public String getLabel() {
            return I18N.CONSTANTS.operatorLessThan();
        }
        @Override
        public boolean accept(FieldType type) {
            return SimpleOperator.isComparable(type);
        }
    },
    LESS_THAN_EQUAL_TO {
        @Override
        public String getLabel() {
            return I18N.CONSTANTS.operatorLessThanEqual();
        }

        @Override
        public boolean accept(FieldType type) {
            return SimpleOperator.isComparable(type);
        }
    },

    ONE_OF {
        @Override
        public String getLabel() {
            return I18N.CONSTANTS.operatorIncludes();
        }

        @Override
        public boolean accept(FieldType type) {
            if(type instanceof EnumType) {
                EnumType enumType = (EnumType) type;
                if(enumType.getCardinality() == Cardinality.MULTIPLE) {
                    return true;
                }
            }
            return false;
        }
    };

    public abstract String getLabel();

    public abstract boolean accept(FieldType type);

    private static boolean isComparable(FieldType type) {
        return type instanceof QuantityType || type instanceof LocalDateType;
    }

}
