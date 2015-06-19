package org.activityinfo.ui.client.component.importDialog;
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

import com.google.common.collect.Lists;
import org.activityinfo.model.legacy.CuidAdapter;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.core.shared.importing.model.ImportModel;
import org.activityinfo.core.shared.importing.source.SourceRow;
import org.activityinfo.core.shared.importing.strategy.FieldImporter;
import org.activityinfo.core.shared.importing.validation.ValidatedRow;
import org.activityinfo.core.shared.importing.validation.ValidatedRowTable;
import org.activityinfo.promise.Promise;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author yuriyz on 4/18/14.
 */
public class PersistImportCommand implements ImportCommand<Void> {

//    private static final Logger LOGGER = Logger.getLogger(PersistImportCommand.class.getName());

    private ImportCommandExecutor commandExecutor;

    @Nullable
    @Override
    public Promise<Void> apply(Void input) {
        final ImportModel model = commandExecutor.getImportModel();

        final ResourceId formClassId = model.getFormTree().getRootFields().iterator().next().getDefiningFormClass().getId();
        final List<FormInstance> toPersist = Lists.newArrayList();
        final ValidatedRowTable validatedRowTable = model.getValidatedRowTable();

        for (SourceRow row : model.getSource().getRows()) {
            ValidatedRow validatedRow = validatedRowTable.getRow(row);
            if (validatedRow.isValid()) { // persist instance only if it's valid
                // new instance per row
                FormInstance newInstance = new FormInstance(CuidAdapter.newLegacyFormInstanceId(formClassId), formClassId);
                for (FieldImporter importer : commandExecutor.getImporters()) {
                    importer.updateInstance(row, newInstance);
                }
                toPersist.add(newInstance);
            }
        }

        return commandExecutor.getResourceLocator().persist(toPersist);
    }

    @Override
    public void setCommandExecutor(ImportCommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }
}
