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

import com.extjs.gxt.ui.client.data.*;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.legacy.client.AsyncMonitor;
import org.activityinfo.legacy.client.Dispatcher;
import org.activityinfo.legacy.client.callback.Created;
import org.activityinfo.legacy.client.callback.Deleted;
import org.activityinfo.legacy.client.callback.SuccessCallback;
import org.activityinfo.legacy.shared.command.CreateEntity;
import org.activityinfo.legacy.shared.command.Delete;
import org.activityinfo.legacy.shared.command.GetSchema;
import org.activityinfo.legacy.shared.command.UpdateEntity;
import org.activityinfo.legacy.shared.command.result.VoidResult;
import org.activityinfo.legacy.shared.model.SchemaDTO;
import org.activityinfo.legacy.shared.model.UserDatabaseDTO;
import org.activityinfo.ui.client.AppEvents;
import org.activityinfo.ui.client.ClientContext;
import org.activityinfo.ui.client.EventBus;
import org.activityinfo.ui.client.page.NavigationEvent;
import org.activityinfo.ui.client.page.NavigationHandler;
import org.activityinfo.ui.client.page.PageId;
import org.activityinfo.ui.client.page.common.dialog.FormDialogCallback;
import org.activityinfo.ui.client.page.common.dialog.FormDialogImpl;
import org.activityinfo.ui.client.page.common.dialog.FormDialogTether;
import org.activityinfo.ui.client.page.common.toolbar.ActionListener;
import org.activityinfo.ui.client.page.common.toolbar.UIActions;
import org.activityinfo.ui.client.page.config.design.dialog.NewDbDialog;
import org.activityinfo.ui.client.page.config.form.DatabaseForm;

import java.util.HashMap;
import java.util.Map;

public class DbListPresenter implements ActionListener {
    public static final PageId PAGE_ID = new PageId("dblist");

    public interface View {
        void setActionEnabled(String id, boolean enabled);

        AsyncMonitor getDeletingMonitor();
    }

    private final EventBus eventBus;
    private final Dispatcher dispatcher;
    private final View view;

    private ListStore<UserDatabaseDTO> store;
    private BaseListLoader<ListLoadResult<UserDatabaseDTO>> loader;
    private UserDatabaseDTO selection;

    @Inject
    public DbListPresenter(EventBus eventBus, Dispatcher dispatcher, View view) {
        this.eventBus = eventBus;
        this.dispatcher = dispatcher;
        this.view = view;

        createLoader();
        createStore();
        loader.load();
    }

    private void createStore() {
        store = new ListStore<UserDatabaseDTO>(loader);
    }

    private void createLoader() {
        loader = new BaseListLoader<ListLoadResult<UserDatabaseDTO>>(new Proxy());
        loader.setRemoteSort(false);
    }

    public ListStore<UserDatabaseDTO> getStore() {
        return store;
    }

    public void onSelectionChanged(UserDatabaseDTO selectedItem) {
        selection = selectedItem;
        enableActions();
    }

    private void enableActions() {
        if (selection == null) {
            view.setActionEnabled(UIActions.DELETE, false);
            view.setActionEnabled(UIActions.EDIT, false);
            view.setActionEnabled(UIActions.RENAME, false);
        } else {
            view.setActionEnabled(UIActions.DELETE, userHasRightToDeleteSelectedDatabase());
            view.setActionEnabled(UIActions.EDIT, userHasRightToEditSelectedDatabase());
            view.setActionEnabled(UIActions.RENAME, userHasRightToEditSelectedDatabase());
        }
    }

    private boolean userHasRightToDeleteSelectedDatabase() {
        return selection.getAmOwner();
    }

    private boolean userHasRightToEditSelectedDatabase() {
        return (selection.isDesignAllowed() || selection.isManageUsersAllowed());
    }

    @Override
    public void onUIAction(String actionId) {
        if (UIActions.DELETE.equals(actionId)) {
            onDelete();
        } else if (UIActions.EDIT.equals(actionId)) {
            onEdit();
        } else if (UIActions.ADD.equals(actionId)) {
            onAdd();
        } else if (UIActions.RENAME.equals(actionId)) {
            onRename();
        }
    }

    private void onDelete() {
        MessageBox.confirm(ClientContext.getAppTitle(),
                I18N.MESSAGES.confirmDeleteDb(selection.getName()),
                new Listener<MessageBoxEvent>() {
                    @Override
                    public void handleEvent(MessageBoxEvent be) {
                        if (be.getButtonClicked().getItemId().equals(Dialog.YES)) {
                            deleteSelection();
                        }
                    }
                });
    }

    private void deleteSelection() {
        dispatcher.execute(new Delete(selection), view.getDeletingMonitor(), new Deleted() {
            @Override
            public void deleted() {
                store.remove(selection);
                selection = null;
            }
        });
    }

    public void onRename() {
        DatabaseForm form = new DatabaseForm(dispatcher);
        form.getBinding().bind(selection);
        form.hideCountryField(); // it's not allowed to change country by design
        final FormDialogImpl dialog = new FormDialogImpl(form);
        dialog.setWidth(400);
        dialog.setHeight(170);
        dialog.setHeadingText(I18N.CONSTANTS.renameDatabase());

        dialog.show(new FormDialogCallback() {
            @Override
            public void onValidated() {
                update(selection, dialog);
            }
        });
    }

    private void update(UserDatabaseDTO db, final FormDialogImpl dialog) {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("name", db.getName());
        properties.put("fullName", db.getFullName());
        properties.put("countryId", db.getCountry().getId());

        dispatcher.execute(new UpdateEntity(db.getEntityName(), db.getId(), properties),
                new SuccessCallback<VoidResult>() {

                    @Override
                    public void onSuccess(VoidResult arg0) {
                        eventBus.fireEvent(AppEvents.SCHEMA_CHANGED);
                        loader.load();
                        dialog.hide();
                    }
                });
    }

    public void onEdit() {
        requestNavigationToDatabaseEditPage();
    }

    public void onAdd() {
        NewDbDialog newDbDialog = new NewDbDialog(dispatcher);
        newDbDialog.show();
        newDbDialog.setSuccessCallback(new SuccessCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                eventBus.fireEvent(AppEvents.SCHEMA_CHANGED);
                loader.load();
            }
        });
    }

    /**
     * Package visible for testing *
     */
    void save(UserDatabaseDTO db, final FormDialogTether dialog) {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("name", db.getName());
        properties.put("fullName", db.getFullName());
        properties.put("countryId", db.getCountry().getId());

        dispatcher.execute(new CreateEntity("UserDatabase", properties), dialog, new Created() {
            @Override
            public void created(int newId) {
                eventBus.fireEvent(AppEvents.SCHEMA_CHANGED);
                loader.load();
                dialog.hide();
            }
        });
    }

    private void requestNavigationToDatabaseEditPage() {
        eventBus.fireEvent(new NavigationEvent(NavigationHandler.NAVIGATION_REQUESTED,
                new DbPageState(DbConfigPresenter.PAGE_ID, selection.getId())));
    }

    protected class Proxy implements DataProxy {
        @Override
        public void load(DataReader dataReader, Object loadConfig, final AsyncCallback callback) {
            dispatcher.execute(new GetSchema(), new AsyncCallback<SchemaDTO>() {
                @Override
                public void onFailure(Throwable caught) {
                    callback.onFailure(caught);
                }

                @Override
                public void onSuccess(SchemaDTO schema) {
                    callback.onSuccess(new BaseListLoadResult<UserDatabaseDTO>(schema.getDatabases()));
                }
            });
        }
    }
}
