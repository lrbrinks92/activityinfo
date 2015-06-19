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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.promise.Promise;

/**
 * @author yuriyz on 7/14/14.
 */
public class FormDesignerActions {

    private final FormDesigner formDesigner;
    private final FormDesignerPanel formDesignerPanel;

    private FormDesignerActions(FormDesigner formDesigner) {
        this.formDesigner = formDesigner;
        this.formDesignerPanel = formDesigner.getFormDesignerPanel();
    }

    public static FormDesignerActions create(FormDesigner formDesigner) {
        return new FormDesignerActions(formDesigner).bind();
    }

    private FormDesignerActions bind() {
        formDesignerPanel.getSaveButton().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                save();
            }
        });
        return this;
    }

    public Promise<Void> save() {

        formDesignerPanel.getStatusMessage().setHTML(I18N.CONSTANTS.saving());
        formDesignerPanel.getSaveButton().setEnabled(false);

        Promise<Void> promise = formDesigner.getResourceLocator().persist(formDesigner.getFormClass());
        promise.then(new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                showFailureDelayed(caught);
                formDesigner.getSavedGuard().setSaved(false);
            }

            @Override
            public void onSuccess(Void result) {
                formDesignerPanel.getSaveButton().setEnabled(true);
                formDesignerPanel.getStatusMessage().setHTML(I18N.CONSTANTS.saved());
                formDesigner.getSavedGuard().setSaved(true);
            }
        });
        return promise;
    }

    private void showFailureDelayed(final Throwable caught) {
        // Show failure message only after a short fixed delay to ensure that
        // the progress stage is displayed. Otherwise if we have a synchronous error, clicking
        // the retry button will look like it's not working.
        Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                formDesignerPanel.getSaveButton().setEnabled(true);
                formDesignerPanel.getStatusMessage().setHTML(I18N.CONSTANTS.failedToSaveClass());
                formDesignerPanel.getSaveButton().setText(I18N.CONSTANTS.retry());
                return false;
            }
        }, 500);
    }
}
