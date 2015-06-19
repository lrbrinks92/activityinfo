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
import org.activityinfo.core.shared.importing.model.ImportModel;
import org.activityinfo.core.shared.importing.source.SourceRow;
import org.activityinfo.core.shared.importing.strategy.FieldImporter;
import org.activityinfo.core.shared.importing.validation.ValidatedRow;
import org.activityinfo.core.shared.importing.validation.ValidatedRowTable;
import org.activityinfo.core.shared.importing.validation.ValidationResult;
import org.activityinfo.promise.Promise;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author yuriyz on 4/18/14.
 */
public class ValidateRowsImportCommand implements ImportCommand<ValidatedRowTable> {

    private ImportCommandExecutor commandExecutor;

    @Nullable
    @Override
    public Promise<ValidatedRowTable> apply(@Nullable Void input) {
        return Promise.resolved(doRowValidation());
    }

    private ValidatedRowTable doRowValidation() {
        final List<ValidatedRow> rows = Lists.newArrayList();
        final ImportModel model = commandExecutor.getImportModel();

        // Row based validation
        for (SourceRow row : model.getSource().getRows()) {
            List<ValidationResult> results = Lists.newArrayList();
            for (FieldImporter importer : commandExecutor.getImporters()) {
                importer.validateInstance(row, results);
            }
            rows.add(new ValidatedRow(row, results));
        }
        ValidatedRowTable validatedRowTable = new ValidatedRowTable(commandExecutor.getColumns(), rows);
        model.setValidatedRowTable(validatedRowTable);
        return validatedRowTable;
    }

    @Override
    public void setCommandExecutor(ImportCommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }
}
