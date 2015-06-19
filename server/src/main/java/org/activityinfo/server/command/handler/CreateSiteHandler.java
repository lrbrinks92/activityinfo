package org.activityinfo.server.command.handler;


import com.google.inject.Inject;
import org.activityinfo.legacy.shared.command.CreateSite;
import org.activityinfo.legacy.shared.command.result.CommandResult;
import org.activityinfo.legacy.shared.command.result.CreateResult;
import org.activityinfo.legacy.shared.exception.CommandException;
import org.activityinfo.server.command.handler.crud.PropertyMap;
import org.activityinfo.server.database.hibernate.entity.User;

@SuppressWarnings("unused")
public class CreateSiteHandler implements CommandHandler<CreateSite> {
    
    private SiteUpdate update;

    @Inject
    public CreateSiteHandler(SiteUpdate update) {
        this.update = update;
    }

    @Override
    public CommandResult execute(CreateSite cmd, User user) throws CommandException {

        update.createOrUpdateSite(user, cmd.getSiteId(), new PropertyMap(cmd.getProperties()));
        
        return new CreateResult(cmd.getSiteId());
    }
}
