package org.activityinfo.test.capacity;

import com.codahale.metrics.*;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Metrics {

    public static final MetricRegistry REGISTRY = new MetricRegistry();

    private static CsvReporter CSV_REPORTER;
    
    private static final Logger LOGGER = Logger.getLogger(Metrics.class.getName());

    private static final ConcurrentHashMap<Metric, Long> LAST_UPDATE = new ConcurrentHashMap<>();

    public static void start() throws Exception {
        File metricsDir = new File("metrics");
        if(!metricsDir.exists()) {
            metricsDir.mkdirs();
        }
        CSV_REPORTER = CsvReporter.forRegistry(REGISTRY)
                .formatFor(Locale.US)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build(metricsDir);

        CSV_REPORTER.start(1, TimeUnit.SECONDS);
        
    }

    public static void stop() {
        CSV_REPORTER.stop();
    }

    public static void log(String label, Meter meter) {
        if(timeForUpdate(meter)) {
            LOGGER.log(Level.INFO, String.format("%20s: %.0f", label, meter.getOneMinuteRate()));
        }
    }

    public static void log(String label, Timer timer) {
        LOGGER.log(Level.INFO, String.format("%20s: %0f", label, timer.getMeanRate()));
    }
    
    private static boolean timeForUpdate(Metric metric) {
        Long lastUpdate = LAST_UPDATE.get(metric);
        if(lastUpdate == null) {
            LAST_UPDATE.put(metric, System.currentTimeMillis());
        } else {
            if((System.currentTimeMillis()-lastUpdate) > 10000) {
                LAST_UPDATE.put(metric, System.currentTimeMillis());
                return true;
            }
        }
        return false;
    }
}
