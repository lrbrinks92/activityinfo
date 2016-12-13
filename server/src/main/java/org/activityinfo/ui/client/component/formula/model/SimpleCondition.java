package org.activityinfo.ui.client.component.formula.model;

import org.activityinfo.model.expr.ConstantExpr;
import org.activityinfo.model.expr.ExprNode;
import org.activityinfo.model.expr.FunctionCallNode;
import org.activityinfo.model.expr.SymbolExpr;
import org.activityinfo.model.expr.functions.ComparisonOperator;

public class SimpleCondition {

    private SymbolExpr field;
    private SimpleOperator operator;
    private ConstantExpr value;

    public SimpleCondition() {
    }

    public SimpleCondition(ComparisonOperator operator, SymbolExpr field, ConstantExpr value) {
        this.field = field;
        this.value = value;
    }


    public void setField(SymbolExpr field) {
        this.field = field;
    }

    public ConstantExpr getValue() {
        return value;
    }

    public void setValue(ConstantExpr value) {
        this.value = value;
    }


    public SymbolExpr getField() {
        return field;
    }

    public static SimpleCondition tryMatch(ExprNode node) {
        if(node instanceof FunctionCallNode) {
            FunctionCallNode callNode = (FunctionCallNode) node;
            if(callNode.getFunction() instanceof ComparisonOperator) {
                ExprNode x = callNode.getArgument(0);
                ExprNode y = callNode.getArgument(1);
//                if(x instanceof SymbolExpr && y instanceof ConstantExpr) {
//                    return new SimpleCondition((ComparisonOperator) callNode.getFunction(),
//                            ((SymbolExpr) x),
//                            ((ConstantExpr) y));
//                }
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleCondition that = (SimpleCondition) o;

        if (field != null ? !field.equals(that.field) : that.field != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;
        return operator != null ? operator.equals(that.operator) : that.operator == null;

    }

    @Override
    public int hashCode() {
        int result = field != null ? field.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (operator != null ? operator.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return field + " " + operator + " " + value;
    }
}
