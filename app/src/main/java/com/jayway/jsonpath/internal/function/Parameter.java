package com.jayway.jsonpath.internal.function;

import com.jayway.jsonpath.internal.EvaluationContext;
import com.jayway.jsonpath.internal.Path;
import com.jayway.jsonpath.internal.function.latebinding.ILateBindingValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Defines a parameter as passed to a function with late binding support for lazy evaluation.
 */
public class Parameter {
    private ParamType type;
    private Path path;
    private ILateBindingValue lateBinding;
    private Boolean evaluated = false;
    private String json;

    public Parameter() {}

    public Parameter(String json) {
        this.json = json;
        this.type = ParamType.JSON;
    }

    public Parameter(Path path) {
        this.path = path;
        this.type = ParamType.PATH;
    }

    public Object getValue() {
        return lateBinding.get();
    }

    public void setLateBinding(ILateBindingValue lateBinding) {
        this.lateBinding = lateBinding;
    }

    public Path getPath() {
        return path;
    }

    public void setEvaluated(Boolean evaluated) {
        this.evaluated = evaluated;
    }

    public boolean hasEvaluated() {
        return evaluated;
    }

    public ParamType getType() {
        return type;
    }

    public void setType(ParamType type) {
        this.type = type;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    /**
     * Translate the collection of parameters into a collection of values of type T.
     *
     * @param type
     *      The type to translate the collection into.
     *
     * @param ctx
     *      Context.
     *
     * @param parameters
     *      Collection of parameters.
     *
     * @param <T>
     *      Type T returned as a List of T.
     *
     * @return
     *      List of T either empty or containing contents.
     */
    public static <T> List<T> toList(final Class<T> type, final EvaluationContext ctx, final List<Parameter> parameters) {
        List<T> values = new ArrayList();
        if (null != parameters) {
            for (Parameter param : parameters) {
                consume(type, ctx, values, param.getValue());
            }
        }
        return values;
    }

    /**
     * Either consume the object as an array and add each element to the collection, or alternatively add each element
     *
     * @param expectedType
     *      the expected class type to consume, if null or not of this type the element is not added to the array.
     *
     * @param ctx
     *      the JSON context to determine if this is an array or value.
     *
     * @param collection
     *      The collection to append into.
     *
     * @param value
     *      The value to evaluate.
     */
    public static void consume(Class expectedType, EvaluationContext ctx, Collection collection, Object value) {
        if (ctx.configuration().jsonProvider().isArray(value)) {
            for (Object o : ctx.configuration().jsonProvider().toIterable(value)) {
                if (o != null && expectedType.isAssignableFrom(o.getClass())) {
                    collection.add(o);
                } else if (o != null && expectedType == String.class) {
                    collection.add(o.toString());
                }
            }
        } else {
            if (value != null && expectedType.isAssignableFrom(value.getClass())) {
                collection.add(value);
            }
        }
    }
}
