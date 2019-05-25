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

import com.jayway.jsonpath.internal.DefaultsImpl;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

import java.util.*;

import static com.jayway.jsonpath.internal.Utils.notNull;
import static java.util.Arrays.asList;

/**
 * Immutable configuration object
 */
public class Configuration {

    private static Defaults DEFAULTS = null;

    /**
     * Set Default configuration
     * @param defaults default configuration settings
     */
    public static synchronized void setDefaults(Defaults defaults){
        DEFAULTS = defaults;
    }

    private static Defaults getEffectiveDefaults(){
        if (DEFAULTS == null) {
          return DefaultsImpl.INSTANCE;
        } else {
          return DEFAULTS;
        }
    }

    private final JsonProvider jsonProvider;
    private final MappingProvider mappingProvider;
    private final Set<Option> options;
    private final Collection<EvaluationListener> evaluationListeners;

    private Configuration(JsonProvider jsonProvider, MappingProvider mappingProvider, EnumSet<Option> options, Collection<EvaluationListener> evaluationListeners) {
        notNull(jsonProvider, "jsonProvider can not be null");
        notNull(mappingProvider, "mappingProvider can not be null");
        notNull(options, "setOptions can not be null");
        notNull(evaluationListeners, "evaluationListeners can not be null");
        this.jsonProvider = jsonProvider;
        this.mappingProvider = mappingProvider;
        this.options = Collections.unmodifiableSet(options);
        this.evaluationListeners = Collections.unmodifiableCollection(evaluationListeners);
    }

    /**
     * Creates a new Configuration by the provided evaluation listeners to the current listeners
     * @param evaluationListener listeners
     * @return a new configuration
     */
    public Configuration addEvaluationListeners(EvaluationListener... evaluationListener){
        return Configuration.builder().jsonProvider(jsonProvider).mappingProvider(mappingProvider).options(options).evaluationListener(evaluationListener).build();
    }

    /**
     * Creates a new Configuration with the provided evaluation listeners
     * @param evaluationListener listeners
     * @return a new configuration
     */
    public Configuration setEvaluationListeners(EvaluationListener... evaluationListener){
        return Configuration.builder().jsonProvider(jsonProvider).mappingProvider(mappingProvider).options(options).evaluationListener(evaluationListener).build();
    }

    /**
     * Returns the evaluation listeners registered in this configuration
     * @return the evaluation listeners
     */
    public Collection<EvaluationListener> getEvaluationListeners(){
        return evaluationListeners;
    }

    /**
     * Creates a new Configuration based on the given {@link com.jayway.jsonpath.spi.json.JsonProvider}
     * @param newJsonProvider json provider to use in new configuration
     * @return a new configuration
     */
    public Configuration jsonProvider(JsonProvider newJsonProvider) {
        return Configuration.builder().jsonProvider(newJsonProvider).mappingProvider(mappingProvider).options(options).evaluationListener(evaluationListeners).build();
    }

    /**
     * Returns {@link com.jayway.jsonpath.spi.json.JsonProvider} used by this configuration
     * @return jsonProvider used
     */
    public JsonProvider jsonProvider() {
        return jsonProvider;
    }

    /**
     * Creates a new Configuration based on the given {@link com.jayway.jsonpath.spi.mapper.MappingProvider}
     * @param newMappingProvider mapping provider to use in new configuration
     * @return a new configuration
     */
    public Configuration mappingProvider(MappingProvider newMappingProvider) {
        return Configuration.builder().jsonProvider(jsonProvider).mappingProvider(newMappingProvider).options(options).evaluationListener(evaluationListeners).build();
    }

    /**
     * Returns {@link com.jayway.jsonpath.spi.mapper.MappingProvider} used by this configuration
     * @return mappingProvider used
     */
    public MappingProvider mappingProvider() {
        return mappingProvider;
    }

    /**
     * Creates a new configuration by adding the new options to the options used in this configuration.
     * @param options options to add
     * @return a new configuration
     */
    public Configuration addOptions(Option... options) {
        EnumSet<Option> opts = EnumSet.noneOf(Option.class);
        opts.addAll(this.options);
        opts.addAll(asList(options));
        return Configuration.builder().jsonProvider(jsonProvider).mappingProvider(mappingProvider).options(opts).evaluationListener(evaluationListeners).build();
    }

    /**
     * Creates a new configuration with the provided options. Options in this configuration are discarded.
     * @param options
     * @return the new configuration instance
     */
    public Configuration setOptions(Option... options) {
        return Configuration.builder().jsonProvider(jsonProvider).mappingProvider(mappingProvider).options(options).evaluationListener(evaluationListeners).build();
    }

    /**
     * Returns the options used by this configuration
     * @return the new configuration instance
     */
    public Set<Option> getOptions() {
        return options;
    }

    /**
     * Check if this configuration contains the given option
     * @param option option to check
     * @return true if configurations contains option
     */
    public boolean containsOption(Option option){
        return options.contains(option);
    }

    /**
     * Creates a new configuration based on default values
     * @return a new configuration based on defaults
     */
    public static Configuration defaultConfiguration() {
        Defaults defaults = getEffectiveDefaults();
        return Configuration.builder().jsonProvider(defaults.jsonProvider()).options(defaults.options()).build();
    }

    /**
     * Returns a new ConfigurationBuilder
     * @return a builder
     */
    public static ConfigurationBuilder builder() {
        return new ConfigurationBuilder();
    }

    /**
     * Configuration builder
     */
    public static class ConfigurationBuilder {

        private JsonProvider jsonProvider;
        private MappingProvider mappingProvider;
        private EnumSet<Option> options = EnumSet.noneOf(Option.class);
        private Collection<EvaluationListener> evaluationListener = new ArrayList<EvaluationListener>();

        public ConfigurationBuilder jsonProvider(JsonProvider provider) {
            this.jsonProvider = provider;
            return this;
        }

        public ConfigurationBuilder mappingProvider(MappingProvider provider) {
            this.mappingProvider = provider;
            return this;
        }

        public ConfigurationBuilder options(Option... flags) {
            if(flags.length > 0) {
                this.options.addAll(asList(flags));
            }
            return this;
        }

        public ConfigurationBuilder options(Set<Option> options) {
            this.options.addAll(options);
            return this;
        }

        public ConfigurationBuilder evaluationListener(EvaluationListener... listener){
            this.evaluationListener = Arrays.asList(listener);
            return this;
        }

        public ConfigurationBuilder evaluationListener(Collection<EvaluationListener> listeners){
            this.evaluationListener = listeners == null ? Collections.<EvaluationListener>emptyList() : listeners;
            return this;
        }

        public Configuration build() {
            if (jsonProvider == null || mappingProvider == null) {
                final Defaults defaults = getEffectiveDefaults();
                if (jsonProvider == null) {
                    jsonProvider = defaults.jsonProvider();
                }
                if (mappingProvider == null){
                    mappingProvider = defaults.mappingProvider();
                }
            }
            return new Configuration(jsonProvider, mappingProvider, options, evaluationListener);
        }
    }

    public interface Defaults {
        /**
         * Returns the default {@link com.jayway.jsonpath.spi.json.JsonProvider}
         * @return default json provider
         */
        JsonProvider jsonProvider();

        /**
         * Returns default setOptions
         * @return setOptions
         */
        Set<Option> options();

        /**
         * Returns the default {@link com.jayway.jsonpath.spi.mapper.MappingProvider}
         *
         * @return default mapping provider
         */
        MappingProvider mappingProvider();

    }
}
