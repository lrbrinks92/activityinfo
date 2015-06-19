package org.activityinfo.server.digest;

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

import com.google.common.base.Preconditions;
import net.lightoze.gwt.i18n.server.ThreadLocalLocaleProvider;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.server.mail.Message;
import org.activityinfo.server.util.html.HtmlTag;
import org.activityinfo.server.util.html.HtmlWriter;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.logging.Logger;

public class DigestMessageBuilder {

    private static final Logger LOGGER = Logger.getLogger(DigestMessageBuilder.class.getName());

    private final DigestModelBuilder digestModelBuilder;
    private final DigestRenderer digestRenderer;

    private UserDigest userDigest;

    public DigestMessageBuilder(DigestModelBuilder digestModelBuilder, DigestRenderer digestRenderer) {
        this.digestModelBuilder = digestModelBuilder;
        this.digestRenderer = digestRenderer;
    }

    public DigestMessageBuilder setUserDigest(UserDigest userDigest) {
        this.userDigest = userDigest;
        return this;
    }

    public Message build() throws IOException, MessagingException {

        Preconditions.checkNotNull(userDigest);
        Preconditions.checkNotNull(userDigest.getUser());

        // set the locale of the messages
        ThreadLocalLocaleProvider.pushLocale(userDigest.getUser().getLocaleObject());

        try {

            DigestModel model = digestModelBuilder.createModel(userDigest);

            if (!model.hasData()) {
                return null;
            }

            // create message, set recipient & bcc
            Message message = new Message();
            message.to(userDigest.getUser().getEmail(), userDigest.getUser().getName());

            String subject = I18N.MESSAGES.digestSubject(userDigest.getDate());
            message.subject(subject);

            // create the html body
            HtmlWriter htmlWriter = new HtmlWriter();

            htmlWriter.startDocument();

            htmlWriter.startDocumentHeader();
            htmlWriter.documentTitle(subject);
            htmlWriter.open(new HtmlTag("style"))
                    .at("type", "text/css")
                    .text("body { font-family:Helvetica; } a {color: black; text-decoration:none;} ")
                    .close();
            htmlWriter.endDocumentHeader();

            htmlWriter.startDocumentBody();

            htmlWriter.paragraph(I18N.MESSAGES.digestGreeting(userDigest.getUser().getName()));

            // the digest content
            htmlWriter.paragraph(digestRenderer.renderHtml(model));

            String signature = I18N.MESSAGES.digestSignature();
            htmlWriter.paragraph(signature);

            htmlWriter.endDocumentBody();
            htmlWriter.endDocument();

            LOGGER.finest("digest:\n" + htmlWriter.toString());

            message.htmlBody(htmlWriter.toString());

            return message;

        } finally {
            ThreadLocalLocaleProvider.popLocale();
        }
    }
}
