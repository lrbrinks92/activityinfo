package org.activityinfo.ui.client.page.entry;

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

import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.FormEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.form.FormPanel.Encoding;
import com.extjs.gxt.ui.client.widget.form.FormPanel.Method;
import com.extjs.gxt.ui.client.widget.form.HiddenField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.legacy.client.Dispatcher;
import org.activityinfo.legacy.shared.command.DeleteSiteAttachment;
import org.activityinfo.legacy.shared.command.result.VoidResult;
import org.activityinfo.legacy.shared.model.SiteDTO;
import org.activityinfo.legacy.shared.type.UUID;
import org.activityinfo.ui.client.ClientContext;
import org.activityinfo.ui.client.page.common.dialog.FormDialogCallback;
import org.activityinfo.ui.client.page.common.dialog.FormDialogImpl;
import org.activityinfo.ui.client.page.common.toolbar.ActionListener;
import org.activityinfo.ui.client.page.common.toolbar.UIActions;

public class AttachmentsPresenter implements ActionListener {

    public interface View {
        void setSelectionTitle(String title);

        void setActionEnabled(String id, boolean enabled);

        void setAttachmentStore(int siteId);

        String getSelectedItem();

        void refreshList();
    }

    private final View view;
    private final Dispatcher dispatcher;
    private AttachmentForm form;
    private SiteDTO currentSite;
    private String blobid;

    @Inject
    public AttachmentsPresenter(Dispatcher service, View view) {
        this.dispatcher = service;
        this.view = view;
    }

    public void showSite(SiteDTO site) {
        currentSite = site;
        view.setSelectionTitle(currentSite.getLocationName());
        view.setActionEnabled(UIActions.UPLOAD, true);
        view.setActionEnabled(UIActions.DELETE, false);
        view.setAttachmentStore(currentSite.getId());
    }

    @Override
    public void onUIAction(String actionId) {
        if (UIActions.DELETE.equals(actionId)) {
            MessageBox.confirm(ClientContext.getAppTitle(),
                    I18N.CONSTANTS.confirmDeleteAttachment(),
                    new Listener<MessageBoxEvent>() {
                        @Override
                        public void handleEvent(MessageBoxEvent be) {
                            if (be.getButtonClicked().getItemId().equals(Dialog.YES)) {
                                onDelete();
                            }
                        }
                    });
        } else if (UIActions.UPLOAD.equals(actionId)) {
            onUpload();
        }

    }

    public void onUpload() {

        form = new AttachmentForm();
        form.setEncoding(Encoding.MULTIPART);
        form.setMethod(Method.POST);

        HiddenField<String> blobField = new HiddenField<String>();
        blobField.setName("blobId");
        blobid = UUID.uuid();
        blobField.setValue(blobid);
        form.add(blobField);

        final FormDialogImpl dialog = new FormDialogImpl(form);
        dialog.setWidth(400);
        dialog.setHeight(200);
        dialog.setHeadingText(I18N.CONSTANTS.newAttachment());

        dialog.show(new FormDialogCallback() {
            @Override
            public void onValidated() {
                form.setAction("/ActivityInfo/attachment?blobId=" + blobid + "&siteId=" + currentSite.getId());
                form.submit();
                dialog.getSaveButton().setEnabled(false);
            }
        });

        form.addListener(Events.Submit, new Listener<FormEvent>() {

            @Override
            public void handleEvent(FormEvent event) {
                dialog.hide();
                view.setAttachmentStore(currentSite.getId());
            }
        });

    }

    public void onDelete() {

        DeleteSiteAttachment attachment = new DeleteSiteAttachment();
        attachment.setBlobId(view.getSelectedItem());

        dispatcher.execute(attachment, new AsyncCallback<VoidResult>() {
            @Override
            public void onFailure(Throwable caught) {
                // callback.onFailure(caught);
            }

            @Override
            public void onSuccess(VoidResult result) {
                view.setActionEnabled(UIActions.DELETE, false);
                view.setAttachmentStore(currentSite.getId());
            }
        });
    }

}
