package com.jayway.jsonpath.internal.filter;

import com.jayway.jsonpath.Predicate;

public interface Evaluator {
    boolean evaluate(ValueNode left, ValueNode right, Predicate.PredicateContext ctx);
}