package com.jayway.jsonpath.internal.filter;

import com.jayway.jsonpath.internal.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LogicalExpressionNode extends ExpressionNode {
    protected List<ExpressionNode> chain = new ArrayList<ExpressionNode>();
    private final LogicalOperator operator;

    public static ExpressionNode createLogicalNot(ExpressionNode op) {
       return new LogicalExpressionNode(op, LogicalOperator.NOT, null);
    }

    public static LogicalExpressionNode createLogicalOr(ExpressionNode left,ExpressionNode right){
        return new LogicalExpressionNode(left, LogicalOperator.OR, right);
    }

    public static LogicalExpressionNode createLogicalOr(Collection<ExpressionNode> operands){
        return new LogicalExpressionNode(LogicalOperator.OR, operands);
    }

    public static LogicalExpressionNode createLogicalAnd(ExpressionNode left,ExpressionNode right){
        return new LogicalExpressionNode(left, LogicalOperator.AND, right);
    }

    public static LogicalExpressionNode createLogicalAnd(Collection<ExpressionNode> operands){
        return new LogicalExpressionNode(LogicalOperator.AND, operands);
    }

    private LogicalExpressionNode(ExpressionNode left, LogicalOperator operator, ExpressionNode right) {
        chain.add(left);
        chain.add(right);
        this.operator = operator;
    }

    private LogicalExpressionNode(LogicalOperator operator, Collection<ExpressionNode> operands) {
        chain.addAll(operands);
        this.operator = operator;
    }

    public LogicalExpressionNode and(LogicalExpressionNode other){
        return createLogicalAnd(this, other);
    }

    public LogicalExpressionNode or(LogicalExpressionNode other){
        return createLogicalOr(this, other);
    }

    public LogicalOperator getOperator() {
        return operator;
    }

    public LogicalExpressionNode append(ExpressionNode expressionNode) {
        chain.add(0, expressionNode);
        return this;
    }

    @Override
    public String toString() {
        return "(" + Utils.join(" " + operator.getOperatorString() + " ", chain) + ")";
    }

    @Override
    public boolean apply(PredicateContext ctx) {
        if(operator == LogicalOperator.OR){
            for (ExpressionNode expression : chain) {
                if(expression.apply(ctx)){
                    return true;
                }
            }
            return false;
        } else if (operator == LogicalOperator.AND) {
            for (ExpressionNode expression : chain) {
                if(!expression.apply(ctx)){
                    return false;
                }
            }
            return true;
        } else {
            ExpressionNode expression = chain.get(0);
            return !expression.apply(ctx);
        }
    }

}
