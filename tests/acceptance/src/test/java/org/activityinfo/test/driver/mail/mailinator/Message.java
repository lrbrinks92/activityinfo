package org.activityinfo.test.driver.mail.mailinator;


import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
class Message {
    private String id;
    private final Map<String, String> headers = new HashMap<>();
    private String subject;

    @JsonProperty("fromfull")
    private String from;
    
    private final List<MessagePart> parts = new ArrayList<>();


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public List<MessagePart> getParts() {
        return parts;
    }

    public String getPlainText() {
        for(MessagePart part : parts) {
            if(part.getContentType().equals("text/plain")) {
                return part.getBody();
            }
        }
        throw new IllegalStateException("No text/plain part");
    }
    
}
