package org.activityinfo.ui.client.component.formdesigner.header;
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
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.FocusPanel;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.ui.client.component.formdesigner.FormDesigner;
import org.activityinfo.ui.client.component.formdesigner.FormDesignerStyles;
import org.activityinfo.ui.client.component.formdesigner.event.HeaderSelectionEvent;
import org.activityinfo.ui.client.component.formdesigner.event.WidgetContainerSelectionEvent;

/**
 * @author yuriyz on 7/11/14.
 */
public class HeaderPresenter {

    private final FormDesigner formDesigner;
    private final HeaderPanel headerPanel;
    private final FormClass formClass;

    public HeaderPresenter(FormDesigner formDesigner) {
        this.formDesigner = formDesigner;
        this.headerPanel = formDesigner.getFormDesignerPanelPresenter().getPanel().getHeaderPanel();
        this.formClass = formDesigner.getFormClass();
        formDesigner.getEventBus().addHandler(WidgetContainerSelectionEvent.TYPE, new WidgetContainerSelectionEvent.Handler() {
            @Override
            public void handle(WidgetContainerSelectionEvent event) {
                setSelected(false);
            }
        });
        headerPanel.getFocusPanel().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                HeaderPresenter.this.onClick();
            }
        });
    }

    public FormClass getFormClass() {
        return formClass;
    }

    public void show() {
        headerPanel.getLabel().setHTML(SafeHtmlUtils.fromString(Strings.nullToEmpty(formDesigner.getFormClass().getLabel())));
        headerPanel.getDescription().setHTML(SafeHtmlUtils.fromString(Strings.nullToEmpty(formDesigner.getFormClass().getDescription())));
    }

    private void onClick() {
        formDesigner.getEventBus().fireEvent(new HeaderSelectionEvent(this));
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                setSelected(true);
            }
        });
    }

    public void setSelected(boolean selected) {
        FocusPanel focusPanel = headerPanel.getFocusPanel();
        if (selected) {
            focusPanel.addStyleName(FormDesignerStyles.INSTANCE.widgetContainerSelected());
        } else {
            focusPanel.removeStyleName(FormDesignerStyles.INSTANCE.widgetContainerSelected());
        }
    }
}
