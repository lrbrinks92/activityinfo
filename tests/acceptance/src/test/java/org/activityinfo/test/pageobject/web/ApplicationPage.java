package org.activityinfo.test.pageobject.web;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import net.lightoze.gwt.i18n.server.LocaleProxy;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.test.driver.OfflineMode;
import org.activityinfo.test.pageobject.api.FluentElement;
import org.activityinfo.test.pageobject.gxt.Gxt;
import org.activityinfo.test.pageobject.gxt.GxtModal;
import org.activityinfo.test.pageobject.web.design.DesignPage;
import org.activityinfo.test.pageobject.web.design.DesignTab;
import org.activityinfo.test.pageobject.web.design.designer.FormDesignerPage;
import org.activityinfo.test.pageobject.web.entry.DataEntryTab;
import org.activityinfo.test.pageobject.web.entry.TablePage;
import org.activityinfo.test.pageobject.web.reports.ReportsTab;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.activityinfo.test.pageobject.api.XPathBuilder.*;
import static org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated;

/**
 * Interface to the single-pageobject application
 */
public class ApplicationPage {
    
    static {
        LocaleProxy.initialize();
    }

    private static final Logger LOGGER = Logger.getLogger(ApplicationPage.class.getName());
    
    private static final By SETTINGS_BUTTON = By.xpath("//div[text() = 'ActivityInfo']/following-sibling::div[2]");
    private static final By LOCALE_MENU_BUTTON = By.xpath("//div[text() = 'ActivityInfo']/following-sibling::div[4]");
    private static final By DESIGN_TAB = By.xpath("//div[contains(text(), 'Design')]");
    
    private final FluentElement page;

    @Inject
    public ApplicationPage(WebDriver webDriver) {
        this.page = new FluentElement(webDriver);        
    }
    
    public ApplicationPage(FluentElement page) {
        this.page = page;
    }

    public Dashboard dashboard() {
        return new Dashboard(container());
    }
    
    public void waitUntilLoaded() {
        page.waitUntil(invisibilityOfElementLocated(By.id("loading")));
    }
    
    public SettingsMenu openSettingsMenu() {
        page.findElement(SETTINGS_BUTTON).click();
        
        return new SettingsMenu(page);
    }

    public LocaleMenu openLocaleMenu() {
        page.findElement(LOCALE_MENU_BUTTON).click();

        return new LocaleMenu(page);
    }
    
    public OfflineMode getOfflineMode() {
        return page.waitFor(new Function<WebDriver, OfflineMode>() {
            @Nullable
            @Override
            public OfflineMode apply(WebDriver driver) {
                List<WebElement> elements = driver.findElements(By.className("x-status-text"));
                for (WebElement element : elements) {
                    if (element.getText().contains(I18N.CONSTANTS.workingOnline()) || element.getText().contains("Last sync")) {
                        return OfflineMode.ONLINE;

                    } else if (element.getText().contains(I18N.CONSTANTS.workingOffline())) {
                        return OfflineMode.OFFLINE;
                    }
                }
                return null;
            }
        });
    }

    public void assertOfflineModeLoads() {
        assertOfflineModeLoads(15, MINUTES);
    }

    public void assertOfflineModeLoads(int timeout, TimeUnit timeoutUnits) {
        page.wait(timeout, timeoutUnits).until(new Predicate<WebDriver>() {

            private String lastStatus = "";

            @Override
            public boolean apply(WebDriver driver) {
                List<WebElement> elements = driver.findElements(By.className("x-status-text"));
                for (WebElement element : elements) {
                    if (element.getText().contains(I18N.CONSTANTS.workingOffline())) {
                        return true;

                    } else if (element.getText().contains(I18N.CONSTANTS.syncError())) {
                        throw new AssertionError(element.getText());


                    } else if (element.getText().contains("%")) {
                        String status = element.getText();
                        if (!lastStatus.equals(status)) {
                            LOGGER.info("Offline Status: " + status);
                            lastStatus = status;
                        }
                    }
                }
                return false;
            }
        });
    }


    public Dashboard navigateToDashboard() {
        try {
            page.find().div(containingText(I18N.CONSTANTS.dashboard())).clickWhenReady();
        } catch(Exception ignored) {
        }

        return new Dashboard(container());
    }
    
    public DataEntryTab navigateToDataEntryTab() {
        try {
            page.find().div(containingText(I18N.CONSTANTS.dataEntry())).clickWhenReady();
        } catch(Exception ignored) {
        }

        // we may got "Save" dialog before leaving the current page
        closeSaveDialogSilently(false);

        return new DataEntryTab(container());
    }

    public DesignTab navigateToDesignTab() {
        try {
            page.findElement(DESIGN_TAB).click();
        } catch(Exception ignored) {
        }
        
        // check for modal dialog prompting to save
        Optional<FluentElement> discard = page.root().find().button(containingText("Discard")).firstIfPresent();
        if(discard.isPresent()) {
            discard.get().click();
        }

        return new DesignTab(container());
    }

    public TablePage navigateToTable(String database, String formName) {
        DesignTab designTab = navigateToDesignTab();
        designTab.selectDatabase(database);

        DesignPage designPage = designTab.design();
        designPage.getDesignTree().select(formName);
        designPage.getToolbarMenu().clickButton(I18N.CONSTANTS.openTable());
        return new TablePage(page);
    }

    public FormDesignerPage navigateToFormDesigner(String database, String formName) {
        DesignTab designTab = navigateToDesignTab();
        designTab.selectDatabase(database);

        DesignPage designPage = designTab.design();
        designPage.getDesignTree().select(formName);
        designPage.getToolbarMenu().clickButton(I18N.CONSTANTS.openFormDesigner());
        page.waitUntil(new Predicate<WebDriver>() {
            @Override
            public boolean apply(WebDriver input) {
                return page.find().div(withClass(FormDesignerPage.DROP_TARGET_CLASS)).exists();
            }
        });
        return new FormDesignerPage(page);
    }


    
    public ReportsTab navigateToReportsTab() {
        FluentElement container = container();
        container.find().div(withText("Reports")).clickWhenReady();

        // we may got "Save" dialog before leaving the current page
        closeSaveDialogSilently(true);
        
        return new ReportsTab(container);
    }

    public void closeSaveDialogSilently(boolean discard) {
        try {
            GxtModal gxtModal = new GxtModal(page, 2);
            if (discard) {
                gxtModal.discardChanges();
            } else {
                gxtModal.accept();
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private FluentElement container() {
        return page.waitFor(By.className(Gxt.BORDER_LAYOUT_CONTAINER));
    }

}
