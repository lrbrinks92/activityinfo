package org.activityinfo.test.config;

import com.google.common.base.Enums;
import com.google.common.base.Optional;
import org.activityinfo.test.config.ConfigProperty;
import org.activityinfo.test.config.ConfigurationError;

import java.util.Arrays;

/**
 * Defines a configuration property of enumerated type
 */
public class EnumConfigProperty<T extends Enum<T>> {

    private final Class<T> enumClass;
    private final ConfigProperty property;

    public EnumConfigProperty(String name, String description, Class<T> enumClass) {
        this.enumClass = enumClass;
        this.property = new ConfigProperty(name, description + " " + Arrays.toString(enumClass.getEnumConstants()));
    }

    public T get() {
        Optional<T> value = Enums.getIfPresent(enumClass, property.get().toUpperCase());
        if(!value.isPresent()) {
            throw new ConfigurationError(String.format(
                    "Invalid value for system property '%s': '%s'. Expected: " +
                            Arrays.toString(enumClass.getEnumConstants())));

        }
        return value.get();
    }
}
