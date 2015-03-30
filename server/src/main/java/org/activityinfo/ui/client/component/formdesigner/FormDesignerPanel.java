package org.activityinfo.ui.client.component.formdesigner;
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;
import org.activityinfo.ui.client.component.formdesigner.header.HeaderPanel;
import org.activityinfo.ui.client.component.formdesigner.palette.FieldPalette;
import org.activityinfo.ui.client.component.formdesigner.properties.PropertiesPanel;
import org.activityinfo.ui.client.page.HasNavigationCallback;
import org.activityinfo.ui.client.page.NavigationCallback;
import org.activityinfo.ui.client.util.GwtUtil;
import org.activityinfo.ui.client.widget.Button;

/**
 * @author yuriyz on 07/04/2014.
 */
public class FormDesignerPanel extends Composite implements ScrollHandler, HasNavigationCallback, FormSavedGuard.HasSavedGuard {

    private final static OurUiBinder uiBinder = GWT.create(OurUiBinder.class);

    interface OurUiBinder extends UiBinder<Widget, FormDesignerPanel> {
    }

    private ScrollPanel scrollAncestor;
    private HasNavigationCallback savedGuard = null;

    @UiField
    HTMLPanel containerPanel;
    @UiField
    FlowPanel dropPanel;
    @UiField
    PropertiesPanel propertiesPanel;
    @UiField
    HeaderPanel headerPanel;
    @UiField
    FieldPalette fieldPalette;
    @UiField
    Button saveButton;
    @UiField
    HTML statusMessage;
    @UiField
    HTML spacer;
    @UiField
    HTML paletteSpacer;

    public FormDesignerPanel() {

        // todo(yuriy) injecting resources takes too much time (~1 second), we have to improve it
        FormDesignerStyles.INSTANCE.ensureInjected();

        initWidget(uiBinder.createAndBindUi(this));
        propertiesPanel.setVisible(false);

        addAttachHandler(new AttachEvent.Handler() {
            @Override
            public void onAttachOrDetach(AttachEvent event) {
                scrollAncestor = GwtUtil.getScrollAncestor(FormDesignerPanel.this);
                scrollAncestor.addScrollHandler(FormDesignerPanel.this);
            }
        });
    }

    @Override
    public void onScroll(ScrollEvent event) {
        calcSpacerHeight();
    }

    protected void calcSpacerHeight() {
        int verticalScrollPosition = scrollAncestor.getVerticalScrollPosition();
        if (verticalScrollPosition > Metrics.MAX_VERTICAL_SCROLL_POSITION) {
            int height = verticalScrollPosition - Metrics.MAX_VERTICAL_SCROLL_POSITION;

//            int selectedWidgetTop = 0;
//            if (selectedWidgetContainer != null) {
//                selectedWidgetTop = selectedWidgetContainer.asWidget().getAbsoluteTop();
//            }
//            if (selectedWidgetTop < 0) {
//                height = height + selectedWidgetTop;
//            }

            //GWT.log("verticalPos = " + verticalScrollPosition + ", height = " + height + ", selectedWidgetTop = " + selectedWidgetTop);
            spacer.setHeight(height + "px");
            paletteSpacer.setHeight(height + "px");
        } else {
            spacer.setHeight("0px");
            paletteSpacer.setHeight("0px");
        }
    }

    public FlowPanel getDropPanel() {
        return dropPanel;
    }

    public PropertiesPanel getPropertiesPanel() {
        return propertiesPanel;
    }

    public HeaderPanel getHeaderPanel() {
        return headerPanel;
    }

    public FieldPalette getFieldPalette() {
        return fieldPalette;
    }

    public Button getSaveButton() {
        return saveButton;
    }

    public HTML getStatusMessage() {
        return statusMessage;
    }

    public HasNavigationCallback getSavedGuard() {
        return savedGuard;
    }

    public void setSavedGuard(HasNavigationCallback savedGuard) {
        this.savedGuard = savedGuard;
    }

    @Override
    public void navigate(NavigationCallback callback) {
        if (savedGuard != null) {
            savedGuard.navigate(callback);
        }
    }
}
