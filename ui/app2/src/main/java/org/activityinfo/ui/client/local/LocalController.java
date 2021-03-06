package org.activityinfo.ui.client.local;

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

import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.activityinfo.i18n.shared.UiConstants;
import org.activityinfo.legacy.shared.Log;
import org.activityinfo.legacy.shared.command.Command;
import org.activityinfo.legacy.shared.command.result.CommandResult;
import org.activityinfo.ui.client.AppEvents;
import org.activityinfo.ui.client.EventBus;
import org.activityinfo.ui.client.dispatch.Dispatcher;
import org.activityinfo.ui.client.dispatch.remote.AbstractDispatcher;
import org.activityinfo.ui.client.dispatch.remote.Remote;
import org.activityinfo.ui.client.inject.ClientSideAuthProvider;
import org.activityinfo.ui.client.local.LocalStateChangeEvent.State;
import org.activityinfo.ui.client.local.capability.LocalCapabilityProfile;
import org.activityinfo.ui.client.local.capability.PermissionRefusedException;
import org.activityinfo.ui.client.local.sync.*;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * This class keeps as much of the offline functionality behind a runAsync
 * clause to defer downloading the related JavaScript until the user actually
 * goes into offline mode.
 */
@Singleton
public class LocalController extends AbstractDispatcher {

    private final EventBus eventBus;
    private final Provider<Synchronizer> synchronizerProvider;
    private UiConstants uiConstants;
    private final Dispatcher remoteDispatcher;
    private final LocalCapabilityProfile capabilityProfile;
    private final Provider<SyncHistoryTable> historyTable;

    private Strategy activeStrategy;
    private Date lastSynced = null;

    @Inject
    public LocalController(EventBus eventBus,
                           @Remote Dispatcher remoteDispatcher,
                           Provider<Synchronizer> gateway,
                           LocalCapabilityProfile capabilityProfile,
                           UiConstants uiConstants,
                           Provider<SyncHistoryTable> historyTable) {
        this.eventBus = eventBus;
        this.remoteDispatcher = remoteDispatcher;
        this.synchronizerProvider = gateway;
        this.capabilityProfile = capabilityProfile;
        this.uiConstants = uiConstants;
        this.historyTable = historyTable;

        Log.trace("OfflineManager: starting");

        if (capabilityProfile.isOfflineModeSupported()) {
            activateStrategy(new LoadingLocalStrategy());
        } else {
            activateStrategy(new NotInstalledStrategy());
        }

        eventBus.addListener(AppEvents.INIT, new Listener<BaseEvent>() {

            @Override
            public void handleEvent(BaseEvent be) {
                fireStatus();
            }
        });
    }

    public Date getLastSyncTime() {
        return lastSynced;
    }

    public void install() {
        if (activeStrategy instanceof NotInstalledStrategy) {
            ((NotInstalledStrategy) activeStrategy).enableOffline();
        }
    }

    public void synchronize() {
        if (activeStrategy instanceof LocalStrategy) {
            ((LocalStrategy) activeStrategy).synchronize();
        }
    }

    public State getState() {
        return activeStrategy.getState();
    }


    @Override
    public <T extends CommandResult> void execute(Command<T> command, AsyncCallback<T> callback) {
        activeStrategy.dispatch(command, callback);
    }

    private void activateStrategy(Strategy strategy) {
        try {
            this.activeStrategy = strategy;
            this.activeStrategy.activate();
            fireStatus();

        } catch (Exception caught) {
            // errors really ought to be handled by the strategy that is passing
            // control to us
            // but we can't afford to let an uncaught exception go as it could
            // leave the app
            // in a state of limbo
            Log.error("Uncaught exception while activatign strategy, defaulting to Not INstalled");
            activateStrategy(new NotInstalledStrategy());
        }
    }

    private void fireStatus() {
        eventBus.fireEvent(new LocalStateChangeEvent(this.activeStrategy.getState()));
    }

    private void loadSynchronizerImpl(final AsyncCallback<Synchronizer> callback) {
        Log.trace("loadSynchronizerImpl() starting...");
        GWT.runAsync(new RunAsyncCallback() {
            @Override
            public void onFailure(Throwable throwable) {
                Log.trace("loadSynchronizerImpl() failed");
                callback.onFailure(throwable);
            }

            @Override
            public void onSuccess() {
                Log.trace("loadSynchronizerImpl() succeeded");

                Synchronizer impl = null;
                try {
                    impl = synchronizerProvider.get();
                } catch (Exception caught) {
                    Log.error("SynchronizationImpl constructor threw exception", caught);
                    callback.onFailure(caught);
                    return;
                }
                callback.onSuccess(impl);
            }
        });
    }

    private void reportFailure(Throwable throwable) {
        Log.error("Exception in offline controller", throwable);

        eventBus.fireEvent(new SyncErrorEvent(SyncErrorType.fromException(throwable)));
    }

    private abstract class Strategy {
        Strategy activate() {
            return this;
        }

        void dispatch(Command command, AsyncCallback callback) {
            // by default, we send to the server
            remoteDispatcher.execute(command, callback);
        }

        abstract State getState();
    }

    /**
     * Strategy for handling the state in which offline mode is not at all
     * available.
     * <p/>
     * The only thing the user can do from here is start installation.
     */
    private class NotInstalledStrategy extends Strategy {

        @Override
        public NotInstalledStrategy activate() {
            return this;
        }

        @Override State getState() {
            return State.UNINSTALLED;
        }

        public void enableOffline() {
            Log.trace("enablingOffline() started");
            capabilityProfile.acquirePermission(new AsyncCallback<Void>() {

                @Override
                public void onSuccess(Void result) {
                    activateStrategy(new InstallingStrategy());
                }

                @Override
                public void onFailure(Throwable caught) {
                    if (!(caught instanceof PermissionRefusedException)) {
                        reportFailure(caught);
                    }
                }
            });
        }
    }

    /**
     * Strategy for handling the state in which installation is in progress.
     * Commands continue to be handled by the remote dispatcher during
     * installation
     */
    private class InstallingStrategy extends Strategy {

        @Override State getState() {
            return State.INSTALLING;
        }

        @Override Strategy activate() {
            eventBus.fireEvent(new SyncStatusEvent(uiConstants.starting(), 0));

            loadSynchronizerImpl(new AsyncCallback<Synchronizer>() {
                @Override
                public void onFailure(Throwable caught) {
                    activateStrategy(new NotInstalledStrategy());
                    reportFailure(caught);
                }

                @Override
                public void onSuccess(final Synchronizer gateway) {
                    gateway.install(new AsyncCallback<Void>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            activateStrategy(new NotInstalledStrategy());
                            LocalController.this.reportFailure(caught);
                        }

                        @Override
                        public void onSuccess(Void result) {
                            activateStrategy(new LocalStrategy(gateway));
                        }
                    });
                }
            });
            return this;
        }
    }

    /**
     * This is a sort of purgatory state that occurs immediately after while
     * we're determining whether offline mode has been enabled and then if so,
     * while we'ere loading the offline module async fragment.
     */
    private class LoadingLocalStrategy extends Strategy {

        /**
         * Commands cannot be executed until everything is loaded...
         */
        private List<CommandRequest> pendingRequests;

        @Override State getState() {
            return State.CHECKING;
        }

        @Override Strategy activate() {
            pendingRequests = new ArrayList<CommandRequest>();
            try {
                historyTable.get().get(new AsyncCallback<Date>() {

                    @Override
                    public void onSuccess(Date result) {
                        loadModule();
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        abandonShip();
                    }
                });
            } catch (Exception e) {
                abandonShip();
            }
            return this;
        }

        private void loadModule() {
            loadSynchronizerImpl(new AsyncCallback<Synchronizer>() {
                @Override
                public void onFailure(Throwable caught) {
                    abandonShip(caught);
                }

                @Override
                public void onSuccess(final Synchronizer gateway) {

                    gateway.validateOfflineInstalled(new AsyncCallback<Void>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            abandonShip(caught);
                        }

                        @Override
                        public void onSuccess(Void result) {
                            activateStrategy(new LocalStrategy(gateway));
                            doDispatch(pendingRequests);
                        }
                    });
                }
            });
        }

        @Override void dispatch(Command command, AsyncCallback callback) {
            pendingRequests.add(new CommandRequest(command, callback));
        }

        void abandonShip(Throwable caught) {
            reportFailure(caught);
            abandonShip();
        }

        // something went wrong while loading the async fragment or
        // in the boot up, revert to the uninstalled state. the user
        // can always reinstall. (not ideal, obviously)
        void abandonShip() {
            activateStrategy(new NotInstalledStrategy());
            doDispatch(pendingRequests);
        }
    }

    /**
     * Strategy for handling the state during which the user is offline. We try
     * to handle commands locally if possible. When unsupported commands are
     * encountered, we offer the user the chance to connect.
     */
    private final class LocalStrategy extends Strategy {
        private Synchronizer localManager;

        private LocalStrategy(Synchronizer localManager) {
            this.localManager = localManager;
        }

        public void synchronize() {
            localManager.synchronize();
        }

        @Override State getState() {
            return State.INSTALLED;
        }

        @Override
        public LocalStrategy activate() {

            // ensure that's the user's authentication is persisted across sessions!
            ClientSideAuthProvider.persistAuthentication();

            localManager.getLastSyncTime(new AsyncCallback<Date>() {

                @Override
                public void onSuccess(Date result) {
                    lastSynced = result;
                    eventBus.fireEvent(new SyncCompleteEvent(result));

                    // do an initial synchronization attempt
                    localManager.synchronize();
                }

                @Override
                public void onFailure(Throwable caught) {
                    localManager.synchronize();
                }
            });
            return this;
        }

        @Override void dispatch(Command command, AsyncCallback callback) {

            localManager.execute(command, callback);
        }
    }

    private static class CommandRequest {
        private final Command command;
        private final AsyncCallback callback;

        public CommandRequest(Command command, AsyncCallback callback) {
            super();
            this.command = command;
            this.callback = callback;
        }

        public void dispatch(Strategy strategy) {
            strategy.dispatch(command, callback);
        }
    }

    private void doDispatch(final Collection<CommandRequest> requests) {
        if (!requests.isEmpty()) {
            // wait until everything's been switched around
            Scheduler.get().scheduleDeferred(new ScheduledCommand() {

                @Override
                public void execute() {
                    for (CommandRequest request : requests) {
                        request.dispatch(activeStrategy);
                    }
                }
            });
        }
    }
}
