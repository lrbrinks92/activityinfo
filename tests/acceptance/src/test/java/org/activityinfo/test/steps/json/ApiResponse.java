package org.activityinfo.test.steps.json;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.sun.jersey.api.client.ClientResponse;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.is;

/**
 * Wrapper around a ClientResponse to simplify accessing the entity multiple times.
 */
class ApiResponse {

    private final ClientResponse response;
    private ObjectMapper objectMapper;
    private String responseEntity;
    
    public ApiResponse(ClientResponse response, ObjectMapper objectMapper) {
        this.response = response;
        this.objectMapper = objectMapper;
    }
    
    public boolean isSuccess() {
        return response.getClientResponseStatus().getFamily() == Response.Status.Family.SUCCESSFUL;
    }
    
    public JsonNode getJson() throws IOException {
        if(!isSuccess()) {
            throw new AssertionError(String.format("Request failed with status code %d:\n%s", response.getStatus(),
                    getResponseEntity()));
        }
        return objectMapper.readTree(getResponseEntity());
    }

    private String getResponseEntity() {
        if(responseEntity == null) {
            responseEntity = response.getEntity(String.class);
        }
        return responseEntity;
    }

    public void assertStatusCodeIs(int statusCode) {
        if(response.getStatus() != statusCode) {
            throw new AssertionError(String.format(
                    "Expected response with status code %d, actual status code was %d.\n%s",
                    statusCode,
                    response.getStatus(),
                    response.getEntity(String.class)));
        }
    }

    public void assertBodyIsEmpty() throws IOException {
        if(response.hasEntity()) {
            byte[] bytes;
            try (InputStream in = response.getEntityInputStream()) {
                bytes = ByteStreams.toByteArray(in);
            }
            if(bytes.length > 0) {
                String content = new String(bytes, Charsets.UTF_8);

                throw new AssertionError(String.format("Expected empty response, found: [%s]", content));
            }
        }
    }

    public void assertErrorMessageContains(String errorMessage) {
        String message = getResponseEntity();
        if(!message.toLowerCase().contains(errorMessage.toLowerCase())) {
            throw new AssertionError(String.format("Expected an error message containing the phrase '%s' but received:\n%s",
                    errorMessage, message));
        }
    }
}

