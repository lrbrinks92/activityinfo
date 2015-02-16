package org.activityinfo.service.guid;

import com.google.inject.ImplementedBy;

@ImplementedBy(SiteIdGuidServiceImpl.class)
public interface SiteIdGuidService {
    int getSiteId(int activityId, String guid);
}
