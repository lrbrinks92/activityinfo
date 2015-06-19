package org.activityinfo.test.capacity.action;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.activityinfo.test.capacity.Metrics;

import java.util.concurrent.ExecutionException;

import static com.codahale.metrics.MetricRegistry.name;


class RegionMetrics {

    private static final LoadingCache<String, RegionMetrics> CACHE = CacheBuilder.newBuilder()
            .concurrencyLevel(150)
            .build(new RegionMetricsFactory());
    
    final Histogram size;
    final Timer latency;
    final Meter succeeded;
    final Meter failed;
    
    RegionMetrics(String id) {
        size = Metrics.REGISTRY.histogram(name("sync", "size", id));
        latency = Metrics.REGISTRY.timer(name("sync", "latency", id));
        succeeded = Metrics.REGISTRY.meter(name("sync", "succeeded", id));
        failed = Metrics.REGISTRY.meter(name("sync", "failed", id));
    }
    
    private static class RegionMetricsFactory extends CacheLoader<String, RegionMetrics> {

        @Override
        public RegionMetrics load(String key) throws Exception {
            int slash = key.indexOf('/');
            if(slash == -1) {
                return new RegionMetrics(key);
            } else {
                return new RegionMetrics(key.substring(0, slash));
            }
        }
    }

    public static RegionMetrics get(String id) throws ExecutionException {
        return CACHE.get(id);
    }
    
}
