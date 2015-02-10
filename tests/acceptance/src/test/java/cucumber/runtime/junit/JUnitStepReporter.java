package cucumber.runtime.junit;

import cucumber.api.PendingException;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.Match;
import gherkin.formatter.model.Result;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;

import java.util.Iterator;

import static cucumber.runtime.Runtime.isPending;


public class JUnitStepReporter implements Reporter {
    
    private final Reporter reporter;
    private RunNotifier runNotifier;
    private final Iterator<Description> runnerSteps;
    
    private EachTestNotifier executionUnitNotifier;
    private EachTestNotifier stepNotifier = null;
    
    private boolean strict = false;
    private boolean ignoredStep;

    public JUnitStepReporter(RunNotifier runNotifier, Description description, Reporter reporterProxy) {
        this.runNotifier = runNotifier;
        this.runnerSteps = description.getChildren().iterator();
        this.reporter = reporterProxy;
        this.stepNotifier = null;
        this.ignoredStep = false;

        this.executionUnitNotifier = new EachTestNotifier(runNotifier, description);
        executionUnitNotifier.fireTestStarted();
    }

    @Override
    public void embedding(String mimeType, byte[] data) {
        reporter.embedding(mimeType, data);
    }

    @Override
    public void write(String text) {
        reporter.write(text);
    }

    public void match(Match match) {
        stepNotifier = new EachTestNotifier(runNotifier, runnerSteps.next());
        reporter.match(match);
    }

    public void result(Result result) {
        Throwable error = result.getError();
        if (Result.SKIPPED == result) {
            stepNotifier.fireTestIgnored();
        } else if (isPendingOrUndefined(result)) {
            addFailureOrIgnoreStep(result);
        } else {
            if (stepNotifier != null) {
                //Should only fireTestStarted if not ignored
                stepNotifier.fireTestStarted();
                if (error != null) {
                    stepNotifier.addFailure(error);
                }
                stepNotifier.fireTestFinished();
            }
            if (error != null) {
                executionUnitNotifier.addFailure(error);
            }
        }
        if (!runnerSteps.hasNext()) {
            // We have run all of our steps. Set the stepNotifier to null so that
            // if an error occurs in an After block, it's reported against the scenario
            // instead (via executionUnitNotifier).
            stepNotifier = null;
        }
        reporter.result(result);
    }

    private boolean isPendingOrUndefined(Result result) {
        Throwable error = result.getError();
        return Result.UNDEFINED == result || isPending(error);
    }

    private void addFailureOrIgnoreStep(Result result) {
        if (strict) {
            stepNotifier.fireTestStarted();
            addFailure(result);
            stepNotifier.fireTestFinished();
        } else {
            ignoredStep = true;
            stepNotifier.fireTestIgnored();
        }
    }

    private void addFailure(Result result) {

        Throwable error = result.getError();
        if (error == null) {
            error = new PendingException();
        }
        stepNotifier.addFailure(error);
        executionUnitNotifier.addFailure(error);
    }

    @Override
    public void before(Match match, Result result) {
        handleHook(result);
        reporter.before(match, result);
    }

    @Override
    public void after(Match match, Result result) {
        handleHook(result);
        reporter.after(match, result);
    }

    private void handleHook(Result result) {
        if (result.getStatus().equals(Result.FAILED)) {
            executionUnitNotifier.addFailure(result.getError());
        }
    }

    public void finishExecutionUnit() {
        if (ignoredStep) {
            executionUnitNotifier.fireTestIgnored();
        }
        executionUnitNotifier.fireTestFinished();
    }
    
}
