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

import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.store.Record;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.google.common.base.Optional;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.ImplementedBy;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.legacy.client.AsyncMonitor;
import org.activityinfo.legacy.client.Dispatcher;
import org.activityinfo.legacy.client.state.StateProvider;
import org.activityinfo.legacy.shared.Log;
import org.activityinfo.legacy.shared.command.AddTarget;
import org.activityinfo.legacy.shared.command.Delete;
import org.activityinfo.legacy.shared.command.GetTargets;
import org.activityinfo.legacy.shared.command.UpdateEntity;
import org.activityinfo.legacy.shared.command.result.CreateResult;
import org.activityinfo.legacy.shared.command.result.TargetResult;
import org.activityinfo.legacy.shared.command.result.VoidResult;
import org.activityinfo.legacy.shared.model.PartnerDTO;
import org.activityinfo.legacy.shared.model.ProjectDTO;
import org.activityinfo.legacy.shared.model.TargetDTO;
import org.activityinfo.legacy.shared.model.UserDatabaseDTO;
import org.activityinfo.ui.client.AppEvents;
import org.activityinfo.ui.client.EventBus;
import org.activityinfo.ui.client.page.PageId;
import org.activityinfo.ui.client.page.PageState;
import org.activityinfo.ui.client.page.common.dialog.FormDialogCallback;
import org.activityinfo.ui.client.page.common.dialog.FormDialogTether;
import org.activityinfo.ui.client.page.common.grid.AbstractGridPresenter;
import org.activityinfo.ui.client.page.common.grid.GridView;
import org.activityinfo.ui.client.page.common.toolbar.UIActions;

import java.util.HashMap;
import java.util.Map;

/**
 * Displays a grid where users can add, remove and change Targets
 */
public class DbTargetEditor extends AbstractGridPresenter<TargetDTO> implements DbPage {

    public static final PageId PAGE_ID = new PageId("targets");

    @ImplementedBy(DbTargetGrid.class)
    public interface View extends GridView<DbTargetEditor, TargetDTO> {
        void init(DbTargetEditor editor, UserDatabaseDTO db, ListStore<TargetDTO> store);

        void createTargetValueContainer(Widget w);

        FormDialogTether showAddDialog(TargetDTO target, UserDatabaseDTO db, FormDialogCallback callback);

        AsyncMonitor getLoadingMonitor();
    }

    private final Dispatcher service;
    private final EventBus eventBus;
    private final View view;

    private UserDatabaseDTO db;
    private ListStore<TargetDTO> store;
    private final TargetIndicatorPresenter targetIndicatorPresenter;

    @Inject
    public DbTargetEditor(EventBus eventBus,
                          Dispatcher service,
                          StateProvider stateMgr,
                          View view,
                          Provider<TargetIndicatorPresenter> targetIndicatorPresenterProvider) {

        super(eventBus, stateMgr, view);
        this.service = service;
        this.eventBus = eventBus;
        this.view = view;
        targetIndicatorPresenter = targetIndicatorPresenterProvider.get();
    }

    @Override
    public void go(UserDatabaseDTO db) {
        this.db = db;

        store = new ListStore<TargetDTO>();
        store.setSortField("name");
        store.setSortDir(Style.SortDir.ASC);

        fillStore();

        view.init(this, db, store);
        view.setActionEnabled(UIActions.DELETE, false);
        view.setActionEnabled(UIActions.EDIT, false);

        view.createTargetValueContainer((Widget) targetIndicatorPresenter.getWidget());
        targetIndicatorPresenter.go(db);
    }

    private void fillStore() {

        service.execute(new GetTargets(db.getId()), view.getLoadingMonitor(), new AsyncCallback<TargetResult>() {

            @Override
            public void onFailure(Throwable caught) {

            }

            @Override
            public void onSuccess(TargetResult result) {
                store.add(result.getData());
            }

        });
    }

    @Override
    protected void onDeleteConfirmed(final TargetDTO model) {
        service.execute(new Delete(model), view.getDeletingMonitor(), new AsyncCallback<VoidResult>() {
            @Override
            public void onFailure(Throwable caught) {
                Log.error("Failed to remove target. ", caught);
            }

            @Override
            public void onSuccess(VoidResult result) {
                store.remove(model);
                store.commitChanges();
                eventBus.fireEvent(AppEvents.SCHEMA_CHANGED);
            }
        });
    }

    @Override
    protected void onAdd() {
        final TargetDTO newTarget = new TargetDTO();

        this.view.showAddDialog(newTarget, db, new FormDialogCallback() {

            @Override
            public void onValidated(final FormDialogTether dlg) {

                service.execute(new AddTarget(db.getId(), newTarget), dlg, new AsyncCallback<CreateResult>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        MessageBox.alert(I18N.CONSTANTS.error(), I18N.CONSTANTS.errorOnServer(), null);
                    }

                    @Override
                    public void onSuccess(CreateResult result) {
                        newTarget.setId(result.getNewId());

                        if (newTarget.get("partnerId") != null) {
                            PartnerDTO partner = db.getPartnerById((Integer) newTarget.get("partnerId"));
                            newTarget.setPartner(partner);
                        }

                        if (newTarget.get("projectId") != null) {
                            ProjectDTO project = db.getProjectById((Integer) newTarget.get("projectId"));
                            newTarget.setProject(project);
                        }

                        store.add(newTarget);
                        store.commitChanges();

                        eventBus.fireEvent(AppEvents.SCHEMA_CHANGED);
                        dlg.hide();
                    }
                });
            }
        });
    }

    @Override
    protected void onEdit(final TargetDTO dto) {

        this.view.showAddDialog(dto, db, new FormDialogCallback() {
            @Override
            public void onValidated(final FormDialogTether dlg) {

                final Record record = store.getRecord(dto);
                service.execute(new UpdateEntity(dto, getChangedProperties(record)),
                        dlg,
                        new AsyncCallback<VoidResult>() {
                            @Override
                            public void onFailure(Throwable caught) {

                            }

                            @Override
                            public void onSuccess(VoidResult result) {

                                PartnerDTO partner = db.getPartnerById((Integer) record.get("partnerId"));
                                dto.setPartner(partner);

                                ProjectDTO project = db.getProjectById((Integer) record.get("projectId"));
                                dto.setProject(project);

                                store.commitChanges();
                                eventBus.fireEvent(AppEvents.SCHEMA_CHANGED);
                                dlg.hide();
                            }
                        });
            }
        });
    }

    protected Map<String, Object> getChangedProperties(Record record) {
        Map<String, Object> changes = new HashMap<String, Object>();

        for (String property : record.getChanges().keySet()) {
            changes.put(property, record.get(property));
        }
        return changes;
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
    public boolean navigate(PageState place) {
        return false;
    }

    @Override
    public void shutdown() {

    }

    @Override
    protected String getStateId() {
        return "TargetGrid";
    }

    @Override
    public void onSelectionChanged(ModelData selectedItem) {
        view.setActionEnabled(UIActions.DELETE, selectedItem != null);
        view.setActionEnabled(UIActions.EDIT, selectedItem != null);
        targetIndicatorPresenter.load(Optional.fromNullable(view.getSelection()));
    }
}
