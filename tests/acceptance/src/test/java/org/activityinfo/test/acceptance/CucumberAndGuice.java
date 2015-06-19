package org.activityinfo.test.acceptance;

import com.google.inject.Injector;
import cucumber.api.junit.Cucumber;
import cucumber.runtime.ClassFinder;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import cucumber.runtime.java.JavaBackend;
import cucumber.runtime.java.ObjectFactory;
import cucumber.runtime.java.guice.ScenarioScope;
import org.junit.runners.model.InitializationError;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;

import static java.lang.reflect.Modifier.isStatic;

public class CucumberAndGuice extends Cucumber {


    public CucumberAndGuice(Class clazz) throws InitializationError, IOException {
        super(clazz);
    }

    @Override
    protected cucumber.runtime.Runtime createRuntime(ResourceLoader resourceLoader, ClassLoader classLoader, 
                                                     RuntimeOptions runtimeOptions) throws InitializationError, IOException {
        
        ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        JavaBackend backend = new JavaBackend(new GuiceObjectFactory(getInjector()), classFinder);
        return new cucumber.runtime.Runtime(resourceLoader, classLoader, Collections.singleton(backend), runtimeOptions);
    }
    
    private Injector getInjector() {

        Method method;
        try {
            method = getTestClass().getJavaClass().getMethod("getInjector");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Expected to find a static method called 'getInjector'");
        }

        if(!isStatic(method.getModifiers())) {
            throw new RuntimeException("getInjector() must be public static.");
        }
        try {
            return (Injector) method.invoke(null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("getInjector() must be public static.");
        } catch (InvocationTargetException e) {
            if(e.getCause() instanceof RuntimeException) {
                throw (RuntimeException)e.getCause();
            }
            throw new RuntimeException("Exception thrown while invoking getInjector()");
        } catch (ClassCastException e) {
            throw new RuntimeException("getInjector() must return " + Injector.class.getName());

        }
    }
    
    private static class GuiceObjectFactory implements ObjectFactory {

        private final Injector injector;

        private GuiceObjectFactory(Injector injector) {
            this.injector = injector;
        }

        @Override
        public void addClass(Class<?> clazz) {}

        @Override
        public void start() {
            injector.getInstance(ScenarioScope.class).enterScope();
        }

        @Override
        public void stop() {
            injector.getInstance(ScenarioScope.class).exitScope();
        }

        @Override
        public <T> T getInstance(Class<T> clazz) {
            return injector.getInstance(clazz);
        }

    }
}
