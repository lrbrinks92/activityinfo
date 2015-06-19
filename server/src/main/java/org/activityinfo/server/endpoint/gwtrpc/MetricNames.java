package org.activityinfo.server.endpoint.gwtrpc;

import org.activityinfo.legacy.shared.command.BatchCommand;
import org.activityinfo.legacy.shared.command.Command;


public class MetricNames {
    
    public static String commandNameMetricName(Class<? extends Command> commandClass) {
        if(commandClass.equals(BatchCommand.class)) {
            return "batch";
        }
        
        String name = commandClass.getSimpleName();

        StringBuilder metricName = new StringBuilder();
        int i = 0;
        metricName.append(Character.toLowerCase(name.charAt(i++)));
        while(i < name.length()) {
            char c = name.charAt(i);
            if(Character.isUpperCase(c)) {
                metricName.append('.').append(Character.toLowerCase(c));
            } else {
                metricName.append(c);
            }
            i++;
        }
        return metricName.toString();
    }
}
