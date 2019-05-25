package com.jayway.jsonpath.internal.function.numeric;

/**
 * Defines the summation of a series of JSONArray numerical values
 *
 * Created by mattg on 6/26/15.
 */
public class Sum extends AbstractAggregation {
    private Double summation = 0d;

    @Override
    protected void next(Number value) {
        summation += value.doubleValue();
    }

    @Override
    protected Number getValue() {
        return summation;
    }
}
