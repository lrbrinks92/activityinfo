package org.activityinfo.ui.client.widget.loading;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import org.activityinfo.ui.client.style.Icons;
import org.activityinfo.ui.client.widget.Button;

/**
 * Loading Panel view for top level views
 */
public class PageLoadingPanel implements IsWidget, LoadingPanelView {

    private final HTMLPanel rootElement;

    @UiField
    Element icon;

    @UiField
    Element heading;

    @UiField
    Element explanation;

    @UiField
    Button retryButton;

    @UiField
    SimplePanel content;


    interface PageLoadingUiBinder extends UiBinder<HTMLPanel, PageLoadingPanel> {
    }

    private static PageLoadingUiBinder ourUiBinder = GWT.create(PageLoadingUiBinder.class);

    public PageLoadingPanel() {
        rootElement = ourUiBinder.createAndBindUi(this);

        Icons.INSTANCE.ensureInjected();
        LoadingStylesheet.INSTANCE.ensureInjected();
    }

    @Override
    public void onLoadingStateChanged(LoadingState state, Throwable caught) {
        rootElement.setStyleName(LoadingStylesheet.INSTANCE.loading(), state == LoadingState.LOADING);
        rootElement.setStyleName(LoadingStylesheet.INSTANCE.failed(), state == LoadingState.FAILED);
        rootElement.setStyleName(LoadingStylesheet.INSTANCE.loaded(), state == LoadingState.LOADED);

        if(state == LoadingState.FAILED) {
            icon.setClassName(ExceptionOracle.getIcon(caught));
            heading.setInnerText(ExceptionOracle.getHeading(caught));
            explanation.setInnerText(ExceptionOracle.getExplanation(caught));
        }
    }

    @Override
    public Widget getWidget() {
        return content.getWidget();
    }

    @Override
    public void setWidget(Widget w) {
        onLoadingStateChanged(LoadingState.LOADED, null);
        content.setWidget(w);
    }

    public void setContentStyleName(String styleName) {
        content.setStyleName(styleName);
    }

    @Override
    public void setWidget(IsWidget widget) {
        setWidget(widget.asWidget());
    }

    @Override
    public HasClickHandlers getRetryButton() {
        return retryButton;
    }

    @Override
    public Widget asWidget() {
        return rootElement;
    }
}