package org.activityinfo.server.command.handler.crud;

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

import com.bedatadriven.rebar.time.calendar.LocalDate;
import com.extjs.gxt.ui.client.data.RpcMap;
import org.activityinfo.legacy.shared.exception.CommandException;

import java.util.Date;
import java.util.Map;
import java.util.Set;

public class PropertyMap {
    private Map<String, Object> map;

    public PropertyMap(Map<String, Object> map) {
        this.map = map;
    }

    public PropertyMap(RpcMap properties) {
        this.map = properties.getTransientMap();
    }

    public <X> X get(String propertyName) {
        return (X) map.get(propertyName);
    }
    
    public int getRequiredInt(String propertyName) {
        Object value = getRequiredProperty(propertyName);
        if(!(value instanceof Integer)) {
            throw new CommandException(String.format("Property '%s' must be an integer", propertyName));
        }
        return (Integer)value;
    }

    public boolean containsKey(String propertyName) {
        return map.containsKey(propertyName);
    }

    public Set<Map.Entry<String, Object>> entrySet() {
        return map.entrySet();
    }

    public String getString(String propertyName) {
        Object value = getRequiredProperty(propertyName);
        if(!(value instanceof String)) {
            throw new CommandException(String.format("Property '%s' must be a string", propertyName));
        }
        return (String)value;
    }

    private Object getRequiredProperty(String propertyName) {
        Object value = map.get(propertyName);

        if(value == null) {
            throw new CommandException(String.format("Property '%s' is required", propertyName));
        }
        return value;
    }

    public String getOptionalString(String propertyName, String defaultValue) {
        if(map.containsKey(propertyName)) {
            return (String) map.get(propertyName);
        } else {
            return defaultValue;
        }
    }
    
    public String getOptionalString(String property) {
        return getOptionalString(property, null);
    }
    
    public Double getOptionalDouble(String propertyName) {
        Object value = map.get(propertyName);
        if(value == null) {
            return null;
        }
        if(value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        throw new CommandException(String.format("Expected a numeric value for property '%s'", propertyName));
    }

    public Set<String> getNames() {
        return map.keySet();
    }

    public Date getDate(String property) {
        Object value = getRequiredProperty(property);
        if(value instanceof Date) {
            return (Date)value;
        } else if(value instanceof LocalDate) {
            return ((LocalDate) value).atMidnightInMyTimezone();
        } else if(value instanceof String) {
            return LocalDate.parse((String)value).atMidnightInMyTimezone();
        }
        throw new CommandException(String.format("Expected a date value for property '%s' in the format YYYY-MM-dd",
                property));
        
    }

    public Map<String, Object> asMap() {
        return map;
    }


}
