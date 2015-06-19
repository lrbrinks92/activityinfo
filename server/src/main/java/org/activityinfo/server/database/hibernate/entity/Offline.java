package org.activityinfo.server.database.hibernate.entity;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Offline {
    boolean sync() default true;
}
