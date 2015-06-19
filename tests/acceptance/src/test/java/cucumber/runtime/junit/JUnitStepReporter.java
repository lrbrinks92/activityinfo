package cucumber.runtime.junit;

import cucumber.api.PendingException;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.Match;
import gherkin.formatter.model.Result;
import gherkin.formatter.model.Step;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;

import java.util.Iterator;
import java.util.List;

import static cucumber.runtime.Runtime.isPending;


public class JUnitStepReporter implements Reporter {
    
    private final Reporter reporter;
    private RunNotifier runNotifier;
    private final Iterator<Step> runnerSteps;
    
    private EachTestNotifier executionUnitNotifier;
   // private EachTestNotifier stepNotifier = null;
    
    private boolean strict = false;
    private boolean ignoredStep;
    private Step currentStep;

    public JUnitStepReporter(RunNotifier runNotifier, Description description, List<Step> steps, Reporter reporterProxy) {
        this.runNotifier = runNotifier;
        this.runnerSteps = steps.iterator();
        this.reporter = reporterProxy;
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

    @Override
    public void match(Match match) {
        currentStep = runnerSteps.next();
        reporter.match(match);
    }

    @Override
    public void result(Result result) {
        Throwable error = result.getError();
        if (Result.SKIPPED == result) {
            stepResult("Skipped");
        
        } else if (isPendingOrUndefined(result)) {
            addFailureOrIgnoreStep(result);
        
        } else {
            if (currentStep != null) {
                //Should only fireTestStarted if not ignored
                stepResult("Started");
                if (error != null) {
                    stepResult("Failure");
                }
                stepResult("Finished");
            }
            if (error != null) {
                executionUnitNotifier.addFailure(error);
            }
        }
        if (!runnerSteps.hasNext()) {
            // We have run all of our steps. Set the stepNotifier to null so that
            // if an error occurs in an After block, it's reported against the scenario
            // instead (via executionUnitNotifier).
            currentStep = null;
        }
        reporter.result(result);
    }

    private void stepResult(String result) {
        if(currentStep != null) {
            System.out.println(currentStep.getName() + ": " + result);
        }
    }

    private boolean isPendingOrUndefined(Result result) {
        Throwable error = result.getError();
        return Result.UNDEFINED == result || isPending(error);
    }

    private void addFailureOrIgnoreStep(Result result) {
        if (strict) {
            stepResult("Started");
            addFailure(result);
            stepResult("Finished");
        } else {
            ignoredStep = true;
            stepResult("Ignored");
        }
    }

    private void addFailure(Result result) {

        Throwable error = result.getError();
        if (error == null) {
            error = new PendingException();
        }
        stepResult("Failed");
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
