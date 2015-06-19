package cucumber.runtime.parallel;

import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.Step;
import org.junit.runner.Description;

import java.util.List;


public interface Node {

    Description getDescription();
    
    void start(Reporter reporter, Formatter formatter);
    
    List<Node> getBranches();
    
    void finish(Reporter reporter, Formatter formatter);

    List<Step> getSteps();
}
