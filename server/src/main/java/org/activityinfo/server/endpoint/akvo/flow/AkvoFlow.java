package org.activityinfo.server.endpoint.akvo.flow;

import com.google.api.client.util.Base64;
import com.google.api.client.util.Lists;
import com.google.common.base.Optional;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import org.activityinfo.io.akvo.flow.QuestionAnswer;
import org.activityinfo.io.akvo.flow.SurveyInstance;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.model.resource.ResourceId;
import org.codehaus.jackson.map.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static com.google.common.base.Optional.of;
import static java.lang.String.format;
import static org.codehaus.jackson.map.DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES;

public class AkvoFlow {
    
    private static final Logger LOGGER = Logger.getLogger(AkvoFlow.class.getName());
    
    static final private String ALGORITHM = "HmacSHA1";

    final private String server;
    final private String access;
    final private Mac mac;
    final private int survey;

    public AkvoFlow(String server, String access, String secret, String survey) {
        try {
            this.server = server;
            this.access = access;

            mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(), ALGORITHM));

            this.survey = Integer.parseInt(survey, 10);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public QuestionAnswer[] getQuestionAnswers(int id) {
        return get("question_answers", of("surveyInstanceId=" + id), QuestionAnswer.Array.class).question_answers;
    }

    public SurveyInstance[] getSurveyInstances(FormInstance parameters, ResourceId timestampId) {
        String timestamp = parameters.getString(timestampId);
        long begin = Long.valueOf(timestamp);
        long end = System.currentTimeMillis();
        List<SurveyInstance> surveyInstances = Lists.newArrayList();
        SurveyInstance.Array array = null;

        LOGGER.info(format("Fetching survey instances in range [%d, %d]", begin, end));
        
        do {
            final String since;

            if (array != null && array.meta != null && array.meta.since != null) {
                since = "&since=" + array.meta.since;
            } else {
                since = "";
            }

            array = get("survey_instances", of("surveyId=" + survey + "&beginDate=" + begin + "&endDate=" + end + since)
                    , SurveyInstance.Array.class);
        } while (surveyInstances.addAll(Arrays.asList(array.survey_instances)));

        SurveyInstance[] value = surveyInstances.toArray(new SurveyInstance[surveyInstances.size()]);

        parameters.set(timestampId, String.valueOf(end));
        return value;
    }

    private <T> T get(String location, Optional<String> parameters, Class<T> type) {
        final Client client = Client.create();

        try {
            String resource = "/api/v1/" + location;
            String url = "http://" + server + resource;
            String date = String.valueOf(System.currentTimeMillis() / 1000);
            String plaintext = format("%s\n%s\n%s", "GET", date, resource);
            String signature = Base64.encodeBase64String(mac.doFinal(plaintext.getBytes()));
            WebResource webResource = client.resource(parameters.isPresent() ? url + "?" + parameters.get() : url);

            return new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false).readValue(webResource
                    .header("Date", date)
                    .header("Authorization", format("%s:%s", access, signature))
                    .get(String.class), type);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            client.destroy();
        }
    }
}
