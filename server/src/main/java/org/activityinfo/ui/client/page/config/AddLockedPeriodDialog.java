package org.activityinfo.ui.client.page.config;

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

import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.FieldEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.HorizontalPanel;
import com.extjs.gxt.ui.client.widget.form.*;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Widget;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.legacy.client.AsyncMonitor;
import org.activityinfo.legacy.shared.model.ActivityDTO;
import org.activityinfo.legacy.shared.model.LockedPeriodDTO;
import org.activityinfo.legacy.shared.model.ProjectDTO;
import org.activityinfo.legacy.shared.model.UserDatabaseDTO;
import org.activityinfo.ui.client.page.config.LockedPeriodsPresenter.AddLockedPeriodView;

public class AddLockedPeriodDialog extends FormPanel implements AddLockedPeriodView {

    private FieldSet fieldsetContainer;
    private RadioGroup radiogroupContainer;
    private Radio radioDatabase;
    private Radio radioActivity;
    private Radio radioProject;
    private LabelField labelDatabase;
    private LabelField labelDatabaseName;
    private LabelField labelProject;
    private LabelField labelActivity;

    private HorizontalPanel panelDatabase;
    private HorizontalPanel panelActivity;
    private HorizontalPanel panelProject;

    private UserDatabaseDTO userDatabase;

    private ComboBox<ProjectDTO> comboboxProjects;
    private ListStore<ProjectDTO> storeProjects;
    private ComboBox<ActivityDTO> comboboxActivities;
    private ListStore<ActivityDTO> storeActivities;

    private TextField<String> textfieldName;
    private DateField datefieldFromDate;
    private DateField datefieldToDate;
    private CheckBox checkboxEnabled;

    private EventBus eventBus = new SimpleEventBus();

    public AddLockedPeriodDialog() {
        super();

        initializeComponent();

        setStartState();
    }

    @Override
    public void setUserDatabase(UserDatabaseDTO userDatabase) {
        this.userDatabase = userDatabase;

        labelDatabaseName.setValue(userDatabase.getName());

        storeProjects.removeAll();
        storeProjects.add(userDatabase.getProjects());

        storeActivities.removeAll();
        storeActivities.add(userDatabase.getActivities());
    }

    private void initializeComponent() {
        setWidth(400);
        setHeight(280);

        setHeaderVisible(false);

        fieldsetContainer = new FieldSet();
        fieldsetContainer.setHeadingHtml(SafeHtmlUtils.htmlEscape(I18N.CONSTANTS.type()));

        comboboxProjects = new ComboBox<ProjectDTO>();
        storeProjects = new ListStore<ProjectDTO>();
        comboboxProjects.setStore(storeProjects);
        comboboxProjects.setDisplayField("name");
        comboboxProjects.setForceSelection(true);
        comboboxProjects.setTriggerAction(TriggerAction.ALL);
        comboboxProjects.setEditable(false);

        comboboxActivities = new ComboBox<ActivityDTO>();
        storeActivities = new ListStore<ActivityDTO>();
        comboboxActivities.setStore(storeActivities);
        comboboxActivities.setDisplayField("name");
        comboboxActivities.setForceSelection(true);
        comboboxActivities.setTriggerAction(TriggerAction.ALL);
        comboboxActivities.setEditable(false);

        radiogroupContainer = new RadioGroup();
        radiogroupContainer.setFieldLabel(I18N.CONSTANTS.type());

        labelDatabase = new LabelField(I18N.CONSTANTS.database());
        labelDatabase.setWidth(100);
        labelDatabase.setUseHtml(false);

        labelDatabaseName = new LabelField();
        labelDatabaseName.setUseHtml(false);

        radioDatabase = new Radio();
        radioDatabase.setFieldLabel(I18N.CONSTANTS.database());
        radiogroupContainer.add(radioDatabase);

        panelDatabase = new HorizontalPanel();
        panelDatabase.add(labelDatabase);
        panelDatabase.add(radioDatabase);
        panelDatabase.add(labelDatabaseName);
        fieldsetContainer.add(panelDatabase);

        radioActivity = new Radio();
        radioActivity.setFieldLabel(I18N.CONSTANTS.activity());

        labelActivity = new LabelField(I18N.CONSTANTS.activity());
        labelActivity.setWidth(100);
        labelActivity.setUseHtml(false);

        panelActivity = new HorizontalPanel();
        panelActivity.add(labelActivity);
        panelActivity.add(radioActivity);
        panelActivity.add(comboboxActivities);
        fieldsetContainer.add(panelActivity);
        radiogroupContainer.add(radioActivity);

        radioProject = new Radio();
        radioProject.setFieldLabel(I18N.CONSTANTS.project());
        radioProject.addListener(Events.Change, new Listener<FieldEvent>() {
            @Override
            public void handleEvent(FieldEvent be) {
                comboboxProjects.setEnabled(radioProject.getValue());
            }
        });

        labelProject = new LabelField(I18N.CONSTANTS.project());
        labelProject.setWidth(100);
        labelProject.setUseHtml(false);

        panelProject = new HorizontalPanel();
        panelProject.add(labelProject);
        panelProject.add(radioProject);
        panelProject.add(comboboxProjects);
        fieldsetContainer.add(panelProject);
        radiogroupContainer.add(radioProject);

        add(fieldsetContainer);

        textfieldName = new TextField<String>();
        textfieldName.setFieldLabel(I18N.CONSTANTS.name());
        textfieldName.setAllowBlank(false);
        add(textfieldName);

        checkboxEnabled = new CheckBox();
        checkboxEnabled.setFieldLabel(I18N.CONSTANTS.enabledColumn());
        add(checkboxEnabled);

        datefieldToDate = new DateField();

        datefieldFromDate = new DateField();
        datefieldFromDate.setFieldLabel(I18N.CONSTANTS.fromDate());
        datefieldFromDate.setAllowBlank(false);
        datefieldFromDate.setValidator(new Validator() {
            @Override
            public String validate(Field<?> field, String value) {
                if (datefieldFromDate.getValue().after(datefieldToDate.getValue())) {
                    return I18N.CONSTANTS.fromDateIsBeforeToDate();
                }
                return null;
            }
        });
        add(datefieldFromDate);


        datefieldToDate.setFieldLabel(I18N.CONSTANTS.toDate());
        datefieldToDate.setAllowBlank(false);
        add(datefieldToDate);

        radiogroupContainer.addListener(Events.Change, new Listener<FieldEvent>() {

            @Override
            public void handleEvent(FieldEvent be) {
                comboboxActivities.setAllowBlank(!radioActivity.getValue());
                comboboxProjects.setAllowBlank(!radioProject.getValue());
                comboboxActivities.clearInvalid();
                comboboxProjects.clearInvalid();
            }
        });
    }

    @Override
    public void initialize() {
    }

    @Override
    public AsyncMonitor getLoadingMonitor() {
        return null;
    }

    @Override
    public void setValue(LockedPeriodDTO value) {
        if (value != null) {
            textfieldName.setValue(value.getName());
            checkboxEnabled.setValue(value.isEnabled());
            datefieldFromDate.setValue(value.getFromDate().atMidnightInMyTimezone());
            datefieldToDate.setValue(value.getToDate().atMidnightInMyTimezone());
        }
    }

    @Override
    public LockedPeriodDTO getValue() {
        LockedPeriodDTO newLockedPeriod = new LockedPeriodDTO();
        newLockedPeriod.setName(textfieldName.getValue());
        newLockedPeriod.setEnabled(checkboxEnabled.getValue());
        newLockedPeriod.setFromDate(datefieldFromDate.getValue());
        newLockedPeriod.setToDate(datefieldToDate.getValue());
        if (radioActivity.getValue() && comboboxActivities.getValue() != null) {
            newLockedPeriod.setParent(comboboxActivities.getValue());
        }
        if (radioProject.getValue() && comboboxProjects.getValue() != null) {
            newLockedPeriod.setParent(comboboxProjects.getValue());
        }
        if (radioDatabase.getValue()) {
            newLockedPeriod.setParent(userDatabase);
        }

        return newLockedPeriod;
    }

    @Override
    public Widget asWidget() {
        return this;
    }

    @Override
    public HandlerRegistration addCreateHandler(org.activityinfo.ui.client.page.config.mvp.CanCreate.CreateHandler
                                                            handler) {
        return eventBus.addHandler(CreateEvent.TYPE, handler);
    }

    @Override
    public void create(LockedPeriodDTO item) {
    }

    @Override
    public void setCreateEnabled(boolean createEnabled) {
    }

    @Override
    public HandlerRegistration addCancelCreateHandler(org.activityinfo.ui.client.page.config.mvp.CanCreate
                                                                  .CancelCreateHandler handler) {
        return eventBus.addHandler(CancelCreateEvent.TYPE, handler);
    }

    @Override
    public HandlerRegistration addStartCreateHandler(org.activityinfo.ui.client.page.config.mvp.CanCreate
                                                                 .StartCreateHandler handler) {
        return null;
    }

    @Override
    public void startCreate() {
        this.show();
    }

    private void setStartState() {
        textfieldName.setValue(null);
        datefieldFromDate.setValue(null);
        datefieldToDate.setValue(null);
        checkboxEnabled.setValue(true);
        radioActivity.setValue(false);
        radioProject.setValue(false);
        radioDatabase.setValue(true);
    }

    @Override
    public void cancelCreate() {
        setStartState();
        this.hide();
    }

    @Override
    public void update(LockedPeriodDTO item) {
    }

    @Override
    public void cancelUpdate(LockedPeriodDTO item) {

    }

    @Override
    public void cancelUpdateAll() {

    }

    @Override
    public void startUpdate() {

    }

    @Override
    public void setUpdateEnabled(boolean updateEnabled) {
    }

    @Override
    public HandlerRegistration addUpdateHandler(org.activityinfo.ui.client.page.config.mvp.CanUpdate.UpdateHandler
                                                            handler) {
        return null;
    }

    @Override
    public HandlerRegistration addCancelUpdateHandler(org.activityinfo.ui.client.page.config.mvp.CanUpdate
                                                                  .CancelUpdateHandler handler) {
        return null;
    }

    @Override
    public HandlerRegistration addRequestUpdateHandler(org.activityinfo.ui.client.page.config.mvp.CanUpdate
                                                                   .RequestUpdateHandler handler) {
        return null;
    }

    @Override
    public AsyncMonitor getCreatingMonitor() {
        return null;
    }

    @Override
    public AsyncMonitor getUpdatingMonitor() {
        return null;
    }
}
