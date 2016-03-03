package org.activityinfo.ui.client.page.home;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import org.activityinfo.core.client.ResourceLocator;
import org.activityinfo.ui.client.page.*;
import org.activityinfo.ui.client.page.instance.InstancePage;
import org.activityinfo.ui.client.page.instance.InstancePlace;
import org.activityinfo.ui.client.style.BaseStylesheet;

public class PageLoader implements org.activityinfo.ui.client.page.PageLoader {

    private final NavigationHandler pageManager;
    private ResourceLocator resourceLocator;

    @Inject
    public PageLoader(NavigationHandler pageManager,
                      PageStateSerializer placeSerializer,
                      ResourceLocator resourceLocator) {

        this.resourceLocator = resourceLocator;
        this.pageManager = pageManager;


        pageManager.registerPageLoader(InstancePage.DESIGN_PAGE_ID, this);
        placeSerializer.registerParser(InstancePage.DESIGN_PAGE_ID, new InstancePlace.Parser(InstancePage.DESIGN_PAGE_ID));

        pageManager.registerPageLoader(InstancePage.TABLE_PAGE_ID, this);
        placeSerializer.registerParser(InstancePage.TABLE_PAGE_ID, new InstancePlace.Parser(InstancePage.TABLE_PAGE_ID));
    }

    @Override
    public void load(final PageId pageId, final PageState pageState, final AsyncCallback<Page> callback) {

        BaseStylesheet.INSTANCE.ensureInjected();

        GWT.runAsync(new RunAsyncCallback() {
            @Override
            public void onSuccess() {
                if (pageState instanceof InstancePlace) {
                    InstancePlace instancePlace = (InstancePlace) pageState;
                    InstancePage page = new InstancePage(resourceLocator, instancePlace.getPageId(), pageManager.getEventBus());
                    page.navigate(pageState);
                    callback.onSuccess(page);
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                callback.onFailure(throwable);
            }
        });
    }

}
