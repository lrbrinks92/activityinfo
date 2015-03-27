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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;
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
import org.activityinfo.ui.client.component.formdesigner.header.HeaderPanel;
import org.activityinfo.ui.client.component.formdesigner.palette.FieldPalette;
import org.activityinfo.ui.client.component.formdesigner.properties.PropertiesPanel;
import org.activityinfo.ui.client.page.HasNavigationCallback;
import org.activityinfo.ui.client.page.NavigationCallback;
import org.activityinfo.ui.client.util.GwtUtil;
import org.activityinfo.ui.client.widget.Button;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author yuriyz on 07/04/2014.
 */
public class FormDesignerPanel extends Composite implements ScrollHandler, HasNavigationCallback, FormSavedGuard.HasSavedGuard {

    private final static OurUiBinder uiBinder = GWT.create(OurUiBinder.class);

    interface OurUiBinder extends UiBinder<Widget, FormDesignerPanel> {
    }

    private final FormDesigner formDesigner;
    private final Map<ResourceId, WidgetContainer> containerMap = Maps.newHashMap();
    private ScrollPanel scrollAncestor;
    private WidgetContainer selectedWidgetContainer;
    private HasNavigationCallback savedGuard = null;

    @UiField
    HTMLPanel containerPanel;
    @UiField
    FlowPanel dropPanel;
    @UiField
    PropertiesPanel propertiesPanel;
    @UiField
    HeaderPanel headerPanel;
    @UiField
    FieldPalette fieldPalette;
    @UiField
    Button saveButton;
    @UiField
    HTML statusMessage;
    @UiField
    HTML spacer;
    @UiField
    HTML paletteSpacer;

    public FormDesignerPanel(@Nonnull final FormDesigner formDesigner) {
        Preconditions.checkNotNull(formDesigner);

        this.formDesigner = formDesigner;

        FormDesignerStyles.INSTANCE.ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
        propertiesPanel.setVisible(false);

        addAttachHandler(new AttachEvent.Handler() {
            @Override
            public void onAttachOrDetach(AttachEvent event) {
                scrollAncestor = GwtUtil.getScrollAncestor(FormDesignerPanel.this);
                scrollAncestor.addScrollHandler(FormDesignerPanel.this);
            }
        });
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {

                savedGuard = formDesigner.getSavedGuard();
                List<Promise<Void>> promises = Lists.newArrayList();

                buildWidgetContainers(formDesigner.getModel().getRootFormClass(), 0);
                fillPanel();
            }
        });
    }

    public void bind(EventBus eventBus) {
        eventBus.addHandler(WidgetContainerSelectionEvent.TYPE, new WidgetContainerSelectionEvent.Handler() {
            @Override
            public void handle(WidgetContainerSelectionEvent event) {
                selectedWidgetContainer = event.getSelectedItem();
                calcSpacerHeight();
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
                            formDesigner.getDragController().makeDraggable(widget, widgetContainer.getDragHandle());
                            dropPanel.add(widget);
                        }
                    }
                } else if (element instanceof FormSection) {
                    FormSection section = (FormSection) element;
                    WidgetContainer widgetContainer = containerMap.get(section.getId());
                    Widget widget = widgetContainer.asWidget();
                    formDesigner.getDragController().makeDraggable(widget, widgetContainer.getDragHandle());
                    dropPanel.add(widget);

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
                Promise<? extends FormFieldWidget> widget = formDesigner.getFormFieldWidgetFactory().createWidget(formDesigner.getFormClass(), formField, NullValueUpdater.INSTANCE);
                containerMap.put(formField.getId(), new FieldWidgetContainer(formDesigner, widget, formField));
            }
        }
    }

    @Override
    public void onScroll(ScrollEvent event) {
        calcSpacerHeight();
    }

    private void calcSpacerHeight() {
        int verticalScrollPosition = scrollAncestor.getVerticalScrollPosition();
        if (verticalScrollPosition > Metrics.MAX_VERTICAL_SCROLL_POSITION) {
            int height = verticalScrollPosition - Metrics.MAX_VERTICAL_SCROLL_POSITION;

//            int selectedWidgetTop = 0;
//            if (selectedWidgetContainer != null) {
//                selectedWidgetTop = selectedWidgetContainer.asWidget().getAbsoluteTop();
//            }
//            if (selectedWidgetTop < 0) {
//                height = height + selectedWidgetTop;
//            }

            //GWT.log("verticalPos = " + verticalScrollPosition + ", height = " + height + ", selectedWidgetTop = " + selectedWidgetTop);
            spacer.setHeight(height + "px");
            paletteSpacer.setHeight(height + "px");
        } else {
            spacer.setHeight("0px");
            paletteSpacer.setHeight("0px");
        }
    }

    public Map<ResourceId, WidgetContainer> getContainerMap() {
        return containerMap;
    }

    public FlowPanel getDropPanel() {
        return dropPanel;
    }

    public PropertiesPanel getPropertiesPanel() {
        return propertiesPanel;
    }

    public HeaderPanel getHeaderPanel() {
        return headerPanel;
    }

    public FieldPalette getFieldPalette() {
        return fieldPalette;
    }

    public Button getSaveButton() {
        return saveButton;
    }

    public HTML getStatusMessage() {
        return statusMessage;
    }

    public HasNavigationCallback getSavedGuard() {
        return savedGuard;
    }

    public void setSavedGuard(HasNavigationCallback savedGuard) {
        this.savedGuard = savedGuard;
    }

    @Override
    public void navigate(NavigationCallback callback) {
        if (savedGuard != null) {
            savedGuard.navigate(callback);
        }
    }
}
