package org.activityinfo.io.akvo.flow.stub;

import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.simple.container.SimpleServerFactory;
import org.activityinfo.io.akvo.flow.QuestionAnswer;
import org.activityinfo.io.akvo.flow.SurveyInstance;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

import static java.util.logging.Level.SEVERE;

final public class AkvoFlowStub implements Closeable {
    static final private Logger LOGGER = Logger.getLogger(AkvoFlowStub.class.getName());

    final private Closeable closeable;

    public AkvoFlowStub(final int port, File surveyInstanceFile, File questionAnswerFile) throws IOException {
        final HashMap<Integer, SurveyInstance[]> surveyInstances = new HashMap<>();
        final HashMap<Integer, QuestionAnswer[]> questionAnswers = new HashMap<>();
        final ResourceConfig resourceConfig = new DefaultResourceConfig();

        ObjectMapper objectMapper = new ObjectMapper();

        if (surveyInstanceFile != null) {
            for (SurveyInstancePair pair : objectMapper.readValue(surveyInstanceFile, SurveyInstancePair[].class)) {
                surveyInstances.put(pair.surveyId, pair.surveyInstances);
            }
        }

        if (questionAnswerFile != null) {
            for (QuestionAnswerPair pair : objectMapper.readValue(questionAnswerFile, QuestionAnswerPair[].class)) {
                questionAnswers.put(pair.surveyInstanceId, pair.questionAnswers);
            }
        }

        resourceConfig.getSingletons().add(new AkvoFlowStubResource(surveyInstances, questionAnswers));

        closeable = SimpleServerFactory.create(String.format("http://localhost:%d/", port), resourceConfig);
    }

    @Override
    public void close() throws IOException {
        closeable.close();
    }

    public static void main(String... args) {
        final File surveyInstanceFile, questionAnswerFile;

        surveyInstanceFile = args.length > 0 ? new File(args[0]) : null;
        questionAnswerFile = args.length > 1 ? new File(args[1]) : null;

        try (AkvoFlowStub akvoFlowStub = new AkvoFlowStub(7357, surveyInstanceFile, questionAnswerFile)) {
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
