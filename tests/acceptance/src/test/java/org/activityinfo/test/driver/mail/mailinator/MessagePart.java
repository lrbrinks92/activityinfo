package org.activityinfo.test.driver.mail.mailinator;


import com.google.common.collect.Maps;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
class MessagePart {
    
    private final Map<String, String> headers = Maps.newHashMap();
    private String body;

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
    
    public String getContentType() {
        String type = getHeader("Content-Type");
        String parts[] = type.split(";");
        return parts[0];
    }
    
    public String getHeader(String headerName) {
        for (Map.Entry<String, String> header : headers.entrySet()) {
            if(header.getKey().equalsIgnoreCase(headerName)) {
                return header.getValue();
            }
        }
        throw new IllegalStateException("No such header: " + headerName);
    }
}
