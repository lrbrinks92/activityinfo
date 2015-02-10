package cucumber.runtime.parallel;

import com.google.common.collect.Lists;
import cucumber.runtime.junit.ExamplesRunner;
import cucumber.runtime.model.CucumberExamples;
import cucumber.runtime.model.CucumberScenarioOutline;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import org.junit.runner.Description;
import org.junit.runner.Runner;

import java.util.List;

/**
 * Created by alex on 30-1-15.
 */
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
        return null;
    }

    @Override
    public void start(Reporter reporter, Formatter formatter) {

    }

    @Override
    public List<Node> getBranches() {
        return null;
    }

    @Override
    public void finish(Reporter reporter, Formatter formatter) {

    }
}
