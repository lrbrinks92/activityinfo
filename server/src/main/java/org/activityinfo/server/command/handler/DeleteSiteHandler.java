package org.activityinfo.server.command.handler;

import com.google.inject.Inject;
import org.activityinfo.legacy.shared.command.DeleteSite;
import org.activityinfo.legacy.shared.command.result.VoidResult;
import org.activityinfo.legacy.shared.exception.CommandException;
import org.activityinfo.server.database.hibernate.entity.Site;
import org.activityinfo.server.database.hibernate.entity.SiteHistory;
import org.activityinfo.server.database.hibernate.entity.User;
import org.json.JSONObject;

import javax.persistence.EntityManager;
import java.util.Date;

@SuppressWarnings("unused")
public class DeleteSiteHandler implements CommandHandler<DeleteSite> {
    
    private final EntityManager entityManager;
    private final PermissionOracle permissionOracle;

    @Inject
    public DeleteSiteHandler(EntityManager entityManager, PermissionOracle permissionOracle) {
        this.entityManager = entityManager;
        this.permissionOracle = permissionOracle;
    }

    @Override
    public VoidResult execute(DeleteSite cmd, User user) throws CommandException {

        Site site = entityManager.find(Site.class, cmd.getSiteId());
        
        permissionOracle.assertEditAllowed(site, user);
        
        site.setDateDeleted(new Date());
        site.setVersion(site.getActivity().incrementSiteVersion());

        logHistory(user, site);
        
        return VoidResult.EMPTY;
    }

    private void logHistory(User user, Site site) {
        try {
            JSONObject change = new JSONObject();
            change.put("type", "Boolean");
            change.put("value", true);

            JSONObject changeSet = new JSONObject();
            changeSet.put("_DELETE", change);

            SiteHistory history = new SiteHistory();
            history.setUser(user);
            history.setSite(site);
            history.setInitial(false);
            history.setTimeCreated(System.currentTimeMillis());
            history.setJson(changeSet.toString());
            entityManager.persist(history);
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
