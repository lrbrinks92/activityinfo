package org.activityinfo.service.feed;

import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormInstance;

public interface FeedService {
    FormClass getParameterFormClass();

    void updateFeed(FormClass formClass, FormInstance parameters);
}
