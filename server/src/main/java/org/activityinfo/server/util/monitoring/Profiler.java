package org.activityinfo.server.util.monitoring;


import com.bedatadriven.appengine.metrics.MetricsRegistry;

import java.util.concurrent.TimeUnit;

public class Profiler {
    
    
    private final long started;
    private String metric;
    private String kind;

    public Profiler(String metric, String kind) {
        this.metric = "custom.cloudmonitoring.googleapis.com/" + metric;
        this.kind = kind;
        this.started = System.nanoTime();
        MetricsRegistry.INSTANCE.meter(this.metric + "/count", kind).mark();
    }

    public void failed() {
        MetricsRegistry.INSTANCE.meter(metric + "/failure", kind).mark();
        reportTime();
    }
    
    public void succeeded() {
        reportTime();
    }

    void reportTime() {
        MetricsRegistry.INSTANCE
                .timer(metric + "/time", kind)
                .update(elapsedTime());
    }


    public long elapsedTime() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    }
}
