package org.activityinfo.server.endpoint.akvo.flow;

import com.google.api.client.util.Maps;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import org.activityinfo.io.akvo.flow.QuestionAnswer;
import org.activityinfo.io.akvo.flow.SurveyInstance;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormField;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.model.legacy.CuidAdapter;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.type.enumerated.EnumItem;
import org.activityinfo.model.type.enumerated.EnumType;
import org.activityinfo.model.type.enumerated.EnumValue;
import org.activityinfo.model.type.primitive.TextType;
import org.activityinfo.model.type.primitive.TextValue;
import org.activityinfo.server.command.ResourceLocatorSync;
import org.activityinfo.service.feed.FeedService;

import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;

import static com.google.appengine.api.taskqueue.QueueFactory.getQueue;
import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;
import static org.activityinfo.model.resource.Resources.toJson;

public class AkvoFlowFeed implements FeedService {
    
    private static final Logger LOGGER = Logger.getLogger(AkvoFlowFeed.class.getName());
    
    static final public String KIND = "AkvoFlowSurveyId";
    static final private DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

    final private ResourceId parameterFormClassId;
    final private ResourceLocatorSync locator;

    public AkvoFlowFeed(ResourceId parameterFormClassId, ResourceLocatorSync locator) {
        this.parameterFormClassId = parameterFormClassId;
        this.locator = locator;
    }

    @Override
    public FormClass getParameterFormClass() {
        return locator.getFormClass(parameterFormClassId);
    }

    @Override
    public void updateFeed(FormClass formClass, FormInstance parameters) {
        ResourceId timestampId = null;
        AkvoFlow akvoFlow = initialize(parameters);

        for (FormField formField : getParameterFormClass().getFields()) {
            if ("timestamp".equals(formField.getCode()) && parameters.getString(formField.getId()) != null) {
                timestampId = formField.getId();
            }
        }

        if (timestampId == null) {
            throw new IllegalStateException("ParameterFormClass is invalid");
        }


        for (SurveyInstance instance : akvoFlow.getSurveyInstances(parameters, timestampId)) {
            LOGGER.info("Found new survey instance " + instance.keyId);
            
            getQueue("akvo-fetch").add(withUrl("/tasks/fetch")
                    .param("formClass", toJson(formClass))
                    .param("parameters", toJson(parameters))
                    .param("id", String.valueOf(instance.keyId))
                    .param("startDate", String.valueOf(instance.collectionDate))
                    .param("endDate", String.valueOf(instance.collectionDate + instance.surveyalTime * 1000L)));
        }

        locator.persist(parameters);
    }

    @Override
    public void fetchInstance(FormClass formClass, FormInstance parameters, String id, long startDate, long endDate) {
        Key key = KeyFactory.createKey(KIND, id);

        try {
            datastoreService.get(key);
        } catch (EntityNotFoundException e) {
            ResourceId formClassId = formClass.getId();
            Map<String, FormField> formFields = Maps.newHashMap();
            AkvoFlow akvoFlow = initialize(parameters);
            FormInstance formInstance = new FormInstance(CuidAdapter.newLegacyFormInstanceId(formClassId), formClassId);

            formInstance.set(CuidAdapter.field(formClassId, CuidAdapter.START_DATE_FIELD), new Date(startDate));
            formInstance.set(CuidAdapter.field(formClassId, CuidAdapter.END_DATE_FIELD), new Date(endDate));

            for (FormField formField : formClass.getFields()) {
                formFields.put(formField.getCode(), formField);
            }

            for (QuestionAnswer questionAnswer : akvoFlow.getQuestionAnswers(Integer.parseInt(id, 10))) {
                String questionId = questionAnswer.textualQuestionId;
                if(questionId.endsWith("_copy")) {
                    questionId = questionId.substring(0, questionId.length() - "_copy".length());
                }
                FormField formField = formFields.get(questionId);

                if (formField == null) {
                    LOGGER.severe("Can't get " + questionId);
                } else if ((formField.getType() instanceof EnumType)) {
                    for (EnumItem enumItem : ((EnumType) formField.getType()).getValues()) {
                        if (enumItem.getLabel().equals(questionAnswer.value)) {
                            formInstance.set(formField.getId(), new EnumValue(enumItem.getId()));
                        }
                    }
                } else if (formField.getType() instanceof TextType) {
                    formInstance.set(formField.getId(), TextValue.valueOf((String)questionAnswer.value));
                    
                } else {
                    LOGGER.severe("Field " + formField.getCode() + " is not an EnumField");
                    
                }
            }

            Entity entity = new Entity(key);
            locator.persist(formInstance);
            datastoreService.put(null, entity);
        }
    }

    private AkvoFlow initialize(FormInstance parameters) {
        ResourceId access = null, secret = null, server = null, survey = null;

        for (FormField formField : getParameterFormClass().getFields()) {
            if (formField.getCode() == null) continue;

            switch (formField.getCode()) {
                case "access":
                    access = formField.getId();
                    break;
                case "secret":
                    secret = formField.getId();
                    break;
                case "server":
                    server = formField.getId();
                    break;
                case "survey":
                    survey = formField.getId();
                    break;
            }
        }

        if (access == null || secret == null || server == null || survey == null) {
            throw new IllegalStateException("ParameterFormClass is invalid");
        }

        return new AkvoFlow(parameters.getString(server), parameters.getString(access),
                            parameters.getString(secret), parameters.getString(survey));
    }
}
