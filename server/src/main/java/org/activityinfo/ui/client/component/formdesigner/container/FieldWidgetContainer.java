package org.activityinfo.ui.client.component.formdesigner.container;
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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import org.activityinfo.model.form.FormField;
import org.activityinfo.promise.Promise;
import org.activityinfo.ui.client.component.form.field.FormFieldWidget;
import org.activityinfo.ui.client.component.formdesigner.FormDesigner;
import org.activityinfo.ui.client.component.formdesigner.event.WidgetContainerSelectionEvent;
import org.activityinfo.ui.client.widget.loading.LoadingClientBundle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author yuriyz on 7/14/14.
 */
public class FieldWidgetContainer implements WidgetContainer {

    public static final String DATA_FIELD_ID = "data-field-id";

    public interface LabelTemplate extends SafeHtmlTemplates {
        @Template("<span style='color: red;'> *</span>")
        SafeHtml mandatoryMarker();
    }

    private static final LabelTemplate LABEL_TEMPLATE = GWT.create(LabelTemplate.class);

    private final Image loadingImage = new Image(LoadingClientBundle.INSTANCE.loadingIcon());

    private final Promise<? extends FormFieldWidget> formFieldWidget;
    private final FormDesigner formDesigner;
    private final FormField formField;

    private final WidgetContainerPanel widgetContainer;
    private boolean loading = true;

    public FieldWidgetContainer(@Nonnull final FormDesigner formDesigner, @Nonnull final Promise<? extends FormFieldWidget> formFieldWidget, @Nonnull final FormField formField) {
        Preconditions.checkNotNull(formDesigner);
        Preconditions.checkNotNull(formFieldWidget);
        Preconditions.checkNotNull(formField);

        this.formDesigner = formDesigner;
        this.formField = formField;
        this.formFieldWidget = formFieldWidget;

        widgetContainer = new WidgetContainerPanel(formDesigner);

        widgetContainer.getWidgetContainer().add(loadingImage);
        widgetContainer.getRemoveButton().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                formDesigner.getFormClass().remove(formField);
            }
        });
        widgetContainer.getFocusPanel().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                formDesigner.getEventBus().fireEvent(new WidgetContainerSelectionEvent(FieldWidgetContainer.this));
            }
        });

        // Workaround(alex): store field id with widget so we can update model order after
        // drag and drop
        widgetContainer.asWidget().getElement().setAttribute(DATA_FIELD_ID, formField.getId().asString());

        if (formFieldWidget.isSettled()) {
            widgetLoaded();
        } else {
            formFieldWidget.then(new Function<FormFieldWidget, Object>() {
                @Nullable
                @Override
                public Object apply(FormFieldWidget input) {
                    widgetLoaded();
                    return null;
                }
            });
        }
    }

    private void widgetLoaded() {
        Preconditions.checkState(formFieldWidget.getState() == Promise.State.FULFILLED);
        loading = false;
        widgetContainer.getWidgetContainer().remove(loadingImage);
        widgetContainer.getWidgetContainer().add(formFieldWidget.get());
        syncWithModel();
    }

    public void syncWithModel() {
        if (loading) { // it's not allowed to sync with model if widget is not loaded yet
            throw new RuntimeException("Widget is not loaded yet. Sync is forbidden.");
        }

        final SafeHtmlBuilder label = new SafeHtmlBuilder();

        if (!Strings.isNullOrEmpty(formField.getCode())) { // append code
            label.appendHtmlConstant("<span class='small'>" + SafeHtmlUtils.fromString(formField.getCode()).asString() + "</span>&nbsp;");
        }

        label.append(SafeHtmlUtils.fromString(Strings.nullToEmpty(formField.getLabel())));
        if (formField.isRequired()) {
            label.append(LABEL_TEMPLATE.mandatoryMarker());
        }
        formFieldWidget.get().setReadOnly(formField.isReadOnly());

        String labelHtml = label.toSafeHtml().asString();
        if (!formField.isVisible()) {
            labelHtml = "<del>" + labelHtml + "</del>";
        }
        widgetContainer.getLabel().setHTML(labelHtml);
        formFieldWidget.get().setType(formField.getType());
    }

    public Widget asWidget() {
        return widgetContainer.asWidget();
    }

    public Widget getDragHandle() {
        return widgetContainer.getDragHandle();
    }

    public FormField getFormField() {
        return formField;
    }

    public FormDesigner getFormDesigner() {
        return formDesigner;
    }

    public boolean isLoading() {
        return loading;
    }
}
