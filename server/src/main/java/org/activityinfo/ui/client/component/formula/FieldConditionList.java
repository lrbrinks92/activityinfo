package org.activityinfo.ui.client.component.formula;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.activityinfo.model.expr.ExprNode;
import org.activityinfo.model.expr.FunctionCallNode;
import org.activityinfo.model.expr.GroupExpr;
import org.activityinfo.model.expr.functions.AndFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * Model of a simple boolean expression that can be reduced to a list of boolean conditions.
 *
 * <p>Condition list takes an expression like {@code A=1 AND B=2} and parses it into a list of simple boolean
 * conditions that can be presented to the user in a simplified user interface.</p>
 */
public class FieldConditionList {

    public enum Criteria {
        ALL,
        ANY
    };


    private final List<FieldCondition> conditions;
    private Criteria criteria;


    public FieldConditionList(FieldCondition condition) {
      this.conditions = Lists.newArrayList(condition);
    }

    public FieldConditionList(FieldCondition... conditions) {
        this.conditions = Lists.newArrayList(conditions);
    }

    private FieldConditionList(ArrayList<FieldCondition> conditions) {
        this.conditions = conditions;
    }

    /**
     * Tries to match an expr node to a list of simple field conditions.
     * @param node
     * @return a new {@code FieldConditionList}, or {@code null} if the expression
     * does not fit the shape of a condition list.
     */
    public static FieldConditionList tryMatch(ExprNode node) {
        ArrayList<FieldCondition> conditions = new ArrayList<>();
        if(tryMatch(conditions, node)) {
            return new FieldConditionList(conditions);
        }
        return null;
    }

    public List<FieldCondition> getConditions() {
        return conditions;
    }

    public Criteria getCriteria() {
        return criteria;
    }

    public void setCriteria(Criteria criteria) {
        this.criteria = criteria;
    }

    private static boolean tryMatch(List<FieldCondition> conditions, ExprNode node) {
        node = simplify(node);
        FieldCondition leaf = tryMatchLeaf(node);
        if(leaf != null) {
            conditions.add(leaf);
            return true;

        } else if(node instanceof FunctionCallNode) {
            // If it's not a leaf, then try to check if this is conjunction (X && Y)
            FunctionCallNode callNode = (FunctionCallNode) node;
            if(callNode.getFunction() == AndFunction.INSTANCE) {
                return tryMatch(conditions, callNode.getArgument(0)) &&
                       tryMatch(conditions, callNode.getArgument(1));
            }
        }
        return false;
    }

    private static ExprNode simplify(ExprNode node) {
        if(node instanceof GroupExpr) {
            return ((GroupExpr) node).getExpr();
        } else {
            return node;
        }
    }

    private static FieldCondition tryMatchLeaf(ExprNode node) {
        FieldCondition condition = FieldCondition.tryMatch(node);
        if(condition != null) {
            return condition;
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FieldConditionList that = (FieldConditionList) o;

        return conditions.equals(that.conditions);
    }

    @Override
    public int hashCode() {
        return conditions.hashCode();
    }

    @Override
    public String toString() {
        return "{ " + Joiner.on(", ").join(conditions) + " }";
    }
}
