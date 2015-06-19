package org.activityinfo.server.command.handler;

import com.google.apphosting.api.ApiProxy;
import com.google.inject.Inject;
import org.activityinfo.legacy.shared.command.BatchCommand;
import org.activityinfo.legacy.shared.command.Command;
import org.activityinfo.legacy.shared.command.result.BatchResult;
import org.activityinfo.legacy.shared.command.result.CommandResult;
import org.activityinfo.legacy.shared.exception.CommandException;
import org.activityinfo.server.command.DispatcherSync;
import org.activityinfo.server.database.hibernate.entity.User;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class BatchCommandHandler implements CommandHandler<BatchCommand> {

    private static final long BUFFER_MILLISECONDS = 10_000;
    
    private final DispatcherSync dispatcher;


    @Inject
    public BatchCommandHandler(DispatcherSync dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public CommandResult execute(BatchCommand cmd, User user) throws CommandException {

        List<CommandResult> results = new ArrayList<>(cmd.getCommands().size());
        
        for(Command command : cmd.getCommands()) {
            results.add(dispatcher.execute(command));
            
            if(timeRemaining() < BUFFER_MILLISECONDS) {
                throw new CommandException("No time remaining to complete batch");
            }
        }

        return new BatchResult(results);
    }

    private long timeRemaining() {
        if(ApiProxy.getCurrentEnvironment() == null) {
            return Long.MAX_VALUE;
        } else {
            return ApiProxy.getCurrentEnvironment().getRemainingMillis();
        }
    }
}
