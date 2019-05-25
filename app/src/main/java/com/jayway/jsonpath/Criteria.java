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

import com.jayway.jsonpath.internal.Path;
import com.jayway.jsonpath.internal.Utils;
import com.jayway.jsonpath.internal.filter.RelationalExpressionNode;
import com.jayway.jsonpath.internal.filter.RelationalOperator;
import com.jayway.jsonpath.internal.filter.ValueNode;
import com.jayway.jsonpath.internal.filter.ValueNodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import static com.jayway.jsonpath.internal.Utils.notNull;
import static com.jayway.jsonpath.internal.filter.ValueNodes.PredicateNode;
import static com.jayway.jsonpath.internal.filter.ValueNodes.ValueListNode;

/**
 *
 */
@SuppressWarnings("unchecked")
public class Criteria implements Predicate {

    private final List<Criteria> criteriaChain;
    private ValueNode left;
    private RelationalOperator criteriaType;
    private ValueNode right;

    private Criteria(List<Criteria> criteriaChain, ValueNode left) {
        this.left = left;
        this.criteriaChain = criteriaChain;
        this.criteriaChain.add(this);
    }

    private Criteria(ValueNode left) {
        this(new LinkedList<Criteria>(), left);
    }

    @Override
    public boolean apply(PredicateContext ctx) {
        for (RelationalExpressionNode expressionNode : toRelationalExpressionNodes()) {
            if(!expressionNode.apply(ctx)){
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return Utils.join(" && ", toRelationalExpressionNodes());
    }

    private Collection<RelationalExpressionNode> toRelationalExpressionNodes(){
        List<RelationalExpressionNode> nodes = new ArrayList<RelationalExpressionNode>(criteriaChain.size());
        for (Criteria criteria : criteriaChain) {
            nodes.add(new RelationalExpressionNode(criteria.left, criteria.criteriaType, criteria.right));
        }
        return nodes;
    }

    /**
     * Static factory method to create a Criteria using the provided key
     *
     * @param key filed name
     * @return the new criteria
     */
    @Deprecated
    //This should be private.It exposes internal classes
    public static Criteria where(Path key) {
        return new Criteria(ValueNode.createPathNode(key));
    }


    /**
     * Static factory method to create a Criteria using the provided key
     *
     * @param key filed name
     * @return the new criteria
     */

    public static Criteria where(String key) {
        return new Criteria(ValueNode.toValueNode(prefixPath(key)));
    }

    /**
     * Static factory method to create a Criteria using the provided key
     *
     * @param key ads new filed to criteria
     * @return the criteria builder
     */
    public Criteria and(String key) {
        checkComplete();
        return new Criteria(this.criteriaChain, ValueNode.toValueNode(prefixPath(key)));
    }

    /**
     * Creates a criterion using equality
     *
     * @param o
     * @return the criteria
     */
    public Criteria is(Object o) {
        this.criteriaType = RelationalOperator.EQ;
        this.right = ValueNode.toValueNode(o);
        return this;
    }

    /**
     * Creates a criterion using equality
     *
     * @param o
     * @return the criteria
     */
    public Criteria eq(Object o) {
        return is(o);
    }

    /**
     * Creates a criterion using the <b>!=</b> operator
     *
     * @param o
     * @return the criteria
     */
    public Criteria ne(Object o) {
        this.criteriaType = RelationalOperator.NE;
        this.right = ValueNode.toValueNode(o);
        return this;
    }

    /**
     * Creates a criterion using the <b>&lt;</b> operator
     *
     * @param o
     * @return the criteria
     */
    public Criteria lt(Object o) {
        this.criteriaType = RelationalOperator.LT;
        this.right = ValueNode.toValueNode(o);
        return this;
    }

    /**
     * Creates a criterion using the <b>&lt;=</b> operator
     *
     * @param o
     * @return the criteria
     */
    public Criteria lte(Object o) {
        this.criteriaType = RelationalOperator.LTE;
        this.right = ValueNode.toValueNode(o);
        return this;
    }

    /**
     * Creates a criterion using the <b>&gt;</b> operator
     *
     * @param o
     * @return the criteria
     */
    public Criteria gt(Object o) {
        this.criteriaType = RelationalOperator.GT;
        this.right = ValueNode.toValueNode(o);
        return this;
    }

    /**
     * Creates a criterion using the <b>&gt;=</b> operator
     *
     * @param o
     * @return the criteria
     */
    public Criteria gte(Object o) {
        this.criteriaType = RelationalOperator.GTE;
        this.right = ValueNode.toValueNode(o);
        return this;
    }

    /**
     * Creates a criterion using a Regex
     *
     * @param pattern
     * @return the criteria
     */
    public Criteria regex(Pattern pattern) {
        notNull(pattern, "pattern can not be null");
        this.criteriaType = RelationalOperator.REGEX;
        this.right = ValueNode.toValueNode(pattern);
        return this;
    }

    /**
     * The <code>in</code> operator is analogous to the SQL IN modifier, allowing you
     * to specify an array of possible matches.
     *
     * @param o the values to match against
     * @return the criteria
     */
    public Criteria in(Object... o) {
        return in(Arrays.asList(o));
    }

    /**
     * The <code>in</code> operator is analogous to the SQL IN modifier, allowing you
     * to specify an array of possible matches.
     *
     * @param c the collection containing the values to match against
     * @return the criteria
     */
    public Criteria in(Collection<?> c) {
        notNull(c, "collection can not be null");
        this.criteriaType = RelationalOperator.IN;
        this.right = new ValueListNode(c);
        return this;
    }

    /**
     * The <code>contains</code> operator asserts that the provided object is contained
     * in the result. The object that should contain the input can be either an object or a String.
     *
     * @param o that should exists in given collection or
     * @return the criteria
     */
    public Criteria contains(Object o) {
        this.criteriaType = RelationalOperator.CONTAINS;
        this.right = ValueNode.toValueNode(o);
        return this;
    }

    /**
     * The <code>nin</code> operator is similar to $in except that it selects objects for
     * which the specified field does not have any value in the specified array.
     *
     * @param o the values to match against
     * @return the criteria
     */
    public Criteria nin(Object... o) {
        return nin(Arrays.asList(o));
    }

    /**
     * The <code>nin</code> operator is similar to $in except that it selects objects for
     * which the specified field does not have any value in the specified array.
     *
     * @param c the values to match against
     * @return the criteria
     */
    public Criteria nin(Collection<?> c) {
        notNull(c, "collection can not be null");
        this.criteriaType = RelationalOperator.NIN;
        this.right = new ValueListNode(c);
        return this;
    }

    /**
     * The <code>subsetof</code> operator selects objects for which the specified field is
     * an array whose elements comprise a subset of the set comprised by the elements of
     * the specified array.
     *
     * @param o the values to match against
     * @return the criteria
     */
    public Criteria subsetof(Object... o) {
        return subsetof(Arrays.asList(o));
    }

    /**
     * The <code>subsetof</code> operator selects objects for which the specified field is
     * an array whose elements comprise a subset of the set comprised by the elements of
     * the specified array.
     *
     * @param c the values to match against
     * @return the criteria
     */
    public Criteria subsetof(Collection<?> c) {
        notNull(c, "collection can not be null");
        this.criteriaType = RelationalOperator.SUBSETOF;
        this.right = new ValueListNode(c);
        return this;
    }

    /**
     * The <code>anyof</code> operator selects objects for which the specified field is
     * an array that contain at least an element in the specified array.
     *
     * @param o the values to match against
     * @return the criteria
     */
    public Criteria anyof(Object... o) {
        return subsetof(Arrays.asList(o));
    }

    /**
     * The <code>anyof</code> operator selects objects for which the specified field is
     * an array that contain at least an element in the specified array.
     *
     * @param c the values to match against
     * @return the criteria
     */
    public Criteria anyof(Collection<?> c) {
        notNull(c, "collection can not be null");
        this.criteriaType = RelationalOperator.ANYOF;
        this.right = new ValueListNode(c);
        return this;
    }

    /**
     * The <code>noneof</code> operator selects objects for which the specified field is
     * an array that does not contain any of the elements of the specified array.
     *
     * @param o the values to match against
     * @return the criteria
     */
    public Criteria noneof(Object... o) {
        return subsetof(Arrays.asList(o));
    }

    /**
     * The <code>noneof</code> operator selects objects for which the specified field is
     * an array that does not contain any of the elements of the specified array.
     *
     * @param c the values to match against
     * @return the criteria
     */
    public Criteria noneof(Collection<?> c) {
        notNull(c, "collection can not be null");
        this.criteriaType = RelationalOperator.NONEOF;
        this.right = new ValueListNode(c);
        return this;
    }

    /**
     * The <code>all</code> operator is similar to $in, but instead of matching any value
     * in the specified array all values in the array must be matched.
     *
     * @param o
     * @return the criteria
     */
    public Criteria all(Object... o) {
        return all(Arrays.asList(o));
    }

    /**
     * The <code>all</code> operator is similar to $in, but instead of matching any value
     * in the specified array all values in the array must be matched.
     *
     * @param c
     * @return the criteria
     */
    public Criteria all(Collection<?> c) {
        notNull(c, "collection can not be null");
        this.criteriaType = RelationalOperator.ALL;
        this.right = new ValueListNode(c);
        return this;
    }

    /**
     * The <code>size</code> operator matches:
     * <p/>
     * <ol>
     * <li>array with the specified number of elements.</li>
     * <li>string with given length.</li>
     * </ol>
     *
     * @param size
     * @return the criteria
     */
    public Criteria size(int size) {
        this.criteriaType = RelationalOperator.SIZE;
        this.right = ValueNode.toValueNode(size);
        return this;
    }

    /**
     * The $type operator matches values based on their Java JSON type.
     *
     * Supported types are:
     *
     *  List.class
     *  Map.class
     *  String.class
     *  Number.class
     *  Boolean.class
     *
     * Other types evaluates to false
     *
     * @param clazz
     * @return the criteria
     */
    public Criteria type(Class<?> clazz) {
        this.criteriaType = RelationalOperator.TYPE;
        this.right = ValueNode.createClassNode(clazz);
        return this;
    }

    /**
     * Check for existence (or lack thereof) of a field.
     *
     * @param shouldExist
     * @return the criteria
     */
    public Criteria exists(boolean shouldExist) {
        this.criteriaType = RelationalOperator.EXISTS;
        this.right = ValueNode.toValueNode(shouldExist);
        this.left = left.asPathNode().asExistsCheck(shouldExist);
        return this;
    }

    /**
     * The <code>notEmpty</code> operator checks that an array or String is not empty.
     *
     * @return the criteria
     */
    @Deprecated
    public Criteria notEmpty() {
        return empty(false);
    }

    /**
     * The <code>notEmpty</code> operator checks that an array or String is empty.
     *
     * @param empty should be empty
     * @return the criteria
     */
    public Criteria empty(boolean empty) {
        this.criteriaType = RelationalOperator.EMPTY;
        this.right = empty ? ValueNodes.TRUE : ValueNodes.FALSE;
        return this;
    }

    /**
     * The <code>matches</code> operator checks that an object matches the given predicate.
     *
     * @param p
     * @return the criteria
     */
    public Criteria matches(Predicate p) {
        this.criteriaType = RelationalOperator.MATCHES;
        this.right = new PredicateNode(p);
        return this;
    }

    /**
     * Parse the provided criteria
     *
     * Deprecated use {@link Filter#parse(String)}
     *
     * @param criteria
     * @return a criteria
     */
    @Deprecated
    public static Criteria parse(String criteria) {
        if(criteria == null){
            throw new InvalidPathException("Criteria can not be null");
        }
        String[] split = criteria.trim().split(" ");
        if(split.length == 3){
            return create(split[0], split[1], split[2]);
        } else if(split.length == 1){
            return create(split[0], "EXISTS", "true");
        } else {
            throw new InvalidPathException("Could not parse criteria");
        }
    }

    /**
     * Creates a new criteria
     * @param left path to evaluate in criteria
     * @param operator operator
     * @param right expected value
     * @return a new Criteria
     */
    @Deprecated
    public static Criteria create(String left, String operator, String right) {
        Criteria criteria = new Criteria(ValueNode.toValueNode(left));
        criteria.criteriaType = RelationalOperator.fromString(operator);
        criteria.right = ValueNode.toValueNode(right);
        return criteria;
    }


    private static String prefixPath(String key){
        if (!key.startsWith("$") && !key.startsWith("@")) {
            key = "@." + key;
        }
        return key;
    }

    private void checkComplete(){
        boolean complete = (left != null && criteriaType != null && right != null);
        if(!complete){
            throw new JsonPathException("Criteria build exception. Complete on criteria before defining next.");
        }
    }

}
