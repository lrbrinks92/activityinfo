package org.activityinfo.ui.client.widget.legacy;
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

import com.extjs.gxt.ui.client.binding.FieldBinding;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.FieldEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.store.Record;
import com.extjs.gxt.ui.client.widget.form.Field;
import com.extjs.gxt.ui.client.widget.form.RadioGroup;

/**
 * @author yuriyz on 10/23/2014.
 */
public class OnlyValidFieldBinding extends FieldBinding {

    /**
     * Creates a new binding instance.
     *
     * @param field    the bound field for the binding
     * @param property property name
     */
    public OnlyValidFieldBinding(Field field, String property) {
        super(field, property);
    }

    @Override
    public void updateModel() {
        if (field.isValid()) { // update model only if field value is valid

            if (field instanceof RadioGroup) { // special handing for radio group

                RadioGroup radioGroup = (RadioGroup) field;

                // hack : boolean represented with radio buttons : order is important - true is first button, false is second button
                if ("classicView".equals(property)) {
                    Field nestedField = radioGroup.getValue();
                    int selectedIndex = radioGroup.getAll().indexOf(nestedField);
                    boolean val = selectedIndex == 0;
                    if (store != null) {
                        Record r = store.getRecord(model);
                        if (r != null) {
                            r.setValid(property, field.isValid());
                            r.set(property, val);
                        }
                    } else {
                        model.set(property, val);
                    }
                    return;
                }
            }

            super.updateModel();

        }
    }

    @Override
    public void bind(ModelData model) {
        super.bind(model);
        field.addListener(Events.KeyUp, new Listener<FieldEvent>() {
            @Override
            public void handleEvent(FieldEvent be) {
                onFieldChange(be);
            }
        });
    }

    @Override
    public void updateField(boolean updateOriginalValue) {
        if (field instanceof RadioGroup) { // special handling for radio group
            RadioGroup radioGroup = (RadioGroup) field;

            Object val = onConvertModelValue(model.get(property));

            // hack : boolean represented with radio buttons : order is important - true is first button, false is second button
            if (val instanceof Boolean) {
                Field nestedField = radioGroup.get((Boolean)val ? 0 : 1);
                nestedField.setValue(Boolean.TRUE);
                return;
            }

            // we do not support other cases right now, fallback to default implementation

        }

        super.updateField(updateOriginalValue);
    }
}
