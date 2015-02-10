package cucumber.runtime;

import com.google.common.base.Optional;
import cucumber.api.Profile;
import cucumber.runtime.java.ObjectFactory;

import java.util.List;

/**
 * Creates Profiles and their ObjectFactories
 */
public interface ProfileFactory {
    
    List<Profile> getProfiles();
    
    Optional<ObjectFactory> createObjectFactory(Profile profile);
    
}
