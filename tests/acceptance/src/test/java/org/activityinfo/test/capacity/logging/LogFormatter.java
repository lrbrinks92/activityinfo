package org.activityinfo.test.capacity.logging;

import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;

import java.util.concurrent.TimeUnit;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;


public class LogFormatter extends Formatter {

    public static final int NAME_WIDTH = 25;
    public static final String DEFAULT_PREFIX = "org.activityinfo.test";
    private final Stopwatch stopwatch = Stopwatch.createStarted();

    @Override
    public String format(LogRecord record) {
        return String.format("%6.2f %6s %s %s\n", 
                stopwatch.elapsed(TimeUnit.SECONDS) / 60d,
                record.getLevel(),
                abbreviate(record.getLoggerName()),
                record.getMessage());
        
    }

    private String abbreviate(String loggerName) {
        
        if(loggerName.startsWith(DEFAULT_PREFIX)) {
            loggerName = loggerName.substring(DEFAULT_PREFIX.length()+1);
        }
        
        if(loggerName.length() < NAME_WIDTH) {
            return Strings.padEnd(loggerName, NAME_WIDTH, ' ');
        } else {
            StringBuilder name = new StringBuilder();
            boolean startOfPackage = true;
            int nameStart = loggerName.lastIndexOf('.');
            for(int i=0;i<loggerName.length();++i) {
                char c = loggerName.charAt(i);
                if(c == '.') {
                    startOfPackage = true;
                    name.append('.');
                    
                } else if(i > nameStart || startOfPackage) {
                    name.append(c);
                    startOfPackage = false;
                }
            }
            if(name.length() < NAME_WIDTH) {
                return Strings.padEnd(name.toString(), NAME_WIDTH, ' ');
            } else {
                return name.substring(name.length()-NAME_WIDTH);
            }
        }
    }
}
