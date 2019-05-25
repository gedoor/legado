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

import com.jayway.jsonpath.internal.filter.FilterCompiler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import static java.util.Arrays.asList;

/**
 *
 */
public abstract class Filter implements Predicate {

    /**
     * Creates a new Filter based on given criteria
     * @param predicate criteria
     * @return a new Filter
     */
    public static Filter filter(Predicate predicate) {
        return new SingleFilter(predicate);
    }

    /**
     * Create a new Filter based on given list of criteria.
     * @param predicates list of criteria all needs to evaluate to true
     * @return the filter
     */
    public static Filter filter(Collection<Predicate> predicates) {
        return new AndFilter(predicates);
    }

    @Override
    public abstract boolean apply(PredicateContext ctx);


    public Filter or(final Predicate other){
        return new OrFilter(this, other);
    }

    public Filter and(final Predicate other){
        return new AndFilter(this, other);
    }

    /**
     * Parses a filter. The filter must match <code>[?(<filter>)]</code>, white spaces are ignored.
     * @param filter filter string to parse
     * @return the filter
     */
    public static Filter parse(String filter){
        return FilterCompiler.compile(filter);
    }

    private static final class SingleFilter extends Filter {

        private final Predicate predicate;

        private SingleFilter(Predicate predicate) {
            this.predicate = predicate;
        }

        @Override
        public boolean apply(PredicateContext ctx) {
            return predicate.apply(ctx);
        }

        @Override
        public String toString() {
            String predicateString = predicate.toString();
            if(predicateString.startsWith("(")){
                return "[?" + predicateString + "]";
            } else {
                return "[?(" + predicateString + ")]";
            }
        }
    }

    private static final class AndFilter extends Filter {

        private final Collection<Predicate> predicates;

        private AndFilter(Collection<Predicate> predicates) {
            this.predicates = predicates;
        }

        private AndFilter(Predicate left, Predicate right) {
            this(asList(left, right));
        }

        public Filter and(final Predicate other){
            Collection<Predicate> newPredicates = new ArrayList<Predicate>(predicates);
            newPredicates.add(other);
            return new AndFilter(newPredicates);
        }

        @Override
        public boolean apply(PredicateContext ctx) {
            for (Predicate predicate : predicates) {
                if(!predicate.apply(ctx)){
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            Iterator<Predicate> i = predicates.iterator();
            StringBuilder sb = new StringBuilder();
            sb.append("[?(");
            while (i.hasNext()){
                String p = i.next().toString();

                if(p.startsWith("[?(")){
                    p = p.substring(3, p.length() - 2);
                }
                sb.append(p);

                if(i.hasNext()){
                    sb.append(" && ");
                }
            }
            sb.append(")]");
            return sb.toString();
        }
    }

    private static final class OrFilter extends Filter {

        private final Predicate left;
        private final Predicate right;
  
        private OrFilter(Predicate left, Predicate right) {
            this.left = left;
            this.right = right;
        }

        public Filter and(final Predicate other){
            return new OrFilter(left, new AndFilter(right, other));
        }

        @Override
        public boolean apply(PredicateContext ctx) {
            boolean a = left.apply(ctx);
            return a || right.apply(ctx);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[?(");

            String l = left.toString();
            String r = right.toString();

            if(l.startsWith("[?(")){
                l = l.substring(3, l.length() - 2);
            }
            if(r.startsWith("[?(")){
                r = r.substring(3, r.length() - 2);
            }

            sb.append(l).append(" || ").append(r);

            sb.append(")]");
            return sb.toString();
        }
    }
}
