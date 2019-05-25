package com.jayway.jsonpath.internal.function;

import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.internal.function.json.Append;
import com.jayway.jsonpath.internal.function.numeric.Average;
import com.jayway.jsonpath.internal.function.numeric.Max;
import com.jayway.jsonpath.internal.function.numeric.Min;
import com.jayway.jsonpath.internal.function.numeric.StandardDeviation;
import com.jayway.jsonpath.internal.function.numeric.Sum;
import com.jayway.jsonpath.internal.function.text.Concatenate;
import com.jayway.jsonpath.internal.function.text.Length;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Implements a factory that given a name of the function will return the Function implementation, or null
 * if the value is not obtained.
 *
 * Leverages the function's name in order to determine which function to execute which is maintained internally
 * here via a static map
 *
 */
public class PathFunctionFactory {

    public static final Map<String, Class> FUNCTIONS;

    static {
        // New functions should be added here and ensure the name is not overridden
        Map<String, Class> map = new HashMap<String, Class>();

        // Math Functions
        map.put("avg", Average.class);
        map.put("stddev", StandardDeviation.class);
        map.put("sum", Sum.class);
        map.put("min", Min.class);
        map.put("max", Max.class);

        // Text Functions
        map.put("concat", Concatenate.class);

        // JSON Entity Functions
        map.put("length", Length.class);
        map.put("size", Length.class);
        map.put("append", Append.class);


        FUNCTIONS = Collections.unmodifiableMap(map);
    }

    /**
     * Returns the function by name or throws InvalidPathException if function not found.
     *
     * @see #FUNCTIONS
     * @see PathFunction
     *
     * @param name
     *      The name of the function
     *
     * @return
     *      The implementation of a function
     *
     * @throws InvalidPathException
     */
    public static PathFunction newFunction(String name) throws InvalidPathException {
        Class functionClazz = FUNCTIONS.get(name);
        if(functionClazz == null){
            throw new InvalidPathException("Function with name: " + name + " does not exist.");
        } else {
            try {
                return (PathFunction)functionClazz.newInstance();
            } catch (Exception e) {
                throw new InvalidPathException("Function of name: " + name + " cannot be created", e);
            }
        }
    }
}
