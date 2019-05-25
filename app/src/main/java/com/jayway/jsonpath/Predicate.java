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

import com.jayway.jsonpath.spi.mapper.MappingException;

/**
 *
 */
public interface Predicate {

    boolean apply(PredicateContext ctx);

    public interface PredicateContext {

        /**
         * Returns the current item being evaluated by this predicate
         * @return current document
         */
        Object item();

        /**
         * Returns the current item being evaluated by this predicate. It will be mapped
         * to the provided class
         * @return current document
         */
        <T> T item(Class<T> clazz) throws MappingException;

        /**
         * Returns the root document (the complete JSON)
         * @return root document
         */
        Object root();

        /**
         * Configuration to use when evaluating
         * @return configuration
         */
        Configuration configuration();
    }
}
