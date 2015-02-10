package org.activityinfo.test.pageobject.api;

import com.google.inject.Injector;
import cucumber.runtime.java.guice.ScenarioScoped;
import org.activityinfo.test.sut.Server;
import org.activityinfo.test.webdriver.WebDriverSession;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.AjaxElementLocatorFactory;
import org.openqa.selenium.support.pagefactory.FieldDecorator;

import javax.inject.Inject;

@ScenarioScoped
public class PageBinder {

    private final Injector injector;
    private final WebDriver webDriver;
    private final Server systemUnderTest;

    @Inject
    public PageBinder(Injector injector, WebDriverSession webDriver, Server systemUnderTest) {
        this.injector = injector;
        this.webDriver = webDriver.getDriver();
        this.systemUnderTest = systemUnderTest;
    }

    public <T> void bind(T instance) {
        bind(injector.getInstance(WebDriver.class), instance);
    }

    public <T> void bind(SearchContext context, T instance) {
        AjaxElementLocatorFactory factory = new AjaxElementLocatorFactory(context, 30);
        FieldDecorator decorator = new MyFieldDecorator(factory);
        PageFactory.initElements(decorator, instance);
    }

    public <T extends PageObject> T navigateTo(Class<T> pageObjectClass) {
        if(!isCurrentPage(pageObjectClass)) {
            webDriver.navigate().to(pageUrl(pageObjectClass));
        }
        return waitFor(pageObjectClass);
    }

    public <T extends PageObject> boolean isCurrentPage(Class<T> pageClass) {
        return withoutTarget(webDriver.getCurrentUrl()).equals(pageUrl(pageClass));
    }

    public <T extends PageObject> String pageUrl(Class<T> pageObjectClass) {
        return systemUnderTest.path(PageObject.getPagePath(pageObjectClass));
    }

    public static String withoutTarget(String url) {
        int hashStart = url.indexOf('#');
        if(hashStart == -1) {
            return url;
        } else {
            return url.substring(0, hashStart);
        }
    }

    /**
     * Creates a new instance of the given {@code PageObject} and matches
     * its elements to the currently open page.
     *
     * @param pageObjectClass the class of a PageObject to create
     * @return a fully initialized PageObject
     * @throws java.lang.AssertionError if the PageObject's elements cannot be found
     */
    public <T extends PageObject> T waitFor(Class<T> pageObjectClass) {
        T instance = injector.getInstance(pageObjectClass);
        bind(injector.getInstance(WebDriver.class), instance);
        instance.waitFor();
        return instance;
    }

    public <T> T create(SearchContext context, Class<T> clazz) {
        T instance = injector.getInstance(clazz);
        bind(context, instance);
        return instance;
    }
}
