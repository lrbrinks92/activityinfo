package org.activityinfo.ui.client.widget.cell;
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

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.HasCell;

/**
 * @author yuriyz on 4/2/14.
 */
public class HasCellAdapter<T,C> implements HasCell<T,C> {

    private Cell<C> cell;
    private C value;
    private FieldUpdater<T, C> fieldUpdater;

    public HasCellAdapter(Cell<C> cell) {
        this(cell, null, null);
    }

    public HasCellAdapter(Cell<C> cell, C value) {
        this(cell, value, null);
    }

    public HasCellAdapter(Cell<C> cell, C value, FieldUpdater<T, C> fieldUpdater) {
        this.cell = cell;
        this.value = value;
        this.fieldUpdater = fieldUpdater;
    }

    @Override
    public Cell<C> getCell() {
        return cell;
    }

    /**
     * Returns the {@link FieldUpdater} instance.
     *
     * @return an instance of FieldUpdater<T, C>
     */
    @Override
    public FieldUpdater<T, C> getFieldUpdater(){
        return fieldUpdater;
    }

    @Override
    public C getValue(T object) {
        return value;
    }
}
