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

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import org.activityinfo.model.form.*;
import org.activityinfo.model.type.subform.ClassType;
import org.activityinfo.model.type.subform.SubFormType;
import org.activityinfo.ui.client.component.form.subform.SubFormCollectionManipulator;
import org.activityinfo.ui.client.component.form.subform.SubFormTabsManipulator;

/**
 * @author yuriyz on 01/18/2016.
 */
public class PanelFiller {

    private final FlowPanel panel;
    private final FormModel model;
    private final FormWidgetCreator widgetCreator;
    private final SubFormsHandler subFormsHandler;

    public PanelFiller(FlowPanel panel, FormModel model, FormWidgetCreator widgetCreator, SubFormsHandler subFormsHandler) {
        this.panel = panel;
        this.model = model;
        this.widgetCreator = widgetCreator;
        this.subFormsHandler = subFormsHandler;
    }

    public void add(FormElementContainer container, int depth) {
        add(container, depth, panel);
    }

    public void add(FormElementContainer container, int depth, FlowPanel panel) {
        for (FormElement element : container.getElements()) {
            if (element instanceof FormSection) {
                panel.add(createHeader(depth, element.getLabel()));
                add((FormElementContainer) element, depth + 1);
            } else if (element instanceof FormField) {
                FormField formField = (FormField) element;
                if (formField.isVisible()) {
                    if (formField.getType() instanceof SubFormType) {
                        FormClass subForm = model.getSubFormByOwnerFieldId(formField.getId());


                        if (ClassType.isCollection(subForm)) { // unkeyed subforms -> simple collection
                            SubFormCollectionManipulator collectionManipulator = new SubFormCollectionManipulator(subForm, model, panel, depth + 1);
                            collectionManipulator.show();

                            subFormsHandler.getSubForms().put(subForm, collectionManipulator);
                        } else { // keyed subforms
                            final SubFormTabsManipulator subFormTabsManipulator = new SubFormTabsManipulator(model.getLocator());

                            final FlowPanel subformPanel = new FlowPanel();
                            subformPanel.addStyleName(FormPanelStyles.INSTANCE.subformPanel());

                            panel.add(createHeader(depth + 1, subForm.getLabel()));
                            panel.add(subformPanel);

                            subformPanel.add(subFormTabsManipulator.getPresenter().getView());
                            subFormTabsManipulator.getPresenter().getView().addStyleName(FormPanelStyles.INSTANCE.subformTabs());

                            subFormTabsManipulator.show(subForm, model);

                            add(subForm, depth + 1, subformPanel);
                        }
                    } else {
                        panel.add(widgetCreator.get(formField.getId()));
                    }
                }
            }
        }
    }

    public static Widget createHeader(int depth, String header) {
        String hn = "h" + (3 + depth);
        return new HTML("<" + hn + ">" + SafeHtmlUtils.htmlEscape(header) + "</" + hn + ">");
    }
}