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

import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.store.Record;
import com.extjs.gxt.ui.client.store.Store;
import com.extjs.gxt.ui.client.store.TreeStore;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.google.common.base.Function;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.ImplementedBy;
import com.google.inject.Inject;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.i18n.shared.UiConstants;
import org.activityinfo.legacy.client.AsyncMonitor;
import org.activityinfo.legacy.client.Dispatcher;
import org.activityinfo.legacy.client.callback.SuccessCallback;
import org.activityinfo.legacy.client.state.StateProvider;
import org.activityinfo.legacy.shared.command.*;
import org.activityinfo.legacy.shared.command.result.CreateResult;
import org.activityinfo.legacy.shared.command.result.VoidResult;
import org.activityinfo.legacy.shared.model.*;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.ui.client.AppEvents;
import org.activityinfo.ui.client.EventBus;
import org.activityinfo.ui.client.page.NavigationEvent;
import org.activityinfo.ui.client.page.NavigationHandler;
import org.activityinfo.ui.client.page.PageId;
import org.activityinfo.ui.client.page.PageState;
import org.activityinfo.ui.client.page.common.dialog.FormDialogCallback;
import org.activityinfo.ui.client.page.common.dialog.FormDialogTether;
import org.activityinfo.ui.client.page.common.grid.AbstractEditorGridPresenter;
import org.activityinfo.ui.client.page.common.grid.TreeGridView;
import org.activityinfo.ui.client.page.common.toolbar.UIActions;
import org.activityinfo.ui.client.page.config.DbPage;
import org.activityinfo.ui.client.page.config.DbPageState;
import org.activityinfo.ui.client.page.config.design.dialog.NewFormDialog;
import org.activityinfo.ui.client.page.config.design.importer.SchemaImportDialog;
import org.activityinfo.ui.client.page.config.design.importer.SchemaImporter;
import org.activityinfo.ui.client.page.instance.InstancePage;
import org.activityinfo.ui.client.page.instance.InstancePlace;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Presenter for the Design Page, which enables the user to define UserDatabases
 * and their Activities, Attributes, and Indicators.
 *
 * @author Alex Bertram
 */
public class DesignPresenter extends AbstractEditorGridPresenter<ModelData> implements DbPage {
    public static final PageId PAGE_ID = new PageId("design");

    @ImplementedBy(DesignView.class)
    public interface View extends TreeGridView<DesignPresenter, ModelData> {
        public void init(DesignPresenter presenter, UserDatabaseDTO db, TreeStore store);

        public FormDialogTether showNewForm(EntityDTO entity, FormDialogCallback callback);

        public Menu getNewMenu();

        public MenuItem getNewAttributeGroup();

        public MenuItem getNewAttribute();

        public MenuItem getNewIndicator();

        public void showForm(ModelData model);

        AsyncMonitor getLoadingMonitor();
    }

    private final EventBus eventBus;
    private final Dispatcher service;
    private final View view;
    private final UiConstants messages;

    private UserDatabaseDTO db;
    private TreeStore<ModelData> treeStore;

    @Inject
    public DesignPresenter(EventBus eventBus,
                           Dispatcher service,
                           StateProvider stateMgr,
                           View view,
                           UiConstants messages) {
        super(eventBus, service, stateMgr, view);
        this.eventBus = eventBus;
        this.service = service;
        this.view = view;
        this.messages = messages;
    }

    @Override
    public void go(UserDatabaseDTO db) {

        this.db = db;

        treeStore = new TreeStore<>();
        fillStore(messages);

        initListeners(treeStore, null);

        this.view.init(this, db, treeStore);
        this.view.setActionEnabled(UIActions.DELETE, false);
        this.view.setActionEnabled(UIActions.EDIT, false);
        this.view.setActionEnabled(UIActions.OPEN_TABLE, false);

        initMenu();
    }

    private void initMenu() {
        Menu newMenu = this.view.getNewMenu();
        if (newMenu == null) {
            return;
        }

        newMenu.addListener(Events.BeforeShow, new Listener<BaseEvent>() {
            @Override
            public void handleEvent(BaseEvent be) {

                ModelData sel = DesignPresenter.this.view.getSelection();
                IsActivityDTO activity = DesignPresenter.this.getSelectedActivity(sel);

                DesignPresenter.this.view.getNewAttributeGroup().setEnabled(activity != null && activity.getClassicView());
                DesignPresenter.this.view.getNewAttribute().setEnabled(activity != null && (sel instanceof AttributeGroupDTO || sel instanceof AttributeDTO) && activity.getClassicView());
                DesignPresenter.this.view.getNewIndicator().setEnabled(activity != null && activity.getClassicView());
            }
        });
    }

    public void refresh() {
        service.execute(new GetSchema(), view.getLoadingMonitor(),
                new AsyncCallback<SchemaDTO>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        //handled by masking async monitor
                    }

                    @Override
                    public void onSuccess(SchemaDTO result) {
                        db = result.getDatabaseById(db.getId());
                        fillStore(messages);
                        DesignPresenter.this.view.showForm(DesignPresenter.this.view.getSelection());
                    }
                });
    }

    @Override
    public void shutdown() {

    }

    private void fillStore(UiConstants messages) {

        treeStore.removeAll();

        for (ActivityDTO activity : db.getActivities()) {
            ActivityDTO activityNode = new ActivityDTO(activity);
            treeStore.add(activityNode, false);

            if (!activityNode.getClassicView()) {
                continue; // skip indicators and attributes in tree if activity is not classicView=true
            }

            final AttributeGroupFolder attributeFolder = new AttributeGroupFolder(messages.attributes());
            treeStore.add(activityNode, attributeFolder, false);

            final IndicatorFolder indicatorFolder = new IndicatorFolder(messages.indicators());
            treeStore.add(activityNode, indicatorFolder, false);

            service.execute(new GetActivityForm(activity.getId())).then(new SuccessCallback<ActivityFormDTO>() {
                @Override
                public void onSuccess(ActivityFormDTO activityForm) {
                    for (AttributeGroupDTO group : activityForm.getAttributeGroups()) {
                        if (group != null) {
                            AttributeGroupDTO groupNode = new AttributeGroupDTO(group);
                            treeStore.add(attributeFolder, groupNode, false);

                            for (AttributeDTO attribute : group.getAttributes()) {
                                AttributeDTO attributeNode = new AttributeDTO(attribute);
                                treeStore.add(groupNode, attributeNode, false);
                            }
                        }
                    }

                    for (IndicatorGroup group : activityForm.groupIndicators()) {
                        for (IndicatorDTO indicator : group.getIndicators()) {
                            IndicatorDTO indicatorNode = new IndicatorDTO(indicator);
                            treeStore.add(indicatorFolder, indicatorNode, false);
                        }
                    }
                }
            });

        }

        for (LocationTypeDTO locationType : db.getCountry().getLocationTypes()) {
            if (Objects.equals(locationType.getDatabaseId(), db.getId()) && !locationType.isDeleted()) {
                treeStore.add(locationType, false);
            }
        }
    }

    @Override
    public Store<ModelData> getStore() {
        return treeStore;
    }

    public TreeStore<ModelData> getTreeStore() {
        return treeStore;
    }

    @Override
    public boolean navigate(PageState place) {
        return place instanceof DbPageState &&
               place.getPageId().equals(PAGE_ID) &&
               ((DbPageState) place).getDatabaseId() == db.getId();
    }

    @Override
    public void onUIAction(String actionId) {
        super.onUIAction(actionId);

        if (UIActions.EXPORT.equals(actionId)) {
            Window.open("/resources/database/" + db.getId() + "/schema.csv", "_blank", null);

        } else if (UIActions.IMPORT.equals(actionId)) {
            SchemaImporter importer = new SchemaImporter(service, db);
            SchemaImportDialog dialog = new SchemaImportDialog(importer);
            dialog.show().then(new Function<Void, Object>() {
                @Nullable
                @Override
                public Object apply(@Nullable Void input) {
                    refresh();
                    return null;
                }
            });
        } else if(UIActions.EDIT.equals(actionId)) {
            eventBus.fireEvent(new NavigationEvent(
                    NavigationHandler.NAVIGATION_REQUESTED,
                    new InstancePlace(getSelectedFormClassId(), InstancePage.DESIGN_PAGE_ID)));

        } else if(UIActions.OPEN_TABLE.equals(actionId)) {
            eventBus.fireEvent(new NavigationEvent(
                    NavigationHandler.NAVIGATION_REQUESTED,
                    new InstancePlace(getSelectedFormClassId(), InstancePage.TABLE_PAGE_ID)));
        }
    }

    public ResourceId getSelectedFormClassId() {
        if (view.getSelection() instanceof IsFormClass) {
            IsFormClass formClass = (IsFormClass) view.getSelection();
            return formClass.getResourceId();
        } else {
            return getSelectedActivity(view.getSelection()).getFormClassId();
        }
    }

    public void onNodeDropped(ModelData source) {

        // update sortOrder

        ModelData parent = treeStore.getParent(source);
        List<ModelData> children = parent == null ? treeStore.getRootItems() : treeStore.getChildren(parent);

        for (int i = 0; i != children.size(); ++i) {
            Record record = treeStore.getRecord(children.get(i));
            record.set("sortOrder", i);
        }
    }

    public void onNew(String entityName) {

        final EntityDTO newEntity;
        ModelData parent;

        ModelData selected = view.getSelection();

        if ("Activity".equals(entityName)) {
            final NewFormDialog newFormDialog = new NewFormDialog();
            newFormDialog.show();
            newFormDialog.setSuccessHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    createNewActivity(newFormDialog);
                }
            });
            return;

        } else if ("LocationType".equals(entityName)) {
            newEntity = new LocationTypeDTO();
            newEntity.set("databaseId", db.getId());
            parent = null;

        } else if ("AttributeGroup".equals(entityName)) {
            IsActivityDTO activity = findActivityFolder(selected);

            newEntity = new AttributeGroupDTO();
            newEntity.set("activityId", activity.getId());
            parent = treeStore.getChild((ModelData) activity, 0);

        } else if ("Attribute".equals(entityName)) {
            AttributeGroupDTO group = findAttributeGroupNode(selected);

            newEntity = new AttributeDTO();
            newEntity.set("attributeGroupId", group.getId());

            parent = group;

        } else if ("Indicator".equals(entityName)) {
            IsActivityDTO activity = findActivityFolder(selected);

            IndicatorDTO newIndicator = new IndicatorDTO();
            newIndicator.setAggregation(IndicatorDTO.AGGREGATE_SUM);

            if (activity instanceof ActivityFormDTO) {
                newIndicator.set("sortOrder", ((ActivityFormDTO)activity).getIndicators().size() + 1);
            }

            newEntity = newIndicator;
            newEntity.set("activityId", activity.getId());

            parent = treeStore.getChild((ModelData) activity, 1);

        } else {
            return; // TODO log error
        }

        createEntity(parent, newEntity);
    }

    private void createNewActivity(NewFormDialog newFormDialog) {
        final ActivityFormDTO newActivity = new ActivityFormDTO(db);
        newActivity.set("databaseId", db.getId());
        newActivity.setName(newFormDialog.getName());
        newActivity.setCategory(newFormDialog.getCategory());

        if (newFormDialog.getViewType() == NewFormDialog.ViewType.CLASSIC || newFormDialog.getViewType() == NewFormDialog.ViewType.CLASSIC_MONTHLY) {

            newActivity.setClassicView(true);
            newActivity.setReportingFrequency(newFormDialog.getViewType() == NewFormDialog.ViewType.CLASSIC ?
                    ActivityFormDTO.REPORT_ONCE : ActivityFormDTO.REPORT_MONTHLY);

            createEntity(null, newActivity);
            return;
        } else if (newFormDialog.getViewType() == NewFormDialog.ViewType.NEW_FORM_DESIGNER) {

            newActivity.setClassicView(false);
            newActivity.setReportingFrequency(ActivityFormDTO.REPORT_ONCE);
            newActivity.setLocationType(newActivityLocationTypeForModernView());

            service.execute(new CreateEntity(newActivity), new SuccessCallback<CreateResult>() {
                @Override
                public void onSuccess(CreateResult result) {

                    newActivity.setId(result.getNewId());

                    eventBus.fireEvent(new NavigationEvent(
                            NavigationHandler.NAVIGATION_REQUESTED,
                            new InstancePlace(newActivity.getResourceId(), InstancePage.DESIGN_PAGE_ID)));
                }
            });

            return;
        }

        throw new RuntimeException("Unsupported view type of activity: " + newFormDialog.getViewType());
    }

    private LocationTypeDTO newActivityLocationTypeForModernView() {
        for (LocationTypeDTO dto : db.getCountry().getLocationTypes()) {
            if (dto.isNationwide()) {
                return dto;
            }
        }

        MessageBox.info(I18N.CONSTANTS.alert(), I18N.MESSAGES.noNationWideLocationType(db.getName(), db.getCountry().getName()), null);
        throw new RuntimeException("Failed to find nationwide location type, db:" + db.getName() + ", country:" + db.getCountry().getName());
    }

    private void createEntity(final ModelData parent, final EntityDTO newEntity) {
        view.showNewForm(newEntity, new FormDialogCallback() {
            @Override
            public void onValidated(final FormDialogTether tether) {

                service.execute(new CreateEntity(newEntity), tether, new AsyncCallback<CreateResult>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        GWT.log(caught.getMessage());
                    }

                    @Override
                    public void onSuccess(CreateResult result) {
                        newEntity.set("id", result.getNewId()); // todo add
                        // setId to
                        // EntityDTO
                        // interface

                        if (parent == null) {
                            treeStore.add(newEntity, false);
                        } else {
                            treeStore.add(parent, newEntity, false);
                        }

                        if (newEntity instanceof IsActivityDTO) {
                            treeStore.add(newEntity, new AttributeGroupFolder(messages.attributes()), false);
                            treeStore.add(newEntity, new IndicatorFolder(messages.indicators()), false);
                        }

                        tether.hide();

                        eventBus.fireEvent(AppEvents.SCHEMA_CHANGED);
                    }
                });

            }
        });
    }

    protected IsActivityDTO findActivityFolder(ModelData selected) {

        while (!(selected instanceof IsActivityDTO)) {
            selected = treeStore.getParent(selected);
        }

        return (IsActivityDTO) selected;
    }

    protected AttributeGroupDTO findAttributeGroupNode(ModelData selected) {
        if (selected instanceof AttributeGroupDTO) {
            return (AttributeGroupDTO) selected;
        }
        if (selected instanceof AttributeDTO) {
            return (AttributeGroupDTO) treeStore.getParent(selected);
        }
        throw new AssertionError("not a valid selection to add an attribute !");

    }

    @Override
    protected void onDeleteConfirmed(final ModelData model) {
        service.execute(new Delete((EntityDTO) model), view.getDeletingMonitor(), new AsyncCallback<VoidResult>() {
            @Override
            public void onFailure(Throwable caught) {

            }

            @Override
            public void onSuccess(VoidResult result) {
                treeStore.remove(model);
                eventBus.fireEvent(AppEvents.SCHEMA_CHANGED);
            }
        });
    }

    @Override
    protected String getStateId() {
        return "Design" + db.getId();
    }

    @Override
    protected Command createSaveCommand() {
        BatchCommand batch = new BatchCommand();

        for (ModelData model : treeStore.getRootItems()) {
            prepareBatch(batch, model);
        }
        return batch;
    }

    protected void prepareBatch(BatchCommand batch, ModelData model) {
        if (model instanceof EntityDTO) {
            Record record = treeStore.getRecord(model);
            if (record.isDirty()) {
                batch.add(new UpdateEntity((EntityDTO) model, this.getChangedProperties(record)));
            }
        }

        for (ModelData child : treeStore.getChildren(model)) {
            prepareBatch(batch, child);
        }
    }

    @Override
    public void onSelectionChanged(ModelData selectedItem) {
        view.setActionEnabled(UIActions.EDIT, this.db.isDesignAllowed() && canEditWithFormDesigner(selectedItem));
        view.setActionEnabled(UIActions.DELETE, this.db.isDesignAllowed() && selectedItem instanceof EntityDTO);
        view.setActionEnabled(UIActions.OPEN_TABLE, getSelectedActivity(selectedItem) != null || selectedItem instanceof IsFormClass);
    }

    private boolean canEditWithFormDesigner(ModelData selectedItem) {
        IsActivityDTO activity = getSelectedActivity(selectedItem);
        if (activity != null) {
            return  activity.getReportingFrequency() == ActivityFormDTO.REPORT_ONCE;
        } else {
            return selectedItem instanceof IsFormClass;
        }
    }

    private IsActivityDTO getSelectedActivity(ModelData selectedItem) {
        if (selectedItem instanceof IsActivityDTO) {
            return (IsActivityDTO) selectedItem;
        } else if (selectedItem instanceof AttributeGroupFolder ||
                selectedItem instanceof IndicatorFolder ||
                selectedItem instanceof AttributeGroupDTO ||
                selectedItem instanceof IndicatorDTO ||
                selectedItem instanceof LocationTypeDTO ||
                selectedItem instanceof AttributeDTO) {
            return getSelectedActivity(treeStore.getParent(selectedItem));
        }
        return null;
    }

    @Override
    public PageId getPageId() {
        return PAGE_ID;
    }

    @Override
    public Object getWidget() {
        return view;
    }

    @Override
    protected void onSaved() {
        eventBus.fireEvent(AppEvents.SCHEMA_CHANGED);
        refresh();
    }
}
