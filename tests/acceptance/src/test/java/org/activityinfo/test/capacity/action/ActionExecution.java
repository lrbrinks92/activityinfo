package org.activityinfo.test.capacity.action;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.google.common.collect.Sets;
import org.activityinfo.test.capacity.Metrics;
import org.activityinfo.test.capacity.model.ScenarioContext;
import org.activityinfo.test.capacity.model.UserRole;
import org.activityinfo.test.driver.ApiApplicationDriver;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.codahale.metrics.MetricRegistry.name;

/**
* Runs a {@code UserAction} with the ApiApplicationDriver
*/
public class ActionExecution implements Runnable {
    
    public static final Set<String> ACTIONS = Sets.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    public static final Counter CONCURRENT_USERS = Metrics.REGISTRY.counter("users.concurrent");

    private static final Logger LOGGER = Logger.getLogger(ActionExecution.class.getName());
    
    
    public static class ActionMetrics {
        
        private Counter concurrent;
        private Timer latency;
        private Meter succeeded;
        private Meter failed;
        
        ActionMetrics(Class<? extends UserAction> actionClass) {
            this(actionClass.getSimpleName());
        }
        
        public ActionMetrics(String actionName) {
            ACTIONS.add(actionName);
            concurrent = Metrics.REGISTRY.counter(name("action", "concurrent", actionName));
            latency = Metrics.REGISTRY.timer(name("action", "latency", actionName));
            succeeded = Metrics.REGISTRY.meter(name("action", "succeeded", actionName));
            failed = Metrics.REGISTRY.meter(name("action", "failed", actionName));                     
        }
        
        private long start() {
            concurrent.inc();
            return CLOCK.getTick();
        }
        
        private void succeeded(long started) {
            long time = CLOCK.getTick() - started;
            concurrent.dec();
            latency.update(time, TimeUnit.NANOSECONDS);
            succeeded.mark();
        }
        
        private void failed() {
            concurrent.dec();
            failed.mark();
        }
        
        public double getOneMinuteSuccessRate() {
            double successCount = succeeded.getOneMinuteRate();
            double failureCount = failed.getOneMinuteRate();
            return (successCount / (successCount+failureCount)) * 100d;
        }
        
        public long getOneMinuteLatencySeconds() {
            return TimeUnit.NANOSECONDS.toSeconds((long) latency.getSnapshot().getMedian());
        }

        public double getSuccessRate() {
            double successCount = succeeded.getCount();
            double failureCount = failed.getCount();
            return (successCount / (successCount+failureCount)) * 100d;
        }
    }
    
    private static final Clock CLOCK = Clock.defaultClock();
    
    private final ScenarioContext context;
    private final UserRole user;
    private final UserAction action;

    public ActionExecution(ScenarioContext context, UserRole user, UserAction action) {
        this.context = context;
        this.user = user;
        this.action = action;
    }
    
    public static ActionMetrics getMetrics(Class<? extends UserAction> actionClass) {
        return new ActionMetrics(actionClass);
    }
    
    @Override
    public void run() {
        CONCURRENT_USERS.inc();
        try {
            if (action instanceof CompositeAction) {
                runActions((CompositeAction) action);
            } else {
                runAction(action);
            }
        } finally {
            CONCURRENT_USERS.dec();
        }
    }

    private void runActions(CompositeAction actions) {
        for(UserAction action : actions.getActions()) {
            if(!runAction(action)) {
                break;
            }
        }
    }
    
    private boolean runAction(UserAction action) {
        String oldThreadName = Thread.currentThread().getName();
        try {

            ActionMetrics actionMetrics = getMetrics(action.getClass());
            long started = actionMetrics.start();

            Thread.currentThread().setName("UserAction " + action.toString());
            try {

                ApiApplicationDriver driver = new ApiApplicationDriver(
                        context.getServer(),
                        context.getAccounts(),
                        context.getAliasTable());

                driver.login(context.getAccounts().ensureAccountExists(user.getNickName()));

                LOGGER.fine(String.format("%s: %s Starting", user.getNickName(), action.toString()));

                action.execute(driver);

                LOGGER.fine(String.format("%s: %s Completed.", user.getNickName(), action.toString()));

                actionMetrics.succeeded(started);

                return true;
                
            } catch (Error | IllegalStateException e) {
                // Fail early and fast on configuration and pure programming errors 
                e.printStackTrace();
                System.exit(-1);
                return false;
                
                
            } catch (Exception e) {
                actionMetrics.failed();
                LOGGER.log(Level.FINE, String.format("%s: %s Failed [%s]",
                        user.getNickName(), action.toString(), e.getMessage()), e);
                return false;
                
            }
        } finally {
            Thread.currentThread().setName(oldThreadName);
        }
    }
}
