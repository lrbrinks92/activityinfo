package org.activityinfo.test.driver.mail.postmark;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Postmark message
 */
class Message {
    @JsonProperty("From")
    private String from;
    
    @JsonProperty("To")
    private String to;
    
    @JsonProperty("Subject")
    private String subject;
    
    @JsonProperty("TextBody")
    private String textBody;
    
    @JsonProperty("HtmlBody")
    private String htmlBody;


    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTextBody() {
        return textBody;
    }

    public void setTextBody(String textBody) {
        this.textBody = textBody;
    }

    public String getHtmlBody() {
        return htmlBody;
    }

    public void setHtmlBody(String htmlBody) {
        this.htmlBody = htmlBody;
    }
}
