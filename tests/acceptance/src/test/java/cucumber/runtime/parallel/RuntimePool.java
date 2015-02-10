package cucumber.runtime.parallel;

import cucumber.runtime.*;
import cucumber.runtime.Runtime;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.java.JavaBackend;
import cucumber.runtime.java.ObjectFactory;

import java.util.Collections;
import java.util.Set;

public class RuntimePool {


    private final RuntimeOptions runtimeOptions;
    private final ResourceLoader resourceLoader;
    private final ClassLoader classLoader;
    private final ClassFinder classFinder;
    private final ObjectFactory objectFactory;

    private ThreadLocal<Runtime> runtimes = new ThreadLocal<>();
    
    public RuntimePool(RuntimeOptions runtimeOptions,
                       ResourceLoader resourceLoader,
                       ClassLoader classLoader,
                       ClassFinder classFinder, ObjectFactory objectFactory) {

        this.runtimeOptions = runtimeOptions;
        this.resourceLoader = resourceLoader;
        this.classLoader = classLoader;
        this.classFinder = classFinder;
        this.objectFactory = objectFactory;
    }
    
    public Runtime get() {
        Runtime runtime = runtimes.get();
        if(runtime == null) {
            JavaBackend backend = new JavaBackend(objectFactory, classFinder);
            runtime = new Runtime(resourceLoader, classLoader, Collections.singleton(backend), runtimeOptions);
            runtimes.set(runtime);
        }
        return runtime;
    }
}
