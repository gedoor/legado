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

import com.jayway.jsonpath.InvalidJsonException;

import java.io.InputStream;
import java.util.Collection;

public interface JsonProvider {

    static final Object UNDEFINED = new Object();

    /**
     * Parse the given json string
     * @param json json string to parse
     * @return Object representation of json
     * @throws InvalidJsonException
     */
    Object parse(String json) throws InvalidJsonException;

    /**
     * Parse the given json string
     * @param jsonStream input stream to parse
     * @param charset charset to use
     * @return Object representation of json
     * @throws InvalidJsonException
     */
    Object parse(InputStream jsonStream, String charset) throws InvalidJsonException;

    /**
     * Convert given json object to a json string
     * @param obj object to transform
     * @return json representation of object
     */
    String toJson(Object obj);

    /**
     * Creates a provider specific json array
     * @return new array
     */
    Object createArray();

    /**
     * Creates a provider specific json object
     * @return new object
     */
    Object createMap();

    /**
     * checks if object is an array
     *
     * @param obj object to check
     * @return true if obj is an array
     */
    boolean isArray(Object obj);

    /**
     * Get the length of an json array, json object or a json string
     *
     * @param obj an array or object or a string
     * @return the number of entries in the array or object
     */
    int length(Object obj);

    /**
     * Converts given array to an {@link Iterable}
     *
     * @param obj an array
     * @return an Iterable that iterates over the entries of an array
     */
    Iterable<?> toIterable(Object obj);


    /**
     * Returns the keys from the given object
     *
     * @param obj an object
     * @return the keys for an object
     */
    Collection<String> getPropertyKeys(Object obj);

    /**
     * Extracts a value from an array anw unwraps provider specific data type
     *
     * @param obj an array
     * @param idx index
     * @return the entry at the given index
     */
    Object getArrayIndex(Object obj, int idx);

    /**
     * Extracts a value from an array
     *
     * @param obj an array
     * @param idx index
     * @param unwrap should provider specific data type be unwrapped
     * @return the entry at the given index
     */
    @Deprecated
    Object getArrayIndex(Object obj, int idx, boolean unwrap);

    /**
     * Sets a value in an array. If the array is too small, the provider is supposed to enlarge it.
     *
     * @param array an array
     * @param idx index
     * @param newValue the new value
     */
    void setArrayIndex(Object array, int idx, Object newValue);

    /**
     * Extracts a value from an map
     *
     * @param obj a map
     * @param key property key
     * @return the map entry or {@link com.jayway.jsonpath.spi.json.JsonProvider#UNDEFINED} for missing properties
     */
    Object getMapValue(Object obj, String key);

    /**
     * Sets a value in an object
     *
     * @param obj   an object
     * @param key   a String key
     * @param value the value to set
     */
    void setProperty(Object obj, Object key, Object value);

    /**
     * Removes a value in an object or array
     *
     * @param obj   an array or an object
     * @param key   a String key or a numerical index to remove
     */
    void removeProperty(Object obj, Object key);

    /**
     * checks if object is a map (i.e. no array)
     *
     * @param obj object to check
     * @return true if the object is a map
     */
    boolean isMap(Object obj);

    /**
     * Extracts a value from a wrapper object. For JSON providers that to not wrap
     * values, this will usually be the object itself.
     *
     * @param obj a value holder object
     * @return the unwrapped value.
     */
    Object unwrap(Object obj);
}
