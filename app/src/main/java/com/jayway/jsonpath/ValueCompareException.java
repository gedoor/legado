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
package com.jayway.jsonpath;

public class ValueCompareException extends JsonPathException {

    public ValueCompareException() {
    }

    /**
     * Construct the exception with message capturing the classes for two objects.
     *
     * @param left first object
     * @param right second object
     */
    public ValueCompareException(final Object left, final Object right) {
        super(String.format("Can not compare a %1s with a %2s", left.getClass().getName(), right.getClass().getName()));
    }

    public ValueCompareException(String message) {
        super(message);
    }

    public ValueCompareException(String message, Throwable cause) {
        super(message, cause);
    }

}
