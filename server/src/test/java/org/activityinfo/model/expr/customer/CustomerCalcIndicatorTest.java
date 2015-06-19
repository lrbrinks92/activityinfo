package org.activityinfo.model.expr.customer;
/*
 * #%L
 * ActivityInfo Server
 * %%
 * Copyright (C) 2009 - 2013 UNICEF
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.extjs.gxt.ui.client.data.PagingLoadResult;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.activityinfo.fixtures.InjectionSupport;
import org.activityinfo.legacy.shared.adapter.ResourceLocatorAdaptor;
import org.activityinfo.legacy.shared.command.*;
import org.activityinfo.legacy.shared.command.result.Bucket;
import org.activityinfo.legacy.shared.command.result.CreateResult;
import org.activityinfo.legacy.shared.model.*;
import org.activityinfo.legacy.shared.reports.content.DimensionCategory;
import org.activityinfo.legacy.shared.reports.model.AttributeGroupDimension;
import org.activityinfo.legacy.shared.reports.model.DateDimension;
import org.activityinfo.legacy.shared.reports.model.DateUnit;
import org.activityinfo.legacy.shared.reports.model.Dimension;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormField;
import org.activityinfo.model.legacy.KeyGenerator;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.type.Cardinality;
import org.activityinfo.model.type.enumerated.EnumItem;
import org.activityinfo.model.type.enumerated.EnumType;
import org.activityinfo.model.type.expr.CalculatedFieldType;
import org.activityinfo.model.type.number.QuantityType;
import org.activityinfo.server.command.CommandTestCase2;
import org.activityinfo.server.command.LocationDTOs;
import org.activityinfo.server.database.OnDataSet;
import org.activityinfo.server.endpoint.export.SiteExporter;
import org.activityinfo.server.endpoint.export.TaskContext;
import org.activityinfo.server.report.NullStorageProvider;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;

import static org.activityinfo.core.client.PromiseMatchers.assertResolves;
import static org.activityinfo.model.legacy.CuidAdapter.activityFormClass;
import static org.activityinfo.model.legacy.CuidAdapter.getLegacyIdFromCuid;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

/**
 * @author yuriyz on 8/1/14.
 */
@SuppressWarnings({"GwtClientClassFromNonInheritedModule", "NonJREEmulationClassesInClientCode"})
@RunWith(InjectionSupport.class)
@OnDataSet("/dbunit/schema1.db.xml")
public class CustomerCalcIndicatorTest extends CommandTestCase2 {




    public static final Dimension INDICATOR_DIMENSION = new Dimension(DimensionType.Indicator);

    public static final Dimension YEAR_DIMENSION = new DateDimension(DateUnit.YEAR);

    public static final double EPSILON = 0.000000000000001;
    FormClass formClass;
    private KeyGenerator keyGenerator = new KeyGenerator();

    @Test
    public void calculations() {
        formClass = createFormClass();
        int activityId = getLegacyIdFromCuid(formClass.getId());

        SiteDTO newSite = newSite(activityId);

        newSite.setIndicatorValue(fieldId("EXP"), 3);
        newSite.setIndicatorValue(fieldId("WATER_ALLOC"), 400);
        newSite.setIndicatorValue(fieldId("PCT_INITIAL"), 50);
        newSite.setIndicatorValue(fieldId("PCT_INITIAL_HARD"), 20);
        newSite.setIndicatorValue(fieldId("PCT_INITIAL_SOFT"), 30);

        CreateResult createSiteResult = execute(new CreateSite(newSite));

        // let the client know the command has succeeded
        newSite.setId(createSiteResult.getNewId());

        PagingLoadResult<SiteDTO> loadResult = execute(GetSites.byId(newSite
                .getId()));
        Assert.assertEquals(1, loadResult.getData().size());

        SiteDTO siteDTO = loadResult.getData().get(0);

        assertThat(indicatorValue(siteDTO, "WATER_EXP"), closeTo(12, 0)); // WATER_EXP = EXP * (WATER_ALLOC / 100)
        assertThat(indicatorValue(siteDTO, "INITIAL"), closeTo(6, 0)); // INITIAL = WATER_EXP * (PCT_INITIAL / 100)
        assertThat(indicatorValue(siteDTO, "INITIAL_HARD"), closeTo(2.4, 0.001)); // INITIAL_HARD = WATER_EXP * (PCT_INITIAL_HARD / 100)
        assertThat(indicatorValue(siteDTO, "INITIAL_SOFT"), closeTo(3.6, 0.001)); // INITIAL_SOFT = WATER_EXP * (PCT_INITIAL_HARD / 100)
        assertThat(indicatorValue(siteDTO, "INITIAL_TOTAL"), closeTo(12d, 0.001)); // INITIAL_TOTAL = INITIAL + INITIAL_HARD + INITIAL_SOFT
    }


    @Test
    public void calculationsWithMissingValues() {
        formClass = createFormClass();
        int activityId = getLegacyIdFromCuid(formClass.getId());

        SiteDTO newSite = newSite(activityId);

        newSite.setIndicatorValue(fieldId("EXP"), 3);
        newSite.setIndicatorValue(fieldId("WATER_ALLOC"), 400);
        newSite.setIndicatorValue(fieldId("PCT_INITIAL"), 50);
        //newSite.setIndicatorValue(fieldId("PCT_INITIAL_HARD"), 0);
        newSite.setIndicatorValue(fieldId("PCT_INITIAL_SOFT"), 30);

        CreateResult createSiteResult = execute(new CreateSite(newSite));

        // let the client know the command has succeeded
        newSite.setId(createSiteResult.getNewId());

        PagingLoadResult<SiteDTO> loadResult = execute(GetSites.byId(newSite.getId()));
        Assert.assertEquals(1, loadResult.getData().size());

        SiteDTO siteDTO = loadResult.getData().get(0);

        assertThat(indicatorValue(siteDTO, "WATER_EXP"), closeTo(12, 0)); // WATER_EXP = EXP * (WATER_ALLOC / 100)
        assertThat(indicatorValue(siteDTO, "INITIAL"), closeTo(6, 0)); // INITIAL = WATER_EXP * (PCT_INITIAL / 100)
        assertThat(indicatorValue(siteDTO, "INITIAL_HARD"), equalTo(0d)); // INITIAL_HARD = WATER_EXP * (PCT_INITIAL_HARD / 100)
        assertThat(indicatorValue(siteDTO, "INITIAL_SOFT"), closeTo(3.6, 0.001)); // INITIAL_SOFT = WATER_EXP * (PCT_INITIAL_HARD / 100)
        assertThat(indicatorValue(siteDTO, "INITIAL_TOTAL"), closeTo(9.6, 0.001)); // INITIAL_TOTAL = INITIAL + INITIAL_HARD + INITIAL_SOFT
    }

    @Test
    public void pivot() throws IOException {
        formClass = createFormClass();
        int activityId = getLegacyIdFromCuid(formClass.getId());


        double alloc[] = new double[] { 20, 10, 50, 20, 10 };

        int year[] = new int[] { 2011, 2012, 2013};

        AttributeGroupDTO group = getAttributeGroup(activityId);


        List<Command> batch = Lists.newArrayList();
        for(int i=0;i!=50;++i) {

            SiteDTO newSite = newSite(activityId);

            newSite.setDate1((new GregorianCalendar(year[i % year.length], 1, 1)).getTime());
            newSite.setDate2((new GregorianCalendar(year[i % year.length], 1, 1)).getTime());

            newSite.setIndicatorValue(fieldId("EXP"), (i % 10) * 1000);
            newSite.setIndicatorValue(fieldId("WATER_ALLOC"), alloc[i % alloc.length]);
            newSite.setIndicatorValue(fieldId("PCT_INITIAL"), 50);
            newSite.setIndicatorValue(fieldId("PCT_INITIAL_HARD"), 20);
            newSite.setIndicatorValue(fieldId("PCT_INITIAL_SOFT"), 30);
            if(i % 2 == 0) {
                newSite.setAttributeValue(group.getAttributes().get(0).getId(), true);
            } else {
                newSite.setAttributeValue(group.getAttributes().get(1).getId(), true);
            }

            batch.add(new CreateSite(newSite));

        }
        execute(new BatchCommand(batch));


        // Export to excel
        ActivityFormDTO activity = execute(new GetActivityForm(activityId));

        SiteExporter exporter = new SiteExporter(new TaskContext(getDispatcherSync(), new NullStorageProvider(), "XYZ"));
        exporter.export(activity, Filter.filter().onActivity(activityId));

        exporter.done();
        try(FileOutputStream fos = new FileOutputStream(Paths.get("target", "calcs.xls").toFile())) {
            exporter.getBook().write(fos);
        }

        // Get a full sum
        fullPivot(activityId);
        pivotByYear(activityId);
        pivotByAttributeGroup(activityId);
    }

    private PivotSites fullPivot(int activityId) {
        PivotSites pivot = new PivotSites();
        pivot.setDimensions(new Dimension(DimensionType.Indicator));
        pivot.setFilter(Filter.filter().onActivity(activityId));

        List<Bucket> buckets = execute(pivot).getBuckets();
        System.out.println(Joiner.on("\n").join(buckets));


        assertThat(buckets, hasItem(total("Expenditure", 225000)));
        assertThat(buckets, hasItem(total("Expenditure on water programme", 48500)));
        assertThat(buckets, hasItem(total("Value of Initial Cost - Not specified", 24250)));
        assertThat(buckets, hasItem(total("Value of Initial Cost - Cap Hard", 9700)));
        assertThat(buckets, hasItem(total("Value of Initial Cost – Cap Soft", 14550)));
        assertThat(buckets, hasItem(total("Total Value of Initial Cost", 48500)));
        return pivot;
    }


    private PivotSites pivotByYear(int activityId) {
        PivotSites pivot = new PivotSites();
        pivot.setDimensions(INDICATOR_DIMENSION, YEAR_DIMENSION);
        pivot.setFilter(Filter.filter().onActivity(activityId));

        List<Bucket> buckets = execute(pivot).getBuckets();
        System.out.println(Joiner.on("\n").join(buckets));

//
//        assertThat(buckets, hasItem(yearTotal("Expenditure", 225000)));
//        assertThat(buckets, hasItem(total("Expenditure on water programme", 48500)));
//        assertThat(buckets, hasItem(total("Value of Initial Cost - Not specified", 24250)));
//        assertThat(buckets, hasItem(total("Value of Initial Cost - Cap Hard", 9700)));
//        assertThat(buckets, hasItem(total("Value of Initial Cost – Cap Soft", 14550)));
//        assertThat(buckets, hasItem(total("Total Value of Initial Cost", 48500)));
        return pivot;
    }


    private PivotSites pivotByAttributeGroup(int activityId) {

        AttributeGroupDTO group = getAttributeGroup(activityId);

        PivotSites pivot = new PivotSites();
        pivot.setDimensions(INDICATOR_DIMENSION, new AttributeGroupDimension(group.getId()));
        pivot.setFilter(Filter.filter().onActivity(activityId));

        List<Bucket> buckets = execute(pivot).getBuckets();
        System.out.println(Joiner.on("\n").join(buckets));

        //
        //        assertThat(buckets, hasItem(yearTotal("Expenditure", 225000)));
        //        assertThat(buckets, hasItem(total("Expenditure on water programme", 48500)));
        //        assertThat(buckets, hasItem(total("Value of Initial Cost - Not specified", 24250)));
        //        assertThat(buckets, hasItem(total("Value of Initial Cost - Cap Hard", 9700)));
        //        assertThat(buckets, hasItem(total("Value of Initial Cost – Cap Soft", 14550)));
        //        assertThat(buckets, hasItem(total("Total Value of Initial Cost", 48500)));
        return pivot;
    }

    private AttributeGroupDTO getAttributeGroup(int activityId) {
        ActivityFormDTO form = execute(new GetActivityForm(activityId));

        return form.getAttributeGroups().get(0);
    }

    private Matcher<Bucket> total(final String label, final double sum) {
        return new TypeSafeMatcher<Bucket>() {
            @Override
            protected boolean matchesSafely(Bucket item) {
                DimensionCategory category = item.getCategory(INDICATOR_DIMENSION);
                if(category == null) {
                    return false;
                }
                return category.getLabel().equals(label) && item.getSum() == sum;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Bucket with indicator label ").appendValue(label).appendText(" and sum ")
                        .appendValue(sum);
            }
        };
    }

    private Matcher<Bucket> yearTotal(final String label, int year, final double sum) {
        return new TypeSafeMatcher<Bucket>() {
            @Override
            protected boolean matchesSafely(Bucket item) {

                DimensionCategory category = item.getCategory(INDICATOR_DIMENSION);
                if(category == null) {
                    return false;
                }
                return category.getLabel().equals(label) && item.getSum() == sum;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Bucket with indicator label ").appendValue(label).appendText(" and sum ")
                        .appendValue(sum);
            }
        };
    }




    private Double indicatorValue(SiteDTO siteDTO, String nameInExpression) {
        int indicatorId = getLegacyIdFromCuid(field(nameInExpression).getId());
        return siteDTO.getIndicatorValue(indicatorId);
    }

    private FormField field(String code) {
        for (FormField field : formClass.getFields()) {
            if (code.equals(field.getCode())) {
                return field;
            }
        }
        throw new RuntimeException("Enable to find field with code: " + code);
    }

    private int fieldId(String nameInExpression) {
        return getLegacyIdFromCuid(field(nameInExpression).getId());
    }

    private SiteDTO newSite(int activityId) {
        LocationDTO location = LocationDTOs.newLocation();
        execute(new CreateLocation(location));

        SiteDTO newSite = new SiteDTO();
        newSite.setId(keyGenerator.generateInt());
        newSite.setActivityId(activityId);
        newSite.setLocationId(location.getId());
        newSite.setPartner(new PartnerDTO(1, "Foobar"));
        newSite.setReportingPeriodId(keyGenerator.generateInt());
        newSite.setDate1((new GregorianCalendar(2014, 1, 1)).getTime());
        newSite.setDate2((new GregorianCalendar(2014, 1, 1)).getTime());
        return newSite;
    }


    private FormClass createFormClass() {
        SchemaDTO schema = execute(new GetSchema());
        UserDatabaseDTO db = schema.getDatabaseById(1);
        LocationTypeDTO locType = schema.getCountryById(1).getLocationTypes().get(0);

        ActivityFormDTO act = new ActivityFormDTO();
        act.setName("Calculated indicators");
        act.setLocationType(locType);
        act.setReportingFrequency(ActivityFormDTO.REPORT_ONCE);

        CreateResult createResult = execute(CreateEntity.Activity(db, act));
        ResourceId classId = activityFormClass(createResult.getNewId());

        ResourceLocatorAdaptor resourceLocator = new ResourceLocatorAdaptor(getDispatcher());
        FormClass formClass = assertResolves(resourceLocator.getFormClass(classId));

        FormField typeField = new FormField(quantityId());
        typeField.setType(new EnumType(Cardinality.SINGLE,
                Arrays.asList(new EnumItem(EnumItem.generateId(), "Budgeted"),
                        new EnumItem(EnumItem.generateId(), "Spent"))));
        typeField.setLabel("Typology");
        formClass.addElement(typeField);

        FormField expField = new FormField(quantityId());
        expField.setType(new QuantityType().setUnits("currency"));
        expField.setLabel("Expenditure");
        expField.setCode("EXP");

        FormField waterAllocField = new FormField(quantityId());
        waterAllocField.setType(new QuantityType().setUnits("%"));
        waterAllocField.setLabel("Allocation watter programme");
        waterAllocField.setCode("WATER_ALLOC");

        FormField pctInitialField = new FormField(quantityId());
        pctInitialField.setType(new QuantityType().setUnits("%"));
        pctInitialField.setLabel("Initial Cost - Not specified");
        pctInitialField.setCode("PCT_INITIAL");

        FormField pctInitialHardField = new FormField(quantityId());
        pctInitialHardField.setType(new QuantityType().setUnits("%"));
        pctInitialHardField.setLabel("Initial Cost - Cap Hard");
        pctInitialHardField.setCode("PCT_INITIAL_HARD");

        FormField pctInitialSoftField = new FormField(quantityId());
        pctInitialSoftField.setType(new QuantityType().setUnits("%"));
        pctInitialSoftField.setLabel("Initial Cost - Cap Soft");
        pctInitialSoftField.setCode("PCT_INITIAL_SOFT");

        FormField pctExtensionField = new FormField(quantityId());
        pctExtensionField.setType(new QuantityType().setUnits("%"));
        pctExtensionField.setLabel("Extension Cost - Not specified");
        pctExtensionField.setCode("PCT_EXTENSION");

        FormField pctExtensionHardField = new FormField(quantityId());
        pctExtensionHardField.setType(new QuantityType().setUnits("%"));
        pctExtensionHardField.setLabel("Extension Cost - Hard");
        pctExtensionHardField.setCode("PCT_EXTENSION_HARD");

        FormField pctExtensionSoftField = new FormField(quantityId());
        pctExtensionSoftField.setType(new QuantityType().setUnits("%"));
        pctExtensionSoftField.setLabel("Extension Cost - Soft");
        pctExtensionSoftField.setCode("PCT_EXTENSION_SOFT");

        FormField pctOpField = new FormField(quantityId());
        pctOpField.setType(new QuantityType().setUnits("%"));
        pctOpField.setLabel("Operational Cost");
        pctOpField.setCode("PCT_OP");

        FormField pctMaintenanceField = new FormField(quantityId());
        pctMaintenanceField.setType(new QuantityType().setUnits("%"));
        pctMaintenanceField.setLabel("Maintenance Cost");
        pctMaintenanceField.setCode("PCT_MAINTENANCE");

        FormField pctOpMaintField = new FormField(quantityId());
        pctOpMaintField.setType(new QuantityType().setUnits("%"));
        pctOpMaintField.setLabel("Operational & Maintenance Cost");
        pctOpMaintField.setCode("PCT_OP_MAINT");

        FormField waterExpField = new FormField(quantityId());
        waterExpField.setType(new QuantityType().setUnits("%"));
        waterExpField.setLabel("Expenditure on water programme");
        waterExpField.setCode("WATER_EXP");
        waterExpField.setType(new CalculatedFieldType("{EXP}*({WATER_ALLOC}/100)"));


        FormField initialField = new FormField(quantityId());
        initialField.setType(new QuantityType().setUnits("%"));
        initialField.setLabel("Value of Initial Cost - Not specified");
        initialField.setCode("INITIAL");

        initialField.setType(new CalculatedFieldType("{WATER_EXP}*({PCT_INITIAL}/100)"));


        FormField initialHardField = new FormField(quantityId());
        initialHardField.setType(new QuantityType().setUnits("%"));
        initialHardField.setLabel("Value of Initial Cost - Cap Hard");
        initialHardField.setCode("INITIAL_HARD");
        initialHardField.setType(new CalculatedFieldType("{WATER_EXP}*({PCT_INITIAL_HARD}/100)"));

        FormField initialSoftField = new FormField(quantityId());
        initialSoftField.setType(new QuantityType().setUnits("%"));
        initialSoftField.setLabel("Value of Initial Cost – Cap Soft");
        initialSoftField.setCode("INITIAL_SOFT");

        initialSoftField.setType(new CalculatedFieldType("{WATER_EXP}*({PCT_INITIAL_SOFT}/100)"));

        FormField initialTotalField = new FormField(quantityId());
        initialTotalField.setType(new QuantityType().setUnits("%"));
        initialTotalField.setLabel("Total Value of Initial Cost");
        initialTotalField.setCode("INITIAL_TOTAL");

        initialTotalField.setType(new CalculatedFieldType("{INITIAL}+{INITIAL_HARD}+{INITIAL_SOFT}"));

        formClass.addElement(expField);
        formClass.addElement(waterAllocField);
        formClass.addElement(pctInitialField);
        formClass.addElement(pctInitialHardField);
        formClass.addElement(pctInitialSoftField);
        formClass.addElement(pctExtensionField);
        formClass.addElement(pctExtensionHardField);
        formClass.addElement(pctExtensionSoftField);
        formClass.addElement(pctOpField);
        formClass.addElement(pctMaintenanceField);
        formClass.addElement(pctOpMaintField);
        formClass.addElement(waterExpField);

        formClass.addElement(initialField);
        formClass.addElement(initialHardField);
        formClass.addElement(initialSoftField);
        formClass.addElement(initialTotalField);

        assertResolves(resourceLocator.persist(formClass));

        FormClass reform = assertResolves(resourceLocator.getFormClass(formClass.getId()));
        assertHasFieldWithLabel(reform, "Expenditure");
        assertHasFieldWithLabel(reform, "Allocation watter programme");
        assertHasFieldWithLabel(reform, "Initial Cost - Not specified");
        assertHasFieldWithLabel(reform, "Initial Cost - Cap Hard");
        assertHasFieldWithLabel(reform, "Initial Cost - Cap Soft");
        assertHasFieldWithLabel(reform, "Extension Cost - Not specified");
        assertHasFieldWithLabel(reform, "Extension Cost - Hard");
        assertHasFieldWithLabel(reform, "Extension Cost - Soft");
        assertHasFieldWithLabel(reform, "Operational Cost");
        assertHasFieldWithLabel(reform, "Maintenance Cost");
        assertHasFieldWithLabel(reform, "Operational & Maintenance Cost");
        assertHasFieldWithLabel(reform, "Expenditure on water programme");
        assertHasFieldWithLabel(reform, "Value of Initial Cost - Not specified");
        assertHasFieldWithLabel(reform, "Value of Initial Cost - Cap Hard");
        assertHasFieldWithLabel(reform, "Value of Initial Cost – Cap Soft");
        assertHasFieldWithLabel(reform, "Total Value of Initial Cost");
        return reform;
    }

    private ResourceId quantityId() {
        return ResourceId.generateFieldId(QuantityType.TYPE_CLASS);
    }

    private static FormField assertHasFieldWithLabel(FormClass formClass, String label) {
        for (FormField field : formClass.getFields()) {
            if (label.equals(field.getLabel())) {
                return field;
            }
        }
        throw new RuntimeException("No field with label: " + label + " found: " + Joiner.on("\n")
                .join(formClass.getFields()));
    }
}
