package org.activityinfo.server.report;

import org.activityinfo.fixtures.InjectionSupport;
import org.activityinfo.fixtures.Modules;
import org.activityinfo.legacy.shared.command.GenerateElement;
import org.activityinfo.legacy.shared.reports.content.MapContent;
import org.activityinfo.legacy.shared.reports.content.MapMarker;
import org.activityinfo.legacy.shared.reports.model.MapReportElement;
import org.activityinfo.legacy.shared.reports.model.clustering.AdministrativeLevelClustering;
import org.activityinfo.legacy.shared.reports.model.clustering.AutomaticClustering;
import org.activityinfo.legacy.shared.reports.model.layers.BubbleMapLayer;
import org.activityinfo.legacy.shared.reports.model.layers.IconMapLayer;
import org.activityinfo.legacy.shared.reports.model.layers.MapLayer;
import org.activityinfo.server.command.CommandTestCase2;
import org.activityinfo.server.database.OnDataSet;
import org.activityinfo.server.report.renderer.image.ImageMapRenderer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


@RunWith(InjectionSupport.class)
@Modules(ReportModule.class)
@OnDataSet("/dbunit/mg-mapping.db.xml")
public class MgMapIntegrationTest extends CommandTestCase2 {

    public static final int NUMBER_OF_BENE_INDICATOR_ID = 12445;
    public static final int MG_OWNER_ID = 1070;
    public static final int FOKOTANY_LEVEL_ID = 1512;

    private File outFolder;
    private MapContent content;
    private MapReportElement map;

    @Before
    public final void setup() {
        outFolder = new File("target/report-tests");
        outFolder.mkdirs();

        setUser(MG_OWNER_ID);
    }

    @Test
    public void siteBoundsToAdminLevelsAreMappedAsBubbles() throws IOException {

        BubbleMapLayer layer = new BubbleMapLayer();
        layer.addIndicator(NUMBER_OF_BENE_INDICATOR_ID);

        generateMap(layer, "mg-bubbles.png");

        assertThat(content.getMarkers().size(), equalTo(10));

        MapMarker marker = getMarkerForSite(1336279918);
        assertThat(marker.getX(), equalTo(262));
        assertThat(marker.getY(), equalTo(113));
    }

    @Test
    public void siteBoundsToAdminLevelsAreMappedAsIcons() throws IOException {

        IconMapLayer layer = new IconMapLayer();
        layer.setIcon("educ");
        layer.getIndicatorIds().add(NUMBER_OF_BENE_INDICATOR_ID);

        generateMap(layer, "mg-icons.png");

        assertThat(content.getMarkers().size(), equalTo(10));

        MapMarker marker = getMarkerForSite(1336279918);
        assertThat(marker.getX(), equalTo(262));
        assertThat(marker.getY(), equalTo(113));
    }


    @Test
    public void siteBoundsToAdminLevelsMappedAsIconsAutoClustered() throws IOException {

        IconMapLayer layer = new IconMapLayer();
        layer.setIcon("educ");
        layer.getIndicatorIds().add(NUMBER_OF_BENE_INDICATOR_ID);
        layer.setClustering(new AutomaticClustering());

        generateMap(layer, "mg-icons-auto.png");

        assertTrue(content.getMarkers().size() > 0);
        assertTrue(content.getUnmappedSites().isEmpty());

    }

    @Test
    public void sitesBoundToAdminLevelsAreAutoClusteredProperly() throws IOException {

        BubbleMapLayer layer = new BubbleMapLayer();
        layer.addIndicator(NUMBER_OF_BENE_INDICATOR_ID);
        layer.setClustering(new AutomaticClustering());

        generateMap(layer, "mg-auto-cluster.png");

        assertThat(content.getMarkers().size(), equalTo(4));
        assertThat(content.getUnmappedSites().size(), equalTo(0));
    }

    @Test
    public void sitesBoundToAdminLevelsAreClusteredByAdminLevelMissingBounds() throws IOException {

        BubbleMapLayer layer = new BubbleMapLayer();
        layer.addIndicator(NUMBER_OF_BENE_INDICATOR_ID);
        layer.setClustering(clusterByFokotany());

        generateMap(layer, "mg-cluster-fok.png");

        assertThat(content.getMarkers().size(), equalTo(10));
        assertThat(content.getUnmappedSites().size(), equalTo(0));
    }

    private AdministrativeLevelClustering clusterByFokotany() {
        AdministrativeLevelClustering clustering = new AdministrativeLevelClustering();
        clustering.getAdminLevels().add(FOKOTANY_LEVEL_ID);
        return clustering;
    }

    private void generateMap(MapLayer layer, String fileName) throws IOException {
        map = new MapReportElement();
        map.addLayer(layer);

        content = (MapContent) execute(new GenerateElement(map));

        renderToFile(map, fileName);
    }

    private MapMarker getMarkerForSite(int siteId) {
        for(MapMarker marker : this.content.getMarkers()) {
            if(marker.getSiteIds().contains(siteId)) {
                return marker;
            }
        }
        throw new AssertionError("No marker for " + siteId);
    }

    private void renderToFile(MapReportElement map, String fileName) throws IOException {
        ImageMapRenderer renderer = new ImageMapRenderer(null, "src/main/webapp/mapicons");
        renderer.renderToFile(map, new File(outFolder, fileName));
    }
}
