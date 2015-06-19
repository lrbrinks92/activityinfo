package org.activityinfo.server.command.handler;

import com.google.inject.Inject;
import org.activityinfo.legacy.shared.command.UpdateSite;
import org.activityinfo.legacy.shared.command.result.CommandResult;
import org.activityinfo.legacy.shared.command.result.VoidResult;
import org.activityinfo.legacy.shared.exception.CommandException;
import org.activityinfo.server.command.handler.crud.PropertyMap;
import org.activityinfo.server.database.hibernate.entity.User;


@SuppressWarnings("unused")
public class UpdateSiteHandler implements CommandHandler<UpdateSite> {
    
    private final SiteUpdate siteUpdate;

    @Inject
    public UpdateSiteHandler(SiteUpdate createSiteHandler) {
        this.siteUpdate = createSiteHandler;
    }

    @Override
    public CommandResult execute(UpdateSite cmd, User user) throws CommandException {
        
        siteUpdate.createOrUpdateSite(user, cmd.getSiteId(), new PropertyMap(cmd.getProperties()));
        
        return VoidResult.EMPTY;
    }
}
