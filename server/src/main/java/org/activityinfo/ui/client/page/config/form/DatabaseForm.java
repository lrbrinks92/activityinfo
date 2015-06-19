package org.activityinfo.ui.client.page.config.form;

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

import com.extjs.gxt.ui.client.binding.FieldBinding;
import com.extjs.gxt.ui.client.binding.FormBinding;
import com.extjs.gxt.ui.client.data.BaseListLoader;
import com.extjs.gxt.ui.client.data.DataProxy;
import com.extjs.gxt.ui.client.data.DataReader;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.legacy.client.Dispatcher;
import org.activityinfo.legacy.client.monitor.MaskingAsyncMonitor;
import org.activityinfo.legacy.shared.command.GetCountries;
import org.activityinfo.legacy.shared.command.result.CountryResult;
import org.activityinfo.legacy.shared.model.CountryDTO;
import org.activityinfo.legacy.shared.model.UserDatabaseDTO;
import org.activityinfo.ui.client.widget.legacy.RemoteComboBox;

public class DatabaseForm extends FormPanel {
    private final FormBinding binding;
    private final ComboBox<CountryDTO> countryField;

    public DatabaseForm(final Dispatcher dispatcher) {
        binding = new FormBinding(this);

        TextField<String> nameField = new TextField<String>();
        nameField.setFieldLabel(I18N.CONSTANTS.name());
        nameField.setAllowBlank(false);
        nameField.setMaxLength(UserDatabaseDTO.MAX_NAME_LENGTH);
        binding.addFieldBinding(new FieldBinding(nameField, "name"));
        add(nameField);

        TextField<String> fullNameField = new TextField<String>();
        fullNameField.setFieldLabel(I18N.CONSTANTS.description());
        fullNameField.setMaxLength(50);
        binding.addFieldBinding(new FieldBinding(fullNameField, "fullName"));
        add(fullNameField);

        countryField = new RemoteComboBox<CountryDTO>();
        countryField.setStore(createCountryStore(dispatcher, countryField));
        countryField.setFieldLabel(I18N.CONSTANTS.country());
        countryField.setValueField("id");
        countryField.setDisplayField("name");
        countryField.setAllowBlank(false);
        countryField.setEnabled(true);
        countryField.setTriggerAction(TriggerAction.ALL);

        binding.addFieldBinding(new FieldBinding(countryField, "country") {
            @Override
            public void updateModel() {
                ((UserDatabaseDTO) model).setCountry((CountryDTO) field.getValue());
            }
        });

        add(countryField);
    }

    private static ListStore<CountryDTO> createCountryStore(final Dispatcher dispatcher, final Component component) {

        return new ListStore<CountryDTO>(new BaseListLoader<CountryResult>(new DataProxy<CountryResult>() {
            @Override
            public void load(DataReader<CountryResult> countryResultDataReader,
                             Object loadConfig,
                             final AsyncCallback<CountryResult> callback) {
                dispatcher.execute(new GetCountries(),
                        new MaskingAsyncMonitor(component, I18N.CONSTANTS.loading()),
                        callback);

            }
        }));
    }

    public FormBinding getBinding() {
        return binding;
    }
    
    public void hideCountryField() {
        countryField.setVisible(false);
    }
}
