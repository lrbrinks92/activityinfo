package org.activityinfo.test.pageobject.web.entry;
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
import org.activityinfo.test.driver.FieldValue;
import org.junit.Assert;

import java.util.List;
import java.util.Map;

/**
 * @author yuriyz on 04/17/2015.
 */
public class DetailsEntry {

    private final List<FieldValue> fieldValues = Lists.newArrayList();

    public DetailsEntry() {
    }

    public List<FieldValue> getFieldValues() {
        return fieldValues;
    }

    public void assertVisible(List<FieldValue> values) {
        Map<String, FieldValue> map = FieldValue.toMap(fieldValues);

        for (FieldValue value : values) {
            FieldValue fieldValue = map.get(value.getField());

            Assert.assertNotNull("Indicator is not visible, name: " + value.getField() + appendValues(values), fieldValue);
            Assert.assertEquals("Value for indicator with name: " + value.getField() + " does not match." + appendValues(values), value.getValue(), fieldValue.getValue());
        }
    }

    private String appendValues(List<FieldValue> values) {
        return "\nActual values: " + fieldValues + "\n" + "Expected values: " + values;
    }

    @Override
    public String toString() {
        return "DetailsEntry{" +
                "fieldValues=" + fieldValues +
                '}';
    }
}
