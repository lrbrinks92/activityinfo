package org.activityinfo.server.endpoint.ona;

import com.google.api.client.util.Maps;
import org.activityinfo.legacy.shared.command.CreateLocation;
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
import org.activityinfo.server.command.DispatcherSync;
import org.activityinfo.server.command.ResourceLocatorSync;
import org.activityinfo.server.endpoint.odk.FieldValueParser;
import org.activityinfo.server.endpoint.odk.InstanceIdService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.activityinfo.model.legacy.CuidAdapter.END_DATE_FIELD;
import static org.activityinfo.model.legacy.CuidAdapter.LOCATION_FIELD;
import static org.activityinfo.model.legacy.CuidAdapter.START_DATE_FIELD;
import static org.activityinfo.model.legacy.CuidAdapter.activityFormClass;
import static org.activityinfo.model.legacy.CuidAdapter.field;
import static org.activityinfo.model.legacy.CuidAdapter.locationInstanceId;
import static org.activityinfo.server.endpoint.odk.FieldValueParserFactory.fromFieldType;

public class XFormInstanceReader {
    static final private String UUID = "_uuid";
    static final private String START = "start";
    static final private String END = "end";

    private final DispatcherSync dispatcher;
    private final ResourceLocatorSync locator;
    private final InstanceIdService idService;

    public XFormInstanceReader(DispatcherSync dispatcher, ResourceLocatorSync locator, InstanceIdService idService) {
        this.dispatcher = dispatcher;
        this.locator = locator;
        this.idService = idService;
    }

    public void build(LinkedHashMap<String, Object> map,
                             Integer classId,
                             Integer locationTypeId) {
        final ResourceId formClassId = activityFormClass(classId);
        final FormClass formClass = locator.getFormClass(formClassId);
        final ArrayList<FormField> formFields = new ArrayList<>(formClass.getFields());
        final Object instanceId = map.get(UUID);
        final Object start = map.get(START);
        final Object end = map.get(END);
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

        if (!idService.exists((String) instanceId)) {
            locator.persist(formInstance);

            if (locationId != null && locationTypeId != null && latitude != null && longitude != null) {
                Map<String, Object> properties = Maps.newHashMap();

                properties.put("id", locationId);
                properties.put("locationTypeId", locationTypeId);
                properties.put("name", "Custom location");
                properties.put("latitude", latitude);
                properties.put("longitude", longitude);

                dispatcher.execute(new CreateLocation(properties));
            } else {
                if (formInstance.get(field(formClassId, LOCATION_FIELD)) != null) {
                    throw new IllegalStateException("No location created, but field was set");
                }
            }

            idService.submit((String) instanceId);
        }
    }
}
