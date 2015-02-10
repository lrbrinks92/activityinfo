package org.activityinfo.model.expr.eval;

import org.activityinfo.model.form.FormField;
import org.activityinfo.model.resource.Record;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.type.expr.CalculatedFieldType;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Created by yuriy on 1/3/2015.
 */
public class PartialEvaluatorTest {

    private static final String symbols = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    // AI-887 : Expression "t" leads to RuntimeException
    // test all single character inputs in the range A-Za-z
    @Test
    public void simpleExpr() {

        for (int i = 0; i< symbols.length(); i++) {
            String symbol = "" + symbols.charAt(i);


            FormField a = new FormField(ResourceId.generateId());
            a.setCode(symbol);
            a.setType(new CalculatedFieldType(symbol));
            a.setLabel(symbol);

            assertPartialEvaluation(a);

            // try lower case version
            symbol = symbol.toLowerCase();
            a.setCode(symbol);
            a.setType(new CalculatedFieldType(symbol));
            a.setLabel(symbol);
            assertPartialEvaluation(a);
        }
    }

    private static void assertPartialEvaluation(FormField field) {
        assertPartialEvaluation(Arrays.asList(field));
    }

    private static void assertPartialEvaluation(List<FormField> fields) {
        FormSymbolTable symbolTable = new FormSymbolTable(fields);
        PartialEvaluator<Record> evaluator = new PartialEvaluator<Record>(symbolTable, new RecordReaderFactory());

        for (FormField field : fields) {
            if (field.getType() instanceof CalculatedFieldType) {
                Assert.assertNotNull(evaluator.partiallyEvaluate(field));
            }
        }
    }
}
