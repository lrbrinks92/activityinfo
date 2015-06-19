package cucumber.runtime.parallel;

import com.google.common.collect.Lists;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * Collects calls to an Reporter or Formatter interface to ensure 
 * that reports on features are sequential
 */
public class RecursiveReporter {

    private final Formatter formatter;
    private final Reporter reporter;
    private final Formatter formatterProxy;
    private final Reporter reporterProxy;   
    
    /**
     * Formatting events fired in this branch
     */
    private final List<Event> events = Lists.newArrayList();

    private final List<RecursiveReporter> children = Lists.newArrayList();
    
    public RecursiveReporter(Formatter formatter, Reporter reporter) {
        this.formatter = formatter;
        this.reporter = reporter;
        this.formatterProxy = createProxy(Formatter.class, formatter);
        this.reporterProxy = createProxy(Reporter.class, reporter);
    }

    @SuppressWarnings("unchecked")
    private <T> T createProxy(Class<T> clazz, final Object listener) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new Handler(listener));
    }

    public Reporter getReporterProxy() {
        return reporterProxy;
    }

    public Formatter getFormatterProxy() {
        return formatterProxy;
    }

    /**
     * 
     * @return a new RecursiveReporter which will maintain its own queue of reporting calls.
     */
    public RecursiveReporter branch() {
        RecursiveReporter child = new RecursiveReporter(formatter, reporter);
        children.add(child);
        return child;
    }

    /**
     * Merge, in their logical order, calls to the reporter/formatter
     */
    public void join() {
        for(RecursiveReporter child : children) {
            events.addAll(child.events);
        }
        children.clear();
    }

    public void done() {
        for (Event event : events) {
            event.fire();
        }
        formatter.done();
        formatter.close();
    }


    private class Handler implements InvocationHandler {

        private final Object listener;

        public Handler(Object listener) {
            this.listener = listener;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (!method.getReturnType().equals(void.class)) {
                throw new UnsupportedOperationException("Cannot queue method invocation with a return value: " + method);
            }
            
            events.add(new Event(listener, method, args));
            return null;
        }
    }
}
