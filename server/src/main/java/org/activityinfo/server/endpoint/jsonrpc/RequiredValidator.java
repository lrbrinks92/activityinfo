package org.activityinfo.server.endpoint.jsonrpc;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validates that a property has been provided
 */
public class RequiredValidator implements ConstraintValidator<Required, Object> {

    @Override
    public void initialize(Required constraintAnnotation) {
        
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if(value == null) {
            return false;
        }
        if(value instanceof Integer) {
            return (Integer) value != 0;
        }
        if(value instanceof String) {
            return ((String) value).length() > 0;
        }
        return true;
    }
}
