package org.activityinfo.io.akvo.flow.stub;

import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.simple.container.SimpleServerFactory;
import org.activityinfo.io.akvo.flow.QuestionAnswer;
import org.activityinfo.io.akvo.flow.SurveyInstance;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.util.HashMap;
import java.util.logging.Logger;

import static java.util.logging.Level.SEVERE;

final public class AkvoFlowStub {
    static final private Logger LOGGER = Logger.getLogger(AkvoFlowStub.class.getName());

    public static void main(String... args) {
        final HashMap<Integer, SurveyInstance[]> surveyInstances = new HashMap<>();
        final HashMap<Integer, QuestionAnswer[]> questionAnswers = new HashMap<>();
        final ResourceConfig resourceConfig = new DefaultResourceConfig();

        try {
            ObjectMapper objectMapper = new ObjectMapper();

            if (args.length > 0) {
                for (SurveyInstancePair pair : objectMapper.readValue(new File(args[0]), SurveyInstancePair[].class)) {
                    surveyInstances.put(pair.surveyId, pair.surveyInstances);
                }
            }

            if (args.length > 1) {
                for (QuestionAnswerPair pair : objectMapper.readValue(new File(args[1]), QuestionAnswerPair[].class)) {
                    questionAnswers.put(pair.surveyInstanceId, pair.questionAnswers);
                }
            }
        } catch (Throwable throwable) {
            LOGGER.log(SEVERE, "A fatal error occurred inside the Akvo Flow stub web server – exiting now.", throwable);
            System.exit(2);
            return;
        }

        resourceConfig.getSingletons().add(new AkvoFlowStubResource(surveyInstances, questionAnswers));

        try (AutoCloseable autoCloseable = SimpleServerFactory.create("http://localhost:7357/", resourceConfig)) {
            System.in.read();
        } catch (Throwable throwable) {
            LOGGER.log(SEVERE, "A fatal error occurred inside the Akvo Flow stub web server – exiting now.", throwable);
            System.exit(1);
        }
    }

    static final private class SurveyInstancePair {
        public int surveyId;
        public SurveyInstance[] surveyInstances;
    }

    static final private class QuestionAnswerPair {
        public int surveyInstanceId;
        public QuestionAnswer[] questionAnswers;
    }
}
