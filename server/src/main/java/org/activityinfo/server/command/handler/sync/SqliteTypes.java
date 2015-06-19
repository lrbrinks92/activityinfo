package org.activityinfo.server.command.handler.sync;

import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Map;


public final class SqliteTypes {

    private static final String TEXT = "TEXT";
    private static final String INT = "INT";
    private static final String REAL = "REAL";

    private SqliteTypes() {}

    private static final Map<Class, String> MAP = new HashMap<>();

    static {
        MAP.put(String.class, TEXT);
        MAP.put(Character.class, TEXT);
        MAP.put(Character.TYPE, TEXT);
        
        MAP.put(Integer.TYPE, INT);
        MAP.put(Integer.class, INT);
        MAP.put(Short.class, INT);
        MAP.put(Short.TYPE, INT);
        MAP.put(Long.class, INT);
        MAP.put(Long.TYPE, INT);
        MAP.put(Byte.class, INT);
        MAP.put(Byte.TYPE, INT);
        MAP.put(Boolean.class, INT);
        MAP.put(Boolean.TYPE, INT);
        
        MAP.put(Float.class, REAL);
        MAP.put(Float.TYPE, REAL);
        MAP.put(Double.class, REAL);
        MAP.put(Double.TYPE, REAL);
    }
    
    public static String getSqliteType(Class javaClass) {
        Preconditions.checkArgument(MAP.containsKey(javaClass), 
                "No sqlite type mapping for java class %s", javaClass.getName());
        
        return MAP.get(javaClass);
    }
}


