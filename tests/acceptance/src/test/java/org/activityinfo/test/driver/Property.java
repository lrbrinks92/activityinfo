package org.activityinfo.test.driver;

public class Property {
    private String key;
    private Object value;

    public Property(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }
    
    public static Property name(String name) {
        return new Property("name", name);
    }

    public static Property property(String key, Object value) {
        return new Property(key, value);
    }
    
}
