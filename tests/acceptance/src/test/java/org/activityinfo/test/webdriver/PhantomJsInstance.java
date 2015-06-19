package org.activityinfo.test.webdriver;

import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import org.activityinfo.test.config.ConfigProperty;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wraps a single PhantomJS process with its own home directory
 * to avoid errors due to local storage and AppCache filling up
 */
public class PhantomJsInstance {
    
    private static final Logger LOGGER = Logger.getLogger(PhantomJsInstance.class.getName());

    public static final ConfigProperty PHANTOM_JS_PATH = new ConfigProperty("phantomjsPath", "PhantomJS binary path");

    private File homeDir;
    private WebDriver webDriver;
    private File logFile;
    private File localStoragePath = null;
    private ProxyController proxyController;
    
    public PhantomJsInstance() {
        this.homeDir = Files.createTempDir();
        this.logFile = new File(homeDir, "phantomjs.log");
    }
    
    public void start() {
        PrintStream errorStream = System.err;

        try {

            // WebDriver dumps EVERYTHING to stderr
            System.setErr(new PrintStream(ByteStreams.nullOutputStream()));

            List<String> arguments = Lists.newArrayList();
            arguments.add("--webdriver-loglevel=INFO");
            if(localStoragePath != null) {
                arguments.add("--local-storage-path=" + localStoragePath.getAbsolutePath());
            }

            Map<String, String> environment = new HashMap<>();
            if (homeDir != null) {
                environment.put("HOME", homeDir.getAbsolutePath());
            }
            
            proxyController = new ProxyController();
            proxyController.start();

            PhantomJSDriverService service = new PhantomJSDriverService.Builder()
                    .usingPhantomJSExecutable(PHANTOM_JS_PATH.getFile())
                    .usingAnyFreePort()
                    .withLogFile(logFile)
                    .withEnvironment(environment)
                    .usingCommandLineArguments(arguments.toArray(new String[0]))
                    .build();

            DesiredCapabilities capabilities = new DesiredCapabilities();
            capabilities.setCapability(CapabilityType.PROXY, proxyController.getWebDriverProxy());

            webDriver = new PhantomJSDriver(service, capabilities);
            webDriver.manage().window().setSize(new Dimension(1400, 1000));

        } finally {
            System.setErr(errorStream);
        }
    }

    public WebDriver getWebDriver() {
        return webDriver;
    }

    public void clearHomeDir() {
        try {
            java.nio.file.Files.deleteIfExists( new File(homeDir, ".qws").toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        try {
            proxyController.stop();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to shutdown proxy", e);
        }
        getWebDriver().quit();
    }
}
