package org.activityinfo.server.endpoint.ona;

import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.base.Preconditions;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormField;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.model.legacy.CuidAdapter;
import org.activityinfo.model.legacy.KeyGenerator;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.type.FieldValue;
import org.activityinfo.model.type.ReferenceValue;
import org.activityinfo.model.type.geo.GeoPoint;
import org.activityinfo.model.type.time.LocalDate;
import org.activityinfo.server.endpoint.odk.FieldValueParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import static com.google.appengine.api.taskqueue.QueueFactory.getQueue;
import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;
import static org.activityinfo.model.legacy.CuidAdapter.END_DATE_FIELD;
import static org.activityinfo.model.legacy.CuidAdapter.LOCATION_FIELD;
import static org.activityinfo.model.legacy.CuidAdapter.START_DATE_FIELD;
import static org.activityinfo.model.legacy.CuidAdapter.field;
import static org.activityinfo.model.legacy.CuidAdapter.locationInstanceId;
import static org.activityinfo.model.resource.Resources.toJson;
import static org.activityinfo.server.endpoint.odk.FieldValueParserFactory.fromFieldType;

public class XFormInstanceReader {
    static final private String UUID = "_uuid";
    static final private String START = "start";
    static final private String END = "end";

    public static void build(LinkedHashMap<String, Object> array[],
                             FormClass formClass,
                             int locationTypeId,
                             int userId) {
        Preconditions.checkNotNull(array, formClass);
        final ResourceId formClassId = formClass.getId();
        final ArrayList<FormField> formFields = new ArrayList<>(formClass.getFields());

        for (int i = 0; i < array.length; i++) {
            final Object instanceId = array[i].get(UUID);
            final Object start = array[i].get(START);
            final Object end = array[i].get(END);
            final ResourceId formInstanceId = CuidAdapter.newLegacyFormInstanceId(formClassId);
            final FormInstance formInstance = new FormInstance(formInstanceId, formClassId);
            Integer locationId = null;
            Double latitude = null;
            Double longitude = null;

            if (!(instanceId instanceof String)) throw new IllegalStateException("Invalid uuid");

            if (start instanceof String) {
                String date[] = ((String) start).split("T");
                formInstance.set(field(formClassId, START_DATE_FIELD), LocalDate.parse(date[0]));
            } else {
                formInstance.set(field(formClassId, START_DATE_FIELD), new LocalDate());
            }

            if (end instanceof String) {
                String date[] = ((String) end).split("T");
                formInstance.set(field(formClassId, END_DATE_FIELD), LocalDate.parse(date[0]));
            } else {
                formInstance.set(field(formClassId, END_DATE_FIELD), new LocalDate());
            }

            for (FormField formField : formFields) {
                final FieldValueParser fieldValueParser = fromFieldType(formField.getType(), false, false);
                final String code = formField.getCode();

                final LinkedHashMap<String, Object> map = array[i];

                if (map != null) {
                    final Object value = map.get(code);

                    if (value instanceof String) {
                        final FieldValue fieldValue = fieldValueParser.parse((String) value);

                        if (fieldValue instanceof GeoPoint) {
                            locationId = new KeyGenerator().generateInt();
                            ReferenceValue referenceValue = new ReferenceValue(locationInstanceId(locationId));
                            ResourceId locationFieldId = field(formClassId, LOCATION_FIELD);
                            GeoPoint geoPoint = (GeoPoint) fieldValue;

                            latitude = geoPoint.getLatitude();
                            longitude = geoPoint.getLongitude();
                            formInstance.set(locationFieldId, referenceValue);
                        } else {
                            formInstance.set(formField.getId(), fieldValue);
                        }
                    }
                }
            }

            getQueue("ona").add(addLocation(withUrl("/tasks/persist")
                    .param("formInstance", toJson(formInstance))
                    .param("instanceId", (String) instanceId)
                    .param("userId", String.valueOf(userId)),
                    locationId, locationTypeId, latitude, longitude));

        }
    }

    private static TaskOptions addLocation(TaskOptions options,
                                           Integer locationId,
                                           Integer locationTypeId,
                                           Double latitude,
                                           Double longitude) {
        if (options != null && locationId != null && locationTypeId != null && latitude != null && longitude != null) {
            return options
                    .param("locationId", String.valueOf(locationId))
                    .param("locationTypeId", String.valueOf(locationTypeId))
                    .param("latitude", String.valueOf(latitude))
                    .param("longitude", String.valueOf(longitude));
        } else {
            return options;
        }
    }
}
