package org.activityinfo.geoadmin.writer;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.OutputStreamOutStream;
import com.vividsolutions.jts.io.WKBWriter;
import org.geotools.feature.FeatureCollection;

import java.io.*;
import java.util.zip.GZIPOutputStream;

public class WkbOutput implements OutputWriter {

    private DataOutputStream dataOut;
    private WKBWriter writer = new WKBWriter();
    private File outputFile;
    private ByteArrayOutputStream baos;
    private int numFeatures = 0;

    public WkbOutput(File outputDir, int adminLevelId) throws IOException {
        outputFile = new File(outputDir, adminLevelId + ".wkb.gz");
        baos = new ByteArrayOutputStream();
        dataOut = new DataOutputStream(baos);
    }

    @Override
    public void start(FeatureCollection features) throws IOException {

    }

    @Override
    public void write(int adminEntityId, Geometry geometry) throws IOException {
        if (!geometry.isValid()) {
            throw new IllegalStateException(adminEntityId + " has invalid geometry");
        }
        dataOut.writeInt(adminEntityId);
        writer.write(geometry, new OutputStreamOutStream(dataOut));
        numFeatures++;
    }

    @Override
    public void close() throws IOException {
        DataOutputStream out = new DataOutputStream(
            new GZIPOutputStream(
                new FileOutputStream(outputFile)));
        out.writeInt(numFeatures);
        out.write(baos.toByteArray());
        out.close();
    }

}
