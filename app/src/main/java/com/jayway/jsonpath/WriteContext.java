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
package com.jayway.jsonpath;

public interface WriteContext {

    /**
     * Returns the configuration used for reading
     *
     * @return an immutable configuration
     */
    Configuration configuration();

    /**
     * Returns the JSON model that this context is operating on
     *
     * @return json model
     */
    <T> T json();

    /**
     * Returns the JSON model that this context is operating on as a JSON string
     *
     * @return json model as string
     */
    String jsonString();

    /**
     * Set the value a the given path
     *
     * @param path      path to set
     * @param newValue  new value
     * @param filters   filters
     * @return a document context
     */
    DocumentContext set(String path, Object newValue, Predicate... filters);

    /**
     * Set the value a the given path
     *
     * @param path      path to set
     * @param newValue  new value
     * @return a document context
     */
    DocumentContext set(JsonPath path, Object newValue);

    /**
     * Replaces the value on the given path with the result of the {@link MapFunction}.
     *
     * @param path           path to be converted set
     * @param mapFunction    Converter object to be invoked
     * @param filters        filters
     * @return a document context
     */
    DocumentContext map(String path, MapFunction mapFunction, Predicate... filters);

    /**
     * Replaces the value on the given path with the result of the {@link MapFunction}.
     *
     * @param path           path to be converted set
     * @param mapFunction    Converter object to be invoked (or lambda:))
     * @return a document context
     */
    DocumentContext map(JsonPath path, MapFunction mapFunction);

    /**
     * Deletes the given path
     *
     * @param path    path to delete
     * @param filters filters
     * @return a document context
     */
    DocumentContext delete(String path, Predicate... filters);

    /**
     * Deletes the given path
     *
     * @param path    path to delete
     * @return a document context
     */
    DocumentContext delete(JsonPath path);

    /**
     * Add value to array
     *
     * <pre>
     * <code>
     * List<Integer> array = new ArrayList<Integer>(){{
     *      add(0);
     *      add(1);
     * }};
     *
     * JsonPath.parse(array).add("$", 2);
     *
     * assertThat(array).containsExactly(0,1,2);
     * </code>
     * </pre>
     *
     * @param path    path to array
     * @param value   value to add
     * @param filters filters
     * @return a document context
     */
    DocumentContext add(String path, Object value, Predicate... filters);

    /**
     * Add value to array at the given path
     *
     * @param path    path to array
     * @param value   value to add
     * @return a document context
     */
    DocumentContext add(JsonPath path, Object value);

    /**
     * Add or update the key with a the given value at the given path
     *
     * @param path    path to object
     * @param key     key to add
     * @param value   value of key
     * @param filters filters
     * @return a document context
     */
    DocumentContext put(String path, String key, Object value, Predicate... filters);

    /**
     * Add or update the key with a the given value at the given path
     *
     * @param path    path to array
     * @param key     key to add
     * @param value   value of key
     * @return a document context
     */
    DocumentContext put(JsonPath path, String key, Object value);

    /**
     * Renames the last key element of a given path.
     * @param path          The path to the old key. Should be resolved to a map
     *                      or an array including map items.
     * @param oldKeyName    The old key name.
     * @param newKeyName    The new key name.
     * @param filters       filters.
     * @return a document content.
     */
    DocumentContext renameKey(String path, String oldKeyName, String newKeyName, Predicate... filters);

    /**
     * Renames the last key element of a given path.
     * @param path          The path to the old key. Should be resolved to a map
     *                      or an array including map items.
     * @param oldKeyName    The old key name.
     * @param newKeyName    The new key name.
     * @return a document content.
     */
    DocumentContext renameKey(JsonPath path, String oldKeyName, String newKeyName);
}