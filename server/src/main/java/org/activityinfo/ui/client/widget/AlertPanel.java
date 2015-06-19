package org.activityinfo.ui.client.widget;
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

import com.google.common.collect.Lists;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import org.activityinfo.core.shared.validation.ValidationUtils;
import org.activityinfo.ui.client.style.ElementStyle;
import org.activityinfo.ui.client.util.GwtUtil;

import java.util.List;

/**
 * @author yuriyz on 4/1/14.
 */
public class AlertPanel extends Composite {

    public static interface VisibilityHandler {
        public void onVisibilityChange(boolean isVisible);
    }

    private static AlertPanelBinder uiBinder = GWT
            .create(AlertPanelBinder.class);

    @UiField
    Button close;
    @UiField
    DivElement container;
    @UiField
    DivElement messages;

    private final List<VisibilityHandler> visibilityHandlers = Lists.newArrayList();

    interface AlertPanelBinder extends UiBinder<Widget, AlertPanel> {
    }

    @UiConstructor
    public AlertPanel(ElementStyle style) {
        initWidget(uiBinder.createAndBindUi(this));
        container.setClassName("alert alert-" + style.name().toLowerCase() + " hidden");
    }

    @UiHandler("close")
    public void onCloseAlert(ClickEvent event) {
        setVisible(false);
    }

    @Override
    public void setVisible(boolean visible) {
        GwtUtil.setVisible(visible, container);
        for (VisibilityHandler handler : visibilityHandlers) {
            handler.onVisibilityChange(visible);
        }
    }

    public void showMessages(SafeHtml message) {
        messages.setInnerSafeHtml(message);
        setVisible(true);
    }

    public void showMessages(String message) {
        showMessages(Lists.newArrayList(message));
    }

    public void showMessages(List<String> message) {
        setVisible(true);
        ValidationUtils.showMessages(message, messages);
    }

    public void addVisibilityHandler(VisibilityHandler handler) {
        visibilityHandlers.add(handler);
    }
}
