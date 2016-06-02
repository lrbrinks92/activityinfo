package org.activityinfo.ui.client.component.form;
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

import com.google.common.collect.Maps;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.ui.client.component.form.subform.SubFormRepeatingManipulator;

import java.util.Map;

/**
 * @author yuriyz on 01/25/2016.
 */
public class SubFormsHandler {

    private final Map<FormClass, SubFormRepeatingManipulator> subForms = Maps.newHashMap();

    public SubFormsHandler() {
    }

    public Map<FormClass, SubFormRepeatingManipulator> getSubForms() {
        return subForms;
    }

    public boolean validate() {
        boolean valid = true;
        for (SubFormRepeatingManipulator manipulator : subForms.values()) {
            for (SimpleFormPanel subFormPanel : manipulator.getForms().values()) {
                if (!subFormPanel.validate()) {
                    valid = false;
                }
            }
        }
        return valid;
    }
}