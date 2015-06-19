package cucumber.runtime.parallel;

import com.google.common.collect.Lists;
import cucumber.runtime.model.CucumberExamples;
import cucumber.runtime.model.CucumberScenarioOutline;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.Step;
import org.junit.runner.Description;

import java.util.Collections;
import java.util.List;

public class OutlineNode implements Node {

    private final Description description;
    
    private List<ExampleNode> examples = Lists.newArrayList();
    
    public OutlineNode(RuntimePool runtime, CucumberScenarioOutline outline) {
        description = Description.createSuiteDescription(outline.getVisualName(),
                outline.getGherkinModel());
        
        for (CucumberExamples cucumberExamples : outline.getCucumberExamplesList()) {
            ExampleNode child = new ExampleNode();
            
        }
        
    }

    @Override
    public Description getDescription() {
        return description;
    }

    @Override
    public void start(Reporter reporter, Formatter formatter) {

    }

    @Override
    public List<Node> getBranches() {
        return Collections.emptyList();
    }

    @Override
    public void finish(Reporter reporter, Formatter formatter) {

    }

    @Override
    public List<Step> getSteps() {
        throw new UnsupportedOperationException();
    }
}
