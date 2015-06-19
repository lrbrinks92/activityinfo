package org.activityinfo.server.command.handler;

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

import com.google.inject.Inject;
import org.activityinfo.legacy.shared.command.Filter;
import org.activityinfo.legacy.shared.command.RenderReportHtml;
import org.activityinfo.legacy.shared.command.result.CommandResult;
import org.activityinfo.legacy.shared.command.result.HtmlResult;
import org.activityinfo.legacy.shared.exception.CommandException;
import org.activityinfo.legacy.shared.reports.model.DateRange;
import org.activityinfo.legacy.shared.reports.model.ReportElement;
import org.activityinfo.server.database.hibernate.entity.User;
import org.activityinfo.server.report.generator.ReportGenerator;
import org.activityinfo.server.report.renderer.itext.HtmlReportRenderer;

import java.io.IOException;
import java.io.StringWriter;
import java.util.logging.Logger;

/**
 * @author Alex Bertram
 * @see org.activityinfo.legacy.shared.command.RenderReportHtml
 */
public class RenderReportHtmlHandler implements CommandHandler<RenderReportHtml> {

    private final ReportGenerator generator;
    private final HtmlReportRenderer renderer;

    private static final Logger LOGGER = Logger.getLogger(RenderReportHtmlHandler.class.getName());

    @Inject
    public RenderReportHtmlHandler(ReportGenerator generator, HtmlReportRenderer renderer) {
        this.generator = generator;
        this.renderer = renderer;
    }

    @Override
    public CommandResult execute(RenderReportHtml cmd, User user) throws CommandException {
        ReportElement model = cmd.getModel();

        LOGGER.fine("Model: " + model);

        // don't show the title: it will be rendered by the container
        model.setTitle(null);

        generator.generateElement(user, model, new Filter(), new DateRange());
        StringWriter writer = new StringWriter();
        try {
            renderer.render(model, writer);
        } catch (IOException e) {
            throw new CommandException(e);
        }
        return new HtmlResult(writer.toString());

    }

}
