package org.activityinfo.model.type.time;
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

import com.google.gson.JsonElement;
import org.activityinfo.model.type.*;

/**
 * Value type that represents a calendar month in the ISO-8601 calendar.
 * There is no representation of time-of-day or time-zone.
 */
public class MonthType implements FieldType, TemporalType {

    public interface TypeClass extends SingletonTypeClass, RecordFieldTypeClass {
    }

    public static final TypeClass TYPE_CLASS = new TypeClass() {

        @Override
        public String getId() {
            return "month";
        }

        @Override
        public FieldType createType() {
            return INSTANCE;
        }
    };

    public static final MonthType INSTANCE = new MonthType();

    private MonthType() {}

    @Override
    public FieldTypeClass getTypeClass() {
        return TYPE_CLASS;
    }

    @Override
    public FieldValue parseJsonValue(JsonElement value) {
        throw new UnsupportedOperationException();
    }
}