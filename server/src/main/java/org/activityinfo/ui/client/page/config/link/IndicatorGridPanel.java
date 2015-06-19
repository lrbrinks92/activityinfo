package org.activityinfo.ui.client.page.config.link;

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

import com.extjs.gxt.ui.client.Style.SelectionMode;
import com.extjs.gxt.ui.client.core.El;
import com.extjs.gxt.ui.client.data.BaseListLoader;
import com.extjs.gxt.ui.client.data.DataProxy;
import com.extjs.gxt.ui.client.data.DataReader;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.util.Point;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.grid.*;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.google.common.collect.Lists;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.legacy.client.Dispatcher;
import org.activityinfo.legacy.shared.command.DimensionType;
import org.activityinfo.legacy.shared.command.Filter;
import org.activityinfo.legacy.shared.command.GetActivityForms;
import org.activityinfo.legacy.shared.command.result.ActivityFormResults;
import org.activityinfo.legacy.shared.command.result.ListResult;
import org.activityinfo.legacy.shared.model.*;
import org.activityinfo.model.type.number.QuantityType;
import org.activityinfo.ui.client.style.legacy.icon.IconImageBundle;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class IndicatorGridPanel extends ContentPanel {

    private class Loader extends BaseListLoader<ListResult<ModelData>> {
        public Loader() {
            super(new Proxy());
        }
    }

    private class Proxy implements DataProxy<List<ModelData>> {

        @Override
        public void load(DataReader<List<ModelData>> reader, Object loadConfig, final AsyncCallback<List<ModelData>> callback) {

            showEmptyText(I18N.CONSTANTS.loading());

            if (selectedDb == null) {
                setEmptyText();
                callback.onSuccess(Lists.<ModelData>newArrayList());
                return;
            }

            dispatcher.execute(new GetActivityForms().setFilter(activityIdsFilter())).then(new AsyncCallback<ActivityFormResults>() {
                @Override
                public void onFailure(Throwable caught) {
                    showEmptyText(I18N.CONSTANTS.failedToLoadEntries());

                    callback.onFailure(caught);
                }

                @Override
                public void onSuccess(ActivityFormResults result) {
                    setEmptyText();
                    callback.onSuccess(constructResult(result.getData()));
                }
            });
        }

        private List<ModelData> constructResult(List<ActivityFormDTO> data) {
            List<ModelData> result = Lists.newArrayList();
            for (ActivityFormDTO activity : data) {
                result.add(activity);
                for (IndicatorGroup group : activity.groupIndicators()) {
                    if (group.getName() == null) {
                        for (IndicatorDTO indicator : group.getIndicators()) {
                            if (indicator.getType() == QuantityType.TYPE_CLASS) {
                                result.add(indicator);
                            }
                        }
                    } else {
                        result.add(group);
                        for (IndicatorDTO indicator : group.getIndicators()) {
                            if (indicator.getType() == QuantityType.TYPE_CLASS) {
                                result.add(indicator);
                            }
                        }
                    }
                }
            }
            return result;
        }

        private Filter activityIdsFilter() {
            List<Integer> activityIds = Lists.newArrayList();
            for(ActivityDTO activity : selectedDb.getActivities()) {
                activityIds.add(activity.getId());
            }

            Filter filter = new Filter();
            filter.addRestriction(DimensionType.Activity, activityIds);
            return filter;
        }

    }


    private static final int INDENT = 10;

    private final Dispatcher dispatcher;

    private Set<Integer> linked = Collections.emptySet();
    private ListStore<ModelData> store;
    private Grid<ModelData> grid;
    private final Loader loader = new Loader();
    private UserDatabaseDTO selectedDb;

    public IndicatorGridPanel(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;

        store = new ListStore<ModelData>(loader);
        grid = new Grid<ModelData>(store, createColumnModel());
        grid.setView(new HighlightingGridView() {

            @Override
            protected boolean isHighlightable(ModelData model) {
                return model instanceof IndicatorDTO;
            }
        });
        setEmptyText();
        grid.setAutoExpandColumn("name");
        grid.setHideHeaders(true);

        grid.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        grid.getSelectionModel().addListener(Events.BeforeSelect, new Listener<SelectionEvent<ModelData>>() {

            @Override
            public void handleEvent(SelectionEvent<ModelData> event) {
                if (!(event.getModel() instanceof IndicatorDTO)) {
                    event.setCancelled(true);
                }
            }
        });
        setLayout(new FitLayout());
        add(grid);
    }

    private void setEmptyText() {
        showEmptyText(I18N.CONSTANTS.selectDatabaseHelp());
    }

    private void showEmptyText(String text) {
        grid.getView().setEmptyText(text);
        grid.getView().refresh(false);
    }

    public HighlightingGridView getGridView() {
        return (HighlightingGridView) grid.getView();
    }

    public int getRowY(IndicatorDTO indicator) {
        int rowIndex = grid.getStore().indexOf(indicator);
        if (rowIndex == -1) {
            throw new IllegalArgumentException("indicatorId=" + indicator.getId());
        }
        Element row = grid.getView().getRow(rowIndex);
        Point p = El.fly(row).getAnchorXY("c", false);
        return p.y;
    }

    public void addMouseOverListener(Listener<GridEvent<IndicatorDTO>> listener) {
        grid.addListener(HighlightingGridView.ROW_MOUSE_OVER, listener);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addSelectionChangeListener(SelectionChangedListener<IndicatorDTO> listener) {
        grid.getSelectionModel().addSelectionChangedListener((SelectionChangedListener) listener);
    }

    private ColumnModel createColumnModel() {
        ColumnConfig icon = new ColumnConfig("icon", 28);
        icon.setRenderer(new GridCellRenderer<ModelData>() {

            @Override
            public Object render(ModelData model,
                                 String property,
                                 ColumnData config,
                                 int rowIndex,
                                 int colIndex,
                                 ListStore<ModelData> store,
                                 Grid<ModelData> grid) {

                if (model instanceof IndicatorDTO) {
                    int id = ((IndicatorDTO) model).getId();
                    if (linked.contains(id)) {
                        return IconImageBundle.ICONS.link().getHTML();
                    }
                }
                return "";
            }

        });

        ColumnConfig name = new ColumnConfig("name", I18N.CONSTANTS.name(), 150);
        name.setRenderer(new GridCellRenderer<ModelData>() {

            @Override
            public Object render(ModelData model,
                                 String property,
                                 ColumnData config,
                                 int rowIndex,
                                 int colIndex,
                                 ListStore<ModelData> store,
                                 Grid<ModelData> grid) {

                StringBuilder html = new StringBuilder();
                html.append("<div style=\"margin-left: ").append(indent(model)).append("px;");
                if (!(model instanceof IndicatorDTO)) {
                    html.append(";font-weight: bold;");
                }
                html.append("\">");
                html.append(model.get("name"));
                html.append("</div>");
                return html.toString();
            }

        });

        return new ColumnModel(Arrays.asList(icon, name));
    }

    protected int indent(ModelData model) {
        if (model instanceof IndicatorGroup) {
            return INDENT;
        } else if (model instanceof IndicatorDTO) {
            IndicatorDTO indicator = (IndicatorDTO) model;
            if (indicator.getCategory() == null) {
                return 2 * INDENT;
            } else {
                return 3 * INDENT;
            }
        }
        return 0;
    }

    public void setDatabase(UserDatabaseDTO db) {
        selectedDb = db;

        setHeadingText(db.getName());
        store.removeAll();

        loader.load();

    }

    public void setLinked(Set<Integer> ids) {
        this.linked = ids;
        if (grid.isRendered()) {
            getGridView().refreshAllRows();
        }
    }

    public IndicatorDTO getSelectedItem() {
        return (IndicatorDTO) grid.getSelectionModel().getSelectedItem();
    }

}
