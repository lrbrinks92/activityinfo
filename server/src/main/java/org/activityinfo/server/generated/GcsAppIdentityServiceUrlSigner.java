package org.activityinfo.server.generated;

import com.google.appengine.api.appidentity.AppIdentityService;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;

import javax.ws.rs.core.UriBuilder;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Calendar;

public class GcsAppIdentityServiceUrlSigner  {

    private static final int EXPIRATION_TIME = 5;
    private static final String BASE_URL = "https://storage.googleapis.com";

    private final AppIdentityService identityService = AppIdentityServiceFactory.getAppIdentityService();

    public URI signUri(final String httpVerb, final String path) {
        final long expiration = expiration();
        final String unsigned = stringToSign(expiration, path, httpVerb);
        final String signature = sign(unsigned);

        return UriBuilder.fromUri(BASE_URL)
                .path(path)
                .queryParam("GoogleAccessId", clientId())
                .queryParam("Expires", expiration)
                .queryParam("Signature", signature)
                .build();
    }

    private static long expiration() {
        final long unitMil = 1000l;
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, EXPIRATION_TIME);
        return calendar.getTimeInMillis() / unitMil;
    }

    private String stringToSign(final long expiration, String path, String httpVerb) {
        final String contentType = "";
        final String contentMD5 = "";
        final String canonicalizedExtensionHeaders = "";
        final String canonicalizedResource = "/" + path;
        return httpVerb + "\n" + contentMD5 + "\n" + contentType + "\n"
                + expiration + "\n" + canonicalizedExtensionHeaders + canonicalizedResource;
    }

    protected String sign(final String stringToSign) {
        final AppIdentityService.SigningResult signingResult = identityService
                .signForApp(stringToSign.getBytes());

        String signature = BaseEncoding.base64().encode(signingResult.getSignature());
        try {
            return URLEncoder.encode(signature, Charsets.UTF_8.name());
        } catch (UnsupportedEncodingException e1) {
            throw new IllegalStateException("JVM Does not support " + Charsets.UTF_8);
        }
    }

    protected String clientId() {
        return identityService.getServiceAccountName();
    }
}