package org.activityinfo.i18n.shared;

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

import com.google.gwt.core.shared.GWT;
import net.lightoze.gwt.i18n.client.LocaleFactory;

/**
 * Contains global instances of UiConstants and UiMessages
 */
public final class I18N {

    private I18N() {
    }

    public static final UiConstants CONSTANTS;
    public static final UiMessages MESSAGES;

    static {
        if (GWT.isClient()) {
            CONSTANTS = GWT.create(UiConstants.class);
            MESSAGES = GWT.create(UiMessages.class);
        } else {
            // on the server side: LocaleProxy is initialized in LocaleModule
            // locale is set for each request in CommandServlet
            CONSTANTS = LocaleFactory.get(UiConstants.class);
            MESSAGES = LocaleFactory.get(UiMessages.class);
        }
    }
}
