package com.jayway.jsonpath.internal.function.numeric;

/**
 * Provides the average of a series of numbers in a JSONArray
 *
 * Created by mattg on 6/26/15.
 */
public class Average extends AbstractAggregation {

    private Double summation = 0d;
    private Double count = 0d;

    @Override
    protected void next(Number value) {
        count++;
        summation += value.doubleValue();
    }

    @Override
    protected Number getValue() {
        if (count != 0d) {
            return summation / count;
        }
        return 0d;
    }
}
