package org.activityinfo.model.resource;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.activityinfo.model.type.FieldValue;
import org.activityinfo.model.type.primitive.BooleanFieldValue;
import org.activityinfo.model.type.primitive.TextValue;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Base class for Resource and Record
 */
public class PropertyBag<T extends PropertyBag> {

    private final Map<String, Object> properties = Maps.newHashMap();

    public PropertyBag<T> copy() {
        final PropertyBag<T> copy = new PropertyBag<>();
        copy.properties.putAll(this.properties);
        return copy;
    }

    /**
     *
     * @return the value of the given property, or {@code null} if there
     * is no value for this property.
     */
    public Object get(String propertyName) {
        return properties.get(propertyName);
    }

    /**
     * @return true if there is a value for the given property
     */
    public boolean has(String propertyName) {
        return properties.containsKey(propertyName);
    }

    /**
     * @return the value of the given boolean property
     * @throws java.lang.NullPointerException
     */
    public boolean getBoolean(String propertyName) {
        return (Boolean) getNonNullPropertyValue(propertyName);
    }


    public Boolean isBoolean(String propertyName) {
        Object value = properties.get(propertyName);

        if(value == Boolean.TRUE) {
            return true;
        } else if(value == Boolean.FALSE) {
            return false;
        } else {
            return null;
        }
    }

    /**
     * @return the value of the given boolean property, or the {@code defaultValue}
     * @throws java.lang.ClassCastException if the value of the property is not a boolean
     * @throws java.lang.NullPointerException if there is no value for the property
     */
    public boolean getBoolean(String propertyName, boolean defaultValue) {
        Object value = properties.get(propertyName);

        if(value == Boolean.TRUE) {
            return true;
        } else if(value == Boolean.FALSE) {
            return false;
        } else {
            return defaultValue;
        }
    }

    /**
     *
     * @return the field's value as an immutable list of {@code Record}s.
     * If the field has no value, or the value is not of the array type,
     * an empty list is returned.
     */
    @Nonnull
    public List<Record> getRecordList(String propertyName) {
        Object value = properties.get(propertyName);
        if(value instanceof List) {
            return (List)value;
        }
        return Collections.emptyList();
    }

    @Nonnull
    public List<String> getStringList(String propertyName) {
        Object value = properties.get(propertyName);
        if(value instanceof List) {
            return (List)value;
        }
        return Collections.emptyList();
    }

    /**
     * @return the value of the given property as a String
     * @throws java.lang.ClassCastException if the value of the property is not a string
     * @throws java.lang.NullPointerException if there is no value for the property
     */
    @Nonnull
    public String getString(String propertyName) {
        return (String) getNonNullPropertyValue(propertyName);
    }

    public String isString(String propertyName) {
        Object value = properties.get(propertyName);
        if(value instanceof String) {
            return (String)value;
        }
        return null;
    }

    /**
     * @return this field's value as a {@code Record}
     *
     */
    @Nonnull
    public Record getRecord(String propertyName) {
        return (Record) getNonNullPropertyValue(propertyName);
    }

    public Record isRecord(String propertyName) {
        Object value = properties.get(propertyName);
        if(value instanceof Record) {
            return (Record) value;
        }
        return null;
    }

    /**
     * @return this field's value as a {@code Resource}
     *
     */
    @Nonnull
    public Resource getResource(String propertyName) {
        Resource value = (Resource) properties.get(propertyName);
        if(value == null) {
            throw new NullPointerException(propertyName);
        }
        return value;
    }

    @Nonnull
    public ResourceId getResourceId(String propertyName) {
        String value = (String) properties.get(propertyName);
        if(value == null) {
            throw new NullPointerException(propertyName);
        }
        return ResourceId.valueOf(value);
    }

    public ResourceId isResourceId(String propertyName) {
        Object value = properties.get(propertyName);
        if(value instanceof ResourceId) {
            return (ResourceId) value;
        }
        return null;
    }

    /**
     * @return the value of this field as a {@code double}
     * @throws java.lang.ClassCastException if the value of the property is not a Number
     * @throws java.lang.NullPointerException if there is no value for the property
     */
    public double getDouble(String propertyName) {
        Number value = (Number) getNonNullPropertyValue(propertyName);
        return value.doubleValue();
    }

    /**
     * @return the value of this field as a {@code integer}
     * @throws java.lang.ClassCastException if the value of the property is not a Number
     * @throws java.lang.NullPointerException if there is no value for the property
     */
    public int getInt(String propertyName) {
        Number value = (Number) getNonNullPropertyValue(propertyName);
        return value.intValue();
    }

    /**
     * Sets the named property to the given {@code record}, or
     * removes the property if {@code record} is {@code null}.
     */
    public T set(String propertyName, Record record) {
        if(record == null) {
            properties.remove(propertyName);
        } else {
            properties.put(propertyName, record);
        }
        return (T)this;
    }


    public PropertyBag<T> set(@Nonnull ResourceId fieldId, FieldValue fieldValue) {
        Preconditions.checkNotNull(fieldId);
        if (fieldValue == null) {
            remove(fieldId.asString());

        } else if (fieldValue instanceof TextValue) {
            set(fieldId.asString(), ((TextValue) fieldValue).toString());

        } else if (fieldValue instanceof BooleanFieldValue) {
            set(fieldId.asString(), fieldValue == BooleanFieldValue.TRUE);

        } else if(fieldValue instanceof IsRecord) {
            set(fieldId.asString(), ((IsRecord) fieldValue).asRecord());

        } else {
            throw new UnsupportedOperationException(fieldId + " = " + fieldValue);
        }
        return this;
    }



    public void remove(String fieldName) {
        properties.remove(fieldName);
    }

    /**
     * Sets the named property to the given {@code string}, or
     * removes the property if {@code string} is {@code null} or
     * empty.
     */
    public T set(String propertyName, String string) {
        if (Strings.isNullOrEmpty(string)) {
            properties.remove(propertyName);
        } else {
            properties.put(propertyName, string);
        }
        return (T)this;
    }

    /**
     * Sets the named property to the given {@code recordList}, or
     * removes the property if {@code recordList} is {@code null} or
     * empty.
     */
    public T set(String propertyName, List<Record> recordList) {
        if (recordList == null || recordList.isEmpty()) {
            properties.remove(propertyName);
        } else {
            properties.put(propertyName, recordList);
        }
        return (T)this;
    }

    /**
     * Sets the named property to the given {@code doubleValue}
     */
    public T set(String propertyName, double doubleValue) {
        properties.put(propertyName, doubleValue);
        return (T)this;
    }

    /**
     * Sets the named property to the name of the given
     * {@code enumValue}, or removes the property if
     * {@code enumValue} is {@code null}.
     *
     * Equivalent to calling:
     * <blockquote>
     * {@code set(propertyName, enumValue == null ? null : enumValue.name()); }
     * </blockquote>
     */
    public T set(String propertyName, Enum<?> enumValue) {
        if(enumValue == null) {
            properties.remove(propertyName);
        } else {
            properties.put(propertyName, enumValue.name());
        }
        return (T)this;
    }

    public void clear() {
        properties.clear();
    }

    private Object getNonNullPropertyValue(String propertyName) {
        Object value = properties.get(propertyName);
        if(value == null) {
            throw new NullPointerException(propertyName);
        }
        return value;
    }

    /**
     * Sets the named property to the given value,
     * or removes the property if the {@code value} is null.
     *
     * @throws java.lang.IllegalArgumentException if {@code value} is not an instance of
     * <ul>
     *     <li>{@code java.lang.String}</li>
     *     <li>{@code java.lang.Number}</li>
     *     <li>{@code java.lang.Boolean}</li>
     *     <li>{@code Record}</li>
     *     <li>{@code java.util.List}</li>
     * </ul>
     */
    public T set(String propertyName, Object value) {
        if(value == null) {
            properties.remove(propertyName);

        } else if(value instanceof ResourceId) {
            properties.put(propertyName, ((ResourceId) value).asString());

        } else {
            assert validPropertyValue(value) : "Invalid " + propertyName + " = " + value +
                                               " (" + value.getClass().getName() + ")";

            properties.put(propertyName, value);

        }
        return (T)this;
    }

    private boolean validPropertyValue(Object value) {
        if(value instanceof List) {
            for(Object element : (List)value) {
                if(!validPropertyValue(element)) {
                    return false;
                }
            }
            return true;

        } else {
            return value instanceof String ||
                   value instanceof Number ||
                   value instanceof Record ||
                   value instanceof Boolean;
        }
    }

    /**
     * Sets the named property to the name of the given
     * {@code booleanValue}
     */
    public T set(String propertyName, boolean booleanValue) {
        properties.put(propertyName, booleanValue);
        return (T)this;
    }

    public void setAll(PropertyBag propertyBag) {
        properties.putAll(propertyBag.properties);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PropertyBag that = (PropertyBag) o;

        return this.getProperties().equals(that.properties);
    }

    @Override
    public int hashCode() {
        return properties.hashCode();
    }

    @Override
    public String toString() {
        return properties.toString();
    }
}
