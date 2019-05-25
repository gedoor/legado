/*
 * Copyright 2011 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jayway.jsonpath.spi.json;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.JsonPathException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class GsonJsonProvider extends AbstractJsonProvider {

    private static final JsonParser PARSER = new JsonParser();
    private final Gson gson;

    /**
     * Initializes the {@code GsonJsonProvider} using the default {@link Gson} object.
     */
    public GsonJsonProvider() {
        this(new Gson());
    }

    /**
     * Initializes the {@code GsonJsonProvider} using a customized {@link Gson} object.
     *
     * @param  gson  the customized Gson object.
     */
    public GsonJsonProvider(final Gson gson) {
        this.gson = gson;
    }

    public Object unwrap(final Object o) {

        if (o == null) {
            return null;
        }

        if (!(o instanceof JsonElement)) {
            return o;
        }

        JsonElement e = (JsonElement) o;

        if (e.isJsonNull()) {
            return null;
        } else if (e.isJsonPrimitive()) {

            JsonPrimitive p = e.getAsJsonPrimitive();
            if (p.isString()) {
                return p.getAsString();
            } else if (p.isBoolean()) {
                return p.getAsBoolean();
            } else if (p.isNumber()) {
                return unwrapNumber(p.getAsNumber());
            }
        }

        return o;
    }

    private static boolean isPrimitiveNumber(final Number n) {
        return n instanceof Integer ||
                n instanceof Double ||
                n instanceof Long ||
                n instanceof BigDecimal ||
                n instanceof BigInteger;
    }

    private static Number unwrapNumber(final Number n) {
        Number unwrapped;

        if (!isPrimitiveNumber(n)) {
            BigDecimal bigDecimal = new BigDecimal(n.toString());
            if (bigDecimal.scale() <= 0) {
                if (bigDecimal.compareTo(new BigDecimal(Integer.MAX_VALUE)) <= 0) {
                    unwrapped = bigDecimal.intValue();
                } else if (bigDecimal.compareTo(new BigDecimal(Long.MAX_VALUE)) <= 0){
                    unwrapped = bigDecimal.longValue();
                } else {
                    unwrapped = bigDecimal;
                }
            } else {
                final double doubleValue = bigDecimal.doubleValue();
                if (BigDecimal.valueOf(doubleValue).compareTo(bigDecimal) != 0) {
                    unwrapped = bigDecimal;
                } else {
                    unwrapped = doubleValue;
                }
            }
        } else {
            unwrapped = n;
        }
        return unwrapped;
    }

    @Override
    public Object parse(final String json) throws InvalidJsonException {
        return PARSER.parse(json);
    }

    @Override
    public Object parse(final InputStream jsonStream, final String charset) throws InvalidJsonException {

        try {
            return PARSER.parse(new InputStreamReader(jsonStream, charset));
        } catch (UnsupportedEncodingException e) {
            throw new JsonPathException(e);
        }
    }

    @Override
    public String toJson(final Object obj) {
        return gson.toJson(obj);
    }

    @Override
    public Object createArray() {
        return new JsonArray();
    }

    @Override
    public Object createMap() {
        return new JsonObject();
    }

    @Override
    public boolean isArray(final Object obj) {
        return (obj instanceof JsonArray || obj instanceof List);
    }

    @Override
    public Object getArrayIndex(final Object obj, final int idx) {
        return toJsonArray(obj).get(idx);
    }

    @Override
    public void setArrayIndex(final Object array, final int index, final Object newValue) {
        if (!isArray(array)) {
            throw new UnsupportedOperationException();
        } else {
            JsonArray arr = toJsonArray(array);
            if (index == arr.size()) {
                arr.add(createJsonElement(newValue));
            } else {
                arr.set(index, createJsonElement(newValue));
            }
        }
    }

    @Override
    public Object getMapValue(final Object obj, final String key) {
        JsonObject jsonObject = toJsonObject(obj);
        Object o = jsonObject.get(key);
        if (!jsonObject.has(key)) {
            return UNDEFINED;
        } else {
            return unwrap(o);
        }
    }

    @Override
    public void setProperty(final Object obj, final Object key, final Object value) {
        if (isMap(obj)) {
            toJsonObject(obj).add(key.toString(), createJsonElement(value));
        } else {
            JsonArray array = toJsonArray(obj);
            int index;
            if (key != null) {
                index = key instanceof Integer ? (Integer) key : Integer.parseInt(key.toString());
            } else {
                index = array.size();
            }

            if (index == array.size()) {
                array.add(createJsonElement(value));
            } else {
                array.set(index, createJsonElement(value));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void removeProperty(final Object obj, final Object key) {
        if (isMap(obj)) {
            toJsonObject(obj).remove(key.toString());
        } else {
            JsonArray array = toJsonArray(obj);
            int index = key instanceof Integer ? (Integer) key : Integer.parseInt(key.toString());
            array.remove(index);
        }
    }

    @Override
    public boolean isMap(final Object obj) {

        // return (obj instanceof JsonObject || obj instanceof Map);
        return (obj instanceof JsonObject);
    }

    @Override
    public Collection<String> getPropertyKeys(final Object obj) {
        List<String> keys = new ArrayList<String>();
        for (Map.Entry<String, JsonElement> entry : toJsonObject(obj).entrySet()) {
            keys.add(entry.getKey());
        }

        return keys;
    }

    @Override
    public int length(final Object obj) {
        if (isArray(obj)) {
            return toJsonArray(obj).size();
        } else if (isMap(obj)) {
            return toJsonObject(obj).entrySet().size();
        } else {
            if (obj instanceof JsonElement) {
                JsonElement element = toJsonElement(obj);
                if (element.isJsonPrimitive()) {
                    return element.toString().length();
                }
            }
        }

        throw new JsonPathException("length operation can not applied to " + obj != null ? obj.getClass().getName()
                                                                                         : "null");
    }

    @Override
    public Iterable<?> toIterable(final Object obj) {
        JsonArray arr = toJsonArray(obj);
        List<Object> values = new ArrayList<Object>(arr.size());
        for (Object o : arr) {
            values.add(unwrap(o));
        }

        return values;
    }

    private JsonElement createJsonElement(final Object o) {
        return gson.toJsonTree(o);
    }

    private JsonArray toJsonArray(final Object o) {
        return (JsonArray) o;
    }

    private JsonObject toJsonObject(final Object o) {
        return (JsonObject) o;
    }

    private JsonElement toJsonElement(final Object o) {
        return (JsonElement) o;
    }
}
