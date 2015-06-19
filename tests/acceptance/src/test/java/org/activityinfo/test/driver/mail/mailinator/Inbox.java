package org.activityinfo.test.driver.mail.mailinator;

import java.util.ArrayList;
import java.util.List;

class Inbox {
    private final List<MessageHeader> headers = new ArrayList<>();

    public List<MessageHeader> getMessages() {
        return headers;
    }
}
