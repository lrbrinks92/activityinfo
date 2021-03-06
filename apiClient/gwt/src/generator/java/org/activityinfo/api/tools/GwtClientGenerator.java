package org.activityinfo.api.tools;

import com.google.gson.JsonParser;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.safehtml.shared.UriUtils;
import com.squareup.javapoet.*;
import io.swagger.models.*;
import io.swagger.models.parameters.Parameter;
import io.swagger.parser.SwaggerParser;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generates the sources for the GWT API client
 */
public class GwtClientGenerator {

    public static final String CLIENT_PACKAGE = "org.activityinfo.api.client";

    private final Swagger spec;
    private DataTypeFactory dataTypeFactory;
    private File outputDir;

    public static void main(String[] args) throws IOException {

        File specFile;
        File outputDir;
        if(args.length == 2) {
            specFile = new File(args[0]);
            outputDir = new File(args[1]);
        } else {
            specFile = new File("api/build/api.json");
            outputDir = new File("apiClient/gwt/build/generated");
        }

        if(!specFile.exists()) {
            System.err.println("Input file " + specFile.getAbsolutePath() + " does not exist.");
            System.exit(-1);
        }

        if(!outputDir.exists()) {
            outputDir.mkdirs();
        }

        System.out.println("Generating sources in " + outputDir);

        GwtClientGenerator generator = new GwtClientGenerator(specFile, outputDir);
        generator.generate();
    }

    public GwtClientGenerator(File specFile, File outputDir) {
        this.outputDir = outputDir;
        spec = new SwaggerParser().read(specFile.getAbsolutePath());
        dataTypeFactory = new DataTypeFactory(spec);
    }

    private void generate() throws IOException {
        generateClientInterface();
        generateClientImpl();
        generateBuilders();
        generateModels();
    }

    private void generateClientInterface() throws IOException {
        TypeSpec.Builder clientInterface = TypeSpec.interfaceBuilder("ActivityInfoClientAsync")
                .addModifiers(Modifier.PUBLIC);


        for (Map.Entry<String, Path> pathEntry : spec.getPaths().entrySet()) {
            Path path = pathEntry.getValue();
            for (Map.Entry<HttpMethod, Operation> entry : path.getOperationMap().entrySet()) {
                if(isIncluded(entry.getValue())) {
                    clientInterface.addMethod(generateOperationMethod(pathEntry.getKey(), entry.getKey(), entry.getValue(), false));
                }
            }
        }

        JavaFile javaFile = JavaFile.builder(CLIENT_PACKAGE, clientInterface.build()).build();

        javaFile.writeTo(outputDir);
    }

    private void generateClientImpl() throws IOException {
        TypeSpec.Builder clientClass = TypeSpec.classBuilder("ActivityInfoClientAsyncImpl")
                .addSuperinterface(ClassName.get(CLIENT_PACKAGE, "ActivityInfoClientAsync"))
                .addModifiers(Modifier.PUBLIC);

        clientClass.addField(FieldSpec.builder(String.class, "baseUrl")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .build());

        clientClass.addField(FieldSpec.builder(Logger.class, "LOGGER")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.getLogger(ActivityInfoClientAsync.class.getName())", Logger.class)
                .build());

        clientClass.addField(FieldSpec.builder(JsonParser.class, "JSON_PARSER")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $T()", JsonParser.class)
                .build());


        clientClass.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addStatement("baseUrl = $S", "/resources")
                .build());

        clientClass.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "baseUrl")
                .addStatement("this.baseUrl = baseUrl")
                .build());




        for (Map.Entry<String, Path> pathEntry : spec.getPaths().entrySet()) {
            Path path = pathEntry.getValue();
            for (Map.Entry<HttpMethod, Operation> entry : path.getOperationMap().entrySet()) {
                if(isIncluded(entry.getValue())) {
                    clientClass.addMethod(generateOperationMethod(pathEntry.getKey(), entry.getKey(), entry.getValue(), true));
                }
            }
        }

        JavaFile javaFile = JavaFile.builder(CLIENT_PACKAGE, clientClass.build())
                .build();

        javaFile.writeTo(outputDir);
    }

    private boolean isIncluded(Operation operation) {
        return !Boolean.FALSE.equals(operation.getVendorExtensions().get("x-gwt-client"));
    }

    private MethodSpec generateOperationMethod(String path, HttpMethod httpMethod, Operation operation,
                                               boolean implementation) {

        DataType responseType = findResponseType(operation);

        MethodSpec.Builder method = MethodSpec.methodBuilder(operation.getOperationId())
                .returns(responseType.getPromisedReturnType());

        if(implementation) {
            method.addModifiers(Modifier.PUBLIC);
        } else {
            method.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
        }

        if(operation.getSummary() != null) {
            method.addJavadoc(operation.getSummary());
            method.addJavadoc("\n\n");
        }
        if(operation.getDescription() != null) {
            method.addJavadoc(operation.getDescription());
            method.addJavadoc("\n");
        }

        Parameter bodyParameter = null;

        for (Parameter parameter : operation.getParameters()) {
            DataType type = dataTypeFactory.get(parameter);
            ParameterSpec.Builder param = ParameterSpec.builder(type.getParameterType(), parameter.getName());
            method.addParameter(param.build());

            if(parameter.getIn().equals("body")) {
                bodyParameter = parameter;
            }
            if(parameter.getDescription() != null) {
                method.addJavadoc("@param ");
                method.addJavadoc(parameter.getName());
                method.addJavadoc(" ");
                method.addJavadoc(parameter.getDescription());
                method.addJavadoc("\n");
            }
        }

        if(implementation) {
            // Classes that we use
            ClassName requestBuilder = ClassName.get(RequestBuilder.class);

            method.addStatement("$T urlBuilder = new $T(baseUrl)", StringBuilder.class, StringBuilder.class);
            addPathParts(method, path);
            addQueryParameters(method, operation);
            method.addStatement("final String url = urlBuilder.toString()");

            method.addStatement("final $T result = new Promise<>()", responseType.getPromisedReturnType());

            method.addStatement("$T requestBuilder = new $T($T.$L, url)",
                    requestBuilder, requestBuilder, requestBuilder, httpMethod.name().toUpperCase());


            if (bodyParameter != null) {
                DataType bodyType = dataTypeFactory.get(bodyParameter);
                method.addStatement("requestBuilder.setHeader($S, $S)", "Content-Type", "application/json");
                method.addStatement("requestBuilder.setRequestData($L)", bodyType.toJsonString(bodyParameter.getName()));
            }

            method.addStatement("requestBuilder.setCallback($L)", generateCallback(operation));
            method.beginControlFlow("try");
            method.addStatement("requestBuilder.send()");
            method.nextControlFlow("catch($T e)", RequestException.class);
            method.addStatement("result.reject(e)");
            method.endControlFlow();

            method.addStatement("return result");
        }

        return method.build();
    }


    private TypeSpec generateCallback(Operation operation) {

        TypeSpec.Builder callback = TypeSpec.anonymousClassBuilder("")
                .superclass(RequestCallback.class);

        MethodSpec.Builder onReceived = MethodSpec.methodBuilder("onResponseReceived")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Request.class, "request")
                .addParameter(com.google.gwt.http.client.Response.class, "response")
                .returns(void.class);

        for (Map.Entry<String, Response> entry : operation.getResponses().entrySet()) {
            Response expectedResponse = entry.getValue();
            int statusCode = Integer.parseInt(entry.getKey());

            if(statusCode >= 200 && statusCode < 300) {
                onReceived.beginControlFlow("if(response.getStatusCode() == $L)", statusCode);
                if (expectedResponse.getSchema() == null) {
                    onReceived.addStatement("result.resolve(null)");
                } else {
                    DataType returnType = dataTypeFactory.get(expectedResponse.getSchema());
                    onReceived.addStatement("result.resolve($L)", returnType.fromJsonString(CodeBlock.of("response.getText()")));
                }
                onReceived.addStatement("return");
                onReceived.endControlFlow();
            }
        }
        // If the response did not match a success case, then treat it as an error

        onReceived.addStatement("LOGGER.log($T.SEVERE, \"Request to \" + url + \" failed with status \" + " +
                        "response.getStatusCode() + \": \" + response.getStatusText())",
                ClassName.get(Level.class));

        onReceived.addStatement("result.reject(new ApiException(response.getStatusCode()))");

        MethodSpec.Builder onFailed = MethodSpec.methodBuilder("onError")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Request.class, "request")
                .addParameter(Throwable.class, "error")
                .addStatement("LOGGER.log($T.SEVERE, \"Request to \" + url + \" failed: \" + error.getMessage(), error)",
                        ClassName.get(Level.class))
                .addStatement("result.reject(error)")
                .returns(void.class);


        callback.addMethod(onReceived.build());
        callback.addMethod(onFailed.build());

        return callback.build();
    }

    private void addPathParts(MethodSpec.Builder method, String path) {
        String[] parts = path.split("/");
        for (String part : parts) {
            if(!part.isEmpty()) {
                if(part.startsWith("{")) {
                    String paramName = part.substring("{".length(), part.length() - "}".length());
                    method.addStatement("urlBuilder.append($S).append($L)", "/", paramName);
                } else {
                    method.addStatement("urlBuilder.append($S)", "/" + part);
                }
            }
        }
    }

    private List<Parameter> queryParameters(Operation operation) {
        List<Parameter> list = new ArrayList<>();
        for (Parameter parameter : operation.getParameters()) {
            if (parameter.getIn().equals("query")) {
                list.add(parameter);
            }
        }
        return list;
    }

    private void addQueryParameters(MethodSpec.Builder method, Operation operation) {
        List<Parameter> queryParameters = queryParameters(operation);

        int i = 0;
        for (Parameter queryParameter : queryParameters) {
            if (queryParameter.getRequired()) {
                method.addStatement("assert $L != null", queryParameter.getName());
            } else {
                method.beginControlFlow("if($L != null)", queryParameter.getName());
            }
            String parametersSeparator = i == 0 ? "?" : "&";
            method.addStatement("urlBuilder.append($S).append($T.encode($L))",
                    parametersSeparator + queryParameter.getName() + "=", UriUtils.class, queryParameter.getName());
            if (!queryParameter.getRequired()) {
                method.endControlFlow();
            }
            i++;
        }
    }

    private DataType findResponseType(Operation operation) {
        for (Response response : operation.getResponses().values()) {
            if(response.getSchema() != null) {
                return dataTypeFactory.get(response.getSchema());
            }
        }
        return new VoidDataType();
    }


    private void generateBuilders() throws IOException {
        for (Map.Entry<String, Model> entry : spec.getDefinitions().entrySet()) {
            String modelName = entry.getKey();
            if( !ProvidedModel.isProvided(modelName)) {

                BuilderGenerator builderGenerator = new BuilderGenerator(dataTypeFactory, modelName, entry.getValue());
                builderGenerator.writeTo(outputDir);
            }
        }
    }

    private void generateModels() throws IOException {
        for (Map.Entry<String, Model> entry : spec.getDefinitions().entrySet()) {
            String modelName = entry.getKey();
            if( !ProvidedModel.isProvided(modelName)) {

                ModelGenerator builderGenerator = new ModelGenerator(dataTypeFactory, modelName, entry.getValue());
                builderGenerator.writeTo(outputDir);
            }
        }
    }

}
