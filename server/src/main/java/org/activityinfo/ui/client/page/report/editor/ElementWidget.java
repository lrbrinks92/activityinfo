package org.activityinfo.ui.client.page.report.editor;

/*
 * #%L
 * ActivityInfo Server
 * %%
 * Copyright (C) 2009 - 2013 UNICEF
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.MessageBox.MessageBoxType;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.legacy.client.Dispatcher;
import org.activityinfo.legacy.shared.command.GenerateElement;
import org.activityinfo.legacy.shared.command.RenderReportHtml;
import org.activityinfo.legacy.shared.command.result.HtmlResult;
import org.activityinfo.legacy.shared.reports.content.Content;
import org.activityinfo.legacy.shared.reports.model.PivotChartReportElement;
import org.activityinfo.legacy.shared.reports.model.ReportElement;
import org.activityinfo.legacy.shared.reports.model.TextReportElement;
import org.activityinfo.ui.client.component.report.view.ChartOFCView;
import org.activityinfo.ui.client.page.report.editor.ElementDialog.Callback;

public class ElementWidget extends Composite {

    private static ElementWidgetUiBinder uiBinder = GWT.create(ElementWidgetUiBinder.class);

    interface ElementWidgetUiBinder extends UiBinder<Widget, ElementWidget> {
    }

    interface EventHandler {
        void onElementRemoveClicked(ElementWidget widget);

        void onElementChanged(ElementWidget widget);

        void onElementMove(ElementWidget elementWidget, int delta);
    }

    interface MyStyle extends CssResource {
        String title();

        String container();

        String editButton();
        
        String removeButton();

        String moveUpButton();
        
        String moveDownButton();
        
        String blockHover();
    }

    @UiField HTMLPanel htmlPanel;

    @UiField MyStyle style;

    @UiField SpanElement titleElement;
    @UiField SpanElement titleChangeElement;
    @UiField DivElement buttonElement;
    @UiField DivElement contentElement;
    @UiField DivElement contentContainerElement;
    @UiField DivElement loadingElement;
    @UiField SpanElement moveDownSpan;
    @UiField SpanElement moveUpSpan;

    private ReportElement model;

    private Dispatcher dispatcher;
    private Provider<ElementDialog> dialogProvider;

    private EventHandler parent;

    @Inject
    public ElementWidget(Dispatcher dispatcher, Provider<ElementDialog> dialogProvider) {
        this.dispatcher = dispatcher;
        this.dialogProvider = dialogProvider;

        initWidget(uiBinder.createAndBindUi(this));

        sinkEvents(Event.MOUSEEVENTS | Event.ONCLICK);
    }

    public void bindHandler(EventHandler handler) {
        this.parent = handler;
    }

    public void bind(ReportElement model) {
        this.model = model;
        titleElement.setInnerText(ElementTitles.format(model));
        // for now, preview html is rendered server side,
        // except for charts which we can't due to appthengine
        // limitations with text rendering. Thats' fine because we
        // want to do everything client side eventually anyway
        if (model instanceof PivotChartReportElement) {
            loadView();
        } else {
            loadHtml();
        }
    }

    private void loadView() {
        dispatcher.execute(new GenerateElement<Content>(model), new AsyncCallback<Content>() {

            @Override
            public void onFailure(Throwable caught) {
                // TODO
            }

            @Override
            public void onSuccess(Content result) {
                model.setContent(result);
                ChartOFCView view = new ChartOFCView();
                view.setHeight(256);
                view.setBorders(false);
                view.show((PivotChartReportElement) model);
                loadingElement.getStyle().setDisplay(Display.NONE);
                htmlPanel.add(view, contentElement);
            }
        });
    }

    public ReportElement getModel() {
        return model;
    }

    private void loadHtml() {

        contentElement.setInnerHTML("");
        loadingElement.getStyle().setDisplay(Display.BLOCK);
        if (model instanceof TextReportElement) {
            renderStaticHtml();
        } else {
            dispatcher.execute(new RenderReportHtml(model), new AsyncCallback<HtmlResult>() {

                @Override
                public void onFailure(Throwable caught) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void onSuccess(HtmlResult result) {
                    updateHtml(result.getHtml());
                }
            });
        }
    }

    private void renderStaticHtml() {
        String text = ((TextReportElement) model).getText();
        updateHtml(text != null ? SafeHtmlUtils.htmlEscape(text) : "");
    }

    private void updateHtml(String html) {
        loadingElement.getStyle().setDisplay(Display.NONE);
        contentElement.setInnerHTML(html);
    }

    @Override
    public void onBrowserEvent(Event event) {
        Element clicked = event.getEventTarget().cast();
        if (event.getTypeInt() == Event.ONCLICK) {
            if (titleChangeElement.isOrHasChild(clicked)) {
                editTitle();
            } else if (clicked.getClassName().contains(style.editButton())) {
                edit();
            } else if (clicked.getClassName().contains(style.removeButton())) {
                parent.onElementRemoveClicked(this);
            } else if (clicked.getClassName().contains(style.moveUpButton())) {
                parent.onElementMove(this, -1);
            } else if (clicked.getClassName().contains(style.moveDownButton())) { 
                parent.onElementMove(this, +1);
                
            } else if (contentElement.isOrHasChild(clicked)) {
                edit();
            } 

        } else if (event.getTypeInt() == Event.ONMOUSEOVER) {
            buttonElement.getStyle().setVisibility(Visibility.VISIBLE);
            titleChangeElement.getStyle().setVisibility(Visibility.VISIBLE);
            contentContainerElement.addClassName(style.blockHover());
        } else if (event.getTypeInt() == Event.ONMOUSEOUT) {
            buttonElement.getStyle().setVisibility(Visibility.HIDDEN);
            titleChangeElement.getStyle().setVisibility(Visibility.HIDDEN);
            contentContainerElement.removeClassName(style.blockHover());
        }
    }

    private void editTitle() {
        final MessageBox box = new MessageBox();
        box.setTitleHtml(SafeHtmlUtils.htmlEscape(I18N.CONSTANTS.changeTitleDialogTitle()));
        box.setType(MessageBoxType.PROMPT);
        box.setButtons(Dialog.OKCANCEL);
        box.show();
        box.getTextBox().setValue(model.getTitle());
        box.addCallback(new Listener<MessageBoxEvent>() {

            @Override
            public void handleEvent(MessageBoxEvent be) {
                if (be.getButtonClicked().getItemId().equals(Dialog.OK)) {
                    model.setTitle(box.getTextBox().getValue());
                    titleElement.setInnerText(ElementTitles.format(model));
                }
            }
        });
    }

    private void edit() {
        ElementDialog dialog = dialogProvider.get();
        dialog.hideCancel();
        dialog.show(model, new Callback() {

            @Override
            public void onOK(boolean dirty) {
                onElementUpdated();
            }

            @Override
            public void onClose(boolean dirty) {
                onElementUpdated();
            }
        });
    }

    private void onElementUpdated() {
        loadHtml();
        parent.onElementChanged(ElementWidget.this);
    }
}
