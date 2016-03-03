package org.activityinfo.ui.client.component.form;
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

import com.google.gwt.dom.builder.shared.IFrameBuilder;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.activityinfo.core.client.ResourceLocator;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.legacy.shared.Log;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.promise.Promise;
import org.activityinfo.ui.client.component.form.field.FieldWidgetMode;
import org.activityinfo.ui.client.component.form.field.FormFieldWidgetFactory;
import org.activityinfo.ui.client.style.ElementStyle;
import org.activityinfo.ui.client.style.ModalStylesheet;
import org.activityinfo.ui.client.widget.Button;
import org.activityinfo.ui.client.widget.LoadingPanel;
import org.activityinfo.ui.client.widget.ModalDialog;
import org.activityinfo.ui.client.widget.loading.ExceptionOracle;
import org.activityinfo.ui.client.widget.loading.PageLoadingPanel;

import javax.inject.Provider;

/**
 * @author yuriyz on 3/28/14.
 */
public class FormDialog {

    private FormDialogCallback callback;

    private final ResourceLocator resourceLocator;

    private final ModalDialog dialog;
    private final SimpleFormPanel formPanel;
    private final LoadingPanel<FormInstance> loadingPanel;

    public FormDialog(ResourceLocator resourceLocator) {
        this.resourceLocator = resourceLocator;

        ModalStylesheet.INSTANCE.ensureInjected();


        formPanel = new SimpleFormPanel(
                resourceLocator,
                new VerticalFieldContainer.Factory(),
                new FormFieldWidgetFactory(resourceLocator, FieldWidgetMode.NORMAL));


        loadingPanel = new LoadingPanel<>(new PageLoadingPanel());
        loadingPanel.setDisplayWidget(formPanel);
        dialog = new ModalDialog(loadingPanel);
        dialog.getPrimaryButton().setText(I18N.CONSTANTS.save());
        dialog.getPrimaryButton().setStyleName("btn btn-primary");
        dialog.getPrimaryButton().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                save();
            }
        });

        Button printButton = new Button(ElementStyle.DEFAULT);
        printButton.setLabel("Print");
        printButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                printForm();
            }
        });
        dialog.getFooter().add(printButton);
    }


    public void setDialogTitle(String text) {
        dialog.setDialogTitle(text);
    }

    public void setDialogTitle(String h3, String h4) {
        String html = "<h3>" + h3 + "</h3><br/><h4>" + h4 + "</h4>" ;
        dialog.setDialogTitle(SafeHtmlUtils.fromSafeConstant(html).asString());
    }

    public void show(final FormInstance instance, FormDialogCallback callback) {
        this.callback = callback;
        loadingPanel.show(new Provider<Promise<FormInstance>>() {
            @Override
            public Promise<FormInstance> get() {
                return Promise.resolved(instance);
            }
        });
        dialog.show();
    }


    public void show(final ResourceId instanceId, FormDialogCallback callback) {
        this.callback = callback;
        loadingPanel.show(new Provider<Promise<FormInstance>>() {

            @Override
            public Promise<FormInstance> get() {
                return resourceLocator.getFormInstance(instanceId);
            }
        });
        dialog.show();
    }

    public void save() {
        dialog.getStatusLabel().setText(""); // clear first

        if (!formPanel.validate()) {
            dialog.getStatusLabel().setText(I18N.CONSTANTS.pleaseFillInAllRequiredFields());
            return;
        }

        dialog.getStatusLabel().setText(I18N.CONSTANTS.saving());
        dialog.getPrimaryButton().setEnabled(false);
        formPanel.getRelevanceHandler().resetValuesForFieldsWithAppliedRelevance();
        resourceLocator.persist(formPanel.getInstance()).then(new AsyncCallback<Void>() {

            @Override
            public void onFailure(Throwable caught) {
                Log.error("Save failed", caught);
                dialog.getStatusLabel().setText(ExceptionOracle.getExplanation(caught));
                dialog.getPrimaryButton().setEnabled(true);
            }

            @Override
            public void onSuccess(Void result) {
                dialog.hide();
                callback.onPersisted(formPanel.getInstance());
            }
        });
    }
    
    
    private void printForm() {
        Printer printer = new Printer();
        printer.print(formPanel.getHtml());
        
    }

}
