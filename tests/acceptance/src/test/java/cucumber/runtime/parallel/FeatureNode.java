package cucumber.runtime.parallel;

import com.google.common.collect.Lists;
import cucumber.runtime.CucumberException;
import cucumber.runtime.model.CucumberFeature;
import cucumber.runtime.model.CucumberScenario;
import cucumber.runtime.model.CucumberScenarioOutline;
import cucumber.runtime.model.CucumberTagStatement;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.Feature;
import gherkin.formatter.model.Step;
import org.junit.runner.Description;
import org.junit.runners.model.InitializationError;

import java.util.List;

public class FeatureNode implements Node {
    
    private final CucumberFeature cucumberFeature;
    private final Description description;
    
    private List<Node> branches = Lists.newArrayList();

    public FeatureNode(CucumberFeature cucumberFeature,
                       RuntimePool runtimePool) throws InitializationError {
        
        this.cucumberFeature = cucumberFeature;
        this.description = Description.createSuiteDescription(getName(), cucumberFeature.getGherkinFeature());

        for (CucumberTagStatement cucumberTagStatement : cucumberFeature.getFeatureElements()) {
            try {
                Node child = createBranch(cucumberTagStatement, runtimePool);
                description.addChild(child.getDescription());
                branches.add(child);

            } catch (InitializationError e) {
                throw new CucumberException("Failed to create scenario node", e);
            }
        }
    }

    public String getName() {
        Feature feature = cucumberFeature.getGherkinFeature();
        return feature.getKeyword() + ": " + feature.getName();
    }

    @Override
    public Description getDescription() {
        return description;
    }
    

    private Node createBranch(CucumberTagStatement cucumberTagStatement, RuntimePool runtime) throws InitializationError {

        if (cucumberTagStatement instanceof CucumberScenario) {
            return new ExecutionUnit(runtime, (CucumberScenario) cucumberTagStatement);

        } else {
            return new OutlineNode(runtime, (CucumberScenarioOutline) cucumberTagStatement);
        }
    }

    @Override
    public void start(Reporter reporter, Formatter formatter) {
        formatter.uri(cucumberFeature.getPath());
        formatter.feature(cucumberFeature.getGherkinFeature());
    }
    
    @Override
    public List<Node> getBranches() {
        return branches;
    }

    @Override
    public void finish(Reporter reporter, Formatter formatter) {
        formatter.eof();
    }

    @Override
    public List<Step> getSteps() {
        throw new UnsupportedOperationException();
    }
}
