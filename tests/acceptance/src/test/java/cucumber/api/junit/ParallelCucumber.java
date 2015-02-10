package cucumber.api.junit;

import com.google.common.collect.Lists;
import cucumber.runtime.*;
import cucumber.runtime.Runtime;
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
import org.activityinfo.test.harness.ProfileFactoryImpl;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

/**
 * <p>
 * Classes annotated with {@code @RunWith(ParametrizedCucumber.class)} will run a Cucumber Feature against
 * a set of {@code ExecutionProfiles}.
 * 
 * The class should be empty without any fields or methods.
 * </p>
 * <p>
 * Cucumber will look for a {@code .feature} file on the classpath, using the same resource
 * path as the annotated class ({@code .class} substituted by {@code .feature}).
 * </p>
 * Additional hints can be given to Cucumber by annotating the class with {@link cucumber.api.CucumberOptions}.
 *
 * @see cucumber.api.CucumberOptions
 */
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
        ArrayList<Parameter> parameters = Lists.newArrayList();

        ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        ObjectFactory objectFactory = JavaBackend.loadObjectFactory(classFinder);
        
        return new RuntimePool(runtimeOptions,
                        resourceLoader,
                        classLoader,
                        classFinder,
                        objectFactory);
    }
    
    @Override
    public Description getDescription() {
        return description;
    }


    @Override
    public void run(RunNotifier notifier) {
        
        int numThreads = Integer.parseInt(System.getProperty("junit.parallel.threads", "16"));

        RecursiveReporter reporter = new RecursiveReporter(
                runtimeOptions.formatter(classLoader),
                runtimeOptions.reporter(classLoader));
        
        JUnitRecursiveRunner runner = new JUnitRecursiveRunner(this, notifier, reporter);
        
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
}
