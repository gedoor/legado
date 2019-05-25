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

/**
 *
 */
public interface Path {


    /**
     * Evaluates this path
     *
     * @param document the json document to apply the path on
     * @param rootDocument the root json document that started this evaluation
     * @param configuration configuration to use
     * @return EvaluationContext containing results of evaluation
     */
    EvaluationContext evaluate(Object document, Object rootDocument, Configuration configuration);

    /**
     * Evaluates this path
     *
     * @param document the json document to apply the path on
     * @param rootDocument the root json document that started this evaluation
     * @param configuration configuration to use
     * @param forUpdate is this a read or a write operation
     * @return EvaluationContext containing results of evaluation
     */
    EvaluationContext evaluate(Object document, Object rootDocument, Configuration configuration, boolean forUpdate);

    /**
     *
     * @return true id this path is definite
     */
    boolean isDefinite();

    /**
     *
     * @return true id this path is a function
     */
    boolean isFunctionPath();

    /**
     *
     * @return true id this path is starts with '$' and false if the path starts with '@'
     */
    boolean isRootPath();

}
