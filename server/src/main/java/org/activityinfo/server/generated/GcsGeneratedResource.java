package org.activityinfo.server.generated;

import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsInputChannel;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
import com.google.common.io.ByteStreams;
import org.activityinfo.server.DeploymentEnvironment;
import org.activityinfo.server.database.hibernate.entity.Domain;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.Channels;
import java.util.logging.Logger;

import static java.lang.String.format;

/**
 * GeneratedResource implementation backed by metadata stored in the AppEngine HRD
 * and data in Google Cloud storage
 */
class GcsGeneratedResource implements GeneratedResource {

    private static final Logger LOGGER = Logger.getLogger(GcsGeneratedResource.class.getName());
    
    private Domain domain;
    private String bucket;
    private GcsGeneratedMetadata metadata;

    GcsGeneratedResource(Domain domain, String bucket, GcsGeneratedMetadata metadata) {
        this.domain = domain;
        this.bucket = bucket;
        this.metadata = metadata;
    }

    @Override
    public String getId() {
        return metadata.getId();
    }

    @Override
    public boolean isComplete() {
        return metadata.isCompleted();
    }

    @Override
    public String getDownloadUri() {
        if(DeploymentEnvironment.isAppEngineDevelopment()) {
            // In the development environment, we need to serve the resource
            // ourselves because the resource is not actually in a real Google Cloud storage bucket
            return UriBuilder.fromUri(domain.getRootUrl())
                    .path("generated")
                    .path(metadata.getId())
                    .path(metadata.getFilename())
                    .build()
                    .toString();
        } else {
            
            // But in production we can send the client directly to the resource using a signed url
            return getSignedDownloadUri().toString();            
        }
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return new GcsOutputStream(bucket, metadata);
    }

    @Override
    public void updateProgress(double percentageComplete) {
        metadata.setPercentageComplete(percentageComplete);
        metadata.save();
    }

    @Override
    public double getProgress() {
        return metadata.getPercentageComplete();
    }

    @Override
    public Response serve() throws IOException {
        if(DeploymentEnvironment.isAppEngineDevelopment()) {
            return serveContent();
        } else {
            return serveRedirect();
        }
    }
    
    public Response serveRedirect() throws IOException {
        return Response.temporaryRedirect(getSignedDownloadUri()).build();
    }

    private URI getSignedDownloadUri() {
        GcsAppIdentityServiceUrlSigner signer = new GcsAppIdentityServiceUrlSigner();
        return signer.signUri("GET", bucket + "/" + metadata.getGcsPath());
    }

    public Response serveContent() throws IOException {
        
        LOGGER.info(format("Serving generated resource %s: %s [%s]", 
                metadata.getId(), 
                metadata.getFilename(),
                metadata.getContentType()));
        
        return Response.ok(new Download(), metadata.getContentType())
                .header("Content-disposition", "attachment")
                .build();
    }

    private class Download implements StreamingOutput {

        @Override
        public void write(OutputStream outputStream) throws IOException, WebApplicationException {
            GcsService gcs = GcsServiceFactory.createGcsService();
            GcsFilename fileName = new GcsFilename(bucket, metadata.getGcsPath());
            long startPosition = 0;
            GcsInputChannel readChannel = gcs.openReadChannel(fileName, startPosition);
            try(InputStream inputStream = Channels.newInputStream(readChannel)) {
                ByteStreams.copy(inputStream, outputStream);
            }
        }
    }
}
