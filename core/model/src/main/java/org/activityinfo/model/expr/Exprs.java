package org.activityinfo.model.expr;

import com.google.common.base.Preconditions;
import org.activityinfo.model.expr.functions.AndFunction;
import org.activityinfo.model.expr.functions.EqualFunction;
import org.activityinfo.model.expr.functions.ExprFunction;
import org.activityinfo.model.expr.functions.OrFunction;
import org.activityinfo.model.query.ColumnModel;
import org.activityinfo.model.resource.ResourceId;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Functions for programatically building expressions
 */
public class Exprs {
    
    public static SymbolExpr symbol(String name) {
        return new SymbolExpr(name);
    }
    
    public static SymbolExpr symbol(ResourceId resourceId) {
        return new SymbolExpr(resourceId);
    }
    
    public static FunctionCallNode equals(ExprNode x, ExprNode y) {
        return new FunctionCallNode(EqualFunction.INSTANCE, x, y);
    }
    
    public static FunctionCallNode equals(ResourceId x, ResourceId y) {
        return new FunctionCallNode(EqualFunction.INSTANCE, symbol(x), symbol(y));
    }
    
    public static ExprNode allTrue(List<ExprNode> nodes) {
        return binaryTree(AndFunction.INSTANCE, nodes);
    }
    
    public static ExprNode anyTrue(List<ExprNode> nodes) {
        if(nodes.isEmpty()) {
            return new ConstantExpr(false);
        } else {
            return binaryTree(OrFunction.INSTANCE, nodes);
        }
    }
    
    private static ExprNode binaryTree(ExprFunction function, List<ExprNode> nodes) {
        Preconditions.checkArgument(!nodes.isEmpty());
        
        Iterator<ExprNode> it = nodes.iterator();
        ExprNode expr = it.next();
        while(it.hasNext()) {
            expr = new FunctionCallNode(function, expr, it.next());
        }
        return expr;
    }

    public static ExprNode idConstant(ResourceId id) {
        return new ConstantExpr(id.asString());
    }
    
    public static ExprNode idEqualTo(ResourceId id) {
        return equals(symbol(ColumnModel.ID_SYMBOL), idConstant(id));
    }
    
    public static ExprNode idEqualTo(Set<ResourceId> ids) {
        List<ExprNode> conditions = new ArrayList<>();
        for (ResourceId id : ids) {
            conditions.add(idEqualTo(id));
        }
        return anyTrue(conditions);
    }

    public static ConstantExpr constant(Number value) {
        return new ConstantExpr(value.doubleValue());
    }



    public static ConstantExpr constant(String value) {
        return new ConstantExpr(value);
    }

}
