package org.activityinfo.test.driver;

import com.codahale.metrics.*;

/**
 * Created by alex on 20-3-15.
 */
public class ApiErrorRate implements Metric {
    
    
    private Meter commands = new Meter();
    private Meter errors = new Meter();

    public void markSubmitted() {
        commands.mark();
    }
    
    public void markError() {
        errors.getCount();
    }
    
    public long getTotalErrorCount() {
        return errors.getCount();
    }
    
    public double getTotalErrorRate() {
        double numerator =  errors.getCount();
        double denominator = commands.getCount();
        return numerator / denominator;
    }
    
    public double getOneMinuteRate() {
        double numerator =  errors.getOneMinuteRate();
        double denominator = commands.getOneMinuteRate();
        return numerator / denominator;
    }
    
}
