package com.jayway.jsonpath.internal.function.numeric;

/**
 * Provides the standard deviation of a series of numbers
 *
 * Created by mattg on 6/27/15.
 */
public class StandardDeviation extends AbstractAggregation {
    private Double sumSq = 0d;
    private Double sum = 0d;
    private Double count = 0d;

    @Override
    protected void next(Number value) {
        sum += value.doubleValue();
        sumSq += value.doubleValue() * value.doubleValue();
        count++;
    }

    @Override
    protected Number getValue() {
        return Math.sqrt(sumSq/count - sum*sum/count/count);
    }
}
