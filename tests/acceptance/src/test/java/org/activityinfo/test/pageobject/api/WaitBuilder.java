package org.activityinfo.test.pageobject.api;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;


public class WaitBuilder {


    Predicate<WebDriver> condition;
    long timeOutInSeconds = 30;
    List<Predicate<WebDriver>> errorConditions = Lists.newArrayList();

    public WaitBuilder(Predicate<WebDriver> condition) {
        this.condition = condition;
    }



    public static WaitBuilder anyElement(final By by) {
        return new WaitBuilder(new AnyElementPredicate(by));
    }

    public static By withClass(String className) {
        return By.className(className);
    }

    public WaitBuilder butFailIf(Predicate<WebDriver> failureCondition) {
        errorConditions.add(failureCondition);
        return this;
    }

    public WaitBuilder butCheckForErrorMessage(By by) {
        errorConditions.add(new ThrowIf(by));
        return this;
    }

    public boolean isReady(WebDriver driver) {
        return condition.apply(driver);
    }

    public boolean isError(WebDriver driver) {
        // Stop early if we find an error condition
        for (Predicate<WebDriver> errorCondition : errorConditions) {
            try {
                if (errorCondition.apply(driver)) {
                    return true;
                }
            } catch(StaleElementReferenceException ignored) {
            }
        }
        return false;
    }

    private static class ThrowIf implements Predicate<WebDriver> {
        private final By by;

        public ThrowIf(By by) {
            this.by = by;
        }

        @Override
        public boolean apply(WebDriver webDriver) {
            List<WebElement> elements;
            try {
                elements = webDriver.findElements(by);
            } catch (Exception ignored) {
                return false;
            }
            if(!elements.isEmpty()) {
                WebElement element = elements.get(0);
                if(element.isDisplayed()) {
                    throw new AssertionError("Found element " + by + ": " + element.getText());
                }
            }
            return false;
        }
    }

    private static class AnyElementPredicate implements Predicate<WebDriver> {
        private final By by;

        public AnyElementPredicate(By by) {
            this.by = by;
        }

        @Override
        public boolean apply(WebDriver webDriver) {
            List<WebElement> elements = webDriver.findElements(by);
            return !elements.isEmpty() && elements.get(0).isDisplayed();
        }

        @Override
        public String toString() {
            return "any element " + by;
        }
    }
}
