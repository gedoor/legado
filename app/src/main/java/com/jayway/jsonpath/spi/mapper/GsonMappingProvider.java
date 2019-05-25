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
package com.jayway.jsonpath.spi.mapper;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPathException;
import com.jayway.jsonpath.TypeRef;

import java.util.concurrent.Callable;

public class GsonMappingProvider implements MappingProvider {


    private final Callable<Gson> factory;

    public GsonMappingProvider(final Gson gson) {
        this(new Callable<Gson>() {
            @Override
            public Gson call() {
                return gson;
            }
        });
    }

    public GsonMappingProvider(Callable<Gson> factory) {
        this.factory = factory;
    }

    public GsonMappingProvider() {
        super();
        try {
            Class.forName("com.google.gson.Gson");
            this.factory = new Callable<Gson>() {
                @Override
                public Gson call() {
                    return new Gson();
                }
            };
        } catch (ClassNotFoundException e) {
            throw new JsonPathException("Gson not found on path", e);
        }
    }

    @Override
    public <T> T map(Object source, Class<T> targetType, Configuration configuration) {
        if(source == null){
            return null;
        }
        try {
            return factory.call().getAdapter(targetType).fromJsonTree((JsonElement) source);
        } catch (Exception e){
            throw new MappingException(e);
        }
    }

    @Override
    public <T> T map(Object source, TypeRef<T> targetType, Configuration configuration) {
        if(source == null){
            return null;
        }
        try {
            return (T) factory.call().getAdapter(TypeToken.get(targetType.getType())).fromJsonTree((JsonElement) source);
        } catch (Exception e){
            throw new MappingException(e);
        }
    }
}
