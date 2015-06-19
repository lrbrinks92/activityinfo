package org.activityinfo.test.driver;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.joda.time.LocalDate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestObject {
    private AliasTable aliasTable;
    private String alias;
    private Map<String, Object> map = new HashMap<>();
    
    public TestObject(AliasTable aliasTable, Property... properties) {
        this.aliasTable = aliasTable;
        for(Property p : properties) {
            map.put(p.getKey(), p.getValue());
        }
    }

    /**
     * @return the id stored in the alias table for this object's {@code name}
     */
    public Integer lookupId() {
        return aliasTable.getId(getName());
    }

    /**
     * Looks up the id of the property in the alias table
     * @return the id mapped to the value of property {@code propertyName}
     */
    public int getId(String propertyName) {
        return aliasTable.getId(getString(propertyName));
    }


    public int getId(String propertyName, int defaultId) {
        if(has(propertyName)) {
            return getId(propertyName);
        }
        return defaultId;
    }
    
    public String getString(String propertyName) {
        return get(propertyName, String.class);
    }

    /**
     * 
     * Looks up the alias for the value of the given
     * property.
     * 
     * <p>Convenience function for {@code aliasTable.getAlias(getString(propertyName))}
     * 
     * @param propertyName the name of the property to retrieve
     * @return a unique alias for this property's value
     */
    public String getAlias(String propertyName) {
        return aliasTable.getAlias(getString(propertyName));
    }

    /**
     * Gets this alias for this object's "name" property, creating
     * one if necessary.
     * 
     * @return the alias for this object's "name" property
     */
    public String getAlias() {
        if(alias == null) {
            alias = aliasTable.createAlias(getName());
        }
        return getAlias("name");
    }
    
    public String getName() {
        return getString("name");
    }
    
    public String getString(String propertyName, String defaultValue) {
        return get(propertyName, String.class, defaultValue);
    }

    public Integer getInteger(String propertyName, Integer defaultValue) {
        return get(propertyName, Integer.class, defaultValue);
    }

    public Boolean getBoolean(String propertyName, Boolean defaultValue) {
        return get(propertyName, Boolean.class, defaultValue);
    }

    public LocalDate getDate(String propertyName) {
        return get(propertyName, LocalDate.class);
    }

    public LocalDate getDate(String propertyName, LocalDate defaultValue) {
        return get(propertyName, LocalDate.class, defaultValue);
    }

    public boolean has(String propertyName) {
        return map.containsKey(propertyName);
    }

    public List<String> getAliasList(List<String> propertyNames) {
        List<String> aliases = Lists.newArrayList();
        for (String s : propertyNames) {
            aliases.add(aliasTable.getAlias((s)));
        }
        return aliases;
    }

    public List<String> getStringList(String propertyName) {
        Object value = map.get(propertyName);
        if(value instanceof String) {
            return Collections.singletonList((String)value);
            
        } else if(value instanceof Iterable) {
            Iterable<?> items = get(propertyName, Iterable.class);
            List<String> list = Lists.newArrayList();

            for (Object item : items) {
                if(item instanceof String) {
                    list.add((String)item);
                } else {
                    throw new AssertionError(String.format("Expected list of Strings, but property '%s' contained '%s' " +
                            "of type '%s'", propertyName, item.toString(), item.getClass().getName()));
                }
            }
            return list;   
        } else {
            throw new AssertionError(String.format("Property '%s' must be provided", propertyName));
        }
    }

    private <T> T get(String propertyName, Class<T> valueClass) {
        return get(propertyName, valueClass, null);
    }

    private <T> T get(String propertyName, Class<T> valueClass, T defaultValue) {
        Object value = map.get(propertyName);

        if(value == null) {
            if(defaultValue != null) {
                return defaultValue;
            } else {
                throw new AssertionError(String.format("Property '%s' must be provided", propertyName));
            }
        } else {

            Preconditions.checkState(valueClass.isAssignableFrom(value.getClass()),
                    "Property '%s' must an instance of %s", propertyName,
                    valueClass.getName());

            return valueClass.cast(value);
        }
    }

}
