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

/**
 * A listener that can be registered on a {@link com.jayway.jsonpath.Configuration} that is notified when a
 * result is added to the result of this path evaluation.
 */
public interface EvaluationListener {

    /**
     * Callback invoked when result is found
     * @param found the found result
     * @return continuation instruction
     */
    EvaluationContinuation resultFound(FoundResult found);

    enum EvaluationContinuation {
        /**
         * Evaluation continues
         */
        CONTINUE,
        /**
         * Current result is included but no further evaluation will be performed.
         */
        ABORT
    }

    /**
     *
     */
    interface FoundResult {
        /**
         * the index of this result. First result i 0
         * @return index
         */
        int index();

        /**
         * The path of this result
         * @return path
         */
        String path();

        /**
         * The result object
         * @return the result object
         */
        Object result();
    }
}
