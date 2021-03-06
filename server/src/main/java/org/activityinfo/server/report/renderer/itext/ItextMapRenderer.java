package org.activityinfo.server.report.renderer.itext;

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

import com.google.code.appengine.awt.Color;
import com.google.code.appengine.awt.Graphics2D;
import com.google.inject.Inject;
import com.lowagie.text.*;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.legacy.shared.model.IndicatorDTO;
import org.activityinfo.legacy.shared.reports.content.*;
import org.activityinfo.legacy.shared.reports.model.MapReportElement;
import org.activityinfo.legacy.shared.reports.model.layers.MapLayer;
import org.activityinfo.legacy.shared.reports.model.layers.PiechartMapLayer;
import org.activityinfo.legacy.shared.reports.model.layers.PiechartMapLayer.Slice;
import org.activityinfo.server.geo.AdminGeometryProvider;
import org.activityinfo.server.report.generator.MapIconPath;
import org.activityinfo.server.report.renderer.image.*;
import org.activityinfo.server.report.util.ColorUtil;

import java.io.File;
import java.io.IOException;

/**
 * Renders a {@link org.activityinfo.legacy.shared.reports.model.MapReportElement
 * MapElement} into an iText document
 */
public class ItextMapRenderer extends ImageMapRenderer implements ItextRenderer<MapReportElement> {

    private ImageCreator imageCreator;
    private ItextGraphic graphic;

    @Inject
    public ItextMapRenderer(AdminGeometryProvider geometryProvider,
                            @MapIconPath String mapIconPath,
                            ImageCreator imageCreator) {
        super(geometryProvider, mapIconPath);
        this.imageCreator = imageCreator;
    }

    @Override
    public void render(DocWriter writer, Document doc, MapReportElement element) {

        try {
            doc.add(ThemeHelper.elementTitle(element.getTitle()));
            ItextRendererHelper.addFilterDescription(doc, element.getContent().getFilterDescriptions());
            ItextRendererHelper.addDateFilterDescription(doc, element.getFilter().getEndDateRange());
            renderMap(writer, element, doc);
            if (!element.getContent().getLegends().isEmpty()) {
                renderLegend(element, doc);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void renderMap(DocWriter writer,
                          MapReportElement element,
                          Document doc) throws BadElementException, DocumentException {

        graphic = imageCreator.createMap(element.getWidth(), element.getHeight());
        drawBasemap(element, new ItextTileHandler(graphic));
        drawOverlays(element, graphic.getGraphics());

       // Chunk chunk = new Chunk(graphic.toItextImage(), 0f, 0f);
        
        Paragraph paragraph = new Paragraph();
        paragraph.add(graphic.toItextImage());
        
        doc.add(paragraph);
    }

    @Override
    protected void drawIcon(Graphics2D g2d, IconMapMarker marker) {
        int x = marker.getX() - marker.getIcon().getAnchorX();
        int y = marker.getY() - marker.getIcon().getAnchorY();
        File imageFile = getImageFile(marker.getIcon().getName());

        try {
            Image image = Image.getInstance(imageFile.getAbsolutePath());
            image.setAbsolutePosition(x, y);

            graphic.addImage(imageFile.toURI().toURL().toString(),
                    x,
                    y,
                    marker.getIcon().getWidth(),
                    marker.getIcon().getHeight());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void renderLegend(MapReportElement element, Document doc) throws DocumentException, IOException {

        Table table = new Table(2);
        table.setBorderWidth(1);
        table.setWidth(100f);
        table.setBorderColor(new Color(100, 100, 100));
        table.setPadding(5);
        table.setSpacing(0);
        table.setCellsFitPage(true);
        table.setTableFitsPage(true);
        table.setWidths(new int[]{1, 3});

        Cell cell = new Cell(I18N.CONSTANTS.legend());
        cell.setHeader(true);
        cell.setColspan(2);
        table.addCell(cell);
        table.endHeaders();

        for (MapLayerLegend legend : element.getContent().getLegends()) {

            Cell symbolCell = new Cell();
            symbolCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            symbolCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            Image symbol = createLegendSymbol(legend, imageCreator);
            symbolCell.addElement(symbol);

            Cell descriptionCell = new Cell();
            addLegendDescription(element, legend.getDefinition(), descriptionCell);

            table.addCell(symbolCell);
            table.addCell(descriptionCell);
        }
        doc.add(table);
    }

    private void addLegendDescription(MapReportElement element,
                                      MapLayer layer,
                                      Cell descriptionCell) throws BadElementException, IOException {

        if (layer instanceof PiechartMapLayer) {
            addPieChartDescription(element, descriptionCell, (PiechartMapLayer) layer);
        } else if (layer.getIndicatorIds().size() == 1) {
            addSingleIndicatorDescription(element, layer, descriptionCell);
        } else {
            addIndicatorList(element, layer, descriptionCell);
        }

    }

    private void addPieChartDescription(MapReportElement element,
                                        Cell descriptionCell,
                                        PiechartMapLayer layer) throws BadElementException {

        for (Slice slice : layer.getSlices()) {
            IndicatorDTO indicator = element.getContent().getIndicatorById(slice.getIndicatorId());
            Color color = ColorUtil.colorFromString(slice.getColor());
            ItextGraphic sliceImage = renderSlice(imageCreator, color, 10);

            Chunk box = new Chunk(sliceImage.toItextImage(), 0, 0);
            Chunk description = new Chunk(indicator.getName());

            Phrase phrase = new Phrase();
            phrase.add(box);
            phrase.add(description);

            Paragraph paragraph = new Paragraph(phrase);

            descriptionCell.add(paragraph);
        }
    }

    private void addSingleIndicatorDescription(MapReportElement element, MapLayer layer, Cell descriptionCell) {
        int indicatorId = layer.getIndicatorIds().get(0);
        IndicatorDTO indicator = element.getContent().getIndicatorById(indicatorId);
        if (indicator == null) {
            throw new NullPointerException("indicatorId:" + indicatorId);
        }
        descriptionCell.add(ThemeHelper.filterDescription(indicator.getName()));
    }

    private void addIndicatorList(MapReportElement element, MapLayer layer, Cell descriptionCell) {
        com.lowagie.text.List list = new List(List.UNORDERED);

        for (int indicatorId : layer.getIndicatorIds()) {
            IndicatorDTO indicator = element.getContent().getIndicatorById(indicatorId);
            list.add(new ListItem(indicator.getName()));
        }

        descriptionCell.add(list);
    }

    public Image createLegendSymbol(MapLayerLegend<?> legend, ImageCreator creator) throws BadElementException {
        if (legend instanceof BubbleLayerLegend) {
            return new BubbleLegendRenderer((BubbleLayerLegend) legend).createImage(creator).toItextImage();
        } else if (legend instanceof IconLayerLegend) {
            return createIconImage((IconLayerLegend) legend);
        } else if (legend instanceof PieChartLegend) {
            return new PieChartLegendRenderer((PieChartLegend) legend).createImage(creator).toItextImage();
        } else if (legend instanceof PolygonLegend) {
            return new PolygonLegendRenderer((PolygonLegend) legend).createImage(creator).toItextImage();
        } else {
            throw new IllegalArgumentException();
        }
    }

    private Image createIconImage(IconLayerLegend legend) {
        try {
            return Image.getInstance(getImageFile(legend.getDefinition().getIcon()).getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("Can't create image for " + legend.getDefinition().getIcon());
        }
    }
}
