package org.activityinfo.api.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.squareup.javapoet.*;
import io.swagger.models.Model;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.Property;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Generates builder classes for models
 */
public class BuilderGenerator {

    private static final TypeName[] PRIMITIVE_TYPES = new TypeName[] { 
            ClassName.get(String.class), 
            ClassName.get(Number.class),
            TypeName.BOOLEAN
    };


    private DataTypeFactory dataTypeFactory;
    private final String modelName;
    private final String className;
    private final Model model;

    /**
     * Default constructor
     */
    private final MethodSpec.Builder constructor;
    private final TypeSpec.Builder builderClass;

    public BuilderGenerator(DataTypeFactory dataTypeFactory, String modelName, Model model) {
        this.dataTypeFactory = dataTypeFactory;
        this.modelName = modelName;
        this.model = model;

        className = modelName + "Builder";
        builderClass = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC);

        constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);

        builderClass.addField(FieldSpec.builder(ClassName.get(JsonObject.class), "jsonObject", Modifier.PRIVATE)
                .initializer("new $T()", JsonObject.class)
                .build());

        builderClass.addMethod(MethodSpec.methodBuilder("toJsonString")
                .addModifiers(Modifier.PUBLIC)
                .addStatement("return jsonObject.toString()")
                .returns(String.class)
                .build());

        builderClass.addMethod(MethodSpec.methodBuilder("toJsonObject")
                .addModifiers(Modifier.PUBLIC)
                .addStatement("return jsonObject")
                .returns(JsonObject.class)
                .build());
        
        generateSetters();
    }

    private void generateSetters() {

        for (Map.Entry<String, Property> entry : model.getProperties().entrySet()) {
            try {
                if(entry.getValue() instanceof ObjectProperty) {
                    addObjectProperty(entry.getKey());

                } else if(entry.getValue() instanceof ArrayProperty) {
                    addArrayProperty(entry.getKey(), entry.getValue());
                    
                } else {
                    addSimpleProperty(entry.getKey(), entry.getValue());
                }
            } catch (Exception e) {
                throw new RuntimeException(String.format("Exception generating setter for property %s.%s",
                        modelName, entry.getKey()), e);
            }
        }
    }

    public void writeTo(File outputDir) throws IOException {
        
        builderClass.addMethod(constructor.build());
        
        JavaFile javaFile = JavaFile.builder(ModelDataType.MODEL_PACKAGE, builderClass.build()).build();
        javaFile.writeTo(outputDir);
    }


    private void addArrayProperty(String propertyName, Property property) {

        ArrayType dataType = (ArrayType) dataTypeFactory.get(property);

        // Add a field of type JsonArray and initialize it to an empty object
        builderClass.addField(FieldSpec.builder(ClassName.get(JsonArray.class), propertyName, Modifier.PRIVATE)
                .initializer("new $T()", JsonArray.class)
                .build());
        
        // Assign the empty object to the owner
        // For example:
        // jsonObject.add("update", updateObject)
        constructor.addStatement("jsonObject.add($S, $L)", propertyName, propertyName);

        // Compose the javadoc
        StringBuilder javadoc = new StringBuilder();
        javadoc.append("Adds a ").append(singular(propertyName)).append(".\n\n");
        if(property.getDescription() != null) {
            javadoc.append("@param value ").append(property.getDescription()).append("\n");
        }
        
        // Create an add method
        // Generate a setter for JsonElement
        builderClass.addMethod(MethodSpec.methodBuilder(accessor("add", singular(propertyName)))
                .addJavadoc(javadoc.toString())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(dataType.getBaseType().getParameterType(), "value")
                .addStatement("$L.add($L)", propertyName, dataType.getBaseType().toJsonElement("value"))
                .addStatement("return this")
                .returns(builderClass())
                .build());
    }

    private ClassName builderClass() {
        return ClassName.get(ModelDataType.MODEL_PACKAGE, className);
    }


    /**
     * Adds an "object" property, which functions like an property bag.
     * 
     * <p>For a property like "fieldValues", we want to add a set of methods like setFieldValue(name, value).</p>
     */
    private void addObjectProperty(String propertyName) {
       
        // Add a field of type JsonObject and initialize it to an empty object
        builderClass.addField(FieldSpec.builder(ClassName.get(JsonObject.class), propertyName, Modifier.PRIVATE)
                .initializer("new $T()", JsonObject.class)
                .build());

        // Assign the empty object to the owner
        // For example:
        // jsonObject.add("update", updateObject)
        constructor.addStatement("jsonObject.add($S, $L)", propertyName, propertyName);

        String nameParameter = "name";
        String valueParameter = "value";


        // Generate a setter for all the primitive types
        for (TypeName type : PRIMITIVE_TYPES) {

            builderClass.addMethod(MethodSpec.methodBuilder(accessor("set", singular(propertyName)))
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ClassName.get(String.class), nameParameter)
                    .addParameter(type, valueParameter)
                    .addStatement("$L.addProperty($L, $L)", propertyName, nameParameter, valueParameter)
                    .addStatement("return this")
                    .returns(builderClass())
                    .build());
        }
        
        // Generate a setter for JsonElement
        builderClass.addMethod(MethodSpec.methodBuilder(accessor("set", singular(propertyName)))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(String.class), nameParameter)
                .addParameter(ClassName.get(JsonElement.class), valueParameter)
                .addStatement("$L.add($L, $L)", propertyName, nameParameter, valueParameter)
                .addStatement("return this")
                .returns(builderClass())
                .build());
    }


    private void addSimpleProperty(String propertyName, Property property) {
        DataType propertyType = dataTypeFactory.get(property);

        StringBuilder javadoc = new StringBuilder();
        javadoc.append("Sets the ").append(propertyName).append(".\n\n");
       
        if(property.getDescription() != null) {
            javadoc.append("@param ").append(propertyName).append(" ")
                    .append(property.getDescription()).append("\n");
        }
        
        builderClass.addMethod(MethodSpec.methodBuilder(accessor("set", propertyName))
                .addJavadoc(javadoc.toString())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(propertyType.getParameterType(), propertyName)
                .addStatement("this.jsonObject.add($S, $L)", propertyName, propertyType.toJsonElement(propertyName))
                .addStatement("return this")
                .returns(builderClass())
                .build());
    }



    private String singular(String propertyName) {
        if(propertyName.endsWith("ies")) {
            return propertyName.substring(0, propertyName.length() - "ies".length()) + "y";
        } else if(propertyName.endsWith("s")) {
            return propertyName.substring(0, propertyName.length()-1);
        } else if(propertyName.equals("children")) {
            return "child";
        }
        throw new IllegalArgumentException("Not plural: " + propertyName);
    }

    private String accessor(String prefix, String propertyName) {
        return prefix + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
    }
}
