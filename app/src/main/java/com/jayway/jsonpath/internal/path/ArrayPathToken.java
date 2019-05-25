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
package com.jayway.jsonpath.internal.path;

import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.PathNotFoundException;

import static java.lang.String.format;

public abstract class ArrayPathToken extends PathToken {

    /**
     * Check if model is non-null and array.
     * @param currentPath
     * @param model
     * @param ctx
     * @return false if current evaluation call must be skipped, true otherwise
     * @throws PathNotFoundException if model is null and evaluation must be interrupted
     * @throws InvalidPathException if model is not an array and evaluation must be interrupted
     */
    protected boolean checkArrayModel(String currentPath, Object model, EvaluationContextImpl ctx) {
        if (model == null){
            if (! isUpstreamDefinite()) {
                return false;
            } else {
                throw new PathNotFoundException("The path " + currentPath + " is null");
            }
        }
        if (!ctx.jsonProvider().isArray(model)) {
            if (! isUpstreamDefinite()) {
                return false;
            } else {
                throw new PathNotFoundException(format("Filter: %s can only be applied to arrays. Current context is: %s", toString(), model));
            }
        }
        return true;
    }
}
