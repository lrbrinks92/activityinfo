package cucumber.api.junit;

import com.google.common.collect.Lists;
import cucumber.runtime.*;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import cucumber.runtime.java.JavaBackend;
import cucumber.runtime.java.ObjectFactory;
import cucumber.runtime.junit.*;
import cucumber.runtime.parallel.*;
import cucumber.runtime.model.CucumberFeature;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.Step;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ForkJoinPool;


public class ParallelCucumber extends Runner implements Node {
    
    private final Description description;
    private final List<Node> branches = Lists.newArrayList();
    private final RuntimeOptions runtimeOptions;
    private final ClassLoader classLoader;
    private final ResourceLoader resourceLoader;

    /**
     * Constructor called by JUnit.
     *
     * @param testClass the class with the @RunWith annotation.
     * @throws java.io.IOException                         if there is a problem
     * @throws org.junit.runners.model.InitializationError if there is another problem
     */
    public ParallelCucumber(Class testClass) throws InitializationError, IOException {
        classLoader = testClass.getClassLoader();
        Assertions.assertNoCucumberAnnotatedMethods(testClass);

        RuntimeOptionsFactory runtimeOptionsFactory = new RuntimeOptionsFactory(testClass);
        runtimeOptions = runtimeOptionsFactory.create();
        resourceLoader = new MultiLoader(classLoader);

        RuntimePool runtimePool = createRuntimePool(resourceLoader, classLoader, runtimeOptions);
        
        description = Description.createSuiteDescription(testClass);

        for(CucumberFeature feature : runtimeOptions.cucumberFeatures(resourceLoader)) {
            FeatureNode featureNode = new FeatureNode(feature, runtimePool);
            description.addChild(featureNode.getDescription());
            branches.add(featureNode);
        }      
    }

    private RuntimePool createRuntimePool(ResourceLoader resourceLoader, ClassLoader classLoader,
                                                   RuntimeOptions runtimeOptions) throws InitializationError {

        ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        ObjectFactory objectFactory = getObjectFactory(classFinder);
        
        return new RuntimePool(runtimeOptions,
                        resourceLoader,
                        classLoader,
                        classFinder,
                        objectFactory);
    }

    private ObjectFactory getObjectFactory(ClassFinder classFinder) {
        return JavaBackend.loadObjectFactory(classFinder);
    }

    @Override
    public Description getDescription() {
        return description;
    }


    @Override
    public void run(RunNotifier notifier) {
        
        int numThreads = Integer.parseInt(System.getProperty("junit.parallel.threads", "1"));

        RecursiveReporter reporter = new RecursiveReporter(
                runtimeOptions.formatter(classLoader),
                runtimeOptions.reporter(classLoader));
        
        RecursiveFeatureRunner runner = new RecursiveFeatureRunner(this, notifier, reporter);
        
        ForkJoinPool pool = new ForkJoinPool(numThreads);
        pool.invoke(runner);
        
        reporter.join();
        reporter.done();
    }

    @Override
    public void start(Reporter reporter, Formatter formatter) {
        
    }

    @Override
    public List<Node> getBranches() {
        return branches;
    }

    @Override
    public void finish(Reporter reporter, Formatter formatter) {

    }

    @Override
    public List<Step> getSteps() {
        throw new UnsupportedOperationException();
    }
}
