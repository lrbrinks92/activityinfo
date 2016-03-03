package org.activityinfo.ui.client.widget;
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.HeadingElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;
import org.activityinfo.ui.client.style.BaseStylesheet;
import org.activityinfo.ui.client.style.ModalStylesheet;

/**
 * @author yuriyz on 3/4/14.
 */
public class ModalDialog  {

    private static SimplePanel backdrop;

    private static ModalDialogBinder uiBinder = GWT
            .create(ModalDialogBinder.class);

    interface ModalDialogBinder extends UiBinder<HTMLPanel, ModalDialog> {
    }

    private IsWidget content;

    HTMLPanel dialog;

    @UiField
    HeadingElement title;

    @UiField
    FlowPanel modalBody;

    @UiField
    DivElement contentDiv;

    @UiField
    Button primaryButton;

    @UiField
    HTMLPanel modalFooter;

    @UiField
    Button cancelButton;

    @UiField
    InlineLabel statusLabel;

    @UiField Button backButton;
    @UiField
    DivElement dialogDiv;

    public ModalDialog() {
        BaseStylesheet.INSTANCE.ensureInjected();
        ModalStylesheet.INSTANCE.ensureInjected();

        dialog = uiBinder.createAndBindUi(this);
        RootPanel.get().add(dialog);
    }

    public ModalDialog(IsWidget content) {
        this();
        this.content = content;
        getModalBody().add(content);
    }

    public ModalDialog(IsWidget content, String dialogTitleHtml) {
        this(content);
        setDialogTitle(dialogTitleHtml);
    }

    public IsWidget getContent() {
        return content;
    }

    public ModalDialog show() {
        showBackdrop();
        showDialog();
        return this;
    }

    private void showDialog() {
        dialog.addStyleName("modal-open");
        dialog.addStyleName("in");
        dialog.getElement().getStyle().setDisplay(Style.Display.BLOCK);
    }

    private void showBackdrop() {
        if(backdrop == null) {
            backdrop = new SimplePanel();
            backdrop.setStyleName("modal-backdrop fade in");
            RootPanel.get().add(backdrop);
        }
        backdrop.getElement().getStyle().setDisplay(Style.Display.BLOCK);
    }

    public void hide() {
        if(backdrop != null) {
            backdrop.getElement().getStyle().setDisplay(Style.Display.NONE);
        }
        dialog.removeStyleName("modal-open");
        dialog.getElement().getStyle().setDisplay(Style.Display.NONE);
    }


    public void setDialogTitle(String dialogTitleHtml) {
        this.title.setInnerHTML(dialogTitleHtml);
    }

    public Button getPrimaryButton() {
        return primaryButton;
    }

    public Button getBackButton() {
        return backButton;
    }


    public void enablePrimaryButton() {
        primaryButton.setEnabled(true);
    }

    public void disablePrimaryButton() {
        primaryButton.setEnabled(false);
    }

    public void disableCancelButton() {
        cancelButton.setEnabled(false);
    }

    public ModalDialog hideOnOk() {
        getPrimaryButton().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                hide();
            }
        });
        return this;
    }

    public ModalDialog hideCancelButton() {
        cancelButton.setVisible(false);
        return this;
    }

    public ModalDialog hideBackButton() {
        backButton.setVisible(false);
        return this;
    }


    @UiHandler("closeButton")
    public void onClose(ClickEvent event) {
        hide();
    }

    @UiHandler("cancelButton")
    public void cancelButton(ClickEvent event) {
        hide();
    }

    public DivElement getDialogDiv() {
        return dialogDiv;
    }

    public FlowPanel getModalBody() {
        return modalBody;
    }

    public Button getCancelButton() {
        return cancelButton;
    }

    public HasText getStatusLabel() {
        return statusLabel;
    }
    
    public HTMLPanel getFooter() {
        return modalFooter;
    }
}