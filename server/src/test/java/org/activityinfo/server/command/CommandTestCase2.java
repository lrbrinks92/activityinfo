package org.activityinfo.server.command;

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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Injector;
import net.lightoze.gwt.i18n.server.LocaleProxy;
import net.lightoze.gwt.i18n.server.ThreadLocalLocaleProvider;
import org.activityinfo.fixtures.Modules;
import org.activityinfo.fixtures.TestHibernateModule;
import org.activityinfo.legacy.client.Dispatcher;
import org.activityinfo.legacy.client.remote.AbstractDispatcher;
import org.activityinfo.legacy.shared.command.Command;
import org.activityinfo.legacy.shared.command.result.CommandResult;
import org.activityinfo.legacy.shared.exception.CommandException;
import org.activityinfo.server.authentication.AuthenticationModuleStub;
import org.activityinfo.server.database.hibernate.entity.User;
import org.activityinfo.server.endpoint.gwtrpc.CommandServlet2;
import org.activityinfo.server.endpoint.gwtrpc.GwtRpcModule;
import org.activityinfo.server.endpoint.gwtrpc.RemoteExecutionContext;
import org.activityinfo.server.util.TemplateModule;
import org.activityinfo.server.util.blob.BlobServiceModuleStub;
import org.activityinfo.server.util.config.ConfigModuleStub;
import org.junit.Before;
import org.junit.BeforeClass;

import javax.persistence.EntityManager;

/**
 * Test fixture for running hibernate-free commands.
 * <p/>
 * The future.
 */
@Modules({
        TestHibernateModule.class,
        TemplateModule.class,
        GwtRpcModule.class,
        AuthenticationModuleStub.class,
        BlobServiceModuleStub.class,
        ConfigModuleStub.class
})
public class CommandTestCase2 {


    @BeforeClass
    public static void setupI18N() {
        LocaleProxy.initialize();
    }

    @Inject
    protected CommandServlet2 servlet;

    @Inject
    protected Injector injector;

    protected void setUser(int userId) {
        AuthenticationModuleStub.setUserId(userId);
    }

    @Before
    public final void setDefaultUser() {
        setUser(1);
    }

    protected <T extends CommandResult> T execute(Command<T> command)
            throws CommandException {

        User user;
        if (AuthenticationModuleStub.getCurrentUser().getUserId() == 0) {
            user = new User();
            user.setName("Anonymous");
            user.setEmail("Anonymous@anonymous");
        } else {
            user = new User();
            user.setId(AuthenticationModuleStub.getCurrentUser().getUserId());
            user.setEmail("foo@foo.com");
            user.setName("Foo Name");
            user.setLocale("en");
        }

        ThreadLocalLocaleProvider.pushLocale(user.getLocaleObject());

        try {


            RemoteExecutionContext context = new RemoteExecutionContext(injector);
            T result = context.startExecute(command);

            // normally each request and so each handleCommand() gets its own
            // EntityManager, but here successive requests in the same test
            // will share an EntityManager, which can be bad if there are
            // collections
            // still living in the first-level cache
            //
            // I think these command tests should ultimately become real end-to-end
            // tests and so would go through the actual servlet process, but for the
            // moment,
            // we'll just add this work aroudn that clears the cache after each
            // command.
            injector.getInstance(EntityManager.class).clear();
            return result;

        } finally {
            ThreadLocalLocaleProvider.popLocale();
        }
    }

    public DispatcherSync getDispatcherSync() {
        return new DispatcherSync() {

            @Override
            public <C extends Command<R>, R extends CommandResult> R execute(
                    C command) {
                try {
                    return CommandTestCase2.this.execute(command);
                } catch (CommandException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public Dispatcher getDispatcher() {
        return new AbstractDispatcher() {

            @Override
            public <T extends CommandResult> void execute(Command<T> command,
                                                          AsyncCallback<T> callback) {
                try {
                    T result = CommandTestCase2.this.execute(command);
                    callback.onSuccess(result);
                } catch(Exception e) {
                    callback.onFailure(e);
                }
            }

        };
    }
}
