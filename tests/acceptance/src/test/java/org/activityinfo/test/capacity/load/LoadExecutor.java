package org.activityinfo.test.capacity.load;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import org.joda.time.Duration;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class LoadExecutor  {

    private static final Logger LOGGER = Logger.getLogger(LoadExecutor.class.getName());

    private ExecutorCompletionService<Void> executorService;

    private Random random = new Random();
    private Function<Duration, Integer> maxConcurrentUsers;

    public LoadExecutor(ExecutorService service) {
        this.executorService = new ExecutorCompletionService<>(service);
    }

    public void setMaxConcurrent(LogisticGrowthFunction function) {
        this.maxConcurrentUsers = function;
    }
    
    public void execute(List<Runnable> tasks) throws InterruptedException {

        LinkedList<Runnable> todo = Lists.newLinkedList(tasks);
        Collections.shuffle(todo);
        
        Stopwatch stopwatch = Stopwatch.createStarted();

        int numActive = 0;
        
        while(todo.size() > 0 || numActive > 0) {

            int maxConcurrent = maxConcurrent(stopwatch);

            while (numActive < maxConcurrent && !todo.isEmpty()) {
                executorService.submit(todo.pop(), null);
                numActive++;
            }

            while(executorService.poll() != null) {
                numActive--;
            }
            Thread.sleep(100);
        }
    }

    private int maxConcurrent(Stopwatch stopwatch) {
        return Math.max(1, maxConcurrentUsers.apply(Duration.millis(stopwatch.elapsed(TimeUnit.MILLISECONDS))));
    }

}