package org.activityinfo.ui.client.component.table.dialog;
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

import com.google.common.collect.Sets;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.core.shared.Projection;
import org.activityinfo.promise.Promise;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.ui.client.component.table.InstanceTableView;
import org.activityinfo.ui.client.style.ElementStyle;
import org.activityinfo.ui.client.widget.ConfirmDialog;

import java.util.Set;
import java.util.logging.Logger;

/**
 * @author yuriyz on 4/8/14.
 */
public class DeleteAction implements ConfirmDialog.Action {

    private static final Logger LOGGER = Logger.getLogger(DeleteAction.class.getName());

    private final InstanceTableView tableView;
    private final Set<ResourceId> selection = Sets.newHashSet();
    private final String formClassLabel;


    public DeleteAction(InstanceTableView tableView) {
        this.tableView = tableView;
        this.formClassLabel = tableView.getFormClassLabel();
        for (Projection projection : tableView.getTable().getSelectionModel().getSelectedSet()) {
            selection.add(projection.getRootInstanceId());
        }
    }


    @Override
    public ConfirmDialog.Messages getConfirmationMessages() {
        return new ConfirmDialog.Messages(
                I18N.CONSTANTS.confirmDeletion(),
                I18N.MESSAGES.removeTableRowsConfirmation(selection.size(), formClassLabel),
                I18N.CONSTANTS.delete());
    }

    @Override
    public ConfirmDialog.Messages getProgressMessages() {
        return new ConfirmDialog.Messages(
                I18N.CONSTANTS.deletionInProgress(),
                I18N.MESSAGES.deletingRows(selection.size(), formClassLabel),
                I18N.CONSTANTS.deleting());
    }

    @Override
    public ConfirmDialog.Messages getFailureMessages() {
        return new ConfirmDialog.Messages(
                I18N.CONSTANTS.deletionFailed(),
                I18N.MESSAGES.retryDeletingRowRange(selection.size(), formClassLabel),
                I18N.CONSTANTS.retry());
    }

    @Override
    public ElementStyle getPrimaryButtonStyle() {
        return ElementStyle.DANGER;
    }

    @Override
    public Promise<Void> execute() {
        return tableView.getResourceLocator().remove(selection);
    }

    @Override
    public void onComplete() {
        tableView.getTable().reload();
    }
}
