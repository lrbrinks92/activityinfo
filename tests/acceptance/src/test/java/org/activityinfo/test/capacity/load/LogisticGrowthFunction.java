package org.activityinfo.test.capacity.load;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import org.joda.time.Duration;
import org.joda.time.Period;

import javax.annotation.Nonnull;

/**
 * Function of time that uses a logistic growth function for a ramp up period
 * followed by a steady state.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Logistic_function">Logistic Function (Wikipedia)</a>
 */
public class LogisticGrowthFunction implements Function<Duration, Integer> {

    private int maxValue = 10;
    private double rampUpDuration = 1000;

    public LogisticGrowthFunction during(Period period) {
        this.rampUpDuration = period.toStandardDuration().getMillis();
        return this;
    }

    @Override
    public Integer apply(@Nonnull Duration input) {
        return apply(input.getMillis() / rampUpDuration);
    }

    @VisibleForTesting
    int apply(double proportionElapsed) {

        // scale x into the range [-6, 6]
        double x = (proportionElapsed * 12d) - 6d;
        double y = 1d / (1d + Math.exp(-x));

        // scale y to our max value
        return (int)Math.round( y * ((double)maxValue));
    }

    public static LogisticGrowthFunction rampUpTo(int maxValue) {
        LogisticGrowthFunction f = new LogisticGrowthFunction();
        f.maxValue = maxValue;
        return f;
    }
}