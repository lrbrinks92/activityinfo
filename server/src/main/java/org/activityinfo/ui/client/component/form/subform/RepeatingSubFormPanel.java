package org.activityinfo.ui.client.component.form.subform;
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

import com.google.common.collect.Maps;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.legacy.shared.Log;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormElementContainer;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.ui.client.component.form.FormModel;
import org.activityinfo.ui.client.component.form.FormPanelStyles;
import org.activityinfo.ui.client.component.form.PanelFiller;
import org.activityinfo.ui.client.component.form.SimpleFormPanel;
import org.activityinfo.ui.client.component.form.event.BeforeSaveEvent;
import org.activityinfo.ui.client.component.form.event.SaveFailedEvent;
import org.activityinfo.ui.client.style.ElementStyle;
import org.activityinfo.ui.client.widget.Button;

import java.util.List;
import java.util.Map;

/**
 * @author yuriyz on 01/18/2016.
 */
public class RepeatingSubFormPanel implements IsWidget {

    private final FlowPanel panel;
    
    private final FormClass subForm;
    private final FormModel formModel;
    private final Button addButton;
    private final SubFormInstanceLoader loader;
    private final int depth;

    private final Map<FormModel.SubformValueKey, SimpleFormPanel> forms = Maps.newHashMap();

    public RepeatingSubFormPanel(FormClass subForm, FormModel formModel, int depth) {
        this.subForm = subForm;
        this.formModel = formModel;
        this.panel = new FlowPanel();
        this.loader = new SubFormInstanceLoader(formModel);
        this.depth = depth;

        addButton = new Button(ElementStyle.LINK);
        addButton.setLabel(I18N.CONSTANTS.addAnother());
        addButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                addForm(newKey(), RepeatingSubFormPanel.this.panel.getWidgetIndex(addButton));
                setDeleteButtonsState();
            }
        });

        bindEvents();
    }

    private void bindEvents() {
        formModel.getEventBus().addHandler(BeforeSaveEvent.TYPE, new BeforeSaveEvent.Handler() {
            @Override
            public void handle(BeforeSaveEvent event) {
                removeEmptyInstances();
            }
        });
        formModel.getEventBus().addHandler(SaveFailedEvent.TYPE, new SaveFailedEvent.Handler() {
            @Override
            public void handle(SaveFailedEvent event) {
                putEmptyInstanceIfAbsent();
            }
        });
    }

    private void putEmptyInstanceIfAbsent() {
        for (FormModel.SubformValueKey key : forms.keySet()) {
            if (formModel.getSubFormInstances().get(key) == null) {
                formModel.getSubFormInstances().put(key, newValueInstance());
            }
        }
    }

    private void removeEmptyInstances() {
        for (FormModel.SubformValueKey key : forms.keySet()) {
            if (formModel.getSubFormInstances().get(key).isEmpty("classId", "keyId", "sort")) {
                formModel.getSubFormInstances().remove(key);
            }
        }
    }

    public void show() {
        loader.loadCollectionInstances(subForm).then(new AsyncCallback<List<FormInstance>>() {
            @Override
            public void onFailure(Throwable caught) {
                Log.error(caught.getMessage(), caught);
            }

            @Override
            public void onSuccess(List<FormInstance> result) {
                render();
            }
        });
    }


    private void render() {

        panel.add(PanelFiller.createHeader(depth, subForm.getLabel()));

//        for (FormModel.SubformValueKey key : keys) {
//            addForm(key);
//        }

        panel.add(addButton);

    }

    private FormModel.SubformValueKey newKey() {
        FormModel.SubformValueKey newKey = new FormModel.SubformValueKey(subForm, KeyInstanceGenerator.newUnkeyedInstance(subForm.getId()));

        formModel.getSubFormInstances().put(newKey, newValueInstance());
        return newKey;
    }

    private void addForm(final FormModel.SubformValueKey key) {
        addForm(key, -1);
    }

    private FormInstance newValueInstance() {
        FormInstance instance = new FormInstance(ResourceId.generateSubmissionId(subForm), subForm.getId());
        instance.setOwnerId(formModel.getWorkingRootInstance().getId());
        return instance;
    }

    private void addForm(final FormModel.SubformValueKey key, int panelIndex) {

        final SimpleFormPanel formPanel = new SimpleFormPanel(formModel.getLocator(), formModel.getStateProvider());
        formPanel.asWidget().addStyleName(FormPanelStyles.INSTANCE.subformPanel());

        formPanel.addDeleteButton().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                panel.remove(formPanel);
                forms.remove(key);

                if (loader.isPersisted(formModel.getSubFormInstances().get(key))) { // schedule deletion only if instance is persisted
                    formModel.getPersistedInstanceToRemoveByLocator().add(formModel.getSubFormInstances().get(key).getId());
                }

                formModel.getSubFormInstances().remove(key);

                setDeleteButtonsState();
            }
        });

        formPanel.show(formModel.getSubFormInstances().get(key));

        forms.put(key, formPanel);

        if (panelIndex == -1) {
            panel.add(formPanel);
        } else {
            panel.insert(formPanel, panelIndex);
        }

        setDeleteButtonsState();

        // set sort field
      //  formModel.getSubFormInstances().get(key).set(SORT_FIELD_ID, panel.getWidgetIndex(formPanel));
    }

    private void setDeleteButtonsState() {
        boolean moreThanOne = forms.size() > 1;
        for (SimpleFormPanel form : forms.values()) {
            form.getDeleteButton().get().setEnabled(moreThanOne);
        }
    }

    public Map<FormModel.SubformValueKey, SimpleFormPanel> getForms() {
        return forms;
    }

    @Override
    public Widget asWidget() {
        return panel;
    }
}