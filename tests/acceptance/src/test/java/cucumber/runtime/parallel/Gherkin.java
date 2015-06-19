package cucumber.runtime.parallel;

import cucumber.runtime.model.CucumberBackground;
import cucumber.runtime.model.CucumberFeature;
import cucumber.runtime.model.CucumberScenario;
import gherkin.formatter.model.Scenario;
import gherkin.formatter.model.Step;


public class Gherkin {

    /**
     * Creates a copy of the given scenario whose name is decorated with the current parameter.
     */
    public static CucumberScenario parametrize(CucumberFeature feature, CucumberScenario scenario, Parameter parameter) {
        CucumberScenario copy = new CucumberScenario(feature,
                copy(scenario.getCucumberBackground()),
                parametrize((Scenario) scenario.getGherkinModel(), parameter));

        for (Step step : scenario.getSteps()) {
            copy.step(copy(step));
        }
        return copy;
    }


    private static CucumberBackground copy(CucumberBackground background) {
        // TODO:
        return background;
    }

    private static Scenario parametrize(Scenario scenario, Parameter parameter) {
        return new Scenario(
                scenario.getComments(),
                scenario.getTags(),
                scenario.getKeyword(),
                parameter.decorateName(scenario.getName()),
                scenario.getDescription(),
                scenario.getLine(),
                scenario.getId());
    }


    public static Step copy(Step step) {
        return new Step(
                step.getComments(),
                step.getKeyword(),
                step.getName(),
                step.getLine(),
                step.getRows(),
                step.getDocString()
        );
    }
}
