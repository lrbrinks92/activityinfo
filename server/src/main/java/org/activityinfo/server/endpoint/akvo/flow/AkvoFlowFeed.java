package org.activityinfo.server.endpoint.akvo.flow;

import com.google.api.client.util.Maps;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormField;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.model.legacy.CuidAdapter;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.type.enumerated.EnumItem;
import org.activityinfo.model.type.enumerated.EnumType;
import org.activityinfo.model.type.enumerated.EnumValue;
import org.activityinfo.server.command.ResourceLocatorSync;
import org.activityinfo.service.feed.FeedService;

import java.util.Date;
import java.util.Map;

public class AkvoFlowFeed implements FeedService {
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
        ResourceId formClassId = formClass.getId(), timestampId = null;
        Map<String, FormField> formFields = Maps.newHashMap();
        AkvoFlow akvoFlow = initialize(parameters);

        for (FormField formField : getParameterFormClass().getFields()) {
            if ("timestamp".equals(formField.getCode()) && parameters.getString(formField.getId()) != null) {
                timestampId = formField.getId();
            }
        }

        if (timestampId == null) {
            throw new IllegalStateException("ParameterFormClass is invalid");
        }

        for (FormField formField : formClass.getFields()) {
            formFields.put(formField.getCode(), formField);
        }

        for (SurveyInstance instance : akvoFlow.getSurveyInstances(parameters, timestampId)) {
            FormInstance formInstance = new FormInstance(CuidAdapter.newLegacyFormInstanceId(formClassId), formClassId);
            Date start = new Date(instance.collectionDate);
            Date end = new Date(instance.collectionDate + instance.surveyalTime * 1000L);

            formInstance.set(CuidAdapter.field(formClassId, CuidAdapter.START_DATE_FIELD), start);
            formInstance.set(CuidAdapter.field(formClassId, CuidAdapter.END_DATE_FIELD), end);

            for (QuestionAnswer questionAnswer : akvoFlow.getQuestionAnswers(instance.keyId)) {
                FormField formField = formFields.get(questionAnswer.textualQuestionId);

                for (EnumItem enumItem : ((EnumType) formField.getType()).getValues()) {
                    if (enumItem.getLabel().equals(questionAnswer.value)) {
                        formInstance.set(formField.getId(), new EnumValue(enumItem.getId()));
                    }
                }
            }

            locator.persist(formInstance);
        }

        locator.persist(parameters);
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
