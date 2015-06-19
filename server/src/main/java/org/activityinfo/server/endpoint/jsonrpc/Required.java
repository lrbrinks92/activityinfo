package org.activityinfo.server.endpoint.jsonrpc;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated element must be provided. If the annotation is an integer, it must not be zero.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RequiredValidator.class)
public @interface Required {
    
    String message() default "required property";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};


}
