package org.activityinfo.test.pageobject.api;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;
import org.activityinfo.test.harness.ScreenShotLogger;
import org.activityinfo.test.sut.Server;
import org.openqa.selenium.WebDriver;

import javax.inject.Inject;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Defines a pageobject object
 */
public abstract class PageObject {

    @Inject
    protected Server server;

    @Inject
    protected WebDriver driver;

    @Inject
    protected PageBinder binder;

    @Inject
    protected ScreenShotLogger logger;

    public String getPageUrl() {
        return server.path(getPagePath(getClass()));
    }

    public static String getPagePath(Class<? extends PageObject> pageClass) {
        Path path = pageClass.getAnnotation(Path.class);
        if(path == null) {
            throw new UnsupportedOperationException(pageClass + " does not have a " +
                    "@" + Path.class.getSimpleName() + " annotation");
        }
        return path.value();
    }
    
    protected final <T> T waitFor(String what, int timeoutInSeconds, Callable<Optional<T>> locator) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        while(stopwatch.elapsed(TimeUnit.SECONDS) < timeoutInSeconds) {
            try {
                Optional<T> value = locator.call();
                if(value.isPresent()) {
                    return value.get();
                }
            } catch (Exception e) {
                throw new AssertionError(String.format("Error while waiting for '%s'",
                         what));            
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new AssertionError(String.format("Interrupted while waiting for '%s'",
                        what));
            }
        }
        logger.snapshot();
        throw new AssertionError(String.format("Timed out after waiting %d seconds for '%s'", timeoutInSeconds, what));
    }

    protected final void waitFor(WaitBuilder spec) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        while(stopwatch.elapsed(TimeUnit.SECONDS) < spec.timeOutInSeconds) {
            if(spec.isReady(driver)) {
                return;
            }
            if(spec.isError(driver)) {
                throw new AssertionError(String.format("Error while waiting for '%s'",
                        spec.condition.toString()));
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new AssertionError(String.format("Interrupted while waiting for '%s'",
                        spec.condition.toString()));
            }
        }
        logger.snapshot();
        throw new AssertionError(String.format("Timed out after waiting %d seconds for '%s'",
                spec.timeOutInSeconds,
                spec.condition.toString()));
    }

    protected final Predicate<WebDriver> browserNavigatesAway() {
        return new Predicate<WebDriver>() {
            @Override
            public boolean apply(WebDriver webDriver) {
                return !binder.isCurrentPage(PageObject.this.getClass());
            }
        };
    }

    protected <T extends PageObject> WaitBuilder navigationTo(final Class<T> pageClass) {
        return new WaitBuilder(new Predicate<WebDriver>() {
            @Override
            public boolean apply(WebDriver webDriver) {
                return binder.isCurrentPage(pageClass);
            }

            @Override
            public String toString() {
                return "navigation to '" + binder.pageUrl(pageClass) + "'";
            }
        });
    }


    protected final Predicate<WebDriver> urlContains(final String text) {
        return new Predicate<WebDriver>() {
            @Override
            public boolean apply(WebDriver input) {
                return input.getCurrentUrl().toLowerCase().contains(text);
            }
        };
    }

    /**
     * Wait until we have navigated to the right URL
     */
    public final void waitFor() {
        final String initialUrl = PageBinder.withoutTarget(driver.getCurrentUrl());
    
        waitFor("navigation to " + getClass().getSimpleName(), 30, new Callable<Optional<PageObject>>() {
            @Override
            public Optional<PageObject> call() throws Exception {
                String currentUrl = PageBinder.withoutTarget(driver.getCurrentUrl());
                
                if(binder.pageUrl(PageObject.this.getClass()).equals(currentUrl)) {
                    return Optional.of(PageObject.this);
                }
                return Optional.absent();
            }
        });
    }
}
