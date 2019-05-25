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

/**
 *
 */
public class RootPathToken extends PathToken {

    private PathToken tail;
    private int tokenCount;
    private final String rootToken;


    RootPathToken(char rootToken) {
        this.rootToken = Character.toString(rootToken);
        this.tail = this;
        this.tokenCount = 1;
    }

    @Override
    public int getTokenCount() {
        return tokenCount;
    }

    public RootPathToken append(PathToken next) {
        this.tail = tail.appendTailToken(next);
        this.tokenCount++;
        return this;
    }

    public PathTokenAppender getPathTokenAppender(){
        return new PathTokenAppender(){
            @Override
            public PathTokenAppender appendPathToken(PathToken next) {
                append(next);
                return this;
            }
        };
    }

    @Override
    public void evaluate(String currentPath, PathRef pathRef, Object model, EvaluationContextImpl ctx) {
        if (isLeaf()) {
            PathRef op = ctx.forUpdate() ?  pathRef : PathRef.NO_OP;
            ctx.addResult(rootToken, op, model);
        } else {
            next().evaluate(rootToken, pathRef, model, ctx);
        }
    }

    @Override
    public String getPathFragment() {
        return rootToken;
    }

    @Override
    public boolean isTokenDefinite() {
        return true;
    }

    public boolean isFunctionPath() {
        return (tail instanceof FunctionPathToken);
    }

    public void setTail(PathToken token) {
        this.tail = token;
    }
}
