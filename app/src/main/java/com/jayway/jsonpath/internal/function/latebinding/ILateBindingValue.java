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

/**
 * Obtain the late binding value at runtime rather than storing the value in the cache thus trashing the cache
 *
 */
public interface ILateBindingValue {
    /**
     * Obtain the value of the parameter at runtime using the parameter state and invocation of other late binding values
     * rather than maintaining cached state which ends up in a global store and won't change as a result of external
     * reference changes.
     *
     * @return
     *      The value of evaluating the context at runtime.
     */
    Object get();
}
