package org.activityinfo.model.form;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.type.*;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The smallest logical unit of data entry.
 */
public class FormField extends FormElement {

    private final ResourceId id;
    private String code;
    private String label;
    private String description;
    private String relevanceConditionExpression;
    private FieldType type;
    private boolean visible = true;
    private Set<ResourceId> superProperties = Sets.newHashSet();
    private boolean required;

    public FormField(ResourceId id) {
        checkNotNull(id);
        this.id = id;
    }

    public ResourceId getId() {
        return id;
    }

    public String getName() {
        return id.asString();
    }

    /**
     * @return user-assigned code for this field that can be
     * used in expressions.
     */
    public String getCode() {
        return code;
    }


    public FormField setCode(String code) {
        this.code = code;
        return this;
    }

    public boolean hasCode() {
        return code != null;
    }

    /**
     *
     * @return true if {@code} is a valid code, starting with a letter and
     * containing only letters, numbers, and the underscore symbol
     */
    public static boolean isValidCode(String code) {
        return code != null && code.matches("^[A-Za-z][A-Za-z0-9_]*");
    }

    @Override
    @Nonnull
    public String getLabel() {
        return label;
    }

    public FormField setLabel(String label) {
        assert label != null;
        this.label = label;
        return this;
    }

    public String getRelevanceConditionExpression() {
        return relevanceConditionExpression;
    }

    public FormField setRelevanceConditionExpression(String relevanceConditionExpression) {
        this.relevanceConditionExpression = relevanceConditionExpression;
        return this;
    }

    /**
     * @return an extended description of this field, presented to be
     * presented to the user during data entry
     */
    @Nonnull
    public String getDescription() {
        return description;
    }

    public FormField setDescription(String description) {
        this.description = description;
        return this;
    }

    public FieldType getType() {
        assert type != null : "type is missing for " + id;
        return type;
    }

    public FormField setType(FieldType type) {
        this.type = type;
        return this;
    }

    /**
     *
     * @return true if this field requires a response before submitting the form
     */
    public boolean isRequired() {
        return required;
    }

    public FormField setRequired(boolean required) {
        this.required = required;
        return this;
    }

    public boolean hasRelevanceConditionExpression() {
        return !Strings.isNullOrEmpty(relevanceConditionExpression);
    }

    /**
     * @return true if this field is visible to the user
     */
    public boolean isVisible() {
        return visible;
    }

    public FormField setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FormField formField = (FormField) o;

        if (id != null ? !id.equals(formField.id) : formField.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "FormField{" +
                "id=" + id +
                ", label=" + label +
                ", type=" + type.getTypeClass().getId() +
                '}';
    }

    public Set<ResourceId> getSuperProperties() {
        return superProperties;
    }

    public void addSuperProperty(ResourceId propertyId) {
        superProperties.add(propertyId);
    }

    public void setSuperProperties(Set<ResourceId> superProperties) {
        this.superProperties = superProperties;
    }

    public FormField setSuperProperty(ResourceId superProperty) {
        this.superProperties = Collections.singleton(superProperty);
        return this;
    }

    public boolean isSubPropertyOf(ResourceId parentProperty) {
        return this.superProperties.contains(parentProperty);
    }

    @Override
    public JsonObject toJsonObject() {
        JsonObject object = new JsonObject();
        object.addProperty("id", id.asString());
        object.addProperty("code", code);
        object.addProperty("label", label);
        object.addProperty("description", description);
        object.addProperty("relevanceCondition", relevanceConditionExpression);
        object.addProperty("visible", visible);
        object.addProperty("required", required);
        
        object.addProperty("type", type.getTypeClass().getId());

        if(!superProperties.isEmpty()) {
            JsonArray superPropertiesArray = new JsonArray();
            for (ResourceId superProperty : superProperties) {
                superPropertiesArray.add(new JsonPrimitive(superProperty.asString()));
            }
            object.add("superProperties", superPropertiesArray);
        }

        if(type instanceof ParametrizedFieldType) {
            object.add("typeParameters", ((ParametrizedFieldType) type).getParametersAsJson());
        }
        
        return object;
    }


    public static FormField fromJson(JsonObject jsonObject) {
        FormField field = new FormField(ResourceId.valueOf(jsonObject.get("id").getAsString()));
        field.setLabel(Strings.nullToEmpty(JsonParsing.toNullableString(jsonObject.get("label"))));
        field.setCode(JsonParsing.toNullableString(jsonObject.get("code")));
        field.setDescription(JsonParsing.toNullableString(jsonObject.get("description")));
        
        if(jsonObject.has("relevanceCondition")) {
            field.setRelevanceConditionExpression(JsonParsing.toNullableString(jsonObject.get("relevanceCondition")));
        } else if(jsonObject.has("relevanceConditionExpression")) {
            field.setRelevanceConditionExpression(JsonParsing.toNullableString(jsonObject.get("relevanceConditionExpression")));
        }
        
        if(jsonObject.has("visible")) {
            field.setVisible(jsonObject.get("visible").getAsBoolean());
        }
        if(jsonObject.has("required")) {
            field.setRequired(jsonObject.get("required").getAsBoolean());
        }

        if(jsonObject.has("superProperties")) {
            JsonArray superPropertiesArray = jsonObject.get("superProperties").getAsJsonArray();
            for (JsonElement jsonElement : superPropertiesArray) {
                field.addSuperProperty(ResourceId.valueOf(jsonElement.getAsString()));
            }
        }
        
        String type;
        JsonElement typeParameters ;
        JsonElement typeElement = jsonObject.get("type");
        
        if(typeElement.isJsonPrimitive()) {
            type = typeElement.getAsString();
            typeParameters = jsonObject.get("typeParameters");
        } else {
            JsonObject typeObject = typeElement.getAsJsonObject();
            type = typeObject.get("typeClass").getAsString();
            typeParameters = typeObject.get("parameters");
        }
        
        FieldTypeClass typeClass = TypeRegistry.get().getTypeClass(type);
        if(typeClass instanceof ParametrizedFieldTypeClass && typeParameters != null) {
            field.setType(((ParametrizedFieldTypeClass) typeClass).deserializeType(typeParameters.getAsJsonObject()));
        } else {
            field.setType(typeClass.createType());
        }

        return field;
    }


}
