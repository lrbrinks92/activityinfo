package org.activityinfo.server.report.generator.map;

/*
 * #%L
 * ActivityInfo Server
 * %%
 * Copyright (C) 2009 - 2013 UNICEF
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.activityinfo.model.type.geo.AiLatLng;
import org.activityinfo.legacy.shared.model.SiteDTO;
import org.activityinfo.legacy.shared.reports.content.Point;
import org.activityinfo.legacy.shared.reports.model.MapSymbol;
import org.activityinfo.legacy.shared.reports.model.PointValue;
import org.activityinfo.server.report.generator.map.cluster.Cluster;
import org.activityinfo.server.report.generator.map.cluster.genetic.BubbleFitnessFunctor;
import org.activityinfo.server.report.generator.map.cluster.genetic.GeneticSolver;
import org.activityinfo.server.report.generator.map.cluster.genetic.MarkerGraph;
import org.activityinfo.server.report.generator.map.cluster.genetic.UpperBoundsCalculator;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/*
 * Real example that didn't work.
 *
 * @author Alex Bertram
 */
public class CoincidentPointsClusterTest extends GraphTest {

    @Test
    public void testSimpleData() throws Exception {

        List<PointValue> points = new ArrayList<PointValue>();
        points.add(new PointValue(new SiteDTO(), new MapSymbol(), 7.0,
                new Point(0, 0)));
        points.add(new PointValue(new SiteDTO(), new MapSymbol(), 2.0,
                new Point(0, 0)));
        points.add(new PointValue(new SiteDTO(), new MapSymbol(), 41.0,
                new Point(100, 100)));
        points.add(new PointValue(new SiteDTO(), new MapSymbol(), 9.0,
                new Point(0, 0)));
        points.add(new PointValue(new SiteDTO(), new MapSymbol(), 39.0,
                new Point(100, 100)));

        double originalSum = 7 + 2 + 9 + 41 + 39;

        // Now build the graph

        MarkerGraph graph = new MarkerGraph(points,
                new BubbleIntersectionCalculator(15));

        GeneticSolver solver = new GeneticSolver();

        List<Cluster> clusters = solver
                .solve(graph, new GsLogCalculator(5, 15),
                        new BubbleFitnessFunctor(), UpperBoundsCalculator.calculate(
                        graph, new FixedRadiiCalculator(5)));

        // check to make sure all values were included
        double sumAfterClustering = 0;
        for (Cluster cluster : clusters) {
            sumAfterClustering += cluster.sumValues();
        }

        Assert.assertEquals(originalSum, sumAfterClustering, 0.0001);

        Assert.assertEquals(2, clusters.size());

        saveClusters(graph, "clusterTest-solution", clusters);

    }

    @Test
    public void testRealData() throws Exception {

        // Define projection for the test case
        TiledMap map = new TiledMap(492, 690, new AiLatLng(2.293492496,
                30.538372993), 9);

        // Read data
        BufferedReader in = new BufferedReader(new InputStreamReader(
                GraphTest.class.getResourceAsStream("/distribscolaire-points.csv")));

        double originalSum = 0;

        List<PointValue> points = new ArrayList<PointValue>();
        while (in.ready()) {

            String line = in.readLine();
            String[] columns = line.split(",");

            double lat = Double.parseDouble(columns[0]);
            double lng = Double.parseDouble(columns[1]);

            PointValue pv = new PointValue();
            pv.setPx(map.fromLatLngToPixel(new AiLatLng(lat, lng)));
            pv.setValue(Double.parseDouble(columns[2]));
            pv.setSymbol(new MapSymbol());
            pv.setSite(new SiteDTO());

            originalSum += pv.getValue();

            points.add(pv);
        }

        // Now build the graph

        MarkerGraph graph = new MarkerGraph(points,
                new BubbleIntersectionCalculator(15));

        // make sure nothing was lost in the merging of coincident points
        double nodeSum = 0;
        for (MarkerGraph.Node node : graph.getNodes()) {
            nodeSum += node.getPointValue().getValue();
        }
        Assert.assertEquals("values after construction of graph", originalSum,
                nodeSum, 0.001);

        saveGraphImage("clusterTest2", graph, 15);

        GeneticSolver solver = new GeneticSolver();

        List<Cluster> clusters = solver
                .solve(graph, new GsLogCalculator(5, 15),
                        new BubbleFitnessFunctor(), UpperBoundsCalculator.calculate(
                        graph, new FixedRadiiCalculator(5)));

        // check to make sure all values were included
        double sumAfterClustering = 0;
        for (Cluster cluster : clusters) {
            sumAfterClustering += cluster.sumValues();
        }

        Assert.assertEquals(originalSum, sumAfterClustering, 0.001);

        Assert.assertEquals(15, clusters.size());

        saveClusters(graph, "clusterTest-solution", clusters);
    }

}
