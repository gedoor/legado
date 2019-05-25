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

public class ArraySliceToken extends ArrayPathToken {

    private final ArraySliceOperation operation;

    ArraySliceToken(final ArraySliceOperation operation) {
        this.operation = operation;
    }

    @Override
    public void evaluate(String currentPath, PathRef parent, Object model, EvaluationContextImpl ctx) {
        if (!checkArrayModel(currentPath, model, ctx))
            return;
        switch (operation.operation()) {
            case SLICE_FROM:
                sliceFrom(currentPath, parent, model, ctx);
                break;
            case SLICE_BETWEEN:
                sliceBetween(currentPath, parent, model, ctx);
                break;
            case SLICE_TO:
                sliceTo(currentPath, parent, model, ctx);
                break;
        }
    }

    private void sliceFrom(String currentPath, PathRef parent, Object model, EvaluationContextImpl ctx) {
        int length = ctx.jsonProvider().length(model);
        int from = operation.from();
        if (from < 0) {
            //calculate slice start from array length
            from = length + from;
        }
        from = Math.max(0, from);

        if (length == 0 || from >= length) {
            return;
        }
        for (int i = from; i < length; i++) {
            handleArrayIndex(i, currentPath, model, ctx);
        }
    }

    private void sliceBetween(String currentPath, PathRef parent, Object model, EvaluationContextImpl ctx) {
        int length = ctx.jsonProvider().length(model);
        int from = operation.from();
        int to = operation.to();

        to = Math.min(length, to);

        if (from >= to || length == 0) {
            return;
        }

        for (int i = from; i < to; i++) {
            handleArrayIndex(i, currentPath, model, ctx);
        }
    }

    private void sliceTo(String currentPath, PathRef parent, Object model, EvaluationContextImpl ctx) {
        int length = ctx.jsonProvider().length(model);
        if (length == 0) {
            return;
        }
        int to = operation.to();
        if (to < 0) {
            //calculate slice end from array length
            to = length + to;
        }
        to = Math.min(length, to);

        for (int i = 0; i < to; i++) {
            handleArrayIndex(i, currentPath, model, ctx);
        }
    }

    @Override
    public String getPathFragment() {
        return operation.toString();
    }

    @Override
    public boolean isTokenDefinite() {
        return false;
    }

}
