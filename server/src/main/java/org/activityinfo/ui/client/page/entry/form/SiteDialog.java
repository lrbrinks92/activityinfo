package org.activityinfo.ui.client.page.entry.form;

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

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.CardLayout;
import com.extjs.gxt.ui.client.widget.layout.FlowLayout;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import org.activityinfo.core.client.ResourceLocator;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.legacy.client.Dispatcher;
import org.activityinfo.legacy.client.callback.SuccessCallback;
import org.activityinfo.legacy.shared.adapter.ResourceLocatorAdaptor;
import org.activityinfo.legacy.shared.command.CreateSite;
import org.activityinfo.legacy.shared.command.UpdateEntity;
import org.activityinfo.legacy.shared.command.UpdateSite;
import org.activityinfo.legacy.shared.command.result.CreateResult;
import org.activityinfo.legacy.shared.command.result.VoidResult;
import org.activityinfo.legacy.shared.exception.IllegalAccessCommandException;
import org.activityinfo.legacy.shared.model.ActivityFormDTO;
import org.activityinfo.legacy.shared.model.LocationDTO;
import org.activityinfo.legacy.shared.model.SiteDTO;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.model.legacy.KeyGenerator;
import org.activityinfo.ui.client.EventBus;
import org.activityinfo.ui.client.page.config.design.dialog.NewFormDialog;
import org.activityinfo.ui.client.page.entry.form.resources.SiteFormResources;
import org.activityinfo.ui.client.style.legacy.icon.IconImageBundle;

import java.util.List;
import java.util.Map;

public class SiteDialog extends Window {

    private static final int HEIGHT = 470;
    private static final int WIDTH = 500;

    private final FormNavigationListView navigationListView;
    private final LayoutContainer sectionContainer;

    private final List<FormSection<SiteDTO>> sections = Lists.newArrayList();
    private LocationFormSection locationForm;
    private final Button finishButton;

    private final Dispatcher dispatcher;
    private final ResourceLocator resourceLocator;
    private final ActivityFormDTO activity;
    private final EventBus eventBus;

    private SiteDialogCallback callback;

    private SiteDTO site = null;

    /**
     * True if this is a brand new site
     */
    private boolean newSite;
    private KeyGenerator keyGenerator;

    public SiteDialog(Dispatcher dispatcher, ActivityFormDTO activity, EventBus eventBus) {
        this.dispatcher = dispatcher;
        this.resourceLocator = new ResourceLocatorAdaptor(dispatcher);
        this.activity = activity;
        this.eventBus = eventBus;

        setHeadingText(I18N.MESSAGES.addNewSiteForActivity(activity.getName()));
        setWidth(WIDTH);
        setHeight(HEIGHT);

        setLayout(new BorderLayout());

        // show alert only for report frequency ONCE
        if (activity.getReportingFrequency() == ActivityFormDTO.REPORT_ONCE) {
            BorderLayoutData alertLayout = new BorderLayoutData(LayoutRegion.NORTH);
            alertLayout.setSize(30);
            add(modernViewAlert(), alertLayout);
        }

        navigationListView = new FormNavigationListView();
        BorderLayoutData navigationLayout = new BorderLayoutData(LayoutRegion.WEST);
        navigationLayout.setSize(150);
        add(navigationListView, navigationLayout);

        sectionContainer = new LayoutContainer();
        final CardLayout sectionLayout = new CardLayout();
        sectionContainer.setLayout(sectionLayout);

        add(sectionContainer, new BorderLayoutData(LayoutRegion.CENTER));

        if (activity.getLocationType().isAdminLevel()) {
            locationForm = new BoundLocationSection(dispatcher, activity);
        } else if (activity.getLocationType().isNationwide()) {
            locationForm = new NullLocationFormSection(activity.getLocationType());
        } else {
            locationForm = new LocationSection(dispatcher, activity);
        }

        addSection(FormSectionModel.forComponent(new ActivitySection(activity))
                .withHeader(I18N.CONSTANTS.siteDialogIntervention())
                .withDescription(I18N.CONSTANTS.siteDialogInterventionDesc()));

        if (!activity.getLocationType().isNationwide()) {
            addSection(FormSectionModel.forComponent(locationForm)
                    .withHeader(I18N.CONSTANTS.location())
                    .withDescription(I18N.CONSTANTS.siteDialogSiteDesc()));
        }

        if (!activity.getAttributeGroups().isEmpty()) {

            addSection(FormSectionModel.forComponent(new AttributeSection(activity))
                    .withHeader(I18N.CONSTANTS.attributes())
                    .withDescription(I18N.CONSTANTS.siteDialogAttributes()));

        }

        if (activity.getReportingFrequency() == ActivityFormDTO.REPORT_ONCE && !activity.getIndicators().isEmpty()) {

            addSection(FormSectionModel.forComponent(new IndicatorSection(activity))
                    .withHeader(I18N.CONSTANTS.indicators())
                    .withDescription(I18N.CONSTANTS.siteDialogIndicators()));

        }

        addSection(FormSectionModel.forComponent(new CommentSection(315, 330))
                .withHeader(I18N.CONSTANTS.comments())
                .withDescription(I18N.CONSTANTS.siteDialogComments()));

        SiteFormResources.INSTANCE.style().ensureInjected();

        navigationListView.getSelectionModel()
                .addSelectionChangedListener(new SelectionChangedListener<FormSectionModel>() {

                    @Override
                    public void selectionChanged(SelectionChangedEvent<FormSectionModel> se) {
                        if (!se.getSelection().isEmpty()) {
                            sectionLayout.setActiveItem(se.getSelectedItem().getComponent());
                        }
                    }
                });

        finishButton = new Button(I18N.CONSTANTS.save(),
                IconImageBundle.ICONS.save(),
                new SelectionListener<ButtonEvent>() {

                    @Override
                    public void componentSelected(ButtonEvent ce) {
                        finishButton.disable();
                        if (validateSections()) {
                            saveLocation();
                        } else {
                            finishButton.enable();
                        }
                    }
                });

        getButtonBar().add(finishButton);
    }

    private LayoutContainer modernViewAlert() {
        Anchor linkToDesign = new Anchor(I18N.CONSTANTS.switchToNewLayout());
        linkToDesign.setHref("#");
        linkToDesign.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (activity.isDesignAllowed()) {
                    Map<String, Object> changes = Maps.newHashMap();
                    changes.put("classicView", Boolean.FALSE);

                    dispatcher.execute(new UpdateEntity(activity, changes)).then(new SuccessCallback<VoidResult>() {
                        @Override
                        public void onSuccess(VoidResult result) {
                            SiteDialog.this.hide();
                            resourceLocator.getFormInstance(site.getInstanceId()).then(new SuccessCallback<FormInstance>() {
                                @Override
                                public void onSuccess(FormInstance result) {
                                    SiteDialogLauncher.showModernFormDialog(activity.getName(), result, callback, newSite, resourceLocator);
                                }
                            });
                        }
                    });
                } else {
                    MessageBox.alert(I18N.CONSTANTS.alert(), I18N.CONSTANTS.noDesignPrivileges(), new SelectionListener<MessageBoxEvent>() {
                        @Override
                        public void componentSelected(MessageBoxEvent ce) {
                        }
                    });
                }
            }
        });

        Anchor linkToMore = new Anchor(I18N.CONSTANTS.learnMore());
        linkToMore.setHref(NewFormDialog.CLASSIC_VIEW_EXPLANATION_URL);
        linkToMore.setTarget("_blank");

        ContentPanel panel = new ContentPanel();
        panel.setHeaderVisible(false);
        panel.setLayout(new FlowLayout());
        panel.add(new Label(I18N.CONSTANTS.alertAboutModerView()));
        panel.add(linkToDesign);
        panel.add(new InlineLabel(I18N.CONSTANTS.orWithSpaces()));
        panel.add(linkToMore);
        return panel;
    }

    public void showNew(SiteDTO site, LocationDTO location, boolean locationIsNew, SiteDialogCallback callback) {
        this.newSite = true;
        this.callback = callback;
        locationForm.updateForm(location, locationIsNew);
        updateForms(site, true);
        show();
    }

    public void showExisting(SiteDTO site, SiteDialogCallback callback) {
        this.newSite = false;
        this.site = site;
        this.callback = callback;
        LocationDTO location = site.getLocation();
        location.setLocationTypeId(activity.getLocationTypeId());

        locationForm.updateForm(location, false);
        updateForms(site, false);
        show();
    }

    private void updateForms(SiteDTO site, boolean isNew) {
        for (FormSectionModel<SiteDTO> section : navigationListView.getStore().getModels()) {
            section.getSection().updateForm(site, isNew);
        }
    }

    private void updateModel(final SiteDTO newSite) {
        for (FormSectionModel<SiteDTO> section : navigationListView.getStore().getModels()) {
            section.getSection().updateModel(newSite);
        }

        // no-location: hack
        locationForm.updateModel(newSite);
    }

    private void addSection(FormSectionModel<SiteDTO> model) {
        navigationListView.addSection(model);
        sectionContainer.add(model.getComponent());
        sections.add(model.getSection());
    }

    private boolean validateSections() {
        for (FormSectionModel<SiteDTO> section : navigationListView.getStore().getModels()) {
            if (!section.getSection().validate()) {
                navigationListView.getSelectionModel().select(section, false);
                section.getSection().validate(); // validate after render to enable validation-error styling
                MessageBox.alert(getHeadingHtml(), I18N.CONSTANTS.pleaseCompleteForm(), null);
                return false;
            }
        }
        return true;
    }

    private void saveLocation() {
        locationForm.save(new AsyncCallback<Void>() {

            @Override
            public void onSuccess(Void result) {
                saveSite();
            }

            @Override
            public void onFailure(Throwable caught) {
                showError(caught);
            }
        });
    }

    private void saveSite() {
        if (newSite) {
            saveNewSite();
        } else {
            updateSite();
        }
    }

    private void saveNewSite() {
        final SiteDTO newSite = new SiteDTO();
        keyGenerator = new KeyGenerator();
        newSite.setId(keyGenerator.generateInt());
        newSite.setActivityId(activity.getId());

        if (activity.getReportingFrequency() == ActivityFormDTO.REPORT_ONCE) {
            newSite.setReportingPeriodId(new KeyGenerator().generateInt());
        }

        updateModel(newSite);

        dispatcher.execute(new CreateSite(newSite), new AsyncCallback<CreateResult>() {

            @Override
            public void onFailure(Throwable caught) {
                showError(caught);
            }

            @Override
            public void onSuccess(CreateResult result) {
                hide();
                callback.onSaved();
            }
        });
    }

    private void updateSite() {

        final SiteDTO updated = new SiteDTO(site);
        updateModel(updated);

        dispatcher.execute(new UpdateSite(site, updated), new AsyncCallback<VoidResult>() {

            @Override
            public void onFailure(Throwable caught) {
                showError(caught);
            }

            @Override
            public void onSuccess(VoidResult result) {
                hide();
                callback.onSaved();
            }
        });
    }

    private void showError(Throwable caught) {
        finishButton.enable();
        if (caught != null && caught instanceof IllegalAccessCommandException) {
            MessageBox.alert(I18N.CONSTANTS.dataEntry(), I18N.CONSTANTS.notAuthorized(), null);
        } else {
            MessageBox.alert(I18N.CONSTANTS.dataEntry(), I18N.CONSTANTS.serverError(), null);
        }
    }
}
