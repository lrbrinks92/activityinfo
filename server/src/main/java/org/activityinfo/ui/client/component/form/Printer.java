package org.activityinfo.ui.client.component.form;

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

import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.FlowLayout;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.RootPanel;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.legacy.client.Dispatcher;
import org.activityinfo.legacy.client.monitor.MaskingAsyncMonitor;
import org.activityinfo.legacy.shared.command.GetActivityForm;
import org.activityinfo.legacy.shared.model.*;
import org.activityinfo.ui.client.page.entry.form.resources.SiteFormResources;

public class Printer {


    public Printer() {
 
    }
    
    public void print(String html) {
        Frame frame = new Frame();
        frame.getElement().setPropertyInt("frameBorder", 0);
        frame.setSize("0", "0");
        frame.setVisible(false);

        final IFrameElement element = frame.getElement().cast();

        Document.get().getDocumentElement().appendChild(element); 
        
        fillIframe(element, html);
        Scheduler.get().scheduleDeferred(new Command() {
            @Override
            public void execute() {
                doPrint(element);
            }
        });
    }

    private final native void fillIframe(IFrameElement iframe, String content) /*-{
      var doc = iframe.document;

      if (iframe.contentDocument) {
        doc = iframe.contentDocument; // For NS6
      } else if (iframe.contentWindow) {
        doc = iframe.contentWindow.document; // For IE5.5 and IE6
      }

      // Put the content in the iframe
      doc.open();
      doc.writeln(content);
      doc.close();
    }-*/;



    private static native void doPrint(IFrameElement frame) /*-{
      var contentWindow = frame.contentWindow;
      contentWindow.focus();
      contentWindow.print();
    }-*/;

    

}
