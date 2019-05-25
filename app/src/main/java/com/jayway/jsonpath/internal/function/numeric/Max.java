package com.jayway.jsonpath.internal.function.numeric;

/**
 * Defines the summation of a series of JSONArray numerical values
 *
 * Created by mattg on 6/26/15.
 */
public class Max extends AbstractAggregation {
    private Double max = Double.MIN_VALUE;

    @Override
    protected void next(Number value) {
        if (max < value.doubleValue()) {
            max = value.doubleValue();
        }
    }

    @Override
    protected Number getValue() {
        return max;
    }
}
