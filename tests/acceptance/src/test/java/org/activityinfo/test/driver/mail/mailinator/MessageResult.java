package org.activityinfo.test.driver.mail.mailinator;


import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
class MessageResult {
    
    private Message data;

    public Message getData() {
        return data;
    }

    public void setData(Message data) {
        this.data = data;
    }
}
