package org.activityinfo.server.endpoint.jsonrpc;

import org.activityinfo.legacy.shared.command.Command;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

/**
 * Validates {@code Command} instances deserialized from JSON
 */
public class CommandValidator {


    private final Validator validator;

    public CommandValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    public void assertValid(Command command) {
        Set<ConstraintViolation<Command>> violations = validate(command);
        if(!violations.isEmpty()) {
            throw new BadRpcRequest(composeMessage(violations));
        }
    }

    public Set<ConstraintViolation<Command>> validate(Command command) {
        return validator.validate(command);
    }

    private String composeMessage(Set<ConstraintViolation<Command>> violations) {
        StringBuilder sb = new StringBuilder();
        sb.append("There were errors in your request.");
        for (ConstraintViolation<?> violation : violations) {
            sb.append("\n");
            sb.append(violation.getPropertyPath()).append(": ").append(violation.getMessage());
        }
        return sb.toString();
    }
}
