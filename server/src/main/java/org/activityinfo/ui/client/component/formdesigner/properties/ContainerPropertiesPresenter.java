package org.activityinfo.ui.client.component.formdesigner.properties;
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
import com.google.common.base.Strings;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormElementContainer;
import org.activityinfo.model.form.FormField;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.type.ReferenceType;
import org.activityinfo.model.type.period.PredefinedPeriods;
import org.activityinfo.model.type.subform.SubFormKind;
import org.activityinfo.model.type.subform.SubFormKindRegistry;
import org.activityinfo.ui.client.component.formdesigner.FormDesigner;
import org.activityinfo.ui.client.component.formdesigner.container.FieldsHolder;

/**
 * @author yuriyz on 01/15/2015.
 */
public class ContainerPropertiesPresenter {

    private final FormDesigner formDesigner;
    private final ContainerPropertiesPanel view;

    private HandlerRegistration labelKeyUpHandler;

    public ContainerPropertiesPresenter(FormDesigner formDesigner) {
        this.formDesigner = formDesigner;
        this.view = formDesigner.getFormDesignerPanel().getContainerPropertiesPanel();
    }

    public void show(final FieldsHolder fieldsHolder) {
        reset();

        view.getLabel().setValue(Strings.nullToEmpty(fieldsHolder.getElementContainer().getLabel()));

        validateLabel();

        labelKeyUpHandler = view.getLabel().addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                if (validateLabel()) {
                    fieldsHolder.getElementContainer().setLabel(view.getLabel().getValue());
                    fieldsHolder.updateUi();
                }
            }
        });

        if (isSubform(fieldsHolder)) {
            view.getSubformKindGroup().setVisible(true);

            final FormClass subForm = (FormClass) fieldsHolder.getElementContainer();
            view.getSubformKind().addChangeHandler(new ChangeHandler() {
                @Override
                public void onChange(ChangeEvent event) {
                    subformKindChanged(view.getSubformKind().getValue(view.getSubformKind().getSelectedIndex()), subForm);
                }
            });

        }
    }

    private boolean isSubform(FieldsHolder fieldsHolder) {
        FormElementContainer elementContainer = fieldsHolder.getElementContainer();
        return elementContainer instanceof FormClass && !elementContainer.equals(formDesigner.getRootFormClass());
    }

    private void subformKindChanged(String selectedValue, final FormClass subForm) {
        Preconditions.checkState(selectedValue != null && selectedValue.startsWith("_"),
                "Value is not valid, it must not be null and start with '_' character.");

        final ReferenceType subFormType = (ReferenceType) subForm.getField(FormClass.TYPE_FIELD_ID).getType();

        if (SubFormKindRegistry.getUserDefinedId().asString().equals(selectedValue)) {
            final SelectSubformTypeDialog dialog = new SelectSubformTypeDialog();
            dialog.setHideHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    ResourceId selectedClassId = dialog.getSelectedClassId();
                    if (selectedClassId == null) { // user simply canceled selection
                        selectedClassId = PredefinedPeriods.MONTHLY.getResourceId();
                        view.getSubformKind().setSelectedIndex(getKindIndex(selectedClassId));
                    }

                    subFormType.setRange(selectedClassId);
                    forceSubformRerender(subForm);
                }
            });
            dialog.show();
            return;
        }

        SubFormKind kind = SubFormKindRegistry.get().getKind(selectedValue);
        if (kind != null) {
            subFormType.setRange(kind.getDefinition().getId());
            return;
        }

        throw new UnsupportedOperationException("Subform type is not supported, type: " + selectedValue);
    }

    private void forceSubformRerender(FormClass subForm) {
        FormField subformOwnerField = formDesigner.getModel().getSubformOwnerField(subForm);
        formDesigner.getWidgetContainer(subformOwnerField.getId()).syncWithModel();
    }

    private int getKindIndex(ResourceId valueId) {
        for (int i = 0; i < view.getSubformKind().getVisibleItemCount(); i++) {
            if (view.getSubformKind().getValue(i).equals(valueId.asString())) {
                return i;
            }
        }
        throw new IllegalArgumentException("Value is unknown, value:" + valueId);
    }

    public void reset() {
        if (labelKeyUpHandler != null) {
            labelKeyUpHandler.removeHandler();
        }
        view.getSubformKindGroup().setVisible(false);
    }

    /**
     * Returns whether code is valid.
     *
     * @return whether code is valid
     */
    private boolean validateLabel() {
        view.getLabelGroup().setShowValidationMessage(false);
        if (Strings.isNullOrEmpty(view.getLabel().getValue())) {
            view.getLabelGroup().setShowValidationMessage(true);
            return false;
        }
        return true;
    }

}
