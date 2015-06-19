package org.activityinfo.test.ui;

import com.google.common.io.Files;
import com.google.inject.Guice;
import com.google.inject.Injector;
import cucumber.runtime.java.guice.impl.ScenarioModule;
import cucumber.runtime.java.guice.impl.SequentialScenarioScope;
import net.lightoze.gwt.i18n.server.ThreadLocalLocaleProvider;
import org.activityinfo.test.driver.AliasTable;
import org.activityinfo.test.driver.ApiApplicationDriver;
import org.activityinfo.test.driver.DriverModule;
import org.activityinfo.test.driver.UiApplicationDriver;
import org.activityinfo.test.pageobject.web.ApplicationPage;
import org.activityinfo.test.pageobject.web.LoginPage;
import org.activityinfo.test.sut.DevServerAccounts;
import org.activityinfo.test.sut.SystemUnderTest;
import org.activityinfo.test.sut.UserAccount;
import org.activityinfo.test.webdriver.WebDriverModule;
import org.activityinfo.test.webdriver.WebDriverSession;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class UiDriver extends TestWatcher {

    private final SequentialScenarioScope scenarioScope;
    private final Injector injector;

    private ApplicationPage applicationPage;
    private UserAccount currentUser;
    private File attachmentDir;
    private String methodName; 
    
    public UiDriver() {
        scenarioScope = new SequentialScenarioScope();
        injector = Guice.createInjector(
                new SystemUnderTest(),
                new WebDriverModule(),
                new ScenarioModule(scenarioScope),
                new DriverModule("web"));

    }

    @Override
    protected void starting(Description description) {
        scenarioScope.enterScope();
        injector.getInstance(WebDriverSession.class).beforeTest(
                description.getTestClass().getName() + "." + description.getMethodName());
        try {
            attachmentDir = attachmentDir(description);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create attachment directory");
        }
        methodName = description.getMethodName();
    }

    @Override
    protected void failed(Throwable e, Description description) {
        WebDriverSession session = injector.getInstance(WebDriverSession.class);
        if(session.isRunning()) {
            attachScreenshot("failure");
        }
    }

    public void attachScreenshot(final String name) {
        try {
            WebDriver webDriver = injector.getInstance(WebDriver.class);
            byte[] screenshot = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);
            File screenshotFile = new File(attachmentDir, methodName + "-" + name + ".png");
            Files.write(screenshot, screenshotFile);
            
        } catch (Exception e) {
            System.err.println("Failed to dump screenshot on failure");
            e.printStackTrace();
        }
    }

    /**
     * Locates and creates the directory for test run attachments used by the Jenkins Attachments Plugin.
     * 
     * @see <a href="https://wiki.jenkins-ci.org/display/JENKINS/JUnit+Attachments+Plugin">Jenkins Attachment Plugin</a>
     *
     */
    private File attachmentDir(Description description) throws IOException {
        String relPath = getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        File dir = new File(relPath + "../../test-results/" + description.getClassName());
        if(!dir.exists()) {
            boolean created = dir.mkdirs();
            if(!created) {
                throw new IOException("Could not create attachment directory " + dir);
            }
        }
        return dir;
    }

    @Override
    protected void finished(Description description) {
        shutdownWebDriver();
        scenarioScope.exitScope();
    }

    private void shutdownWebDriver() {
        WebDriverSession session = injector.getInstance(WebDriverSession.class);
        if(session.isRunning()) {
            session.stop();
        }
    }

    public UserAccount anyAccount() {
        return injector.getInstance(DevServerAccounts.class).any();
    }

    public UiApplicationDriver ui() {
        return injector.getInstance(UiApplicationDriver.class);
    }

    public ApiApplicationDriver setup() {
        return (ApiApplicationDriver) ui().setup();
    }

    public void loginAsAny() {
        currentUser = anyAccount();
        ui().login(currentUser);
    }

    public ApplicationPage applicationPage() {
        if(applicationPage == null) {
            applicationPage = injector.getInstance(LoginPage.class)
                    .navigateTo().loginAs(currentUser).andExpectSuccess();
        }
        return applicationPage;
    }

    public String alias(String testHandle) {
        return injector.getInstance(AliasTable.class).getAlias(testHandle);
    }

    public void setLocale(String locale) {
        ThreadLocalLocaleProvider.pushLocale(Locale.forLanguageTag(locale));
        injector.getInstance(DevServerAccounts.class).setLocale(locale);
    }

    public String getAlias(String testHandle) {
        return setup().getAliasTable().getAlias(testHandle);
    }
}
