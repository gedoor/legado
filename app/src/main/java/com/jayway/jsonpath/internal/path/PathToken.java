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

import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.internal.PathRef;
import com.jayway.jsonpath.internal.Utils;
import com.jayway.jsonpath.internal.function.PathFunction;
import com.jayway.jsonpath.spi.json.JsonProvider;

import java.util.List;

public abstract class PathToken {

    private PathToken prev;
    private PathToken next;
    private Boolean definite = null;
    private Boolean upstreamDefinite = null;

    PathToken appendTailToken(PathToken next) {
        this.next = next;
        this.next.prev = this;
        return next;
    }

    void handleObjectProperty(String currentPath, Object model, EvaluationContextImpl ctx, List<String> properties) {

        if(properties.size() == 1) {
            String property = properties.get(0);
            String evalPath = Utils.concat(currentPath, "['", property, "']");
            Object propertyVal = readObjectProperty(property, model, ctx);
            if(propertyVal == JsonProvider.UNDEFINED){
                // Conditions below heavily depend on current token type (and its logic) and are not "universal",
                // so this code is quite dangerous (I'd rather rewrite it & move to PropertyPathToken and implemented
                // WildcardPathToken as a dynamic multi prop case of PropertyPathToken).
                // Better safe than sorry.
                assert this instanceof PropertyPathToken : "only PropertyPathToken is supported";

                if(isLeaf()) {
                    if(ctx.options().contains(Option.DEFAULT_PATH_LEAF_TO_NULL)){
                        propertyVal =  null;
                    } else {
                        if(ctx.options().contains(Option.SUPPRESS_EXCEPTIONS) ||
                           !ctx.options().contains(Option.REQUIRE_PROPERTIES)){
                            return;
                        } else {
                            throw new PathNotFoundException("No results for path: " + evalPath);
                        }
                    }
                } else {
                    if (! (isUpstreamDefinite() && isTokenDefinite()) &&
                       !ctx.options().contains(Option.REQUIRE_PROPERTIES) ||
                       ctx.options().contains(Option.SUPPRESS_EXCEPTIONS)){
                        // If there is some indefiniteness in the path and properties are not required - we'll ignore
                        // absent property. And also in case of exception suppression - so that other path evaluation
                        // branches could be examined.
                        return;
                    } else {
                        throw new PathNotFoundException("Missing property in path " + evalPath);
                    }
                }
            }
            PathRef pathRef = ctx.forUpdate() ? PathRef.create(model, property) : PathRef.NO_OP;
            if (isLeaf()) {
                ctx.addResult(evalPath, pathRef, propertyVal);
            }
            else {
                next().evaluate(evalPath, pathRef, propertyVal, ctx);
            }
        } else {
            String evalPath = currentPath + "[" + Utils.join(", ", "'", properties) + "]";

            assert isLeaf() : "non-leaf multi props handled elsewhere";

            Object merged = ctx.jsonProvider().createMap();
            for (String property : properties) {
                Object propertyVal;
                if(hasProperty(property, model, ctx)) {
                    propertyVal = readObjectProperty(property, model, ctx);
                    if(propertyVal == JsonProvider.UNDEFINED){
                        if(ctx.options().contains(Option.DEFAULT_PATH_LEAF_TO_NULL)) {
                            propertyVal = null;
                        } else {
                            continue;
                        }
                    }
                } else {
                    if(ctx.options().contains(Option.DEFAULT_PATH_LEAF_TO_NULL)){
                        propertyVal = null;
                    } else if (ctx.options().contains(Option.REQUIRE_PROPERTIES)) {
                        throw new PathNotFoundException("Missing property in path " + evalPath);
                    } else {
                        continue;
                    }
                }
                ctx.jsonProvider().setProperty(merged, property, propertyVal);
            }
            PathRef pathRef = ctx.forUpdate() ? PathRef.create(model, properties) : PathRef.NO_OP;
            ctx.addResult(evalPath, pathRef, merged);
        }
    }

    private static boolean hasProperty(String property, Object model, EvaluationContextImpl ctx) {
        return ctx.jsonProvider().getPropertyKeys(model).contains(property);
    }

    private static Object readObjectProperty(String property, Object model, EvaluationContextImpl ctx) {
        return ctx.jsonProvider().getMapValue(model, property);
    }


    protected void handleArrayIndex(int index, String currentPath, Object model, EvaluationContextImpl ctx) {
        String evalPath = Utils.concat(currentPath, "[", String.valueOf(index), "]");
        PathRef pathRef = ctx.forUpdate() ? PathRef.create(model, index) : PathRef.NO_OP;
        int effectiveIndex = index < 0 ? ctx.jsonProvider().length(model) + index : index;
        try {
            Object evalHit = ctx.jsonProvider().getArrayIndex(model, effectiveIndex);
            if (isLeaf()) {
                ctx.addResult(evalPath, pathRef, evalHit);
            } else {
                next().evaluate(evalPath, pathRef, evalHit, ctx);
            }
        } catch (IndexOutOfBoundsException e) {
        }
    }

    PathToken prev(){
        return prev;
    }

    PathToken next() {
        if (isLeaf()) {
            throw new IllegalStateException("Current path token is a leaf");
        }
        return next;
    }

    boolean isLeaf() {
        return next == null;
    }

    boolean isRoot() {
        return  prev == null;
    }

    boolean isUpstreamDefinite() {
        if (upstreamDefinite == null) {
            upstreamDefinite = isRoot() || prev.isTokenDefinite() && prev.isUpstreamDefinite();
        }
        return upstreamDefinite;
    }

    public int getTokenCount() {
        int cnt = 1;
        PathToken token = this;

        while (!token.isLeaf()){
            token = token.next();
            cnt++;
        }
        return cnt;
    }

    public boolean isPathDefinite() {
        if(definite != null){
            return definite.booleanValue();
        }
        boolean isDefinite = isTokenDefinite();
        if (isDefinite && !isLeaf()) {
            isDefinite = next.isPathDefinite();
        }
        definite = isDefinite;
        return isDefinite;
    }

    @Override
    public String toString() {
        if (isLeaf()) {
            return getPathFragment();
        } else {
            return getPathFragment() + next().toString();
        }
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    public void invoke(PathFunction pathFunction, String currentPath, PathRef parent, Object model, EvaluationContextImpl ctx) {
        ctx.addResult(currentPath, parent, pathFunction.invoke(currentPath, parent, model, ctx, null));
    }

    public abstract void evaluate(String currentPath, PathRef parent,  Object model, EvaluationContextImpl ctx);

    public abstract boolean isTokenDefinite();

    protected abstract String getPathFragment();

    public void setNext(final PathToken next) {
        this.next = next;
    }
}
