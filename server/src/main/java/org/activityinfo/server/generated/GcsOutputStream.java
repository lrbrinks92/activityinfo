package org.activityinfo.server.generated;

import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.logging.Logger;

/**
 * Wraps an output stream to a GCS object, marking the resource as complete when 
 * the stream is closed.
 */
class GcsOutputStream extends OutputStream {
    
    private static final Logger LOGGER = Logger.getLogger(GcsOutputStream.class.getName());

    private final GcsGeneratedMetadata metadata;
    private final OutputStream out;

    public GcsOutputStream(String bucket, GcsGeneratedMetadata metadata) throws IOException {
        GcsService gcs = GcsServiceFactory.createGcsService();
        GcsFileOptions fileOptions = new GcsFileOptions.Builder()
                .mimeType(metadata.getContentType())
                .contentDisposition("attachment; filename=" + metadata.getFilename())
                .build();
        GcsFilename fileName = new GcsFilename(bucket, metadata.getGcsPath());

        this.metadata = metadata;
        this.out = Channels.newOutputStream(gcs.createOrReplace(fileName, fileOptions));
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void write(byte[] b) throws IOException {
        out.write(b);
    }

    @Override
    public void close() throws IOException {
        LOGGER.info("Closing and finalizing generated resource " + metadata.getId());
        out.close();
        metadata.markComplete();
    }
}
