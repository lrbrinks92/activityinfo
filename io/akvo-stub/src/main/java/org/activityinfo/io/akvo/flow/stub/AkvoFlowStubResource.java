package org.activityinfo.io.akvo.flow.stub;

import org.activityinfo.io.akvo.flow.Meta;
import org.activityinfo.io.akvo.flow.QuestionAnswer;
import org.activityinfo.io.akvo.flow.SurveyInstance;
import org.codehaus.jackson.map.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.DatatypeConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

@Path("/api/v1")
public class AkvoFlowStubResource {
    static final private String ACCESS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    static final private String ALGORITHM = "HmacSHA1";
    static final private String SECRET = "MkIQZEtDJdylOAjQ6tw1pIISSjNvU7MqPN0OCTKZ4jaeNA1O6o4hDQ8uuEK9dNRF/RS9bdgVj8+j";
    static final private String SINCE = "Now";

    final private Mac mac;
    final private Map<Integer, SurveyInstance[]> surveyInstances;
    final private Map<Integer, QuestionAnswer[]> questionAnswers;

    public AkvoFlowStubResource(Map<Integer, SurveyInstance[]> instances, Map<Integer, QuestionAnswer[]> answers) {
        surveyInstances = instances;
        questionAnswers = answers;

        try {
            mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(SECRET.getBytes(), ALGORITHM));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/survey_instances")
    public Response getSurveyInstances(@Context UriInfo uriInfo,
                                       @HeaderParam("Date") String date,
                                       @HeaderParam("Authorization") String authorization,
                                       @QueryParam("since") String since,
                                       @QueryParam("beginDate") long beginDate,
                                       @QueryParam("endDate") long endDate,
                                       @QueryParam("surveyId") int surveyId) {
        assertAuthorized(uriInfo, date, authorization);

        SurveyInstance.Array array = new SurveyInstance.Array();
        List<SurveyInstance> list = new ArrayList<>();
        array.meta = new Meta();
        array.meta.since = SINCE;

        if (!SINCE.equals(since) && surveyInstances.containsKey(surveyId)) {
            for (SurveyInstance surveyInstance : surveyInstances.get(surveyId)) {
                if (showSurveyInstance(surveyInstance, beginDate, endDate)) {
                    list.add(surveyInstance);
                }
            }
        }

        array.survey_instances = list.toArray(new SurveyInstance[list.size()]);

        try {
            return Response.ok(new ObjectMapper().writeValueAsString(array)).build();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Path("/question_answers")
    public Response getQuestionAnswers(@Context UriInfo uriInfo,
                                       @HeaderParam("Date") String date,
                                       @HeaderParam("Authorization") String authorization,
                                       @QueryParam("surveyInstanceId") int surveyInstanceId) {
        assertAuthorized(uriInfo, date, authorization);

        QuestionAnswer.Array array = new QuestionAnswer.Array();

        array.question_answers = questionAnswers.containsKey(surveyInstanceId) ?
                questionAnswers.get(surveyInstanceId) : new QuestionAnswer[0];

        try {
            return Response.ok(new ObjectMapper().writeValueAsString(array)).build();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    private void assertAuthorized(UriInfo uriInfo, String date, String authorization) {
        final String uri;

        if (uriInfo == null || uriInfo.getPath() == null || uriInfo.getPath().length() == 0) {
            uri = "";
        } else if (uriInfo.getPath().charAt(0) == '/') {
            uri = uriInfo.getPath();
        } else {
            uri = '/' + uriInfo.getPath();
        }

        String plaintext = String.format("GET\n%s\n%s", date, uri);
        String signature = DatatypeConverter.printBase64Binary(mac.doFinal(plaintext.getBytes()));

        if (date != null && authorization != null) {
            if (authorization.equals(ACCESS + ':' + signature)) {
                return;
            }
        }

        throw new WebApplicationException(UNAUTHORIZED);
    }

    private boolean showSurveyInstance(SurveyInstance surveyInstance, long beginDate, long endDate) {
        final long date = surveyInstance.collectionDate + surveyInstance.surveyalTime * 1000L;
        return beginDate <= date && date < endDate;
    }
}
