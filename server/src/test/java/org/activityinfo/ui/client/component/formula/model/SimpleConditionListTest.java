package org.activityinfo.ui.client.component.formula.model;

import org.activityinfo.model.expr.ExprLexer;
import org.activityinfo.model.expr.ExprNode;
import org.activityinfo.model.expr.ExprParser;
import org.activityinfo.model.expr.functions.EqualFunction;
import org.activityinfo.model.expr.functions.LessFunction;
import org.activityinfo.model.expr.functions.NotEqualFunction;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static org.activityinfo.model.expr.Exprs.constant;
import static org.activityinfo.model.expr.Exprs.symbol;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;


public class SimpleConditionListTest {

    @Test
    public void simpleComparison() {
        assertThat(SimpleCondition.tryMatch(parse("A < 3")),
                equalTo(new SimpleCondition(LessFunction.INSTANCE, symbol("A"), constant(3))));

        assertThat(SimpleConditionSet.tryMatch(parse("A < 3")),
                equalTo(
                        new SimpleConditionSet(
                                new SimpleCondition(LessFunction.INSTANCE, symbol("A"), constant(3)))));

    }

    @Test
    public void twoConditions() {
        assertThat(SimpleConditionSet.tryMatch(parse("(A < 3) && (B == 'Foo')")),
                equalTo(
                        new SimpleConditionSet(
                                new SimpleCondition(LessFunction.INSTANCE, symbol("A"), constant(3)),
                                new SimpleCondition(EqualFunction.INSTANCE, symbol("B"), constant("Foo")))));

    }


    @Test
    public void threeConditions() {
        assertThat(SimpleConditionSet.tryMatch(parse("(A < 3) && (B == 'Foo') && (C != 99)")),
                equalTo(
                        new SimpleConditionSet(
                                new SimpleCondition(LessFunction.INSTANCE, symbol("A"), constant(3)),
                                new SimpleCondition(EqualFunction.INSTANCE, symbol("B"), constant("Foo")),
                                new SimpleCondition(NotEqualFunction.INSTANCE, symbol("C"), constant(99)))));

    }

    @Test
    public void failedMatch() {
        assertThat(SimpleConditionSet.tryMatch(parse("(A*3) < 9")), CoreMatchers.nullValue());
    }


    private ExprNode parse(String expr) {
        ExprLexer lexer = new ExprLexer(expr);
        ExprParser parser = new ExprParser(lexer);
        return parser.parse();
    }

}
