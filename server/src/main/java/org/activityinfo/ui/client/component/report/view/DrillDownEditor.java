package org.activityinfo.ui.client.component.report.view;

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

import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.legacy.client.Dispatcher;
import org.activityinfo.legacy.client.state.StateProvider;
import org.activityinfo.legacy.shared.command.DimensionType;
import org.activityinfo.legacy.shared.command.Filter;
import org.activityinfo.legacy.shared.reports.content.EntityCategory;
import org.activityinfo.legacy.shared.reports.content.PivotTableData;
import org.activityinfo.legacy.shared.reports.util.DateUtil;
import org.activityinfo.ui.client.AppEvents;
import org.activityinfo.ui.client.EventBus;
import org.activityinfo.ui.client.page.common.Shutdownable;
import org.activityinfo.ui.client.page.entry.SiteGridPanel;
import org.activityinfo.ui.client.page.entry.grouping.NullGroupingModel;

import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.layout.FillLayout;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;

public class DrillDownEditor implements Shutdownable {

    private final EventBus eventBus;
    private final DateUtil dateUtil;
    private Listener<PivotCellEvent> eventListener;
    private SiteGridPanel gridPanel;
    private Dialog dialog;

    public DrillDownEditor(EventBus eventBus, Dispatcher service,
                           StateProvider stateMgr, DateUtil dateUtil) {

        this.eventBus = eventBus;
        this.dateUtil = dateUtil;
        this.gridPanel = new SiteGridPanel(service);
        
        createDialog();
        
        eventListener = new Listener<PivotCellEvent>() {
            @Override
            public void handleEvent(PivotCellEvent be) {
                onDrillDown(be);
            }
        };
        eventBus.addListener(AppEvents.DRILL_DOWN, eventListener);
    }

    @Override
    public void shutdown() {
        eventBus.removeListener(AppEvents.DRILL_DOWN, eventListener);
    }

    public void onDrillDown(PivotCellEvent event) {

        // construct our filter from the intersection of rows and columns
        Filter filter = new Filter(filterFromAxis(event.getRow()),
                filterFromAxis(event.getColumn()));

        // apply the effective filter
        final Filter effectiveFilter = new Filter(filter, event.getElement()
                .getContent().getEffectiveFilter());

        effectiveFilter.getRestrictions(DimensionType.Indicator).iterator()
                .next();
        effectiveFilter.clearRestrictions(DimensionType.Indicator);

        gridPanel.load(NullGroupingModel.INSTANCE, effectiveFilter);
        dialog.show();
        
    }

    private Filter filterFromAxis(PivotTableData.Axis axis) {

        Filter filter = new Filter();
        while (axis != null) {
            if (axis.getDimension() != null) {
                if (axis.getDimension().getType() == DimensionType.Date) {
                    filter.setDateRange(dateUtil.rangeFromCategory(axis
                            .getCategory()));
                } else if (axis.getCategory() instanceof EntityCategory) {
                    filter.addRestriction(axis.getDimension().getType(),
                            ((EntityCategory) axis.getCategory()).getId());
                }
            }
            axis = axis.getParent();
        }
        return filter;
    }
    
    private void createDialog() {
    	dialog = new Dialog();
    	dialog.setHeadingText(I18N.CONSTANTS.sites());
    	dialog.setButtons(Dialog.CLOSE);
    	dialog.setLayout(new FitLayout());
    	dialog.setSize(600, 500);
    	
    	gridPanel.setHeaderVisible(false);
    	gridPanel.setLayout(new FillLayout());
    	gridPanel.setBorders(false);
        dialog.add(gridPanel);    	
    }

    public SiteGridPanel getGridPanel() {
        return gridPanel;
    }
}
