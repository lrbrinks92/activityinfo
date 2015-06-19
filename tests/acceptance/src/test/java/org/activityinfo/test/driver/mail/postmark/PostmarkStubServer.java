package org.activityinfo.test.driver.mail.postmark;

import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.simple.container.SimpleServerFactory;
import org.activityinfo.test.config.ConfigProperty;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Provides a locally-running double of Postmark's API to
 */
public class PostmarkStubServer  {

    public static final ConfigProperty POSTMARK_STUB_PORT = new ConfigProperty("postmarkStubPort",
        "The port number on which to run the postmark stub server");
    
    private static Closeable SERVER;
    
    static final ConcurrentLinkedQueue<Message> SENT_MESSAGES = new ConcurrentLinkedQueue<Message>();
    
    
    public static void start() throws IOException {
        synchronized (PostmarkStubServer.class) {
            if (SERVER == null) {
                SERVER = SimpleServerFactory.create("http://localhost:" + POSTMARK_STUB_PORT.get(),
                        new DefaultResourceConfig(PostmarkApi.class));
            }
        }
    }
    
    public static void stop() {
        synchronized (PostmarkStubServer.class) {
            if (SERVER != null) {
                try {
                    SERVER.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    SERVER = null;
                }
            }
        }
    }
}
