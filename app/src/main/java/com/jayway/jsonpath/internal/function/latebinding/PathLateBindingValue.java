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
package com.jayway.jsonpath.internal.function.latebinding;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.internal.Path;

/**
 * Defines the contract for late bindings, provides document state and enough context to perform the evaluation at a later
 * date such that we can operate on a dynamically changing value.
 *
 * Acts like a lambda function with references, but since we're supporting JDK 6+, we're left doing this...
 *
 */
public class PathLateBindingValue implements ILateBindingValue {
    private final Path path;
    private final Object rootDocument;
    private final Configuration configuration;

    public PathLateBindingValue(final Path path, final Object rootDocument, final Configuration configuration) {
        this.path = path;
        this.rootDocument = rootDocument;
        this.configuration = configuration;
    }

    /**
     * Evaluate the expression at the point of need for Path type expressions
     *
     * @return the late value
     */
    public Object get() {
        return path.evaluate(rootDocument, rootDocument, configuration).getValue();
    }
}
