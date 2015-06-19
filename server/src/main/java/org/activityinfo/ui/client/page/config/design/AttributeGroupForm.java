package org.activityinfo.ui.client.page.config.design;

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

import com.extjs.gxt.ui.client.binding.Converter;
import com.extjs.gxt.ui.client.binding.FieldBinding;
import com.extjs.gxt.ui.client.binding.FormBinding;
import com.extjs.gxt.ui.client.event.BindingEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.widget.form.CheckBox;
import com.extjs.gxt.ui.client.widget.form.NumberField;
import com.extjs.gxt.ui.client.widget.form.TextField;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.legacy.shared.model.AttributeDTO;
import org.activityinfo.legacy.shared.model.AttributeGroupDTO;
import org.activityinfo.ui.client.widget.legacy.MappingComboBox;
import org.activityinfo.ui.client.widget.legacy.MappingComboBoxBinding;
import org.activityinfo.ui.client.widget.legacy.OnlyValidFieldBinding;

import java.util.List;

class AttributeGroupForm extends AbstractDesignForm {

    public static final AttributeDTO NONE_ATTRIBUTE = new AttributeDTO();

    private FormBinding binding;

    public AttributeGroupForm() {

        binding = new FormBinding(this);

        NumberField idField = new NumberField();
        idField.setFieldLabel("ID");
        idField.setReadOnly(true);
        binding.addFieldBinding(new FieldBinding(idField, "id"));
        add(idField);

        TextField<String> nameField = new TextField<String>();
        nameField.setFieldLabel(I18N.CONSTANTS.name());
        nameField.setMaxLength(AttributeGroupDTO.NAME_MAX_LENGTH);
        nameField.setAllowBlank(false);
        binding.addFieldBinding(new OnlyValidFieldBinding(nameField, "name"));

        add(nameField);

        MappingComboBox typeField = new MappingComboBox();
        typeField.add(true, I18N.CONSTANTS.multipleChoice());
        typeField.add(false, I18N.CONSTANTS.singleChoice());
        typeField.setFieldLabel(I18N.CONSTANTS.choiceType());
        binding.addFieldBinding(new MappingComboBoxBinding(typeField, "multipleAllowed"));
        add(typeField);

        CheckBox mandatoryCB = new CheckBox();
        mandatoryCB.setFieldLabel(I18N.CONSTANTS.mandatory());
        binding.addFieldBinding(new FieldBinding(mandatoryCB, "mandatory"));
        this.add(mandatoryCB);

        CheckBox workflowCB = new CheckBox();
        workflowCB.setFieldLabel(I18N.CONSTANTS.partOfWorkflow());
        binding.addFieldBinding(new FieldBinding(workflowCB, "workflow"));
        this.add(workflowCB);


        final MappingComboBox<AttributeDTO> defaultValueField = new MappingComboBox<>();
        defaultValueField.setFieldLabel(I18N.CONSTANTS.defaultValue());
        defaultValueField.setAllowBlank(true);

        final MappingComboBoxBinding defaultValueBinding = new MappingComboBoxBinding(defaultValueField, "defaultValue");
        defaultValueBinding.setConverter(new Converter() {
            @Override
            public Object convertModelValue(Object value) {
                if (value instanceof Integer) {
                    AttributeDTO attributeById = ((AttributeGroupDTO) defaultValueBinding.getModel()).getAttributeById((Integer) value);
                    // create new object (do not use combobox.wrap() because store may not be filled yet)
                    return new MappingComboBox.Wrapper<>(attributeById, attributeById.getName());
                }
                return super.convertModelValue(value);
            }

            @Override
            public Integer convertFieldValue(Object value) {
                Object wrapper = super.convertFieldValue(value);
                if (wrapper instanceof MappingComboBox.Wrapper) {
                    MappingComboBox.Wrapper<AttributeDTO> attributeDTO = (MappingComboBox.Wrapper) wrapper;
                    AttributeDTO wrappedValue = attributeDTO.getWrappedValue();
                    if (wrappedValue != null && wrappedValue != NONE_ATTRIBUTE) {
                        return wrappedValue.getId();
                    }
                }
                return null;
            }
        });
        binding.addFieldBinding(defaultValueBinding);
        binding.addListener(Events.Bind, new Listener<BindingEvent>() {

            @Override
            public void handleEvent(BindingEvent be) {
                defaultValueField.getStore().removeAll();
                AttributeGroupDTO model = (AttributeGroupDTO) binding.getModel();
                List<AttributeDTO> attributes = model.getAttributes();
                defaultValueField.add(NONE_ATTRIBUTE, I18N.CONSTANTS.none());
                if (attributes != null) {
                    for (AttributeDTO attr : attributes) {
                        defaultValueField.add(attr, attr.getName());
                    }
                }
                if (model.getDefaultValue() == null) {
                    defaultValueField.setValue(defaultValueField.wrap(NONE_ATTRIBUTE));
                }
                // only show the field if the user has a choice
                defaultValueField.setVisible( model.getAttributes() != null &&
                                             !model.getAttributes().isEmpty());
            }
        });
        this.add(defaultValueField);

        hideFieldWhenNull(idField);

    }

    @Override
    public FormBinding getBinding() {
        return binding;
    }
}
