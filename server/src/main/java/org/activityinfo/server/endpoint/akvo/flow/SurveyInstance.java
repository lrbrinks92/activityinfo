package org.activityinfo.server.endpoint.akvo.flow;

final public class SurveyInstance {
    static final public class Array {
        public SurveyInstance survey_instances[];
        public Meta meta;
    }

    static final public class Single {
        public SurveyInstance survey_instance;
    }

    public String surveyCode;
    public String surveyId;
    public String submitterName;
    public String deviceIdentifier;
    public String approximateLocationFlag;
    public Boolean approvedFlag;
    public String userID;
    public Long collectionDate;
    public Object questionAnswersStore;
    public Integer surveyalTime;
    public Integer keyId;
    public String surveyedLocaleIdentifier;
    public String surveyedLocaleId;
    public String surveyedLocaleDisplayName;
}
