package org.activityinfo.server.endpoint.akvo.flow;

final public class QuestionAnswer {
    static final public class Array {
        public QuestionAnswer question_answers[];
    }

    static final public class Single {
        public QuestionAnswer question_answer;
    }

    public Object value;
    public String type;
    public Object oldValue;
    public Integer arbitratyNumber;     // Sic!
    public Integer surveyId;
    public String questionId;
    public String questionText;
    public Long collectionDate;
    public Integer surveyInstanceId;
    public Integer keyId;
    public String textualQuestionId;
    public String questionID;           // Presumably a miscapitalization of questionId
}
