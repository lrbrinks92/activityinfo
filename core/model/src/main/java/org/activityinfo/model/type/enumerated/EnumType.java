package org.activityinfo.model.type.enumerated;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.resource.Record;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.resource.ResourceIdPrefixType;
import org.activityinfo.model.type.*;

import java.util.*;

public class EnumType implements ParametrizedFieldType {

    public interface EnumTypeClass extends ParametrizedFieldTypeClass, RecordFieldTypeClass { }

    public static final EnumTypeClass TYPE_CLASS = new EnumTypeClass() {

        @Override
        public String getId() {
            return "enumerated";
        }

        @Override
        public FieldType deserializeType(Record typeParameters) {

            Cardinality cardinality = Cardinality.valueOf(typeParameters.getString("cardinality"));

            List<EnumItem> enumItems = Lists.newArrayList();
            List<Record> enumValueRecords = typeParameters.getRecordList("values");
            for(Record record : enumValueRecords) {
                enumItems.add(EnumItem.fromRecord(record));
            }
            return new EnumType(cardinality, enumItems);
        }


        @Override
        public FieldType deserializeType(JsonObject parametersObject) {
            Cardinality cardinality = Cardinality.valueOf(
                    parametersObject.get("cardinality").getAsString().toUpperCase());

            List<EnumItem> enumItems = Lists.newArrayList();
            JsonArray enumItemArray = parametersObject.get("values").getAsJsonArray();
            for(JsonElement record : enumItemArray) {
                enumItems.add(EnumItem.fromJsonObject(record.getAsJsonObject()));
            }
            return new EnumType(cardinality, enumItems);
        
        }

        @Override
        public EnumType createType() {
            return new EnumType();
        }

        @Override
        public FormClass getParameterFormClass() {
            return new FormClass(ResourceIdPrefixType.TYPE.id("enum"));
        }

        @Override
        public EnumValue deserialize(Record record) {
            return EnumValue.fromRecord(record);
        }

    };

    private final Cardinality cardinality;
    private final List<EnumItem> values;
    private final List<EnumItem> defaultValues = Lists.newArrayList();

    public EnumType() {
        this.cardinality = Cardinality.SINGLE;
        this.values = Lists.newArrayList();
    }

    public EnumType(Cardinality cardinality, List<EnumItem> values) {
        this.cardinality = cardinality;
        this.values = values != null ? values : new ArrayList<EnumItem>();
    }


    public EnumType(Cardinality cardinality, EnumItem... values) {
        this.cardinality = cardinality;
        this.values = Arrays.asList(values);
    }


    public Cardinality getCardinality() {
        return cardinality;
    }

    public List<EnumItem> getValues() {
        return values;
    }

    public List<EnumItem> getDefaultValues() {
        return defaultValues;
    }

    @Override
    public ParametrizedFieldTypeClass getTypeClass() {
        return TYPE_CLASS;
    }

    @Override
    public FieldValue parseJsonValue(JsonElement value) {
        if(value instanceof JsonPrimitive) {
            ResourceId id = ResourceId.valueOf(value.getAsString());
            return new EnumValue(id);
        } else if(value instanceof JsonArray) {
            Set<ResourceId> ids = new HashSet<>();
            JsonArray array = (JsonArray) value;
            for (JsonElement jsonElement : array) {
                ResourceId id = ResourceId.valueOf(jsonElement.getAsString());
                ids.add(id);
            }
            return new EnumValue(ids);
        } else {
            return null;
        }
    }

    @Override
    public Record getParameters() {

        List<Record> enumValueRecords = Lists.newArrayList();
        for(EnumItem enumItem : getValues()) {
            enumValueRecords.add(enumItem.asRecord());
        }

        return new Record()
                .set("classId", getTypeClass().getParameterFormClass().getId())
                .set("cardinality", cardinality.name())
                .set("values", enumValueRecords);
    }

    @Override
    public JsonObject getParametersAsJson() {
        
        JsonArray enumValueArray = new JsonArray();
        for (EnumItem enumItem : getValues()) {
            enumValueArray.add(enumItem.toJsonObject());
        }
        
        JsonObject object = new JsonObject();
        object.addProperty("cardinality", cardinality.name().toLowerCase());
        object.add("values", enumValueArray);
        return object;
    }

    @Override
    public boolean isValid() {
        return values.size() > 0;
    }

}
