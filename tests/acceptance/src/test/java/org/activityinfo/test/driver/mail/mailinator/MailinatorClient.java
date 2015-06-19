package org.activityinfo.test.driver.mail.mailinator;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.RateLimiter;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import org.activityinfo.test.config.ConfigProperty;
import org.activityinfo.test.driver.mail.EmailDriver;
import org.activityinfo.test.driver.mail.NotificationEmail;
import org.activityinfo.test.sut.UserAccount;
import org.codehaus.jackson.map.ObjectMapper;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;

/**
 * Fetches emails via the mailinator API.
 * 
 * This is used to during the full acceptance test to verify the full flow of email.
 */
public class MailinatorClient implements EmailDriver {
    /**
     * Accessing the free mailinator API too rapidly is not permitted and will result in the account
     * being banned. This {@code RATE_LIMITER} ensures we don't make too many requests.
     */
    private static final RateLimiter RATE_LIMITER = RateLimiter.create(0.5);

    private static final String SENDER_EMAIL = "notifications@activityinfo.org";


    private static final ConfigProperty API_KEY = new ConfigProperty("mailinatorApiKey", 
            "API Key required to access mailinator API");

    public static final int MAILINATOR_API_RETRY_LIMIT = 10;

    private final String key;
    private ObjectMapper objectMapper;
    private WebResource root;


    public MailinatorClient() {
        this(API_KEY.get());
    }

    public MailinatorClient(String key) {
        this.key = key;
        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        Client client = Client.create(clientConfig);
        objectMapper = new ObjectMapper();
        root = client.resource("https://api.mailinator.com/api");
    }
    
    @Override
    public UserAccount newAccount() {
        String email = Long.toHexString(new SecureRandom().nextLong()) + "@mailinator.com";
        return new UserAccount(email, "notasecret");
    }

    public List<MessageHeader> queryInbox(UserAccount account) throws IOException {
        return queryInbox(account, 1);
    }

    public List<MessageHeader> queryInbox(UserAccount account, int retries) throws IOException {

        RATE_LIMITER.acquire();

        try {
            String json = root
                    .path("inbox")
                    .queryParam("token", key)
                    .queryParam("to", account.nameFromEmail())
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .get(String.class);

            return objectMapper.readValue(json, Inbox.class).getMessages();
        } catch (ClientHandlerException | UniformInterfaceException e) {

            // in case client handler fail to retrieve the results retry it

            try { // sleep, accessing the free mailinator API too rapidly is not permitted
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                throw new AssertionError(e1);
            }

            if (retries > MAILINATOR_API_RETRY_LIMIT) {
                throw new AssertionError("Failed to connect to mailinator API, after " + MAILINATOR_API_RETRY_LIMIT + " retries.");
            }

            System.out.println(e.getMessage());

            retries++;
            return queryInbox(account, retries);
        }
    }
    
    public Message queryMessage(MessageHeader messageHeader) throws IOException {
        
        RATE_LIMITER.acquire();
        
        String json = root
                .path("email")
                .queryParam("token", key)
                .queryParam("msgid", messageHeader.getId())
                .type(MediaType.APPLICATION_JSON_TYPE)
                .get(String.class);
        
        System.out.println(json);

        Message data = objectMapper.readValue(json, MessageResult.class).getData();
        Preconditions.checkNotNull(data);
        return data;
    }


    @Override
    public NotificationEmail lastNotificationFor(UserAccount account) throws IOException {

        List<MessageHeader> messages = queryInbox(account);
        for (MessageHeader header : messages) {
            if (header.getFrom().equals(SENDER_EMAIL)) {
                Message message = queryMessage(header);
                return new NotificationEmail(header.getSubject(), message.getPlainText());
            }
        }
        throw new AssertionError("No emails from " + SENDER_EMAIL + " found.");
    }
}
