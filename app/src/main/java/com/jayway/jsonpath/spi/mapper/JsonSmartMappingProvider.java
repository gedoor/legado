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

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.TypeRef;
import net.minidev.json.JSONValue;
import net.minidev.json.writer.JsonReader;
import net.minidev.json.writer.JsonReaderI;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.Callable;

public class JsonSmartMappingProvider implements MappingProvider {

    private static JsonReader DEFAULT = new JsonReader();

    static {
        DEFAULT.registerReader(Long.class, new LongReader());
        DEFAULT.registerReader(long.class, new LongReader());
        DEFAULT.registerReader(Integer.class, new IntegerReader());
        DEFAULT.registerReader(int.class, new IntegerReader());
        DEFAULT.registerReader(Double.class, new DoubleReader());
        DEFAULT.registerReader(double.class, new DoubleReader());
        DEFAULT.registerReader(Float.class, new FloatReader());
        DEFAULT.registerReader(float.class, new FloatReader());
        DEFAULT.registerReader(BigDecimal.class, new BigDecimalReader());
        DEFAULT.registerReader(String.class, new StringReader());
        DEFAULT.registerReader(Date.class, new DateReader());
        DEFAULT.registerReader(BigInteger.class, new BigIntegerReader());
        DEFAULT.registerReader(boolean.class, new BooleanReader());
    }


    private final Callable<JsonReader> factory;

    public JsonSmartMappingProvider(final JsonReader jsonReader) {
        this(new Callable<JsonReader>() {
            @Override
            public JsonReader call() {
                return jsonReader;
            }
        });
    }

    public JsonSmartMappingProvider(Callable<JsonReader> factory) {
        this.factory = factory;
    }

    public JsonSmartMappingProvider() {
        this(DEFAULT);
    }



    @Override
    public <T> T map(Object source, Class<T> targetType, Configuration configuration) {
        if(source == null){
            return null;
        }
        if (targetType.isAssignableFrom(source.getClass())) {
            return (T) source;
        }
        try {
            if(!configuration.jsonProvider().isMap(source) && !configuration.jsonProvider().isArray(source)){
                return factory.call().getMapper(targetType).convert(source);
            }
            String s = configuration.jsonProvider().toJson(source);
            return (T) JSONValue.parse(s, targetType);
        } catch (Exception e) {
            throw new MappingException(e);
        }

    }

    @Override
    public <T> T map(Object source, TypeRef<T> targetType, Configuration configuration) {
        throw new UnsupportedOperationException("Json-smart provider does not support TypeRef! Use a Jackson or Gson based provider");
    }

    private static class StringReader extends JsonReaderI<String> {
        public StringReader() {
            super(null);
        }
        public String convert(Object src) {
            if(src == null){
                return null;
            }
            return src.toString();
        }
    }
    private static class IntegerReader extends JsonReaderI<Integer> {
        public IntegerReader() {
            super(null);
        }
        public Integer convert(Object src) {
            if(src == null){
                return null;
            }
            if(Integer.class.isAssignableFrom(src.getClass())){
               return (Integer) src;
            } else if (Long.class.isAssignableFrom(src.getClass())) {
                return ((Long) src).intValue();
            } else if (Double.class.isAssignableFrom(src.getClass())) {
                return ((Double) src).intValue();
            } else if (BigDecimal.class.isAssignableFrom(src.getClass())) {
                return ((BigDecimal) src).intValue();
            } else if (Float.class.isAssignableFrom(src.getClass())) {
                return ((Float) src).intValue();
            } else if (String.class.isAssignableFrom(src.getClass())) {
                return Integer.valueOf(src.toString());
            }
            throw new MappingException("can not map a " + src.getClass() + " to " + Integer.class.getName());
        }
    }
    private static class LongReader extends JsonReaderI<Long> {
        public LongReader() {
            super(null);
        }
        public Long convert(Object src) {
            if(src == null){
                return null;
            }
            if(Long.class.isAssignableFrom(src.getClass())){
                return (Long) src;
            } else if (Integer.class.isAssignableFrom(src.getClass())) {
                return ((Integer) src).longValue();
            } else if (Double.class.isAssignableFrom(src.getClass())) {
                return ((Double) src).longValue();
            } else if (BigDecimal.class.isAssignableFrom(src.getClass())) {
                return ((BigDecimal) src).longValue();
            } else if (Float.class.isAssignableFrom(src.getClass())) {
                return ((Float) src).longValue();
            } else if (String.class.isAssignableFrom(src.getClass())) {
                return Long.valueOf(src.toString());
            }
            throw new MappingException("can not map a " + src.getClass() + " to " + Long.class.getName());
        }
    }

    private static class DoubleReader extends JsonReaderI<Double> {
        public DoubleReader() {
            super(null);
        }
        public Double convert(Object src) {
            if(src == null){
                return null;
            }
            if(Double.class.isAssignableFrom(src.getClass())){
                return (Double) src;
            } else if (Integer.class.isAssignableFrom(src.getClass())) {
                return ((Integer) src).doubleValue();
            } else if (Long.class.isAssignableFrom(src.getClass())) {
                return ((Long) src).doubleValue();
            } else if (BigDecimal.class.isAssignableFrom(src.getClass())) {
                return ((BigDecimal) src).doubleValue();
            } else if (Float.class.isAssignableFrom(src.getClass())) {
                return ((Float) src).doubleValue();
            } else if (String.class.isAssignableFrom(src.getClass())) {
                return Double.valueOf(src.toString());
            }
            throw new MappingException("can not map a " + src.getClass() + " to " + Double.class.getName());
        }
    }
    private static class FloatReader extends JsonReaderI<Float> {
        public FloatReader() {
            super(null);
        }
        public Float convert(Object src) {
            if(src == null){
                return null;
            }
            if(Float.class.isAssignableFrom(src.getClass())){
                return (Float) src;
            } else if (Integer.class.isAssignableFrom(src.getClass())) {
                return ((Integer) src).floatValue();
            } else if (Long.class.isAssignableFrom(src.getClass())) {
                return ((Long) src).floatValue();
            } else if (BigDecimal.class.isAssignableFrom(src.getClass())) {
                return ((BigDecimal) src).floatValue();
            } else if (Double.class.isAssignableFrom(src.getClass())) {
                return ((Double) src).floatValue();
            } else if (String.class.isAssignableFrom(src.getClass())) {
                return Float.valueOf(src.toString());
            }
            throw new MappingException("can not map a " + src.getClass() + " to " + Float.class.getName());
        }
    }
    private static class BigDecimalReader extends JsonReaderI<BigDecimal> {
        public BigDecimalReader() {
            super(null);
        }
        public BigDecimal convert(Object src) {
            if(src == null){
                return null;
            }
            return new BigDecimal(src.toString());
        }
    }
    private static class BigIntegerReader extends JsonReaderI<BigInteger> {
        public BigIntegerReader() {
            super(null);
        }
        public BigInteger convert(Object src) {
            if(src == null){
                return null;
            }
            return new BigInteger(src.toString());
        }
    }
    private static class DateReader extends JsonReaderI<Date> {
        public DateReader() {
            super(null);
        }
        public Date convert(Object src) {
            if(src == null){
                return null;
            }
            if(Date.class.isAssignableFrom(src.getClass())){
                return (Date) src;
            } else if(Long.class.isAssignableFrom(src.getClass())){
                return new Date((Long) src);
            } else if(String.class.isAssignableFrom(src.getClass())){
                try {
                    return DateFormat.getInstance().parse(src.toString());
                } catch (ParseException e) {
                    throw new MappingException(e);
                }
            }
            throw new MappingException("can not map a " + src.getClass() + " to " + Date.class.getName());
        }
    }
    private static class BooleanReader extends JsonReaderI<Boolean> {
        public BooleanReader() {
            super(null);
        }
        public Boolean convert(Object src) {
            if(src == null){
                return null;
            }
            if (Boolean.class.isAssignableFrom(src.getClass())) {
                return (Boolean) src;
            }
            throw new MappingException("can not map a " + src.getClass() + " to " + Boolean.class.getName());
        }
    }
}
