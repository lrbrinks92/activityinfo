package cucumber.api;

import java.io.Serializable;
import java.util.Set;

/**
 * Marker interface for classes that define a profile against
 * which to run all scenarios against
 */
public interface Profile {

    /**
     * @return an arbitrary object used to define uniqueness (in {@link #equals(Object)}
     */
    Serializable getId();

    String getName();

    Set<String> getTags();
}
