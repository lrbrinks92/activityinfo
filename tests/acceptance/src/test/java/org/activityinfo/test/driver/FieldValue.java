package org.activityinfo.test.driver;

import com.google.common.base.Optional;
import org.activityinfo.model.type.FieldTypeClass;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FieldValue {
    
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^[-\\d\\.,+]+");
    
    private String field;
    private String value;
    private Optional<? extends FieldTypeClass> type = Optional.absent();

    public FieldValue() {
    }

    public FieldValue(String field, String value) {
        this.field = field;
        this.value = value;
    }

    public FieldValue(String field, Double value) {
        this.field = field;
        this.value = Double.toString(value);
    }

    public FieldValue(String fieldName, int value) {
        this.field = fieldName;
        this.value = Integer.toString(value);
    }


    public String getField() {
        return field;
    }

    public FieldValue setField(String field) {
        this.field = field;
        return this;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }


    public void setValue(double value) {
        this.value = Double.toString(value);
    }

    public Object maybeNumberValue() {
        try {
            Matcher matcher = NUMBER_PATTERN.matcher(value);
            if(matcher.find()) {
                return Double.parseDouble(matcher.group());
            } 
        } catch (NumberFormatException ignored) {
        }
        return value;
    }
    
    public Double asDouble() {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format(
                    "The property '%s' with value '%s' is not an number: %s",
                    field, value, e.getMessage()), e);
        }
    }

    public Optional<? extends FieldTypeClass> getType() {
        return type;
    }

    public FieldValue setType(Optional<? extends FieldTypeClass> type) {
        this.type = type;
        return this;
    }

    public static Map<String, FieldValue> toMap(Iterable<FieldValue> values) {
        Map<String, FieldValue> map = new HashMap<>();
        for(FieldValue value : values) {
            map.put(value.getField(), value);
        }
        return map;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FieldValue that = (FieldValue) o;

        if (field != null ? !field.equals(that.field) : that.field != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = field != null ? field.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FieldValue{" +
                "field='" + field + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
