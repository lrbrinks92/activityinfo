package org.activityinfo.test.capacity;

import org.activityinfo.test.sut.Server;


public class TestContext {
    
    public static final int MAX_THREADS = 100;
    public static final int CORE_THREAD_POOL = 3;
    private static final long KEEP_ALIVE_TIME = 60;
    
    private final Server server;
    
    public TestContext() {
        this.server = new Server();
    }

    public Server getServer() {
        return server;
    }

}
