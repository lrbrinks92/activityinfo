package org.activityinfo.ui.client.component.formula.model;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.activityinfo.model.expr.ExprNode;
import org.activityinfo.model.expr.FunctionCallNode;
import org.activityinfo.model.expr.GroupExpr;
import org.activityinfo.model.expr.functions.AndFunction;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Model of a simple boolean expression that can be reduced to a list of boolean conditions.
 *
 * <p>Condition list takes an expression like {@code A=1 AND B=2} and parses it into a list of simple boolean
 * conditions that can be presented to the user in a simplified user interface.</p>
 */
public class SimpleConditionSet {

    public enum Criteria {
        ALL,
        ANY
    };


    private final List<SimpleCondition> conditions;

    private Criteria criteria = Criteria.ALL;


    public SimpleConditionSet(SimpleCondition condition) {
      this.conditions = Lists.newArrayList(condition);
    }

    public SimpleConditionSet(SimpleCondition... conditions) {
        this.conditions = Lists.newArrayList(conditions);
    }

    private SimpleConditionSet(ArrayList<SimpleCondition> conditions) {
        this.conditions = conditions;
    }

    /**`
     * Tries to match an expr node to a list of simple field conditions.
     * @param node
     * @return a new {@code SimpleConditionSet}, or {@code null} if the expression
     * does not fit the shape of a condition list.
     */
    public static SimpleConditionSet tryMatch(ExprNode node) {
        ArrayList<SimpleCondition> conditions = new ArrayList<>();
        if(tryMatch(conditions, node)) {
            return new SimpleConditionSet(conditions);
        }
        return null;
    }

    public List<SimpleCondition> getConditions() {
        return conditions;
    }

    @Nonnull
    public Criteria getCriteria() {
        return criteria;
    }

    public void setCriteria(@Nonnull Criteria criteria) {
        this.criteria = criteria;
    }

    private static boolean tryMatch(List<SimpleCondition> conditions, ExprNode node) {
        node = simplify(node);
        SimpleCondition leaf = tryMatchLeaf(node);
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

    private static SimpleCondition tryMatchLeaf(ExprNode node) {
        SimpleCondition condition = SimpleCondition.tryMatch(node);
        if(condition != null) {
            return condition;
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleConditionSet that = (SimpleConditionSet) o;

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
