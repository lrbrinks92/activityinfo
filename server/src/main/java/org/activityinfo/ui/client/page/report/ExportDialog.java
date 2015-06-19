package org.activityinfo.ui.client.page.report;

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

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.core.El;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.ProgressBar;
import com.extjs.gxt.ui.client.widget.Text;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout.VBoxLayoutAlign;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayoutData;
import com.google.common.base.Strings;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.http.client.*;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.legacy.client.Dispatcher;
import org.activityinfo.legacy.shared.command.Filter;
import org.activityinfo.legacy.shared.command.FilterUrlSerializer;
import org.activityinfo.legacy.shared.command.RenderElement;
import org.activityinfo.legacy.shared.command.RenderElement.Format;
import org.activityinfo.legacy.shared.command.result.UrlResult;
import org.activityinfo.legacy.shared.reports.model.ReportElement;
import org.activityinfo.promise.Promise;

public class ExportDialog extends Dialog {

    private final Dispatcher dispatcher;
    private ProgressBar bar;
    private Text downloadLink;
    private boolean canceled = false;
    private Button button;

    public ExportDialog(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;

        setWidth(350);
        setHeight(175);
        setHeadingText(I18N.CONSTANTS.export());
        setClosable(false);
        setButtonAlign(HorizontalAlignment.CENTER);

        VBoxLayout layout = new VBoxLayout();
        layout.setVBoxLayoutAlign(VBoxLayoutAlign.LEFT);

        setLayout(layout);

        bar = new ProgressBar();
        bar.setWidth(300);
        add(bar, new VBoxLayoutData(new Margins(20, 15, 25, 15)));

        downloadLink = new Text(I18N.CONSTANTS.clickToDownload());
        downloadLink.setTagName("a");
        downloadLink.setVisible(false);
        add(downloadLink, new VBoxLayoutData(0, 15, 0, 15));

    }

    @Override
    protected void createButtons() {

        button = new Button();
        button.setText(I18N.CONSTANTS.cancel());
        button.addSelectionListener(new SelectionListener<ButtonEvent>() {

            @Override
            public void componentSelected(ButtonEvent ce) {
                ExportDialog.this.canceled = true;
                bar.reset();
                hide();
            }
        });
        getButtonBar().add(button);
    }

    public void export(String filename, ReportElement model, Format format) {
        showStartProgress();

        RenderElement command = new RenderElement(model, format);
        command.setFilename(filename);

        dispatcher.execute(command, new AsyncCallback<UrlResult>() {

            @Override
            public void onFailure(Throwable caught) {
                showError();
            }

            @Override
            public void onSuccess(UrlResult result) {
                if (!canceled) {
                    initiateDownload(result.getUrl());
                }
            }
        });
    }


    private void showStartProgress() {
        show();
        bar.updateText(I18N.CONSTANTS.exportProgress());
        bar.auto();
    }

    private void showError() {
        MessageBox.alert(I18N.CONSTANTS.export(), I18N.CONSTANTS.serverError(), new Listener<MessageBoxEvent>() {

            @Override
            public void handleEvent(MessageBoxEvent be) {
                ExportDialog.this.hide();
            }
        });
    }

    private void initiateDownload(String url) {
        bar.reset();
        bar.updateProgress(1.0, I18N.CONSTANTS.downloadReady());
        button.setText(I18N.CONSTANTS.close());
        tryStartDownloadWithIframe(url);
        downloadLink.getElement().setAttribute("href", url);
        downloadLink.setVisible(true);
        layout(true);
    }

    public void exportSites(Filter filter) {

        show();
        bar.updateText(I18N.CONSTANTS.exportProgress());

        RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.POST, "/ActivityInfo/export");
        requestBuilder.setHeader("Content-type", "application/x-www-form-urlencoded");
        requestBuilder.setRequestData("filter=" + FilterUrlSerializer.toUrlFragment(filter));
        requestBuilder.setCallback(new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                final String exportId = response.getText();
                if(Strings.isNullOrEmpty(exportId)) {
                    showError();
                } else {
                    getDownloadUrl(exportId).then(new AsyncCallback<String>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            showError();
                        }

                        @Override
                        public void onSuccess(String downloadUrl) {
                            initiateDownload(downloadUrl);
                        }
                    });
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                showError();
            }
        });
        try {
            requestBuilder.send();
        } catch (RequestException e) {
            showError();
        }
    }

    private Promise<String> getDownloadUrl(String exportId) {
        final Promise<String> url = new Promise<>();
        schedulePoll(exportId, url);
        return url;
    }

    private void schedulePoll(final String exportId, final Promise<String> downloadUrl) {
        Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                if(!canceled) {
                    pollServer(exportId, downloadUrl);
                }
                return false;
            }
        }, 1000);
    }

    private void pollServer(final String exportId, final Promise<String> downloadUrl) {
        RequestBuilder request = new RequestBuilder(RequestBuilder.GET, "/generated/status/" + exportId);
        request.setCallback(new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                if (response.getStatusCode() == Response.SC_OK) {
                    JSONObject status = JSONParser.parseStrict(response.getText()).isObject();
                    double progress = status.get("progress").isNumber().doubleValue();
                    JSONValue downloadUri = status.get("downloadUri");
                    
                    if(downloadUri != null) {
                        downloadUrl.onSuccess(downloadUri.isString().stringValue());
                    } else {
                        bar.updateProgress(progress, I18N.CONSTANTS.exportProgress());
                        schedulePoll(exportId, downloadUrl);
                    }
                } else {
                    showError();
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                downloadUrl.onFailure(exception);
            }
        });
        try {
            request.send();
        } catch (RequestException e) {
            downloadUrl.onFailure(e);
        }
    }

    private void tryStartDownloadWithIframe(String url) {
        com.google.gwt.user.client.ui.Frame frame = new com.google.gwt.user.client.ui.Frame(url);
        El el = El.fly(frame.getElement());
        el.setStyleAttribute("width", 0);
        el.setStyleAttribute("height", 0);
        el.setStyleAttribute("position", "absolute");
        el.setStyleAttribute("border", 0);
        RootPanel.get().add(frame);
    }
}
