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

import com.jayway.jsonpath.internal.PathRef;

import static java.lang.String.format;

public class ArrayIndexToken extends ArrayPathToken {

    private final ArrayIndexOperation arrayIndexOperation;

    ArrayIndexToken(final ArrayIndexOperation arrayIndexOperation) {
        this.arrayIndexOperation = arrayIndexOperation;
    }

    @Override
    public void evaluate(String currentPath, PathRef parent, Object model, EvaluationContextImpl ctx) {
        if (!checkArrayModel(currentPath, model, ctx))
            return;
        if (arrayIndexOperation.isSingleIndexOperation()) {
            handleArrayIndex(arrayIndexOperation.indexes().get(0), currentPath, model, ctx);
        } else {
            for (Integer index : arrayIndexOperation.indexes()) {
                handleArrayIndex(index, currentPath,  model, ctx);
            }
        }
    }

    @Override
    public String getPathFragment() {
        return arrayIndexOperation.toString();
    }

    @Override
    public boolean isTokenDefinite() {
        return arrayIndexOperation.isSingleIndexOperation();
    }

}
