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
import com.jayway.jsonpath.internal.EvaluationAbortException;
import com.jayway.jsonpath.internal.EvaluationContext;
import com.jayway.jsonpath.internal.Path;
import com.jayway.jsonpath.internal.PathRef;
import com.jayway.jsonpath.internal.function.ParamType;
import com.jayway.jsonpath.internal.function.Parameter;

import java.util.Arrays;

public class CompiledPath implements Path {

    private final RootPathToken root;

    private final boolean isRootPath;


    public CompiledPath(RootPathToken root, boolean isRootPath) {
        this.root = invertScannerFunctionRelationship(root);
        this.isRootPath = isRootPath;
    }

    @Override
    public boolean isRootPath() {
        return isRootPath;
    }



    /**
     * In the event the writer of the path referenced a function at the tail end of a scanner, augment the query such
     * that the root node is the function and the parameter to the function is the scanner.   This way we maintain
     * relative sanity in the path expression, functions either evaluate scalar values or arrays, they're
     * not re-entrant nor should they maintain state, they do however take parameters.
     *
     * @param path
     *      this is our old root path which will become a parameter (assuming there's a scanner terminated by a function
     *
     * @return
     *      A function with the scanner as input, or if this situation doesn't exist just the input path
     */
    private RootPathToken invertScannerFunctionRelationship(final RootPathToken path) {
        if (path.isFunctionPath() && path.next() instanceof ScanPathToken) {
            PathToken token = path;
            PathToken prior = null;
            while (null != (token = token.next()) && !(token instanceof FunctionPathToken)) {
                prior = token;
            }
            // Invert the relationship $..path.function() to $.function($..path)
            if (token instanceof FunctionPathToken) {
                prior.setNext(null);
                path.setTail(prior);

                // Now generate a new parameter from our path
                Parameter parameter = new Parameter();
                parameter.setPath(new CompiledPath(path, true));
                parameter.setType(ParamType.PATH);
                ((FunctionPathToken)token).setParameters(Arrays.asList(parameter));
                RootPathToken functionRoot = new RootPathToken('$');
                functionRoot.setTail(token);
                functionRoot.setNext(token);

                // Define the function as the root
                return functionRoot;
            }
        }
        return path;
    }

    @Override
    public EvaluationContext evaluate(Object document, Object rootDocument, Configuration configuration, boolean forUpdate) {
        EvaluationContextImpl ctx = new EvaluationContextImpl(this, rootDocument, configuration, forUpdate);
        try {
            PathRef op = ctx.forUpdate() ?  PathRef.createRoot(rootDocument) : PathRef.NO_OP;
            root.evaluate("", op, document, ctx);
        } catch (EvaluationAbortException abort) {}

        return ctx;
    }

    @Override
    public EvaluationContext evaluate(Object document, Object rootDocument, Configuration configuration){
        return evaluate(document, rootDocument, configuration, false);
    }

    @Override
    public boolean isDefinite() {
        return root.isPathDefinite();
    }

    @Override
    public boolean isFunctionPath() {
        return root.isFunctionPath();
    }

    @Override
    public String toString() {
        return root.toString();
    }
}
