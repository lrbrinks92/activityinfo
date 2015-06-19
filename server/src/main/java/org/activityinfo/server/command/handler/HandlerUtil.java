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

import org.activityinfo.legacy.shared.command.Command;
import org.activityinfo.legacy.shared.command.Month;
import org.activityinfo.legacy.shared.command.result.CommandResult;
import org.activityinfo.legacy.shared.impl.AuthorizationHandler;
import org.activityinfo.legacy.shared.impl.CommandHandlerAsync;

import java.util.Calendar;
import java.util.Date;

/**
 * Convenience methods for <code>CommandHandler</code>s
 */
public final class HandlerUtil {

    private HandlerUtil() {
    }

    /**
     * Returns the <code>CommandHandler</code> that corresponds to the given
     * <code>Command</code>. 
     *
     * @param cmd The <code>Command</code> for which a
     *            <code>CommandHandler</code> is to be returned
     * @return A <code>CommandHandler</code> capable of handling the given
     * <code>Command</code>
     */
    @SuppressWarnings("unchecked")
    public static Class handlerForCommand(Command cmd) {

        String commandName = cmd.getClass().getName().substring(cmd.getClass().getPackage().getName().length() + 1);
        String sharedHandlerName;

        sharedHandlerName = "org.activityinfo.legacy.shared.impl." + commandName + "Handler";

        try {
            return CommandHandler.class.getClassLoader().loadClass(sharedHandlerName);

        } catch (ClassNotFoundException e) {
            String serverHandlerName = "org.activityinfo.server.command.handler." + commandName + "Handler";
            try {
                return CommandHandler.class.getClassLoader().loadClass(serverHandlerName);
            } catch (Exception ex) {
                throw new IllegalArgumentException("No handler " + serverHandlerName + " found for " + commandName, e);
            }
        }
    }

    static Month monthFromRange(Date date1, Date date2) {

        Calendar c1 = Calendar.getInstance();
        c1.setTime(date1);
        if (c1.get(Calendar.DAY_OF_MONTH) != 1) {
            return null;
        }

        Calendar c2 = Calendar.getInstance();
        c2.setTime(date2);
        if (c2.get(Calendar.DAY_OF_MONTH) != c2.getActualMaximum(Calendar.DAY_OF_MONTH)) {
            return null;
        }

        if (c2.get(Calendar.MONTH) != c1.get(Calendar.MONTH) || c2.get(Calendar.YEAR) != c2.get(Calendar.YEAR)) {

            return null;
        }

        return new Month(c1.get(Calendar.YEAR), c1.get(Calendar.MONTH) + 1);

    }
}
