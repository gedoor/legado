package com.jayway.jsonpath.internal.path;

import com.jayway.jsonpath.Predicate;
import com.jayway.jsonpath.internal.function.Parameter;

import java.util.Collection;
import java.util.List;

import static java.util.Collections.singletonList;

public class PathTokenFactory {

    public static RootPathToken createRootPathToken(char token) {
        return new RootPathToken(token);
    }

    public static PathToken createSinglePropertyPathToken(String property, char stringDelimiter) {
        return new PropertyPathToken(singletonList(property), stringDelimiter);
    }

    public static PathToken createPropertyPathToken(List<String> properties, char stringDelimiter) {
        return new PropertyPathToken(properties, stringDelimiter);
    }

    public static PathToken createSliceArrayPathToken(final ArraySliceOperation arraySliceOperation) {
        return new ArraySliceToken(arraySliceOperation);
    }

    public static PathToken createIndexArrayPathToken(final ArrayIndexOperation arrayIndexOperation) {
        return new ArrayIndexToken(arrayIndexOperation);
    }

    public static PathToken createWildCardPathToken() {
        return new WildcardPathToken();
    }

    public static PathToken crateScanToken() {
        return new ScanPathToken();
    }

    public static PathToken createPredicatePathToken(Collection<Predicate> predicates) {
        return new PredicatePathToken(predicates);
    }

    public static PathToken createPredicatePathToken(Predicate predicate) {
        return new PredicatePathToken(predicate);
    }

    public static PathToken createFunctionPathToken(String function, List<Parameter> parameters) {
        return new FunctionPathToken(function, parameters);
    }
}
