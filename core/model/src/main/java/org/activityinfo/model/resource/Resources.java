package org.activityinfo.model.resource;

import com.google.gson.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Resources {

    /**
     * The resource id of the user database.
     *
     * All registered users are resources owned by this
     * resource.
     */
    public static final String USER_DATABASE_ID = "_users";


    public static final String ROOT_RESOURCE_ID = "_root";

    public static Resource createResource() {
        return new Resource();
    }

    public static Resource createResource(Record record) {
        Resource resource = new Resource();
        resource.setId(ResourceId.generateId());
        resource.getProperties().putAll(record.getProperties());
        return resource;
    }

    /**
     * @return  {@code} true if {@code x} and {@code y} have the same identity
     * and have equal properties
     */
    public static boolean deepEquals(Resource x, Resource y) {
        if(x == y) {
            return true;
        }
        if(!Objects.equals(x.getId(), y.getId()) ||
           !Objects.equals(x.getOwnerId(), y.getOwnerId())) {
            return false;
        }

        if(x.getProperties().size() != y.getProperties().size()) {
            return false;
        }
        for(String propertyName : x.getProperties().keySet()) {
            if(!Objects.equals(x.get(propertyName), y.get(propertyName))) {
                return false;
            }
        }
        return true;
    }

    public static Resource fromJson(String json) {
        JsonParser parser = new JsonParser();
        JsonObject resourceObject = parser.parse(json).getAsJsonObject();

        return fromJson(resourceObject);
    }

    public static Resource fromJson(JsonObject resourceObject) {
        Resource resource = Resources.createResource();
        for(Map.Entry<String, JsonElement> property : resourceObject.entrySet()) {
            String name = property.getKey();
            switch (name) {
                case "@id":
                    resource.setId(ResourceId.valueOf(resourceObject.getAsJsonPrimitive(name).getAsString()));
                    break;
                case "@owner":
                    resource.setOwnerId(ResourceId.valueOf(resourceObject.getAsJsonPrimitive(name).getAsString()));
                    break;
                default:
                    // normal value
                    if (!property.getValue().isJsonNull()) {
                        resource.set(property.getKey(), propertyFromJson(property.getValue()));
                    }
                    break;
            }
        }

        return resource;
    }

    private static Object propertyFromJson(JsonElement propertyValue) {
        assert !propertyValue.isJsonNull();

        if(propertyValue.isJsonPrimitive()) {
            JsonPrimitive value = propertyValue.getAsJsonPrimitive();
            if(value.isString()) {
                return value.getAsString();
            } else if(value.isNumber()) {
                return value.getAsDouble();
            } else if(value.isBoolean()) {
                return value.getAsBoolean();
            } else {
                throw new UnsupportedOperationException("value: " + value);
            }
        } else if(propertyValue.isJsonArray()) {
            return arrayFromJson(propertyValue);

        } else if(propertyValue.isJsonObject()) {
            return recordFromJson(propertyValue.getAsJsonObject());
        } else {
            throw new UnsupportedOperationException("value: " + propertyValue);
        }
    }

    private static Object arrayFromJson(JsonElement propertyValue) {
        List list = new ArrayList();
        JsonArray array = propertyValue.getAsJsonArray();
        for(int i=0;i!=array.size();++i) {
            list.add(propertyFromJson(array.get(i)));
        }
        return list;
    }

    private static Record recordFromJson(JsonObject jsonObject) {
        Record record = new Record();
        for(Map.Entry<String, JsonElement> field : jsonObject.entrySet()) {
            if(!field.getValue().isJsonNull()) {
                record.set(field.getKey(), propertyFromJson(field.getValue()));
            }
        }
        return record;
    }

    public static String toJson(IsResource resource) {
        return toJsonObject(resource.asResource()).toString();
    }

    public static String toJson(Resource resource) {
        return toJsonObject(resource).toString();
    }
    
    public static JsonObject toJsonObject(Resource resource) {
        JsonObject resourceObject = new JsonObject();
        resourceObject.addProperty("@id", resource.getId().asString());
        resourceObject.addProperty("@owner", resource.getOwnerId().asString());

        for(Map.Entry<String, Object> property : resource.getProperties().entrySet()) {
            if(property.getValue() != null) {
                resourceObject.add(property.getKey(), propertyValueToJson(property.getValue()));
            }
        }
        return resourceObject;
    }

    private static JsonElement propertyValueToJson(Object value) {
        if(value instanceof String) {
            return new JsonPrimitive((String)value);
        } else if(value instanceof Number) {
            return new JsonPrimitive((Number)value);
        } else if(value instanceof Boolean) {
            return new JsonPrimitive((Boolean)value);
        } else if(value instanceof List) {
            return toJsonArray((List)value);
        } else if(value instanceof Record) {
            return toJsonObject((Record)value);
        } else {
            throw new UnsupportedOperationException("value: " + value + " (" + value.getClass().getName() + ")") ;
        }
    }

    public static JsonElement toJsonObject(Record value) {
        JsonObject jsonObject = new JsonObject();
        for(Map.Entry<String, Object> property :  value.getProperties().entrySet()) {
            if(property.getValue() != null) {
                jsonObject.add(property.getKey(), propertyValueToJson(property.getValue()));
            }
        }
        return jsonObject;
    }

    private static JsonElement toJsonArray(List array) {
        JsonArray jsonArray = new JsonArray();
        for(Object element : array) {
            jsonArray.add(propertyValueToJson(element));
        }
        return jsonArray;
    }
}
