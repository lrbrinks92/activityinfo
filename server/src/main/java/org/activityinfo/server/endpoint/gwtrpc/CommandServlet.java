package org.activityinfo.server.endpoint.gwtrpc;

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

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.activityinfo.legacy.shared.command.Command;
import org.activityinfo.legacy.shared.command.RemoteCommandService;
import org.activityinfo.legacy.shared.command.result.CommandResult;
import org.activityinfo.legacy.shared.exception.CommandException;
import org.activityinfo.legacy.shared.exception.InvalidAuthTokenException;
import org.activityinfo.model.auth.AnonymousUser;
import org.activityinfo.model.auth.AuthenticatedUser;
import org.activityinfo.server.authentication.ServerSideAuthProvider;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Process command objects from the client and returns CommandResults.
 * <p/>
 * This servlet is at the heart of the command execution pipeline, but delegates
 * all logic processing to the
 * {@link org.activityinfo.server.command.handler.CommandHandler} corresponding
 * to the given {@link org.activityinfo.legacy.shared.command.Command}s.
 * <p/>
 * CommandHandlers are loaded based on name from the
 * org.activityinfo.server.command.handler package.
 * <p/>
 * E.g. UpdateEntity =>
 * org.activityinfo.server.command.handler.UpdateEntityHandler
 */
@Singleton
public class CommandServlet extends RemoteServiceServlet implements RemoteCommandService {

    private static final Logger LOGGER = Logger.getLogger(CommandServlet.class.getName());

    @Inject
    private Injector injector;

    @Inject
    private ServerSideAuthProvider authProvider;

    @Inject(optional = true)
    private PersistentPolicyProvider policyProvider;


    @Override
    public List<CommandResult> execute(String authToken, List<Command> commands) throws CommandException {
        if (!checkAuthentication(authToken)) {
            throw new InvalidAuthTokenException("Auth Tokens do not match, possible XSRF attack");
        }

        try {
            return handleCommands(commands);

        } catch (Exception caught) {
            LOGGER.log(Level.SEVERE, "Could not execute commands!", caught);
            throw new CommandException();
        }
    }

    public CommandResult execute(String authToken, Command command) throws CommandException {
        if (!checkAuthentication(authToken)) {
            throw new InvalidAuthTokenException("Auth Tokens do not match, possible XSRF attack");
        }

        return handleCommand(command);
    }

    /**
     * Publicly visible for testing *
     */
    public List<CommandResult> handleCommands(List<Command> commands) {

        List<CommandResult> results = new ArrayList<CommandResult>();
        for (Command command : commands) {

            LOGGER.log(Level.INFO, authProvider.get().getEmail() + ": " + command.getClass().getSimpleName());

            try {
                results.add(handleCommand(command));
            } catch (CommandException e) {
                results.add(e);
            }
        }
        return results;
    }

    protected CommandResult handleCommand(Command command) throws CommandException {
        RemoteExecutionContext context = null;
        try {
            context = new RemoteExecutionContext(injector);
            CommandResult result = context.startExecute(command);
            if(result instanceof CommandException) {
                LOGGER.log(Level.SEVERE, "Exception executing " + command.getClass().getSimpleName(), 
                        (CommandException)result);
            }
            return result;
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception executing " + command.getClass().getSimpleName(), e);
            throw new CommandException(command, context, e);
        }
    }

    @Override
    public void log(String message, Throwable t) {
        super.log(message, t);
        LOGGER.log(Level.SEVERE, message, t);
    }

    @Override
    public void log(String msg) {
        super.log(msg);
        LOGGER.log(Level.INFO, msg);
    }

    @Override
    protected SerializationPolicy doGetSerializationPolicy(HttpServletRequest request,
                                                           String moduleBaseURL,
                                                           String strongName) {
        if (policyProvider == null) {
            return super.doGetSerializationPolicy(request, moduleBaseURL, strongName);
        } else {
            return policyProvider.getSerializationPolicy(moduleBaseURL, strongName);
        }
    }

    private boolean checkAuthentication(String authToken) {
        if (authToken.equals(AnonymousUser.AUTHTOKEN)) {
            authProvider.set(AuthenticatedUser.getAnonymous());
        } else {

            // TODO(alex): renable this check once the authToken has been
            // removed from the host page

            // user is already authenticated, but ensure that the authTokens match
            // if(!authToken.equals(authProvider.get().getAuthToken())) {
            // throw new InvalidAuthTokenException("Auth Tokens do not match, possible XSRF attack");
            // }
        }
        return true;
    }
}
