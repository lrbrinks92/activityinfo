package org.activityinfo.ui.client.page.entry.form;

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

import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.util.Format;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.Text;
import com.extjs.gxt.ui.client.widget.form.*;
import com.extjs.gxt.ui.client.widget.layout.TableData;
import com.extjs.gxt.ui.client.widget.layout.TableLayout;
import com.extjs.gxt.ui.client.widget.tips.ToolTipConfig;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.activityinfo.legacy.client.type.IndicatorNumberFormat;
import org.activityinfo.legacy.shared.model.ActivityFormDTO;
import org.activityinfo.legacy.shared.model.IndicatorDTO;
import org.activityinfo.legacy.shared.model.IndicatorGroup;
import org.activityinfo.legacy.shared.model.SiteDTO;
import org.activityinfo.model.type.FieldTypeClass;

import java.util.List;
import java.util.Set;

public class IndicatorSection extends LayoutContainer implements FormSection<SiteDTO> {

    public static final int NUMBER_FIELD_WIDTH = 50;
    public static final int TEXT_FIELD_WIDTH = 3 * NUMBER_FIELD_WIDTH;

    private List<Field> indicatorFields = Lists.newArrayList();
    private Set<IndicatorDTO> calculatedIndicators = Sets.newHashSet();

    public IndicatorSection(ActivityFormDTO activity) {

        TableLayout layout = new TableLayout(3);
        layout.setCellPadding(5);
        layout.setCellVerticalAlign(Style.VerticalAlignment.TOP);

        setLayout(layout);
        setStyleAttribute("fontSize", "8pt");
        setScrollMode(Scroll.AUTOY);

        for (IndicatorGroup group : activity.groupIndicators()) {

            if (group.getName() != null) {
                addGroupHeader(group.getName());
            }

            for (IndicatorDTO indicator : group.getIndicators()) {
                if (indicator.isCalculated()) {
                    calculatedIndicators.add(indicator);
                    continue; // it's not allowed to specify value for calculated indicators
                }
                if (indicator.getAggregation() != IndicatorDTO.AGGREGATE_SITE_COUNT) {
                    addIndicator(indicator);
                }
            }
        }
    }

    private void addGroupHeader(String name) {

        TableData layoutData = new TableData();
        layoutData.setColspan(3);

        Text header = new Text(name);
        header.setStyleAttribute("fontSize", "9pt");
        header.setStyleAttribute("fontWeight", "bold");
        header.setStyleAttribute("marginTop", "6pt");

        add(header, layoutData);
    }

    private void addIndicator(IndicatorDTO indicator) {
        String name = indicator.getName();
        if (indicator.isMandatory()) {
            name += " *";
        }
        Text indicatorLabel = new Text(Format.htmlEncode(name));
        indicatorLabel.setStyleAttribute("fontSize", "9pt");
        add(indicatorLabel);

        FieldTypeClass type = indicator.getType();

        Field indicatorField = addIndicatorField(indicator, type);

        indicatorField.setName(indicator.getPropertyName());

        if (indicator.getDescription() != null && !indicator.getDescription().isEmpty()) {
            ToolTipConfig tip = new ToolTipConfig();
            tip.setDismissDelay(0);
            tip.setShowDelay(100);
            tip.setText(indicator.getDescription());

            indicatorField.setToolTip(tip);
        }

        indicatorFields.add(indicatorField);
    }

    private Field addIndicatorField(IndicatorDTO indicator, FieldTypeClass type) {
        if (type == FieldTypeClass.NARRATIVE) {

            TextArea textArea = new TextArea();

            textArea.setEnabled(!indicator.isCalculated());
            textArea.setWidth(TEXT_FIELD_WIDTH);
//            textArea.setAutoWidth(true);
            if (indicator.isMandatory()) {
                textArea.setAllowBlank(false);
            }
            add(textArea);
            add(new Text()); // avoid layout shift
            return textArea;
        } else if (type == FieldTypeClass.QUANTITY) {
            NumberField numberField = new NumberField();

            numberField.setFormat(IndicatorNumberFormat.INSTANCE);
            numberField.setWidth(NUMBER_FIELD_WIDTH);
            numberField.setStyleAttribute("textAlign", "right");
            if (indicator.isMandatory()) {
                numberField.setAllowBlank(false);
            }

            add(numberField);

            Text unitLabel = new Text(indicator.getUnits());
            unitLabel.setStyleAttribute("fontSize", "9pt");

            add(unitLabel);
            return numberField;
        } else if (type == FieldTypeClass.FREE_TEXT) {
            TextField textField = new TextField();

            textField.setWidth(TEXT_FIELD_WIDTH);
//            textField.setAutoWidth(true);
            if (indicator.isMandatory()) {
                textField.setAllowBlank(false);
            }
            add(textField);
            add(new Text()); // avoid layout shift
            return textField;
        } else if (type == FieldTypeClass.BOOLEAN) {
            CheckBox checkBox = new CheckBox();

            add(checkBox);
            add(new Text()); // avoid layout shift
            return checkBox;
        }
        return new NumberField();
    }

    @Override
    public boolean validate() {
        boolean valid = true;
        for (Field field : indicatorFields) {
            valid &= field.validate();
        }
        return valid;
    }

    @Override
    public void updateModel(SiteDTO m) {
        for (Field field : indicatorFields) {
            m.set(field.getName(), field.getValue());
        }
        for (IndicatorDTO indicator : calculatedIndicators) {
            m.remove(indicator.getPropertyName());
        }
    }

    @Override
    public void updateForm(SiteDTO m, boolean isNew) {
        for (Field field : indicatorFields) {
            field.setValue(m.get(field.getName()));
        }
    }

    @Override
    public Component asComponent() {
        return this;
    }
}
