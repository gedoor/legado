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

import com.jayway.jsonpath.internal.function.Parameter;
import com.jayway.jsonpath.spi.json.JsonProvider;

/**
 * Defines the JSON document Late binding approach to function arguments.
 *
 */
public class JsonLateBindingValue implements ILateBindingValue {
    private final JsonProvider jsonProvider;
    private final Parameter jsonParameter;

    public JsonLateBindingValue(JsonProvider jsonProvider, Parameter jsonParameter) {
        this.jsonProvider = jsonProvider;
        this.jsonParameter = jsonParameter;
    }

    /**
     * Evaluate the JSON document at the point of need using the JSON parameter and associated document model which may
     * itself originate from yet another function thus recursively invoking late binding methods.
     *
     * @return the late value
     */
    @Override
    public Object get() {
        return jsonProvider.parse(jsonParameter.getJson());
    }
}
