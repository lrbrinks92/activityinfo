package org.activityinfo.test.capacity.logging;

import com.google.common.base.Joiner;
import com.google.common.io.Resources;
import org.activityinfo.test.capacity.Metrics;
import org.activityinfo.test.capacity.action.ActionExecution;
import org.activityinfo.test.capacity.action.SynchronizeAction;
import org.activityinfo.test.driver.ApiApplicationDriver;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Sets up logging for the test run
 */
public class CapacityTestLogging {


    private static ScheduledExecutorService SCHEDULER;

    public static void setup() throws IOException {
        // Divert System.err because webDriver just dumps everything without synchronization
        // System.setErr(new PrintStream(new NullOutputStream()));


        try(InputStream in = Resources.getResource(CapacityTestLogging.class, "logging.properties").openStream()) {
            LogManager.getLogManager().readConfiguration(in);
        }

        SCHEDULER = Executors.newSingleThreadScheduledExecutor();
        SCHEDULER.scheduleWithFixedDelay(new MetricLogger(), 10, 10, TimeUnit.SECONDS);
    }

    public static void stop() throws InterruptedException {
        SCHEDULER.shutdown();
        SCHEDULER.awaitTermination(5, TimeUnit.SECONDS);
    }

    private static class MetricLogger implements Runnable {
        
        private Logger logger = Logger.getLogger(Metrics.class.getName());

        private ActionExecution.ActionMetrics syncMetrics;
        
        public MetricLogger() {
            syncMetrics = ActionExecution.getMetrics(SynchronizeAction.class);
        }

        @Override
        public void run() {
            logger.log(Level.INFO, Joiner.on("   ").join(concurrentUsers(), throughput(), sync(), failure()));
                
        }
        
        private String concurrentUsers() {
            return String.format("users[%3d]", ActionExecution.CONCURRENT_USERS.getCount());
        }
        
        private String throughput() {
            return String.format("throughput[%4.1f qps]",  ApiApplicationDriver.COMMAND_RATE.getOneMinuteRate());
        }
        
        private String failure() {
            long count = ApiApplicationDriver.ERROR_RATE.getTotalErrorCount();
            if(count == 0) {
                return "";
            } else {
                return String.format("errors[%4d]", count);
            }
        }
        
        private String sync() {
            long concurrentUser = ActionExecution.CONCURRENT_USERS.getCount();
            double meanSizeKb = SynchronizeAction.TOTAL_SIZE_METRIC.getSnapshot().getMedian() / 1024d;
            long latency = syncMetrics.getOneMinuteLatencySeconds();
            double successRate = syncMetrics.getOneMinuteSuccessRate();
            return String.format("sync[ %3d %4.0fkb %2ds %2.0f%%]", concurrentUser, meanSizeKb, latency, successRate);
        }
    }
}
