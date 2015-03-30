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

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.Widget;
import org.activityinfo.model.form.*;
import org.activityinfo.model.legacy.CuidAdapter;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.promise.Promise;
import org.activityinfo.ui.client.component.form.field.FormFieldWidget;
import org.activityinfo.ui.client.component.formdesigner.container.FieldWidgetContainer;
import org.activityinfo.ui.client.component.formdesigner.container.SectionWidgetContainer;
import org.activityinfo.ui.client.component.formdesigner.container.WidgetContainer;
import org.activityinfo.ui.client.component.formdesigner.drop.NullValueUpdater;
import org.activityinfo.ui.client.component.formdesigner.event.WidgetContainerSelectionEvent;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author yuriyz on 03/30/2015.
 */
public class FormDesignerPanelPresenter {

    private final FormDesigner formDesigner;
    private final FormDesignerPanel panel;
    private final Map<ResourceId, WidgetContainer> containerMap = Maps.newHashMap();

    public FormDesignerPanelPresenter(@Nonnull final FormDesigner formDesigner, @Nonnull FormDesignerPanel panel) {
        Preconditions.checkNotNull(formDesigner);
        Preconditions.checkNotNull(panel);

        this.formDesigner = formDesigner;
        this.panel = panel;

        panel.setSavedGuard(formDesigner.getSavedGuard());
        buildWidgetContainers(formDesigner.getModel().getRootFormClass(), 0);
        fillPanel();
    }


    public void bind(EventBus eventBus) {
        eventBus.addHandler(WidgetContainerSelectionEvent.TYPE, new WidgetContainerSelectionEvent.Handler() {
            @Override
            public void handle(WidgetContainerSelectionEvent event) {
                panel.calcSpacerHeight();
            }
        });
    }

    private void fillPanel() {
        final FormClass formClass = formDesigner.getModel().getRootFormClass();

        // Exclude legacy builtin fields that the user won't be able to remove or reorder
        final Set<ResourceId> builtinFields = builtinFields(formClass.getId());

        formClass.traverse(formClass, new TraverseFunction() {
            @Override
            public void apply(FormElement element, FormElementContainer container) {
                if (element instanceof FormField) {
                    if (!builtinFields.contains(element.getId())) {
                        FormField formField = (FormField) element;
                        WidgetContainer widgetContainer = containerMap.get(formField.getId());
                        if (widgetContainer != null) { // widget container may be null if domain is not supported, should be removed later
                            Widget widget = widgetContainer.asWidget();
                            getPanel().getFieldPalette().getDragController().makeDraggable(widget, widgetContainer.getDragHandle());
                            panel.getDropPanel().add(widget);
                        }
                    }
                } else if (element instanceof FormSection) {
                    FormSection section = (FormSection) element;
                    WidgetContainer widgetContainer = containerMap.get(section.getId());
                    Widget widget = widgetContainer.asWidget();
                    getPanel().getFieldPalette().getDragController().makeDraggable(widget, widgetContainer.getDragHandle());
                    panel.getDropPanel().add(widget);

                } else {
                    throw new UnsupportedOperationException("Unknown form element.");
                }
            }
        });
    }

    private Set<ResourceId> builtinFields(ResourceId formClassId) {
        Set<ResourceId> fieldIds = new HashSet<>();
        fieldIds.add(CuidAdapter.field(formClassId, CuidAdapter.START_DATE_FIELD));
        fieldIds.add(CuidAdapter.field(formClassId, CuidAdapter.END_DATE_FIELD));
        fieldIds.add(CuidAdapter.field(formClassId, CuidAdapter.COMMENT_FIELD));
        fieldIds.add(CuidAdapter.field(formClassId, CuidAdapter.PARTNER_FIELD));
        fieldIds.add(CuidAdapter.field(formClassId, CuidAdapter.PROJECT_FIELD));
        return fieldIds;
    }

    public void buildWidgetContainers(FormElementContainer container, int depth) {
        for (FormElement element : container.getElements()) {
            if (element instanceof FormSection) {
                FormSection formSection = (FormSection) element;
                containerMap.put(formSection.getId(), new SectionWidgetContainer(formDesigner, formSection));
                buildWidgetContainers(formSection, depth + 1);
            } else if (element instanceof FormField) {
                final FormField formField = (FormField) element;
                Promise<? extends FormFieldWidget> widget = formDesigner.getFormFieldWidgetFactory().createWidget(formField, NullValueUpdater.INSTANCE);
                containerMap.put(formField.getId(), new FieldWidgetContainer(formDesigner, widget, formField));
            }
        }
    }

    public Map<ResourceId, WidgetContainer> getContainerMap() {
        return containerMap;
    }

    public FormDesignerPanel getPanel() {
        return panel;
    }
}
