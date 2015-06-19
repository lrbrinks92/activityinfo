package org.activityinfo.server.endpoint.export;

import com.google.common.base.Preconditions;
import org.activityinfo.legacy.shared.command.Command;
import org.activityinfo.legacy.shared.command.result.CommandResult;
import org.activityinfo.legacy.shared.exception.CommandException;
import org.activityinfo.server.command.DispatcherSync;
import org.activityinfo.server.generated.StorageProvider;


public class TaskContext implements DispatcherSync {
    
    private final DispatcherSync dispatcherSync;
    private final StorageProvider storageProvider;
    private final String taskId;

    public TaskContext(DispatcherSync dispatcherSync, StorageProvider storageProvider, String taskId) {
        this.dispatcherSync = dispatcherSync;
        this.storageProvider = storageProvider;
        this.taskId = taskId;
    }

    public void updateProgress(double percentComplete) {
        Preconditions.checkArgument(percentComplete >= 0 && percentComplete <= 1.0d, 
                "percentComplete must be in the range [0, 1], was: %f", percentComplete);
    
        storageProvider.get(taskId).updateProgress(percentComplete);
    }

    @Override
    public <C extends Command<R>, R extends CommandResult> R execute(C command) throws CommandException {
        return dispatcherSync.execute(command);
    }
}
