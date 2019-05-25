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
package com.jayway.jsonpath.internal;

import com.jayway.jsonpath.Configuration;

import java.util.Collection;
import java.util.List;

public interface EvaluationContext {

    /**
     *
     * @return the configuration used for this evaluation
     */
    Configuration configuration();

    /**
     * The json document that is evaluated
     *
     * @return the document
     */
    Object rootDocument();

    /**
     * This method does not adhere to configuration settings. It will return a single object (not wrapped in a List) even if the
     * configuration contains the {@link com.jayway.jsonpath.Option#ALWAYS_RETURN_LIST}
     *
     * @param <T> expected return type
     * @return evaluation result
     */
    <T> T getValue();

    /**
     * See {@link com.jayway.jsonpath.internal.EvaluationContext#getValue()}
     *
     * @param unwrap tells th underlying json provider if primitives should be unwrapped
     * @param <T> expected return type
     * @return evaluation result
     */
    <T> T getValue(boolean unwrap);


    /**
     * Returns the list of formalized paths that represent the result of the evaluation
     * @param <T>
     * @return list of paths
     */
    <T> T getPath();


    /**
     * Convenience method to get list of hits as String path representations
     *
     * @return list of path representations
     */
    List<String> getPathList();

    Collection<PathRef> updateOperations();

}
