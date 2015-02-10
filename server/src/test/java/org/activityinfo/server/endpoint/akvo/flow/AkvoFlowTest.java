package org.activityinfo.server.endpoint.akvo.flow;

import com.google.appengine.api.taskqueue.dev.LocalTaskQueue;
import com.google.appengine.api.taskqueue.dev.QueueStateInfo.TaskStateInfo;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalTaskQueueTestConfig;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import org.activityinfo.io.akvo.flow.stub.AkvoFlowStub;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.model.resource.Resource;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.type.FieldValue;
import org.activityinfo.model.type.enumerated.EnumValue;
import org.activityinfo.model.type.primitive.TextValue;
import org.activityinfo.model.type.time.LocalDate;
import org.activityinfo.server.command.ResourceLocatorSync;
import org.activityinfo.service.lookup.ReferenceChoice;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.io.ByteStreams.toByteArray;
import static com.google.common.io.Resources.getResource;
import static java.net.URLDecoder.decode;
import static org.activityinfo.model.legacy.CuidAdapter.END_DATE_FIELD;
import static org.activityinfo.model.legacy.CuidAdapter.SITE_DOMAIN;
import static org.activityinfo.model.legacy.CuidAdapter.START_DATE_FIELD;
import static org.activityinfo.model.legacy.CuidAdapter.activityFormClass;
import static org.activityinfo.model.legacy.CuidAdapter.attributeGroupField;
import static org.activityinfo.model.legacy.CuidAdapter.attributeId;
import static org.activityinfo.model.legacy.CuidAdapter.field;
import static org.activityinfo.model.legacy.CuidAdapter.indicatorField;
import static org.activityinfo.model.resource.Resources.fromJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

public class AkvoFlowTest {
    static final private String QUEUE = "akvo-fetch";

    private LocalServiceTestHelper helper;

    @Before
    public void setUp() throws Exception {
        final File queue = new File(getResource("queue.xml").toURI());
        final LocalTaskQueueTestConfig queueConfig = new LocalTaskQueueTestConfig().setQueueXmlPath(queue.getPath());
        final LocalDatastoreServiceTestConfig datastoreConfig = new LocalDatastoreServiceTestConfig();

        helper = new LocalServiceTestHelper(queueConfig, datastoreConfig);
        helper.setUp();
    }

    @Test
    public void testAkvoFlow() throws Exception {
        final File answersFile = new File(getResource("answers.json").toURI());
        final File formClassFile = new File(getResource("formClass.json").toURI());
        final File instancesFile = new File(getResource("instances.json").toURI());
        final File parameterFormClassFile = new File(getResource("parameterFormClass.json").toURI());
        final File parameterFormInstanceFile = new File(getResource("parameterFormInstance.json").toURI());

        final Resource formClass;
        final Resource parameterFormClass;
        final Resource parameterFormInstance;

        try (FileInputStream fileInputStream = new FileInputStream(formClassFile)) {
            formClass = fromJson(new String(toByteArray(fileInputStream)));
        }

        try (FileInputStream fileInputStream = new FileInputStream(parameterFormClassFile)) {
            parameterFormClass = fromJson(new String(toByteArray(fileInputStream)));
        }

        try (FileInputStream fileInputStream = new FileInputStream(parameterFormInstanceFile)) {
            parameterFormInstance = fromJson(new String(toByteArray(fileInputStream)));
        }

        try (AkvoFlowStub akvoFlowStub = new AkvoFlowStub(7357, instancesFile, answersFile)) {
            Locator locator = new Locator(parameterFormClass);
            AkvoFlowFeed akvoFlowFeed = new AkvoFlowFeed(parameterFormClass.getId(), locator);

            assertEquals(parameterFormClass, akvoFlowFeed.getParameterFormClass().asResource());
            akvoFlowFeed.updateFeed(FormClass.fromResource(formClass),
                                    FormInstance.fromResource(parameterFormInstance));

            Resource updatedParameterFormInstance = locator.getFormInstance(parameterFormInstance.getId()).asResource();
            LocalTaskQueue localTaskQueue = LocalTaskQueueTestConfig.getLocalTaskQueue();
            TaskStateInfo taskStateInfo = getOnlyElement(localTaskQueue.getQueueStateInfo().get(QUEUE).getTaskInfo());
            String parameters[] = taskStateInfo.getBody().split("&");

            assertNotEquals(parameterFormInstance, updatedParameterFormInstance);
            updatedParameterFormInstance.set(indicatorField(4), TextValue.valueOf("0"));    // Reset timestamp indicator
            assertEquals(parameterFormInstance, updatedParameterFormInstance);
            assertEquals(5, parameters.length);

            for (String parameter : parameters) {
                String components[] = parameter.split("=");
                assertEquals(2, components.length);

                switch (components[0]) {
                    case "formClass":
                        assertEquals(formClass, fromJson(decode(components[1], "UTF-8")));
                        break;
                    case "parameters":
                        Resource decoded = fromJson(decode(components[1], "UTF-8"));
                        assertEquals(locator.getFormInstance(parameterFormInstance.getId()).asResource(), decoded);
                        assertNotEquals(parameterFormInstance, decoded);
                        break;
                    case "id":
                        assertEquals("0", components[1]);
                        break;
                    case "startDate":
                        assertEquals("907600321", components[1]);
                        break;
                    case "endDate":
                        assertEquals("987654321", components[1]);
                        break;
                    default:
                        fail("An invalid parameter was added to the task definition");
                }
            }

            akvoFlowFeed.fetchInstance(FormClass.fromResource(formClass),
                                       FormInstance.fromResource(parameterFormInstance), "0", 907600321L, 987654321L);

            FormInstance formInstance = getOnlyElement(locator.getFormInstances());
            Map<ResourceId, FieldValue> fieldValueMap = formInstance.getFieldValueMap();

            assertEquals(SITE_DOMAIN, formInstance.getId().getDomain());
            assertEquals(activityFormClass(1), formInstance.getOwnerId());
            assertEquals(activityFormClass(1), formInstance.getClassId());
            assertEquals(4, fieldValueMap.size());
            assertEquals(new EnumValue(attributeId(0)), fieldValueMap.get(attributeGroupField(0)));
            assertEquals(new EnumValue(attributeId(5)), fieldValueMap.get(attributeGroupField(1)));
            assertEquals(new LocalDate(1970, 1, 11), fieldValueMap.get(field(activityFormClass(1), START_DATE_FIELD)));
            assertEquals(new LocalDate(1970, 1, 12), fieldValueMap.get(field(activityFormClass(1), END_DATE_FIELD)));
        }
    }

    @After
    public void tearDown() {
        helper.tearDown();
        helper = null;
    }
}

final class Locator implements ResourceLocatorSync {
    final private ResourceId parameterFormClassId;
    final private FormClass parameterFormClass;
    final private Map<ResourceId, FormInstance> formInstances;

    Locator(Resource resource) {
        formInstances = Maps.newHashMap();
        parameterFormClass = FormClass.fromResource(resource);
        parameterFormClassId = resource.getId();
    }

    Iterable<FormInstance> getFormInstances() {
        return Iterables.filter(formInstances.values(), new Predicate<FormInstance>() {
            @Override
            public boolean apply(@Nullable FormInstance input) {
                return input != null && !parameterFormClassId.equals(input.getOwnerId());
            }
        });
    }

    @Override
    public void persist(FormInstance formInstance) {
        formInstances.put(formInstance.getId(), formInstance);
    }

    @Override
    public FormClass getFormClass(ResourceId resourceId) {
        return parameterFormClassId.equals(resourceId) ? parameterFormClass : null;
    }

    @Override
    public FormInstance getFormInstance(ResourceId resourceId) {
        return formInstances.get(resourceId);
    }

    @Override
    public List<ReferenceChoice> getReferenceChoices(Set<ResourceId> range) {
        return null;
    }
}
