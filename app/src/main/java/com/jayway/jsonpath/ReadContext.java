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

public interface ReadContext {

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
     * Reads the given path from this context
     *
     * @param path    path to read
     * @param filters filters
     * @param <T>
     * @return result
     */
    <T> T read(String path, Predicate... filters);

    /**
     * Reads the given path from this context
     *
     * @param path    path to read
     * @param type    expected return type (will try to map)
     * @param filters filters
     * @param <T>
     * @return result
     */
    <T> T read(String path, Class<T> type, Predicate... filters);

    /**
     * Reads the given path from this context
     *
     * @param path path to apply
     * @param <T>
     * @return result
     */
    <T> T read(JsonPath path);

    /**
     * Reads the given path from this context
     *
     * @param path path to apply
     * @param type    expected return type (will try to map)
     * @param <T>
     * @return result
     */
    <T> T read(JsonPath path, Class<T> type);

    /**
     * Reads the given path from this context
     *
     * Sample code to create a TypeRef
     * <code>
     *       TypeRef ref = new TypeRef<List<Integer>>() {};
     * </code>
     *
     * @param path path to apply
     * @param typeRef  expected return type (will try to map)
     * @param <T>
     * @return result
     */
    <T> T read(JsonPath path, TypeRef<T> typeRef);

    /**
     * Reads the given path from this context
     *
     * Sample code to create a TypeRef
     * <code>
     *       TypeRef ref = new TypeRef<List<Integer>>() {};
     * </code>
     *
     * @param path path to apply
     * @param typeRef  expected return type (will try to map)
     * @param <T>
     * @return result
     */
    <T> T read(String path, TypeRef<T> typeRef);

    /**
     * Stops evaluation when maxResults limit has been reached
     * @param maxResults
     * @return the read context
     */
    ReadContext limit(int maxResults);

    /**
     * Adds listener to the evaluation of this path
     * @param listener listeners to add
     * @return the read context
     */
    ReadContext withListeners(EvaluationListener... listener);

}
