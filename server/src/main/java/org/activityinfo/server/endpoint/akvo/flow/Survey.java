package org.activityinfo.server.endpoint.akvo.flow;

final public class Survey {
    static final public class Array {
        public Survey surveys[];
    }

    static final public class Single {
        public Survey survey;
    }

    public String name;
    public String path;
    public String displayName;
    public String version;
    public String description;
    public String code;
    public String sector;
    public String pointType;
    public String defaultLanguageCode;
    public Boolean requireApproval;
    public Long createdDateTime;
    public Long lastUpdateDateTime;
    public Integer sourceId;
    public Integer surveyGroupId;
    public Integer instanceCount;
    public Integer keyId;
    public String status;
    public String questionGroupList;
}
