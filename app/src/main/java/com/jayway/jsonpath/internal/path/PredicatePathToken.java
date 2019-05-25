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

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.Predicate;
import com.jayway.jsonpath.internal.PathRef;

import java.util.Collection;
import java.util.Collections;

import static java.lang.String.format;
import static java.util.Arrays.asList;

/**
 *
 */
public class PredicatePathToken extends PathToken {


    private final Collection<Predicate> predicates;

    PredicatePathToken(Predicate filter) {
        this.predicates = Collections.singletonList(filter);
    }

    PredicatePathToken(Collection<Predicate> predicates) {
        this.predicates = predicates;
    }

    @Override
    public void evaluate(String currentPath, PathRef ref, Object model, EvaluationContextImpl ctx) {
        if (ctx.jsonProvider().isMap(model)) {
            if (accept(model, ctx.rootDocument(), ctx.configuration(), ctx)) {
                PathRef op = ctx.forUpdate() ? ref : PathRef.NO_OP;
                if (isLeaf()) {
                    ctx.addResult(currentPath, op, model);
                } else {
                    next().evaluate(currentPath, op, model, ctx);
                }
            }
        } else if (ctx.jsonProvider().isArray(model)){
            int idx = 0;
            Iterable<?> objects = ctx.jsonProvider().toIterable(model);

            for (Object idxModel : objects) {
                if (accept(idxModel, ctx.rootDocument(),  ctx.configuration(), ctx)) {
                    handleArrayIndex(idx, currentPath, model, ctx);
                }
                idx++;
            }
        } else {
            if (isUpstreamDefinite()) {
                throw new InvalidPathException(format("Filter: %s can not be applied to primitives. Current context is: %s", toString(), model));
            }
        }
    }

    public boolean accept(final Object obj, final Object root, final Configuration configuration, EvaluationContextImpl evaluationContext) {
        Predicate.PredicateContext ctx = new PredicateContextImpl(obj, root, configuration, evaluationContext.documentEvalCache());

        for (Predicate predicate : predicates) {
            try {
                if (!predicate.apply(ctx)) {
                    return false;
                }
            } catch (InvalidPathException e) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getPathFragment() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for(int i = 0; i < predicates.size(); i++){
            if(i != 0){
                sb.append(",");
            }
            sb.append("?");
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public boolean isTokenDefinite() {
        return false;
    }



}
