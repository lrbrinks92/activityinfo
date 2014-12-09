package org.activityinfo.store.hrd.dao;

import org.activityinfo.model.auth.AuthenticatedUser;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.store.hrd.StoreContext;
import org.activityinfo.store.hrd.auth.Authorization;
import org.activityinfo.store.hrd.auth.WorkspaceAuthDAO;
import org.activityinfo.store.hrd.cache.CommitStatusCache;
import org.activityinfo.store.hrd.entity.workspace.WorkspaceEntityGroup;
import org.activityinfo.store.hrd.tx.ReadTx;

public class ReadContext {
    private StoreContext store;
    private ReadTx tx;
    private WorkspaceEntityGroup workspace;
    private AuthenticatedUser user;
    private CommitStatusCache.TransactionLevel commitStatusCache;
    private WorkspaceAuthDAO authDAO;

    public ReadContext(StoreContext store, WorkspaceEntityGroup workspace, AuthenticatedUser user, ReadTx tx) {
        this.store = store;
        this.workspace = workspace;
        this.user = user;
        this.tx = tx;

        this.commitStatusCache = store.getCommitStatusCache().begin(workspace, tx);
        this.authDAO = new WorkspaceAuthDAO(workspace, user, tx);
    }

    public AuthenticatedUser getUser() {
        return user;
    }

    public StoreContext getStore() {
        return store;
    }

    public ReadTx getTx() {
        return tx;
    }

    public CommitStatusCache.TransactionLevel getCommitStatusCache() {
        return commitStatusCache;
    }

    public WorkspaceEntityGroup getWorkspace() {
        return workspace;
    }

    public Authorization authorizationFor(ResourceId id) {
        return authDAO.forResource(id);
    }
}