package org.activityinfo.test.driver.mail.mailinator;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * An item in the mailinator inbox
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class MessageHeader {
    @JsonProperty("seconds_ago")
    private long secondsAgo;
    
    private String id;
    private String to;
    private long time;
    private String subject;
    
    @JsonProperty("fromfull")
    private String from;
    
    @JsonProperty("been_read")
    private boolean read;

    public long getSecondsAgo() {
        return secondsAgo;
    }

    public void setSecondsAgo(long secondsAgo) {
        this.secondsAgo = secondsAgo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
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

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }


    @Override
    public String toString() {
        return new StringBuilder().append("{")
                .append(id).append(" ")
                .append(from).append(" ")
                .append(subject).append("}")
                .toString();
    }
}
