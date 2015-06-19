package org.activityinfo.ui.client.component.report.editor.map;

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

import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.Status;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.activityinfo.model.type.geo.AiLatLng;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.legacy.client.Dispatcher;
import org.activityinfo.legacy.client.monitor.MaskingAsyncMonitor;
import org.activityinfo.legacy.shared.Log;
import org.activityinfo.legacy.shared.command.GenerateElement;
import org.activityinfo.legacy.shared.reports.content.AdminOverlay;
import org.activityinfo.legacy.shared.reports.content.MapContent;
import org.activityinfo.legacy.shared.reports.model.MapReportElement;
import org.activityinfo.ui.client.EventBus;
import org.activityinfo.ui.client.component.report.editor.map.symbols.LeafletMarkerDrilldownEventHandler;
import org.activityinfo.ui.client.component.report.editor.map.symbols.LeafletReportOverlays;
import org.activityinfo.ui.client.page.report.HasReportElement;
import org.activityinfo.ui.client.page.report.ReportChangeHandler;
import org.activityinfo.ui.client.page.report.ReportEventBus;
import org.discotools.gwt.leaflet.client.LeafletResourceInjector;
import org.discotools.gwt.leaflet.client.controls.zoom.Zoom;
import org.discotools.gwt.leaflet.client.crs.epsg.EPSG3857;
import org.discotools.gwt.leaflet.client.events.Event;
import org.discotools.gwt.leaflet.client.events.handler.EventHandler;
import org.discotools.gwt.leaflet.client.events.handler.EventHandler.Events;
import org.discotools.gwt.leaflet.client.events.handler.EventHandlerManager;
import org.discotools.gwt.leaflet.client.map.MapOptions;
import org.discotools.gwt.leaflet.client.types.LatLng;

/**
 * Displays the content of a MapElement using Google Maps.
 */
public class MapEditorMapView extends ContentPanel implements HasReportElement<MapReportElement> {

    private static final int DEFAULT_ZOOM_CONTROL_OFFSET_X = 5;
    private static final int ZOOM_CONTROL_OFFSET_Y = 5;

    private final Dispatcher dispatcher;
    private final ReportEventBus reportEventBus;

    private final Status statusWidget;
    private final LeafletMarkerDrilldownEventHandler markerDrilldownEventHandler;

    private MapReportElement model = new MapReportElement();
    private LeafletReportOverlays overlays;

    // Model of a the map
    private MapContent content;

    private boolean zoomSet = false;

    // True when the first layer is just put on the map
    private boolean isFirstLayerUpdate = true;

    private int zoomControlOffsetX = DEFAULT_ZOOM_CONTROL_OFFSET_X;

    private LeafletMap map;

    public MapEditorMapView(Dispatcher dispatcher, EventBus eventBus) {
        this.dispatcher = dispatcher;
        this.reportEventBus = new ReportEventBus(eventBus, this);
        this.reportEventBus.listen(new ReportChangeHandler() {

            @Override
            public void onChanged() {
                loadContent();
            }
        });

        this.markerDrilldownEventHandler = new LeafletMarkerDrilldownEventHandler(dispatcher);

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                int bottomX = getAbsoluteLeft() + getOffsetWidth();
                int bottomY = getAbsoluteTop() + getOffsetHeight();
                markerDrilldownEventHandler.setPosition(bottomX, bottomY);
            }
        });

        setLayout(new FitLayout());
        setHeaderVisible(false);

        statusWidget = new Status();
        ToolBar toolBar = new ToolBar();
        toolBar.add(statusWidget);
        setBottomComponent(toolBar);

        LeafletResourceInjector.ensureInjected();
    }

    public void setZoomControlOffsetX(int offset) {
        zoomControlOffsetX = offset;
        if (map != null) {
            try {
                Zoom zoomControl = map.getMap().getZoomControl();
                Element container = zoomControl.getContainer();
                container.getStyle().setMarginLeft(zoomControlOffsetX, Unit.PX);
            } catch (Exception e) {
                Log.error("Exception thrown while setting zoom control", e);
            }
        }
    }

    @Override
    public void bind(MapReportElement model) {
        this.model = model;
        loadContent();
    }

    @Override
    public void disconnect() {
        reportEventBus.disconnect();
    }

    @Override
    public MapReportElement getModel() {
        return model;
    }


    /**
     * Updates the size of the map and adds Overlays to reflect the content of
     * the current selected indicators
     */
    private void loadContent() {


        // Don't update when no layers are present
        if (model.getLayers().isEmpty()) {
            isFirstLayerUpdate = true;
            return;
        }
        // Prevent setting the extents for the MapWidget when more then 1 layer
        // is added
        if (isFirstLayerUpdate && model.getLayers().size() > 0) {
            isFirstLayerUpdate = false;
        }

        dispatcher.execute(new GenerateElement<MapContent>(model),
                new MaskingAsyncMonitor(this, I18N.CONSTANTS.loadingMap()),
                new AsyncCallback<MapContent>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        MessageBox.alert("Load failed", caught.getMessage(), null);
                    }

                    @Override
                    public void onSuccess(MapContent result) {
                        onContentLoaded(result);
                    }
                });
    }

    private void onContentLoaded(MapContent result) {
        Log.debug("MapPreview: Received content");

        content = result;

        statusWidget.setStatus(result.getUnmappedSites().size() + " " + I18N.CONSTANTS.siteLackCoordiantes(), null);

        if (!isRendered()) {
            return;
        }

        if (map == null) {
            createMap();
        }
        overlays.clear();
        overlays.setBaseMap(result.getBaseMap());
        overlays.addMarkers(result.getMarkers(), markerDrilldownEventHandler);

        for (AdminOverlay overlay : result.getAdminOverlays()) {
            overlays.addAdminLayer(overlay);
        }

        if (!zoomSet) {
            if (model.getZoomLevel() != -1 && model.getCenter() != null) {
                map.getMap()
                   .setView(new LatLng(model.getCenter().getLat(), model.getCenter().getLng()),
                           model.getZoomLevel(),
                           true);
            } else {
                map.fitBounds(result.getExtents());
            }
            zoomSet = true;
        }
    }

    private void createMap() {
        MapOptions mapOptions = new MapOptions();
        mapOptions.setCenter(new LatLng(content.getExtents().getCenterY(), content.getExtents().getCenterX()));
        mapOptions.setZoom(6);
        mapOptions.setProperty("crs", new EPSG3857());

        map = new LeafletMap(mapOptions);

        add(map);
        layout();

        EventHandlerManager.addEventHandler(map.getMap(), Events.moveend, new EventHandler() {

            @Override
            public void handle(Event event) {
                updateModelFromMap();
            }
        });
        EventHandlerManager.addEventHandler(map.getMap(), Events.zoomend, new EventHandler() {

            @Override
            public void handle(Event event) {
                updateModelFromMap();
            }
        });

        overlays = new LeafletReportOverlays(map.getMap());
    }


    private void updateModelFromMap() {
        model.setZoomLevel(map.getMap().getZoom());
        LatLng center = map.getMap().getBounds().getCenter();
        model.setCenter(new AiLatLng(center.lat(), center.lng()));
    }
}
