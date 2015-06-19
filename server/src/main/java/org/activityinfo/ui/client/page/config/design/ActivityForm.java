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

import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.binding.FieldBinding;
import com.extjs.gxt.ui.client.binding.FormBinding;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.widget.form.*;
import com.google.gwt.user.client.ui.Anchor;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.legacy.client.Dispatcher;
import org.activityinfo.legacy.shared.model.ActivityFormDTO;
import org.activityinfo.legacy.shared.model.LocationTypeDTO;
import org.activityinfo.legacy.shared.model.Published;
import org.activityinfo.legacy.shared.model.UserDatabaseDTO;
import org.activityinfo.ui.client.page.config.design.dialog.NewFormDialog;
import org.activityinfo.ui.client.widget.legacy.MappingComboBox;
import org.activityinfo.ui.client.widget.legacy.MappingComboBoxBinding;
import org.activityinfo.ui.client.widget.legacy.OnlyValidFieldBinding;

/**
 * FormClass for editing ActivityDTO
 */
class ActivityForm extends AbstractDesignForm {

    private FormBinding binding;

    public ActivityForm(Dispatcher service, UserDatabaseDTO database) {
        super();

        binding = new FormBinding(this);

        this.setHeaderVisible(false);
        this.setScrollMode(Scroll.AUTOY);
        this.setLabelWidth(150);
        this.setBorders(false);

        final NumberField idField = new NumberField();
        idField.setFieldLabel("ID");
        idField.setReadOnly(true);
        binding.addFieldBinding(new FieldBinding(idField, "id"));
        add(idField);

        TextField<String> nameField = new TextField<String>();
        nameField.setAllowBlank(false);
        nameField.setFieldLabel(I18N.CONSTANTS.name());
        nameField.setMaxLength(ActivityFormDTO.NAME_MAX_LENGTH);
        binding.addFieldBinding(new OnlyValidFieldBinding(nameField, "name"));
        this.add(nameField);

        TextField<String> categoryField = new TextField<String>();
        categoryField.setFieldLabel(I18N.CONSTANTS.category());
        categoryField.setMaxLength(ActivityFormDTO.CATEGORY_MAX_LENGTH);
        binding.addFieldBinding(new OnlyValidFieldBinding(categoryField, "category"));
        add(categoryField);

        final MappingComboBox<Integer> locationTypeCombo = new MappingComboBox<Integer>();
        for (LocationTypeDTO type : database.getCountry().getLocationTypes()) {
            if (!type.isDeleted()) { // ai-1045 : we still fetching deleted location types but don't want to show it to user
                locationTypeCombo.add(type.getId(), type.getName());
            }
        }
        locationTypeCombo.setAllowBlank(false);
        locationTypeCombo.setFieldLabel(I18N.CONSTANTS.locationType());
        this.add(locationTypeCombo);

        binding.addFieldBinding(new MappingComboBoxBinding(locationTypeCombo, "locationTypeId"));

        final MappingComboBox frequencyCombo = new MappingComboBox();
        frequencyCombo.setAllowBlank(false);
        frequencyCombo.setFieldLabel(I18N.CONSTANTS.reportingFrequency());
        frequencyCombo.add(ActivityFormDTO.REPORT_ONCE, I18N.CONSTANTS.reportOnce());
        frequencyCombo.add(ActivityFormDTO.REPORT_MONTHLY, I18N.CONSTANTS.monthly());

        binding.addFieldBinding(new MappingComboBoxBinding(frequencyCombo, "reportingFrequency"));
        this.add(frequencyCombo);

        MappingComboBox publishedCombo = new MappingComboBox();
        publishedCombo.setAllowBlank(false);
        publishedCombo.setFieldLabel(I18N.CONSTANTS.published());
        publishedCombo.add(Published.NOT_PUBLISHED.getIndex(), I18N.CONSTANTS.notPublished());
        publishedCombo.add(Published.ALL_ARE_PUBLISHED.getIndex(), I18N.CONSTANTS.allArePublished());
        binding.addFieldBinding(new MappingComboBoxBinding(publishedCombo, "published"));

        binding.addListener(Events.Bind, new Listener<BindingEvent>() {

            @Override
            public void handleEvent(BindingEvent be) {
                //                locationTypeCombo.setEnabled(!isSaved(be.getModel()));
                frequencyCombo.setEnabled(!isSaved(be.getModel()));
            }
        });

        this.add(publishedCombo);

        // hack : we represent boolean value with radiobuttons (instead of checkbox)
        // therefore radio buttons order is important: true - first button selected, false - second button selected
        final Radio classicView = new Radio();
        classicView.setBoxLabel(I18N.CONSTANTS.classicView());
        classicView.setToolTip(I18N.CONSTANTS.classicViewExplanation());

        final Radio modernView = new Radio();
        modernView.setBoxLabel(I18N.CONSTANTS.modernView());
        modernView.setToolTip(I18N.CONSTANTS.modernViewExplanation());

        frequencyCombo.addSelectionChangedListener(new SelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent se) {
                Object value = frequencyCombo.getValue();
                boolean isMonthlySelected = value instanceof ModelData && ((ModelData)value).get("value") instanceof Integer &&
                        ((ModelData)value).get("value").equals(ActivityFormDTO.REPORT_MONTHLY);
                if (isMonthlySelected && modernView.getValue()) {
                    classicView.setValue(true);
                }
                modernView.setEnabled(!isMonthlySelected);
            }
        });

        RadioGroup radioViewGroup = new RadioGroup();
        radioViewGroup.add(classicView); // order is important! - true is first button, false is second button
        radioViewGroup.add(modernView);

        radioViewGroup.setFieldLabel(I18N.CONSTANTS.viewType());

        binding.addFieldBinding(new OnlyValidFieldBinding(radioViewGroup, "classicView"));

        this.add(radioViewGroup);
        this.add(new LabelField(I18N.CONSTANTS.classicViewExplanation()));
        this.add(new LabelField(I18N.CONSTANTS.modernViewExplanation()));

        Anchor linkOnExplanation = new Anchor();
        linkOnExplanation.setTarget("_blank");
        linkOnExplanation.setHref(NewFormDialog.CLASSIC_VIEW_EXPLANATION_URL);
        linkOnExplanation.setText(I18N.CONSTANTS.moreAboutView());

        this.add(new AdapterField(linkOnExplanation));

        hideFieldWhenNull(idField);
    }

    @Override
    public FormBinding getBinding() {
        return binding;
    }

    private boolean isSaved(ModelData model) {
        return model.get("id") != null;
    }
}
