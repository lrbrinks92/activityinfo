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
import com.extjs.gxt.ui.client.store.TreeStore;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import org.activityinfo.legacy.client.monitor.NullAsyncMonitor;
import org.activityinfo.legacy.shared.model.EntityDTO;
import org.activityinfo.legacy.shared.model.UserDatabaseDTO;
import org.activityinfo.legacy.client.AsyncMonitor;
import org.activityinfo.ui.client.page.common.dialog.FormDialogCallback;
import org.activityinfo.ui.client.page.common.dialog.FormDialogTether;
import org.activityinfo.ui.client.page.common.grid.ConfirmCallback;

import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;

public class MockDesignTree implements DesignPresenter.View {

    public ModelData selection = null;
    public Map<String, Object> newEntityProperties = new HashMap<String, Object>();

    @Override
    public void init(DesignPresenter presenter, UserDatabaseDTO db,
                     TreeStore store) {

    }

    @Override
    public FormDialogTether showNewForm(EntityDTO entity,
                                        FormDialogCallback callback) {

        for (String property : newEntityProperties.keySet()) {
            ((ModelData) entity).set(property,
                    newEntityProperties.get(property));
        }

        FormDialogTether tether = createNiceMock(FormDialogTether.class);
        replay(tether);

        callback.onValidated(tether);
        return tether;
    }

    @Override
    public Menu getNewMenu() {
        return new Menu();
    }

    @Override
    public MenuItem getNewAttributeGroup() {
        return new MenuItem();
    }

    @Override
    public MenuItem getNewAttribute() {
        return new MenuItem();
    }

    @Override
    public MenuItem getNewIndicator() {
        return new MenuItem();
    }

    @Override
    public void showForm(ModelData model) {

    }

    @Override
    public AsyncMonitor getLoadingMonitor() {
        return new NullAsyncMonitor();
    }

    protected void mockEditEntity(EntityDTO entity) {

    }

    @Override
    public void setActionEnabled(String actionId, boolean enabled) {

    }

    @Override
    public void confirmDeleteSelected(ConfirmCallback callback) {

    }

    @Override
    public ModelData getSelection() {
        return selection;
    }

    @Override
    public AsyncMonitor getDeletingMonitor() {
        return null;
    }

    @Override
    public AsyncMonitor getSavingMonitor() {
        return null;
    }

    @Override
    public void refresh() {

    }
}
