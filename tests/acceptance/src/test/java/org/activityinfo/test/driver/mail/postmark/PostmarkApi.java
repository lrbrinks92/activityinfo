package org.activityinfo.test.driver.mail.postmark;

import org.codehaus.jackson.map.ObjectMapper;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.io.IOException;

@Path("/email")
public class PostmarkApi {
    
    private ObjectMapper objectMapper = new ObjectMapper();
    
    @POST
    public void post(String jsonBody) throws IOException {
        PostmarkStubServer.SENT_MESSAGES.add(objectMapper.readValue(jsonBody, Message.class));
    }

}
