package cucumber.runtime.junit;

import com.google.common.collect.Lists;
import cucumber.runtime.parallel.ExecutionUnit;
import cucumber.runtime.parallel.Node;
import cucumber.runtime.parallel.RecursiveReporter;
import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;

import java.util.List;
import java.util.concurrent.RecursiveAction;

public class JUnitRecursiveRunner extends RecursiveAction {

    private Node node;
    private RunNotifier runNotifier;
    private RecursiveReporter reporter;

    public JUnitRecursiveRunner(Node node, RunNotifier runNotifier, RecursiveReporter reporter) {
        this.runNotifier = runNotifier;
        this.reporter = reporter;
        this.node = node;
    }


    @Override
    protected void compute() {
        if(node instanceof ExecutionUnit) {
            runLeaf();
        } else {
            runBranchWithNotifications();
        }
    }

    private void runBranchWithNotifications() {
        EachTestNotifier testNotifier = new EachTestNotifier(runNotifier, node.getDescription());
        testNotifier.fireTestStarted();

        try {
            runBranches();
            testNotifier.fireTestFinished();

        } catch (AssumptionViolatedException e) {
            testNotifier.fireTestIgnored();
        } catch (StoppedByUserException e) {
            throw e;
        } catch (Throwable e) {
            testNotifier.addFailure(e);
        }
    }
    
    private void runBranches() {
        System.out.println("Starting " + node.getDescription().getDisplayName());
        node.start(reporter.getReporterProxy(), reporter.getFormatterProxy());

        List<JUnitRecursiveRunner> branches = Lists.newArrayList();
        for (Node branch : node.getBranches()) {
            branches.add(new JUnitRecursiveRunner(branch, runNotifier, reporter.branch()));
        }
        invokeAll(branches);
        reporter.join();
        
        node.finish(reporter.getReporterProxy(), reporter.getFormatterProxy());
    }


    private void runLeaf() {
        
        // Wrap the reporter proxy so that we can fire events to JUnit
        // in real time
        JUnitStepReporter jUnitReporter = new JUnitStepReporter(runNotifier, 
                node.getDescription(), reporter.getReporterProxy());
        
        node.start(jUnitReporter, reporter.getFormatterProxy());
        node.finish(jUnitReporter, reporter.getFormatterProxy());
        
        jUnitReporter.finishExecutionUnit();
    }
}
