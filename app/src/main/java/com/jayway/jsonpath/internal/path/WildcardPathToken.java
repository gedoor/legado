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

import java.util.Collections;

import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.internal.PathRef;

import static java.util.Arrays.asList;

/**
 *
 */
public class WildcardPathToken extends PathToken {

    WildcardPathToken() {
    }

    @Override
    public void evaluate(String currentPath, PathRef parent, Object model, EvaluationContextImpl ctx) {
        if (ctx.jsonProvider().isMap(model)) {
            for (String property : ctx.jsonProvider().getPropertyKeys(model)) {
                handleObjectProperty(currentPath, model, ctx, Collections.singletonList(property));
            }
        } else if (ctx.jsonProvider().isArray(model)) {
            for (int idx = 0; idx < ctx.jsonProvider().length(model); idx++) {
                try {
                    handleArrayIndex(idx, currentPath, model, ctx);
                } catch (PathNotFoundException p){
                    if(ctx.options().contains(Option.REQUIRE_PROPERTIES)){
                        throw p;
                    }
                }
            }
        }
    }


    @Override
    public boolean isTokenDefinite() {
        return false;
    }

    @Override
    public String getPathFragment() {
        return "[*]";
    }
}
