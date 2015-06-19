package org.activityinfo.ui.client.component.importDialog.validation.cells;
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

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import org.activityinfo.core.shared.importing.strategy.ColumnAccessor;
import org.activityinfo.core.shared.importing.validation.ValidatedRow;
import org.activityinfo.core.shared.importing.validation.ValidationResult;
import org.activityinfo.ui.client.component.importDialog.validation.ValidationPageStyles;

/**
 * @author yuriyz on 5/5/14.
 */
public class ValidationResultCell extends AbstractCell<ValidatedRow> {

    public static interface Templates extends SafeHtmlTemplates {

        public static final Templates INSTANCE = GWT.create(Templates.class);

        @Template("<div class='{0}'>{1}</div>")
        public SafeHtml html(String style, String text);
    }


    private final ColumnAccessor accessor;
    private final int columnIndex;

    public ValidationResultCell(ColumnAccessor accessor, int columnIndex) {
        super();
        this.accessor = accessor;
        this.columnIndex = columnIndex;
    }

    @Override
    public void render(Context context, ValidatedRow data, SafeHtmlBuilder sb) {
        final SafeHtml safeHtml = Templates.INSTANCE.html(style(data.getResult(columnIndex).getState()), accessor.getValue(data.getSourceRow()));
        sb.append(safeHtml);
    }

    private static String style(ValidationResult.State state) {
        if (state != null) {
            switch (state) {
                case OK:
                    return ValidationPageStyles.INSTANCE.stateOk();
                case CONFIDENCE:
                    return ValidationPageStyles.INSTANCE.stateConfidence();
                case ERROR:
                case MISSING:
                    return ValidationPageStyles.INSTANCE.stateError();
            }
        }
        return "";
    }
}
