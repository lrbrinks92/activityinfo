package org.activityinfo.geoadmin.writer;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONWriter;
import org.geotools.feature.FeatureCollection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.zip.GZIPOutputStream;

public class GoogleMapsWriter implements OutputWriter {

    private StringWriter stringWriter = new StringWriter();
    private JSONWriter writer;
    private GooglePolylineEncoder encoder;
    private File outputDir;
    private int adminLevelId;

    public GoogleMapsWriter(File outputDir, int adminLevelId) throws IOException {
        this.outputDir = outputDir;
        this.adminLevelId = adminLevelId;

        encoder = new GooglePolylineEncoder();
        writer = new JSONWriter(stringWriter);
        // writer.setIndent("  ");
    }

    @Override
    public void start(FeatureCollection features) throws IOException {
        try {
            writer.object();
            writer.key("zoomFactor");
            writer.value(encoder.getZoomFactor());
            writer.key("numLevels");
            writer.value(encoder.getNumLevels());
            writer.key("entities");
            writer.array();
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void write(int adminEntityId, Geometry geometry) throws IOException {
        try {
            writer.object();
            writer.key("id");
            writer.value(adminEntityId);
            writer.key("polygons");
            writer.array();

            for (int i = 0; i != geometry.getNumGeometries(); ++i) {
                Polygon polygon = (Polygon) geometry.getGeometryN(i);
                writeLinearRing(polygon.getExteriorRing());
                for (int j = 0; j != polygon.getNumInteriorRing(); ++j) {
                    writeLinearRing(polygon.getInteriorRingN(j));
                }
            }

            writer.endArray();
            writer.endObject();
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    private void writeLinearRing(LineString ring) throws IOException, JSONException {
        PolylineEncoded encoded = encoder.dpEncode(ring.getCoordinates());
        writer.object();
        writer.key("points");
        writer.value(encoded.getPoints());
        writer.key("levels");
        writer.value(encoded.getLevels());
        writer.endObject();
    }

    @Override
    public void close() throws IOException {
        try {
            writer.endArray();
            writer.endObject();
        } catch (JSONException e) {
            throw new IOException(e);
        }

        // write uncompressed JSON file
        File outFile = new File(outputDir, adminLevelId + ".json");
        Files.write(stringWriter.toString(), outFile, Charsets.UTF_8);

        // write compressed JSON
        File compressedOutFile = new File(outputDir, adminLevelId + ".json.gz");
        FileOutputStream fos = new FileOutputStream(compressedOutFile);
        GZIPOutputStream zos = new GZIPOutputStream(fos);
        zos.write(stringWriter.toString().getBytes(Charsets.UTF_8));
        zos.finish();
        zos.close();
    }

}
