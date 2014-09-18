package org.activityinfo.ui.app.client.page.pivot;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.activityinfo.model.analysis.*;
import org.activityinfo.model.auth.AuthenticatedUser;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormField;
import org.activityinfo.model.resource.*;
import org.activityinfo.model.table.Bucket;
import org.activityinfo.model.table.TableData;
import org.activityinfo.model.type.FieldType;
import org.activityinfo.model.type.enumerated.EnumType;
import org.activityinfo.model.type.expr.CalculatedFieldType;
import org.activityinfo.service.cubes.CubeBuilder;
import org.activityinfo.service.cubes.PivotTableData;
import org.activityinfo.service.cubes.PivotTableDataBuilder;
import org.activityinfo.service.store.FolderRequest;
import org.activityinfo.store.test.TestResourceStore;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

public class PivotTest {

    private TestResourceStore store;

    private List<FormClass> forms = Lists.newArrayList();
    private FormClass costs;
    private FormClass wp;
    private FormClass camp;
    private FormClass survey;

    @Before
    public void loadData() throws IOException {
        store = new TestResourceStore();
        store.load("lcca.json");

        for(Resource resource : store.all()) {
            if(FormClass.CLASS_ID.asString().equals(resource.isString("classId"))) {
                forms.add(FormClass.fromResource(resource));
            }
        }

        costs = findForm("Documented Cost Information");
        wp = findForm("Water Collection Point information over time - Monthly Reports");
        camp = findForm("Camp Information per month  - Monthly Reports");
        survey = findForm("Bambasi Water Service Ladders Survey");

    }


    @Test
    public void bambisi() throws Exception {

        // aggregate to zone, household id
        // aggregate to weekday and water point
        // weight the results by (sat = 1, other=2)
        // number of households per water point per week
        // weight by the sampling fraction

        // 1 km = 30 min
        // 6 min < ok

        PivotTableModel model = new PivotTableModel();


        MeasureModel iniCost = new MeasureModel();
        iniCost.setId("HP_1_IN_HDW");
        iniCost.setLabel("HP1 Capital cost hand-dug wells as designed: Initial");
        iniCost.setSource(costs.getId());
        iniCost.setValueExpression("V_InCostAgg");
        iniCost.setMeasurementType(MeasurementType.FLOW);
        iniCost.setCriteriaExpression("[System Identifier]=='Hand Dug Wells (All)'");
        model.addMeasure(iniCost);



    }




    @Test
    public void serialization() throws Exception {

        PivotTableModel model = new PivotTableModel();

        MeasureModel iniCost = new MeasureModel();
        iniCost.setId("HP_1_IN_HDW");
        iniCost.setLabel("HP1 Capital cost hand-dug wells as designed: Initial");
        iniCost.setSource(costs.getId());
        iniCost.setValueExpression("V_InCostAgg");
        iniCost.setMeasurementType(MeasurementType.FLOW);
        iniCost.setCriteriaExpression("[System Identifier]=='Hand Dug Wells (All)'");
        model.addMeasure(iniCost);

        Record record = model.asRecord();

        execute(PivotTableModel.fromRecord(record));
    }

    @Test
    public void grandTotals() throws Exception {

        dumpIndicators();

        PivotTableModel model = new PivotTableModel();
        model.getDimensions().add(dim("Typology", dimSource(costs, "[Cost typology]")));

        for(FormField field : costs.getFields()) {
            if(field.getCode() != null && field.getCode().startsWith("V_")) {
                MeasureModel measure = new MeasureModel();
                measure.setId(field.getCode());
                measure.setSource(costs.getId());
                measure.setLabel(field.getCode());
                measure.setMeasurementType(MeasurementType.FLOW);
                measure.setValueExpression(field.getId().asString());
                model.addMeasure(measure);
            }
        }

        List<Bucket> buckets = execute(model);

        PivotTableData pivotTable = new PivotTableDataBuilder().build(
            Arrays.asList("_measure"),
            Arrays.<String>asList("Typology"), buckets);

        dump(pivotTable);
    }

    @Test
    public void yellowTable() throws Exception {

        dumpIndicators();

        PivotTableModel model = new PivotTableModel();
        model.getDimensions().add(dim("Typology", dimSource(costs, "[Cost typology]")));
      //  model.getDimensions().add(dim("System Id", dimSource(costs, "[System Identifier]")));


        DimensionModel system = dim("System", dimSource(wp, "[Site].[Water Collection Point Identifier]"));

        model.getDimensions().add(system);


        model.getDimensions().add(dim("Indicator"));
        model.getDimensions().add(dim("Year",
            dimSource(costs, "[Year of Expediture]"),
            dimSource(wp, "Year"),
            dimSource(camp, "Year")));


        Map<String, String> systemCriteria = new HashMap<>();
        systemCriteria.put("Hand Dug Wells", "[System Identifier] == 'Hand Dug Wells (All)'");
        systemCriteria.put("Initial Piped Scheme",
            "([System Identifier] == 'Whole Area (Including Host Community)') || " +
            "([System Identifier] == 'Initial Piped Scheme') || " +
            "([System Identifier] == 'Extended Piped Scheme EXCL. Host Comm.') || " +
            "([System Identifier] == 'Extended Piped Scheme INCL. Host Comm') || " +
            "([System Identifier] == 'Whole Camp (Excluding Host Community)')");

        systemCriteria.put("Piped Scheme Extension Refugees",
            "([System Identifier] == 'Whole Area (Including Host Community)') || " +
            "([System Identifier] == 'Piped Scheme Extension Refugees') || " +
            "([System Identifier] == 'Extended Piped Scheme EXCL. Host Comm.') || " +
            "([System Identifier] == 'Extended Piped Scheme INCL. Host Comm') || " +
            "([System Identifier] == 'Whole Camp (Excluding Host Community)')");


        systemCriteria.put("Piped Scheme Extension Host Community",
            "([System Identifier] == 'Whole Area (Including Host Community)') || " +
            "([System Identifier] == 'Piped Scheme Extension Host Community') || " +
            "([System Identifier] == 'Whole Camp (Excluding Host Community)')");

        systemCriteria.put("Water trucking/pumping", "[System Identifier] == 'Emergency (Trucking/prov. PS)'");

        for (String systemDim : systemCriteria.keySet()) {

            MeasureModel investmentCost = new MeasureModel();
            investmentCost.setId("investment_" + systemDim);
            investmentCost.setSource(costs.getId());
            investmentCost.setLabel("Investment cost " + systemDim);
            investmentCost.setMeasurementType(MeasurementType.FLOW);
            investmentCost.setValueExpression("V_InCostAgg+V_ExtCostAgg");
            investmentCost.setCriteriaExpression(systemCriteria.get(systemDim));
            investmentCost.setDimensionTag("System", systemDim);
            investmentCost.setDimensionTag("Indicator", "Investment Cost");
            model.addMeasure(investmentCost);

            MeasureModel capMaint = new MeasureModel();
            capMaint.setId("capmaint_" + systemDim);
            capMaint.setSource(costs.getId());
            capMaint.setLabel("Capital maintenance cost " + systemDim);
            capMaint.setMeasurementType(MeasurementType.FLOW);
            capMaint.setValueExpression("V_CapMaInSys+V_CapMaSysExt");
            capMaint.setCriteriaExpression(systemCriteria.get(systemDim));
            capMaint.setDimensionTag("System", systemDim);
            capMaint.setDimensionTag("Indicator", "Capital maintenance cost");
            model.addMeasure(capMaint);


            MeasureModel dirSupp = new MeasureModel();
            dirSupp.setId("dirSupp" + systemDim);
            dirSupp.setSource(costs.getId());
            dirSupp.setLabel("Capital maintenance cost " + systemDim);
            dirSupp.setMeasurementType(MeasurementType.FLOW);
            dirSupp.setValueExpression("V_DirSupAgg");
            dirSupp.setCriteriaExpression(systemCriteria.get(systemDim));
            dirSupp.setDimensionTag("System", systemDim);
            dirSupp.setDimensionTag("Indicator", "Direct support");
            model.addMeasure(dirSupp);
        }


        MeasureModel hdwPlanned = new MeasureModel();
        hdwPlanned.setId("Nr planned HDW");
        hdwPlanned.setSource(wp.getId());
        hdwPlanned.setLabel("Nr planned HDW");
        hdwPlanned.setMeasurementType(MeasurementType.STOCK);
        hdwPlanned.setValueExpression("[Nr planned HDW]");
        hdwPlanned.setDimensionTag("Typology", "Budgeted");
        hdwPlanned.setDimensionTag("System", "Hand Dug Wells");
        hdwPlanned.setDimensionTag("Indicator", "Number of (planned/functional) wells");

        model.addMeasure(hdwPlanned);

        MeasureModel hdwActual = new MeasureModel();
        hdwActual.setId("Nbr functunal HDW");
        hdwActual.setSource(wp.getId());
        hdwActual.setLabel("Nbr functunal HDW");
        hdwActual.setMeasurementType(MeasurementType.STOCK);
        hdwActual.setValueExpression("[Nbr functunal HDW]");
        hdwActual.setDimensionTag("Typology", "Spent");
        hdwActual.setDimensionTag("System", "Hand Dug Wells");
        hdwActual.setDimensionTag("Indicator", "Number of (planned/functional) wells");
        model.addMeasure(hdwActual);


        MeasureModel tpsPlanned = new MeasureModel();
        tpsPlanned.setId("Nbr Planned taps");
        tpsPlanned.setSource(wp.getId());
        tpsPlanned.setLabel("Nbr Planned taps");
        tpsPlanned.setMeasurementType(MeasurementType.STOCK);
        tpsPlanned.setValueExpression("[Nbr Planned taps]");
        tpsPlanned.setDimensionTag("Typology", "Budgeted");
        tpsPlanned.setDimensionTag("Indicator", "Number of (planned/functioning) taps");
        model.addMeasure(tpsPlanned);

        MeasureModel tpsFunctional = new MeasureModel();
        tpsFunctional.setId("Nbr functioning taps");
        tpsFunctional.setSource(wp.getId());
        tpsFunctional.setLabel("Nbr functioning taps");
        tpsFunctional.setMeasurementType(MeasurementType.STOCK);
        tpsFunctional.setValueExpression("[Nbr functioning taps]");
        tpsFunctional.setDimensionTag("Typology", "Spent");
        tpsFunctional.setDimensionTag("Indicator", "Number of (planned/functioning) taps");
        model.addMeasure(tpsFunctional);

        for(String systemId : Arrays.asList("Hand Dug Wells", "Piped Scheme Extension Refugees", "Water trucking/pumping")) {
            for(String typology : Arrays.asList("Budgeted", "Spent")) {
                MeasureModel population = new MeasureModel();
                population.setId("population_" + systemId + "_" + typology);
                population.setLabel(typology + " Population " + systemId);
                population.setSource(camp.getId());
                population.setValueExpression("[Camp Population]");
                population.setMeasurementType(MeasurementType.STOCK);
                population.setDimensionTag("Indicator", "Refugee population");
                population.setDimensionTag("System", systemId);
                population.setDimensionTag("Typology", typology);
                model.addMeasure(population);
            }
        }


        List<Bucket> buckets = execute(model);

        PivotTableData pivotTable = new PivotTableDataBuilder().build(
            Arrays.asList("System", "Indicator"),
            Arrays.<String>asList("Year", "Typology"), buckets);

        dump(pivotTable);
    }

    @Test
    public void hdwStock() throws Exception {
        dumpIndicators();

        PivotTableModel model = new PivotTableModel();
        model.getDimensions().add(dim("System Identifier", dimSource(wp, "[Site].[Water Collection Point Identifier]")));
        model.getDimensions().add(dim("Month", dimSource(wp, "Month")));

        MeasureModel wellCount = new MeasureModel();
        wellCount.setId("Nr actual HDW");
        wellCount.setLabel("Nr actual HDW");
        wellCount.setSource(wp.getId());
        wellCount.setValueExpression("[Nr actual HDW]");
        wellCount.setMeasurementType(MeasurementType.FLOW);
        model.addMeasure(wellCount);

        List<Bucket> buckets = execute(model);


        PivotTableData pivotTable = new PivotTableDataBuilder().build(
            Arrays.asList("System Identifier"),
            Arrays.<String>asList("Month"), buckets);


        dump(pivotTable);
    }

    private void dump(PivotTableData pivotTable) {

        printHeaders(pivotTable.getRootColumn());

        List<PivotTableData.Axis> columns = pivotTable.getRootColumn().getLeaves();
        printRows(pivotTable.getRootRow(), columns, "");

    }

    private void printRows(PivotTableData.Axis rootRow, List<PivotTableData.Axis> columns, String indent) {
        for(PivotTableData.Axis row : rootRow.getChildren()) {
            System.out.print(indent);
            System.out.print(row.getDimensionValue());
            if (row.isLeaf()) {
                for(PivotTableData.Axis col : columns) {
                    System.out.print("\t");
                    PivotTableData.Cell cell = row.getCell(col);
                    if(cell != null && cell.getValue() != 0) {
                        System.out.print(cell.getValue());
                    }
                }
                System.out.println();
            } else {
                System.out.println();
                printRows(row, columns, indent + " ");
            }
        }
    }

    private void printHeaders(PivotTableData.Axis parentColumn) {
        for(int i=1; i<=parentColumn.getDepth();++i) {
            for (PivotTableData.Axis column : parentColumn.getDescendantsAtDepth(i)) {
                System.out.print("\t");
                System.out.print(column.getDimensionValue());
                int breadth = column.getLeaves().size() - 1;
                for (int j = 0; j < breadth; ++j) {
                    System.out.print("\t");
                }
            }
            System.out.println();
        }
    }

    @Test
    public void hp1() throws Exception {
        dumpIndicators();


        PivotTableModel model = new PivotTableModel();
        model.getDimensions().add(dim("Year", dimSource(costs, "[Year of expediture]"), dimSource(wp, "Year")));
        model.getDimensions().add(dim("System",
            dimSource(costs, "[System Identifier]"),
            dimSource(wp, "[Site].[Water Collection Point Identifier]")));
        model.getDimensions().add(dim("Typology", dimSource(costs, "[Cost typology]")));


        MeasureModel iniCost = new MeasureModel();
        iniCost.setId("HP_1_IN_HDW");
        iniCost.setLabel("HP1 Capital cost hand-dug wells as designed: Initial");
        iniCost.setSource(costs.getId());
        iniCost.setValueExpression("V_InCostAgg");
        iniCost.setMeasurementType(MeasurementType.FLOW);
        iniCost.setCriteriaExpression("[System Identifier]=='Hand Dug Wells (All)'");
        model.addMeasure(iniCost);

        MeasureModel extCosts = new MeasureModel();
        extCosts.setId("extCosts");
        extCosts.setLabel("HP1 Capital cost hand-dug wells as designed: Extended");
        extCosts.setSource(costs.getId());
        extCosts.setValueExpression("V_ExtCostAgg");
        extCosts.setCriteriaExpression("[System Identifier]=='Hand Dug Wells (All)'");
        extCosts.setMeasurementType(MeasurementType.FLOW);
        model.addMeasure(extCosts);

        MeasureModel wellCount = new MeasureModel();
        wellCount.setId("wellCount");
        wellCount.setLabel("HDW Count");
        wellCount.setSource(wp.getId());
        wellCount.setValueExpression("[TPSfnct]");
        wellCount.setMeasurementType(MeasurementType.STOCK);
        wellCount.setCriteriaExpression("[Site].[Water Collection Point Identifier]='Hand Dug Wells (All)'");
        model.addMeasure(wellCount);

        execute(model);
    }

    private List<Bucket> execute(PivotTableModel model) throws Exception {
        CubeBuilder cubeBuilder = new CubeBuilder(store);
        List<Bucket> buckets = cubeBuilder.buildCube(model);

        dump(model, buckets);

        return buckets;
    }


    @Test
    public void hp3() throws Exception {
        dumpIndicators();

        PivotTableModel model = new PivotTableModel();

        MeasureModel population = new MeasureModel();
        population.setSource(camp.getId());
        population.setValueExpression("[Camp Population]");
        population.setCriteriaExpression("[Year]=='2012'");
        population.setMeasurementType(MeasurementType.STOCK);
        model.addMeasure(population);

        MeasureModel points = new MeasureModel();
        points.setSource(wp.getId());
        points.setValueExpression("[TPSfnct]");
        points.setCriteriaExpression("[Site].[Water Collection Point Identifier]=='Water trucking' && [Year]=='2012'");
        points.setMeasurementType(MeasurementType.STOCK);
        //points.setAggregationFunction(AggregationFunction.MEAN);
        model.addMeasure(points);

        execute(model);
    }


    @Test
    public void hp3_ops() throws Exception {
        dumpIndicators();

        PivotTableModel model = new PivotTableModel();
        model.getDimensions().add(dim("Year", dimSource(costs, "[Year of expediture]")));
        model.getDimensions().add(dim("Typology", dimSource(costs, "[Cost typology]")));
        model.getDimensions().add(dim("System Identifier", dimSource(costs, "[System Identifier]")));

        for(String name : Arrays.asList("V_OperCost", "V_MainCost", "V_OpMaCost", "V_OpMaAgg")) {

            MeasureModel cost = new MeasureModel();
            cost.setSource(costs.getId());
            cost.setId(name);
            cost.setValueExpression(name);
            cost.setCriteriaExpression("[System Identifier]=='Emergency (Trucking/prov. PS)'");
            cost.setMeasurementType(MeasurementType.FLOW);
            //cost.setAggregationFunction(AggregationFunction.SUM);
            model.addMeasure(cost);
        }

        execute(model);
    }



    @Test
    public void ps34() throws Exception {
        dumpIndicators();

        PivotTableModel model = new PivotTableModel();
        model.getDimensions().add(dim("Year", dimSource(costs, "[Year of expediture]")));
        model.getDimensions().add(dim("Typology", dimSource(costs, "[Cost typology]")));
        model.getDimensions().add(dim("System Identifier", dimSource(costs, "[System Identifier]")));

        MeasureModel numerator = new MeasureModel();
        numerator.setSource(costs.getId());
        numerator.setId("numerator");
        numerator.setMeasurementType(MeasurementType.FLOW);
        numerator.setValueExpression("V_CapMaInSys+V_CapMaSysExt");
        model.addMeasure(numerator);

        execute(model);
    }


    @Test
    public void s10() throws Exception {
        dumpIndicators();

        PivotTableModel model = new PivotTableModel();
        model.getDimensions().add(dim("Year", dimSource(costs, "[Year of expediture]")));
        model.getDimensions().add(dim("Typology", dimSource(costs, "[Cost typology]")));
        model.getDimensions().add(dim("System Identifier", dimSource(costs, "[System Identifier]")));

        MeasureModel numerator = new MeasureModel();
        numerator.setSource(costs.getId());
        numerator.setId("numerator");
        numerator.setLabel("Budgeted - Extension");
        numerator.setMeasurementType(MeasurementType.FLOW);
        numerator.setValueExpression("V_DirSupAgg");
//        numerator.setCriteriaExpression("[Cost typology]=='Budgeted' && ([System Identifier] != 'Hand Dug Wells (All)') && " +
//            "([System Identifier] != 'Emergency (Trucking/prov. PS)')");
        model.addMeasure(numerator);

        execute(model);
    }


    private DimensionSource dimSource(FormClass costs, String valueExpr) {
        return new DimensionSource(costs.getId(), valueExpr);
    }

    private void dumpTable(TableData tableData) {

        ArrayList<String> columns = Lists.newArrayList(tableData.getColumns().keySet());
        System.out.println(Joiner.on(",").join(columns));
        for(int i=0;i!=tableData.getNumRows();++i) {
            for(int j=0;j<tableData.getColumns().size();++j) {
                if(j > 0) {
                    System.out.print(",");
                }
                System.out.print("" + tableData.getColumnView(columns.get(j)).get(i));
            }
            System.out.println();
        }
    }

    private DimensionSource source(FormClass form, String fieldName) {
        return new DimensionSource(form.getId(), findField(form, fieldName).getId());
    }

    private DimensionSource source(FormClass form, String fieldName, String criteria) {
        ResourceId fieldId = findField(form, fieldName).getId();
        DimensionSource dimensionSource = new DimensionSource(form.getId(), fieldId);
        dimensionSource.setCriteria(criteria);
        return dimensionSource;
    }


    private DimensionModel dim(String label, DimensionSource... models) {
        DimensionModel dimModel = new DimensionModel();
        dimModel.setId(label);
        dimModel.setLabel(label);
        dimModel.getSources().addAll(Arrays.asList(models));
        return dimModel;
    }

    private FormField findField(FormClass formClass, String label) {
        List<String> labels = Lists.newArrayList();
        for(FormField field : formClass.getFields()) {
            labels.add(field.getLabel());
            if(field.getLabel().equals(label)) {
                return field;
            }
        }
        throw new IllegalArgumentException(label + ". Have: " + Joiner.on(", ").join(labels));
    }

    private FormClass findForm(String label) {
        for(FormClass form : forms) {
            if(form.getLabel().equals(label)) {
                return form;
            }
        }
        throw new IllegalArgumentException(label);
    }

    private void dumpIndicators() {
        AuthenticatedUser user = new AuthenticatedUser("", 1, "");
        ResourceNode lcca = store.getOwnedOrSharedWorkspaces(user).get(0);
        FolderProjection projection = store.queryTree(user, new FolderRequest(lcca.getId()));

        for(ResourceNode form : projection.getRootNode().getChildren()) {
            if(form.getClassId().equals(FormClass.CLASS_ID)) {
                FormClass formClass = FormClass.fromResource(store.get(form.getId()));

                System.out.println();
                System.out.println(formClass.getLabel());
                System.out.println("-------------------------");

                for(FormField field : formClass.getFields()) {
                    System.out.println("  " + field.getId().asString() + "   " +
                        Strings.padEnd(Strings.nullToEmpty(field.getCode()), 15, '.') + toString(field.getType()));
                    System.out.println(Strings.repeat(" ", 17) + field.getLabel());
                }
            }
        }
    }

    private String toString(FieldType type) {

        if(type instanceof CalculatedFieldType) {
            return ((CalculatedFieldType) type).getExpression();
        } else if(type instanceof EnumType) {
            return "Enum: " + Joiner.on(", ").join(((EnumType) type).getValues());
        } else {
            return type.getTypeClass().getId();
        }
    }


    public void dumpModel(PivotTableModel model) {

        System.out.println("CUBE [LCCA Ethiopia]");

        System.out.println("\tDIMENSIONS");
        for(DimensionModel dim : model.getDimensions()) {
            System.out.println("\t\t" + dim.getId() + ": " + dim.getDescription());
        }
        System.out.println("\tMEASURES");

        for(MeasureModel measure : model.getMeasures()) {

            System.out.println("AGGREGATE " + measure.getLabel() + ": " + measure.getValueExpression());
            System.out.println("\tFROM [" + getFormLabel(measure.getSourceId()) + "]");

            if(!Strings.isNullOrEmpty(measure.getCriteriaExpression())) {
                System.out.println("\tWHERE " + measure.getCriteriaExpression());
            }

            System.out.println("\tBY");
//            for(DimensionModel dim : model.getDimensions()) {
//                System.out.println("\t\t" + dim.getId() + " = " + dim.getSource(measure.getSourceId()));
//            }

//            System.out.println("\tWITH");
//            measure.ge

        }

    }


    public void dump(PivotTableModel model, List<Bucket> buckets) {

        List<String> headers = Lists.newArrayList();
        headers.add("Measure");
        for(DimensionModel dimension : model.getDimensions()) {
            headers.add(dimension.getLabel());
        }
        headers.add("Value");
        System.out.println(Joiner.on("\t").join(headers));

        for(Bucket bucket : buckets) {
            List<String> cells = Lists.newArrayList();
            cells.add(bucket.getDimensionValue("_measure"));
            for (DimensionModel dim : model.getDimensions()) {
                cells.add(Strings.nullToEmpty(bucket.getDimensionValue(dim.getId())));
            }
            cells.add(Double.toString(bucket.getValue()));
            System.out.println(Joiner.on("\t").join(cells));
        }
    }

    private String getFormLabel(ResourceId sourceId) {
        return store.get(sourceId).getString(FormClass.LABEL_FIELD_ID);
    }
}