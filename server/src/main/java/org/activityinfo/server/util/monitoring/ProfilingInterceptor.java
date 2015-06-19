package org.activityinfo.server.util.monitoring;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;


public class ProfilingInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        String metricId = null;
        boolean timed = false;

        Count countAnnotation = invocation.getMethod().getAnnotation(Count.class);
        if(countAnnotation != null) {
            metricId = countAnnotation.value();
        }
        Timed timeAnnotation = invocation.getMethod().getAnnotation(Timed.class);
        if(timeAnnotation != null) {
            metricId = timeAnnotation.name();
            timed = true;
        }
//        
//        Profiler profiler = metrics.profile(metricId);
//        try {
//            Object result = invocation.proceed();
//            profiler.markSuccess();
//            if(timed) {
//                profiler.reportTime();
//            }
//            return result;
//        } catch (Throwable caught) {
//            profiler.failed();
//            if(timed) {
//                profiler.reportTime();
//            }
//            throw caught;
//        }
        throw new UnsupportedOperationException();
    }
}
