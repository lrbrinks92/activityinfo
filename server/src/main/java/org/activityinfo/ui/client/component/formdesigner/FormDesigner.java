package org.activityinfo.ui.client.component.formdesigner;
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

import com.allen_sauer.gwt.dnd.client.DragController;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import org.activityinfo.core.client.ResourceLocator;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormElement;
import org.activityinfo.model.form.FormField;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.ui.client.component.form.field.FieldWidgetMode;
import org.activityinfo.ui.client.component.form.field.FormFieldWidgetFactory;
import org.activityinfo.ui.client.component.formdesigner.container.FieldWidgetContainer;
import org.activityinfo.ui.client.component.formdesigner.drop.DropPanelDropController;
import org.activityinfo.ui.client.component.formdesigner.drop.ForwardDropController;
import org.activityinfo.ui.client.component.formdesigner.header.HeaderPresenter;
import org.activityinfo.ui.client.component.formdesigner.properties.PropertiesPresenter;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

/**
 * @author yuriyz on 07/07/2014.
 */
public class FormDesigner {

    private final EventBus eventBus = new SimpleEventBus();
    private final ResourceLocator resourceLocator;
    private final FormDesignerModel model;
    private final PropertiesPresenter propertiesPresenter;
    private final HeaderPresenter headerPresenter;
    private final FormDesignerPanelPresenter formDesignerPanelPresenter;
    private final FormFieldWidgetFactory formFieldWidgetFactory;
    private final FormSavedGuard savedGuard;
    private final FormDesignerActions formDesignerActions;

    public FormDesigner(@Nonnull ResourceLocator resourceLocator, @Nonnull FormClass formClass) {
        Preconditions.checkNotNull(resourceLocator);
        Preconditions.checkNotNull(formClass);

        this.resourceLocator = resourceLocator;
        this.formFieldWidgetFactory = new FormFieldWidgetFactory(resourceLocator, FieldWidgetMode.DESIGN);
        this.model = new FormDesignerModel(formClass);
        this.formDesignerPanelPresenter = new FormDesignerPanelPresenter(this, FormDesignerPanelProvider.getCleanPanel());
        this.propertiesPresenter = new PropertiesPresenter(formDesignerPanelPresenter.getPanel().getPropertiesPanel(), this);


        ForwardDropController forwardDropController = new ForwardDropController(formDesignerPanelPresenter.getPanel().getDropPanel());
        forwardDropController.add(new DropPanelDropController(formDesignerPanelPresenter.getPanel().getDropPanel(), this));

        formDesignerPanelPresenter.getPanel().getFieldPalette().bind(eventBus, forwardDropController);
        formDesignerPanelPresenter.bind(eventBus);
        model.bind(eventBus);

        headerPresenter = new HeaderPresenter(this);
        headerPresenter.show();

        savedGuard = new FormSavedGuard(this);
        formDesignerActions = new FormDesignerActions(this);
    }

    public FormDesignerActions getFormDesignerActions() {
        return formDesignerActions;
    }

    public FormSavedGuard getSavedGuard() {
        return savedGuard;
    }

    public FormDesignerPanelPresenter getFormDesignerPanelPresenter() {
        return formDesignerPanelPresenter;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public ResourceLocator getResourceLocator() {
        return resourceLocator;
    }

    public FormClass getFormClass() {
        return model.getRootFormClass();
    }

    public FormDesignerModel getModel() {
        return model;
    }

    public FormFieldWidgetFactory getFormFieldWidgetFactory() {
        return formFieldWidgetFactory;
    }

    public DragController getDragController() {
        return formDesignerPanelPresenter.getPanel().getFieldPalette().getDragController();
    }

    public void updateFieldOrder() {

        Map<ResourceId, FormField> fieldMap = Maps.newHashMap();
        for (FormField field : getFormClass().getFields()) {
            fieldMap.put(field.getId(), field);
        }

        // update the order of the model
        List<FormElement> elements = Lists.newArrayList();
        FlowPanel panel = formDesignerPanelPresenter.getPanel().getDropPanel();
        for (int i = 0; i != panel.getWidgetCount(); ++i) {
            Widget widget = panel.getWidget(i);
            String fieldId = widget.getElement().getAttribute(FieldWidgetContainer.DATA_FIELD_ID);
            elements.add(fieldMap.get(ResourceId.valueOf(fieldId)));
        }

        getFormClass().getElements().clear();
        getFormClass().getElements().addAll(elements);
    }
}
