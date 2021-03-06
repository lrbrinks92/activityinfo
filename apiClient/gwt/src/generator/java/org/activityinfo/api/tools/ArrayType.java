package org.activityinfo.api.tools;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.List;

public class ArrayType extends DataType {
    
    private DataType baseType;

    public ArrayType(DataType baseType) {
        this.baseType = baseType;
    }

    public DataType getBaseType() {
        return baseType;
    }

    @Override
    public TypeName getReturnTypeName() {
        ClassName list = ClassName.get(List.class);
        return ParameterizedTypeName.get(list, baseType.getReturnTypeName());
    }

    @Override
    public TypeName getParameterType() {
        return getReturnTypeName();
    }

    @Override
    public String toJsonString(String valueExpr) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CodeBlock toJsonElement(String propertyExpr) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CodeBlock fromJsonString(CodeBlock jsonStringExpr) {
        return fromJsonElement(parse(jsonStringExpr));
    }

    @Override
    public CodeBlock fromJsonElement(CodeBlock jsonElementExpr) {
        return baseType.fromJsonArray(CodeBlock.of("$L.getAsJsonArray()", jsonElementExpr));
    }
}
