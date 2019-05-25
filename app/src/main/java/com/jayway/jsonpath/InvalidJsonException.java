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

@SuppressWarnings("serial")
public class InvalidJsonException extends JsonPathException {

    /**
     * Problematic JSON if available.
     */
    private final String json;
    
    public InvalidJsonException() {
        json = null;
    }

    public InvalidJsonException(String message) {
        super(message);
        json = null;
    }

    public InvalidJsonException(String message, Throwable cause) {
        super(message, cause);
        json = null;
    }

    public InvalidJsonException(Throwable cause) {
        super(cause);
        json = null;
    }
    
    /**
     * Rethrow the exception with the problematic JSON captured.
     */
    public InvalidJsonException(final Throwable cause, final String json) {
        super(cause);
        this.json = json;
    }
    
    /**
     * @return the problematic JSON if available.
     */
    public String getJson() {
        return json;
    }
}
