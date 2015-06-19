package org.activityinfo.test.webdriver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import org.activityinfo.test.config.ConfigProperty;
import org.littleshoot.proxy.*;
import org.littleshoot.proxy.extras.SelfSignedMitmManager;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.openqa.selenium.Proxy;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Configures and runs an embedded http proxy server that sits between the
 * webDriver-controlled browser and the system under test. By controlling the proxy, 
 * we can simulate network connection/disconnections, and test cross functional requirements
 * such as high-latency connections.
 */
public class ProxyController {
    
    public static final ConfigProperty LATENCY = new ConfigProperty("latency", "Simulate high-latency connections");
    
    private final HttpFiltersSource filterSource;

    private int proxyPort;
    private HttpProxyServer proxyServer;
    
    private boolean connected = true;
    
    public ProxyController(HttpFiltersSource filterSource) {
        this.proxyPort = ThreadLocalRandom.current().nextInt(4000, 6000);
        this.filterSource = filterSource;
    }
    
    public ProxyController() {
        this(null);
    }

    public Proxy getWebDriverProxy() {
        String proxy = "localhost:" + proxyPort;
        return new Proxy()
                .setHttpProxy(proxy)
                .setSslProxy(proxy);
    }
    
    public void start() {
        this.proxyServer = DefaultHttpProxyServer.bootstrap()
                .withPort(proxyPort)
                .withManInTheMiddle(new SelfSignedMitmManager())
                .withFiltersSource(new FiltersSource())
                .start();
    }
    
    public void stop() {
        this.proxyServer.stop();
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isConnected() {
        return connected;
    }
    

    private class FiltersSource extends HttpFiltersSourceAdapter {

        @Override
        public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
            if (!connected) {
                return new DisconnectedFilter(originalRequest, ctx);
            }
            if(filterSource != null) {
                return filterSource.filterRequest(originalRequest, ctx);
            } else {
                return new ShapingFilter(originalRequest, ctx);
            }
        }
    }
    
    private static class ShapingFilter extends HttpFiltersAdapter {

        private final String uri;
        private long latency = 0L;

        public ShapingFilter(HttpRequest originalRequest, ChannelHandlerContext ctx) {
            super(originalRequest, ctx);
            this.uri = originalRequest.getUri();
            if(LATENCY.isPresent()) {
                this.latency = ThreadLocalRandom.current().nextInt(500, 1500);
            } 
        }

        @Override
        public HttpObject responsePre(HttpObject httpObject) {
            if(latency > 0) {
                System.out.println("Sleeping " + uri);
                try {
                    Thread.sleep(latency);
                    latency = 0;
                } catch (InterruptedException ignored) {
                }
            }
            return super.responsePre(httpObject);
        }
    }
    
    private static class DisconnectedFilter extends HttpFiltersAdapter {
        
        public DisconnectedFilter(HttpRequest originalRequest, ChannelHandlerContext ctx) {
            super(originalRequest, ctx);
        }
        
        @Override
        public HttpObject responsePre(HttpObject httpObject) {
            // closes connection
            return null;
        }
    }
}
