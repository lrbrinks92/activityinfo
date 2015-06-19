package org.activityinfo.server.endpoint.jsonrpc;

import com.google.api.client.repackaged.com.google.common.base.Joiner;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import org.activityinfo.legacy.shared.command.AddTarget;
import org.activityinfo.legacy.shared.command.Command;
import org.activityinfo.legacy.shared.model.TargetDTO;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;

public class CommandValidatorTest {

    private CommandValidator validator;

    @Before
    public void setUp() throws Exception {
        validator = new CommandValidator();
    }

    @Test
    public void missingRootProperty() {
        
        AddTarget command = new AddTarget();
        assertThat(validator.validate(command), hasItems(
                forProperty("databaseId"), 
                forProperty("target")));
    }
    
    @Test
    public void missingDates() {

        TargetDTO target = new TargetDTO();
        AddTarget command = new AddTarget();
        command.setTarget(target);

        assertThat(validator.validate(command), hasItems(
                forProperty("target.name"),
                forProperty("target.fromDate"),
                forProperty("target.toDate")));
    }
    
    @Test
    public void nameTooLong() {
        TargetDTO target = new TargetDTO();
        target.setName(Strings.repeat("xoxoxo", 2000));
        AddTarget command = new AddTarget();
        command.setTarget(target);
        
        assertThat(validator.validate(command), hasItems(
                forProperty("target.name")));
    }
    
    private Matcher<ConstraintViolation<Command>> forProperty(final String name) {
        return new TypeSafeMatcher<ConstraintViolation<Command>>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("violation of constraints for ").appendValue(name);
            }

            @Override
            protected boolean matchesSafely(ConstraintViolation<Command> violation) {
                return name.equals(violation.getPropertyPath().toString());
            }
        };
    }
    

}