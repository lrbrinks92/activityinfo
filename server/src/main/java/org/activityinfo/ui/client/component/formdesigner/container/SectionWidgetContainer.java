package org.activityinfo.ui.client.component.formdesigner.container;
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

import com.google.common.base.Strings;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import org.activityinfo.model.form.FormSection;
import org.activityinfo.ui.client.component.formdesigner.FormDesigner;
import org.activityinfo.ui.client.component.formdesigner.event.WidgetContainerSelectionEvent;

/**
 * @author yuriyz on 7/14/14.
 */
public class SectionWidgetContainer implements WidgetContainer {

    private FormDesigner formDesigner;
    private FormSection formSection;
    private final WidgetContainerPanel widgetContainer;
    private final HTML html = new HTML();

    public SectionWidgetContainer(final FormDesigner formDesigner, final FormSection formSection) {
        this.formDesigner = formDesigner;
        this.formSection = formSection;

        widgetContainer = new WidgetContainerPanel(formDesigner);
        widgetContainer.getWidgetContainer().add(html);
        widgetContainer.getRemoveButton().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                formDesigner.getFormClass().remove(formSection);
            }
        });
        widgetContainer.getFocusPanel().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                formDesigner.getEventBus().fireEvent(new WidgetContainerSelectionEvent(SectionWidgetContainer.this));
            }
        });
        syncWithModel();
    }

    public void syncWithModel() {
        widgetContainer.getLabel().setHTML("<h3>" + SafeHtmlUtils.fromString(Strings.nullToEmpty(formSection.getLabel())) + "</h3>");
    }

    @Override
    public Widget asWidget() {
        return widgetContainer.asWidget();
    }

    @Override
    public Widget getDragHandle() {
        return widgetContainer.getDragHandle();
    }

    public FormDesigner getFormDesigner() {
        return formDesigner;
    }
}