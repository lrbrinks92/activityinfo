package org.activityinfo.legacy.shared.adapter;


import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.activityinfo.core.client.InstanceQuery;
import org.activityinfo.core.client.form.tree.AsyncFormTreeBuilder;
import org.activityinfo.core.shared.Projection;
import org.activityinfo.core.shared.application.ApplicationProperties;
import org.activityinfo.core.shared.criteria.ClassCriteria;
import org.activityinfo.core.shared.criteria.IdCriteria;
import org.activityinfo.fixtures.InjectionSupport;
import org.activityinfo.legacy.shared.command.GetLocations;
import org.activityinfo.legacy.shared.command.result.LocationResult;
import org.activityinfo.legacy.shared.model.LocationDTO;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.model.formTree.FieldPath;
import org.activityinfo.model.formTree.TFormTree;
import org.activityinfo.model.legacy.CuidAdapter;
import org.activityinfo.model.legacy.KeyGenerator;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.type.geo.GeoPoint;
import org.activityinfo.model.type.number.Quantity;
import org.activityinfo.model.type.time.LocalDate;
import org.activityinfo.promise.Promise;
import org.activityinfo.server.command.CommandTestCase2;
import org.activityinfo.server.database.OnDataSet;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.activityinfo.core.client.PromiseMatchers.assertResolves;
import static org.activityinfo.core.shared.criteria.ParentCriteria.isChildOf;
import static org.activityinfo.legacy.shared.adapter.LocationClassAdapter.getAdminFieldId;
import static org.activityinfo.legacy.shared.adapter.LocationClassAdapter.getNameFieldId;
import static org.activityinfo.model.legacy.CuidAdapter.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(InjectionSupport.class)
@OnDataSet("/dbunit/sites-simple1.db.xml")
public class ResourceLocatorAdaptorTest extends CommandTestCase2 {

    private static final int CAUSE_ATTRIBUTE_GROUP_ID = 1;

    private static final int PROVINCE_ADMIN_LEVEL_ID = 1;

    private static final ResourceId PROVINCE_CLASS = CuidAdapter.adminLevelFormClass(PROVINCE_ADMIN_LEVEL_ID);

    private static final int PEAR_DATABASE_ID = 1;

    private static final int HEALTH_CENTER_LOCATION_TYPE = 1;

    private static final ResourceId HEALTH_CENTER_CLASS = CuidAdapter.locationFormClass(HEALTH_CENTER_LOCATION_TYPE);

    private static final int NFI_DIST_ID = 1;

    private static final ResourceId NFI_DIST_FORM_CLASS = CuidAdapter.activityFormClass(NFI_DIST_ID);

    public static final int VILLAGE_TYPE_ID = 1;

    public static final ResourceId VILLAGE_CLASS = CuidAdapter.locationFormClass(VILLAGE_TYPE_ID);

    public static final int IRUMU = 21;


    private ResourceLocatorAdaptor resourceLocator;

    @Before
    public final void setup() {
        resourceLocator = new ResourceLocatorAdaptor(getDispatcher());
    }

    @Test
    public void simpleAdminEntityQuery() {
        assertThat(queryByClass(adminLevelFormClass(PROVINCE_ADMIN_LEVEL_ID)), Matchers.hasSize(4));
    }

    @Test
    public void simplePartnerQuery() {
        assertThat(queryByClass(partnerFormClass(PEAR_DATABASE_ID)), Matchers.hasSize(3));
    }

    @Test
    public void simpleLocationQuery() {
        assertThat(queryByClass(locationFormClass(HEALTH_CENTER_LOCATION_TYPE)), Matchers.hasSize(4));
    }

    @Test
    @OnDataSet("/dbunit/jordan-locations.db.xml")
    public void getLocation() {
        ResourceId classId = locationFormClass(50512);
        FormInstance instance = assertResolves(resourceLocator.getFormInstance(locationInstanceId(1590565828)));
        Set<ResourceId> adminUnits = instance.getReferences(field(classId, ADMIN_FIELD));
        System.out.println(adminUnits);

    }

    @Test
    public void persistSiteException() {

        FormInstance instance = new FormInstance(CuidAdapter.cuid(SITE_DOMAIN, new KeyGenerator().generateInt()),
                NFI_DIST_FORM_CLASS);

        Promise<Void> result;

        result = resourceLocator.persist(instance);
        assertThat(result.getState(), equalTo(Promise.State.REJECTED));

        result = resourceLocator.persist(Arrays.asList(instance, instance));
        assertThat(result.getState(), equalTo(Promise.State.REJECTED));
    }

    @Test
    @OnDataSet("/dbunit/sites-calculated-indicators.db.xml")
    public void persistSiteWithCalculatedIndicators() {
        FormInstance instance = new FormInstance(CuidAdapter.cuid(SITE_DOMAIN, new KeyGenerator().generateInt()),
                NFI_DIST_FORM_CLASS);

        instance.set(indicatorField(1), 1);
        instance.set(indicatorField(2), 2);
        instance.set(locationField(NFI_DIST_ID), locationInstanceId(1));
        instance.set(partnerField(NFI_DIST_ID), partnerInstanceId(1));
        instance.set(projectField(NFI_DIST_ID), projectInstanceId(1));
        instance.set(field(NFI_DIST_FORM_CLASS, START_DATE_FIELD), new LocalDate(2014, 1, 1));
        instance.set(field(NFI_DIST_FORM_CLASS, END_DATE_FIELD), new LocalDate(2014, 1, 1));
        instance.set(field(NFI_DIST_FORM_CLASS, COMMENT_FIELD), "My comment");

        assertResolves(resourceLocator.persist(instance));

        TFormTree formTree = new TFormTree(assertResolves(new AsyncFormTreeBuilder(resourceLocator).apply(NFI_DIST_FORM_CLASS)));
        InstanceQuery query = new InstanceQuery(Lists.newArrayList(formTree.getRootPaths()), new IdCriteria(instance.getId()));

        Projection firstRead = singleSiteProjection(query);

        assertEquals(new Quantity(1), firstRead.getValue(path(indicatorField(1))));
        assertEquals(new Quantity(2), firstRead.getValue(path(indicatorField(2))));
        assertEquals(new Quantity(3), firstRead.getValue(path(indicatorField(11))));
        assertEquals(new Quantity(0.5), firstRead.getValue(path(indicatorField(12))));

        // set indicators to null
        instance.set(indicatorField(1).asString(), null);
        instance.set(indicatorField(2).asString(), null);

        // persist it
        assertResolves(resourceLocator.persist(instance));

        // read from server
        Projection secondRead = singleSiteProjection(query);

        assertEquals(null, secondRead.getValue(path(indicatorField(1))));
        assertEquals(null, secondRead.getValue(path(indicatorField(2))));
        assertEquals(new Quantity(0), secondRead.getValue(path(indicatorField(11))));
        assertEquals(new Quantity(Double.NaN), secondRead.getValue(path(indicatorField(12)))); // make sure NaN is not returned |
    }

    private FieldPath path(ResourceId... fieldIds) {
        return new FieldPath(fieldIds);
    }

    private Projection singleSiteProjection(InstanceQuery query) {
        List<Projection> projections = assertResolves(resourceLocator.query(query));
        assertEquals(projections.size(), 1);
        return projections.get(0);
    }

    @Test
    public void persistLocation() {

        FormInstance instance = new FormInstance(newLegacyFormInstanceId(HEALTH_CENTER_CLASS),
                HEALTH_CENTER_CLASS);
        instance.set(field(HEALTH_CENTER_CLASS, NAME_FIELD), "CS Ubuntu");
        instance.set(field(HEALTH_CENTER_CLASS, GEOMETRY_FIELD), new GeoPoint(-1, 13));
        instance.set(field(HEALTH_CENTER_CLASS, ADMIN_FIELD), entity(IRUMU));

        assertResolves(resourceLocator.persist(instance));

        // ensure that everything worked out
        GetLocations query = new GetLocations(getLegacyIdFromCuid(instance.getId()));
        LocationResult result = execute(query);
        LocationDTO location = result.getData().get(0);

        assertThat(location.getName(), equalTo("CS Ubuntu"));
        assertThat(location.getAdminEntity(1).getName(), equalTo("Ituri"));
        assertThat(location.getAdminEntity(2).getName(), equalTo("Irumu"));
        assertThat(location.getLatitude(), equalTo(-1d));
        assertThat(location.getLongitude(), equalTo(13d));
    }

    @Test
    public void updateLocation() {

//        <location locationId="1" name="Penekusu Kivu" locationTypeId="1"
//        X="1.532" Y="27.323" timeEdited="1"/>
//        <locationAdminLink locationId="1" adminEntityId="2"/>
//        <locationAdminLink locationId="1" adminEntityId="12"/>

        FormInstance instance = assertResolves(resourceLocator.getFormInstance(locationInstanceId(1)));
        instance.set(field(HEALTH_CENTER_CLASS, NAME_FIELD), "New Penekusu");

        assertResolves(resourceLocator.persist(instance));

        GetLocations query = new GetLocations(1);
        LocationResult result = execute(query);
        LocationDTO location = result.getData().get(0);

        assertThat(location.getName(), equalTo("New Penekusu"));
        assertThat(location.getLocationTypeId(), equalTo(1));
        assertThat(location.getLatitude(), equalTo(27.323));
        assertThat(location.getLongitude(), equalTo(1.532));
        assertThat(location.getAdminEntity(1).getId(), equalTo(2));
        assertThat(location.getAdminEntity(2).getId(), equalTo(12));
    }

    @Test
    public void projection() {

        // fields to request
        FieldPath locationName = new FieldPath(LocationClassAdapter.getNameFieldId(HEALTH_CENTER_CLASS));
        FieldPath locationAdminUnit = new FieldPath(LocationClassAdapter.getAdminFieldId(HEALTH_CENTER_CLASS));
        FieldPath locationAdminUnitName = new FieldPath(locationAdminUnit,
                AdminLevelClassAdapter.getNameFieldId(PROVINCE_CLASS));


        List<Projection> projections = assertResolves(resourceLocator.query(
                new InstanceQuery(
                        Lists.newArrayList(locationName, locationAdminUnitName),
                        new ClassCriteria(HEALTH_CENTER_CLASS))));

        System.out.println(Joiner.on("\n").join(projections));
    }

    private List<FormInstance> queryByClass(ResourceId classId) {
        Promise<List<FormInstance>> promise = resourceLocator.queryInstances(new ClassCriteria(classId));

        List<FormInstance> list = assertResolves(promise);

        System.out.println(Joiner.on("\n").join(list));
        return list;
    }


    @Test
    public void locationProjection() {

        ResourceLocatorAdaptor adapter = new ResourceLocatorAdaptor(getDispatcher());
        FieldPath villageName = new FieldPath(getNameFieldId(VILLAGE_CLASS));
        FieldPath provinceName = new FieldPath(
                getAdminFieldId(VILLAGE_CLASS),
                field(PROVINCE_CLASS, CuidAdapter.NAME_FIELD));

        List<Projection> projections = assertResolves(adapter.query(
                new InstanceQuery(
                        asList(villageName, provinceName),
                        new ClassCriteria(VILLAGE_CLASS))));

        System.out.println(Joiner.on("\n").join(projections));

        assertThat(projections.size(), equalTo(4));
        assertThat(projections.get(0).getStringValue(provinceName), equalTo("Sud Kivu"));
    }


    @Test
    public void deleteLocation() {

        ResourceLocatorAdaptor adapter = new ResourceLocatorAdaptor(getDispatcher());
        ResourceId instanceToDelete = CuidAdapter.locationInstanceId(1);
        assertResolves(adapter.remove(Arrays.asList(instanceToDelete)));

        List<FormInstance> formInstances = assertResolves(adapter.queryInstances(new ClassCriteria(CuidAdapter.locationFormClass(1))));

        for (FormInstance instance : formInstances) {
            if (instance.getId().equals(instanceToDelete)) {
                throw new AssertionError();
            }
        }
    }

    @Test
    public void siteProjections() {

        ResourceId partnerClassId = CuidAdapter.partnerFormClass(PEAR_DATABASE_ID);

        ResourceLocatorAdaptor adapter = new ResourceLocatorAdaptor(getDispatcher());
        FieldPath villageName = new FieldPath(getNameFieldId(VILLAGE_CLASS));
        FieldPath provinceName = new FieldPath(getAdminFieldId(VILLAGE_CLASS), field(PROVINCE_CLASS, CuidAdapter.NAME_FIELD));
        FieldPath partnerName = new FieldPath(field(CuidAdapter.activityFormClass(NFI_DIST_ID), PARTNER_FIELD), field(partnerClassId, NAME_FIELD));
        FieldPath indicator1 = new FieldPath(indicatorField(1));
        FieldPath startDate = new FieldPath(field(NFI_DIST_FORM_CLASS, CuidAdapter.START_DATE_FIELD));
        FieldPath endDate = new FieldPath(field(NFI_DIST_FORM_CLASS, CuidAdapter.END_DATE_FIELD));


        List<Projection> projections = assertResolves(adapter.query(
                new InstanceQuery(
                        asList(partnerName, villageName, provinceName, indicator1, endDate),
                        new ClassCriteria(NFI_DIST_FORM_CLASS))));

        System.out.println(Joiner.on("\n").join(projections));

        final Projection firstProjection = projections.get(0);
        assertThat(projections.size(), equalTo(3));
        assertThat(firstProjection.getStringValue(provinceName), equalTo("Sud Kivu"));
        assertEquals(firstProjection.getValue(startDate), null);
        assertEquals(firstProjection.getValue(endDate), new LocalDate(2009, 1, 2));
    }

    @Test
    public void geodb() {

        ResourceLocatorAdaptor adapter = new ResourceLocatorAdaptor(getDispatcher());

        FormInstance geodbFolder = assertResolves(adapter.getFormInstance(FolderListAdapter.GEODB_ID));

        List<FormInstance> countries = assertResolves(adapter.queryInstances(isChildOf(geodbFolder.getId())));
        assertThat(countries, Matchers.hasSize(1));

        FormInstance rdc = countries.get(0);
        assertThat(rdc.getString(ApplicationProperties.COUNTRY_NAME_FIELD), equalTo("Rdc"));

        List<FormInstance> levels = assertResolves(adapter.queryInstances(isChildOf(rdc.getId())));
        System.out.println(levels);

    }

}
