package cucumber.runtime.parallel;

import cucumber.api.Profile;
import cucumber.runtime.Runtime;


public class Parameter {
    private final RuntimePool runtime;
    private Profile profile;

    public Parameter(Profile profile, RuntimePool runtime) {
        this.profile = profile;
        this.runtime = runtime;
    }

    public Runtime getRuntime() {
        return runtime.get();
    }

    public Profile getProfile() {
        return profile;
    }

    public String decorateName(String visualName) {
        return String.format("%s [%s]", visualName, profile.getName());
    }
    
}

