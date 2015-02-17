package org.activityinfo.server.endpoint.ona;

import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormField;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.model.legacy.KeyGenerator;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.type.FieldValue;
import org.activityinfo.model.type.geo.GeoPoint;
import org.activityinfo.model.type.time.LocalDate;
import org.activityinfo.server.command.ResourceLocatorSync;
import org.activityinfo.server.endpoint.odk.FieldValueParser;
import org.activityinfo.service.guid.SiteIdGuidService;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import static org.activityinfo.model.legacy.CuidAdapter.END_DATE_FIELD;
import static org.activityinfo.model.legacy.CuidAdapter.GUID_FIELD;
import static org.activityinfo.model.legacy.CuidAdapter.SITE_DOMAIN;
import static org.activityinfo.model.legacy.CuidAdapter.START_DATE_FIELD;
import static org.activityinfo.model.legacy.CuidAdapter.activityFormClass;
import static org.activityinfo.model.legacy.CuidAdapter.cuid;
import static org.activityinfo.model.legacy.CuidAdapter.field;
import static org.activityinfo.server.endpoint.odk.FieldValueParserFactory.fromFieldType;

public class XFormInstanceReader {
    static final private String UUID = "_uuid";
    static final private String START = "start";
    static final private String END = "end";

    private final ResourceLocatorSync locator;
    private final SiteIdGuidService siteIdGuidService;

    public XFormInstanceReader(ResourceLocatorSync locator,
                               SiteIdGuidService siteIdGuidService) {
        this.locator = locator;
        this.siteIdGuidService = siteIdGuidService;
    }

    public void build(LinkedHashMap<String, Object> map,
                             Integer activityId,
                             Integer locationTypeId) {
        final ResourceId formClassId = activityFormClass(activityId);
        final FormClass formClass = locator.getFormClass(formClassId);
        final ArrayList<FormField> formFields = new ArrayList<>(formClass.getFields());
        final Object instanceId = map.get(UUID);
        final Object start = map.get(START);
        final Object end = map.get(END);
        final int siteId = siteIdGuidService.getSiteId(activityId, (String) instanceId);
        final FormInstance formInstance = new FormInstance(cuid(SITE_DOMAIN, siteId), formClassId);
        Integer locationId = null;
        Double latitude = null;
        Double longitude = null;

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
                    GeoPoint geoPoint = (GeoPoint) fieldValue;
                    locationId = new KeyGenerator().generateInt();
                    latitude = geoPoint.getLatitude();
                    longitude = geoPoint.getLongitude();
                } else {
                    formInstance.set(formField.getId(), fieldValue);
                }
            }
        }

        formInstance.set(field(formClassId, GUID_FIELD), instanceId);

        if (locationId != null && locationTypeId != null && latitude != null && longitude != null) {
            locator.persist(formInstance, locationId, locationTypeId, latitude, longitude);
        } else {
            locator.persist(formInstance);
        }
    }
}
