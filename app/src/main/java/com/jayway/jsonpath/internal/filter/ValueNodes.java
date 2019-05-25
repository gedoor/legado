package com.jayway.jsonpath.internal.filter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPathException;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.Predicate;
import com.jayway.jsonpath.internal.Path;
import com.jayway.jsonpath.internal.Utils;
import com.jayway.jsonpath.internal.path.PathCompiler;
import com.jayway.jsonpath.internal.path.PredicateContextImpl;
import com.jayway.jsonpath.spi.json.JsonProvider;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

/**
 * Moved these nodes out of the ValueNode abstract class.
 * This is to avoid this possible issue:
 *
 * Classes that refer to their own subclasses in their static initializers or in static fields.
 * Such references can cause JVM-level deadlocks in multithreaded environment, when
 * one thread tries to load superclass and another thread tries to load subclass at the same time.
 */
public interface ValueNodes {

    NullNode NULL_NODE = new NullNode();
    BooleanNode TRUE = new BooleanNode("true");
    BooleanNode FALSE = new BooleanNode("false");
    UndefinedNode UNDEFINED = new UndefinedNode();

    //----------------------------------------------------
    //
    // ValueNode Implementations
    //
    //----------------------------------------------------
    class PatternNode extends ValueNode {
        private final String pattern;
        private final Pattern compiledPattern;
        private final String flags;

        PatternNode(CharSequence charSequence) {
            String tmp = charSequence.toString();
            int begin = tmp.indexOf('/');
            int end = tmp.lastIndexOf('/');
            this.pattern = tmp.substring(begin + 1, end);
            int flagsIndex = end + 1;
            this.flags = tmp.length() > flagsIndex ? tmp.substring(flagsIndex) : "";
            this.compiledPattern = Pattern.compile(pattern, PatternFlag.parseFlags(flags.toCharArray()));
        }

        PatternNode(Pattern pattern) {
            this.pattern = pattern.pattern();
            this.compiledPattern = pattern;
            this.flags = PatternFlag.parseFlags(pattern.flags());
        }


        Pattern getCompiledPattern() {
            return compiledPattern;
        }

        @Override
        public Class<?> type(Predicate.PredicateContext ctx) {
            return Void.TYPE;
        }

        public boolean isPatternNode() {
            return true;
        }

        public PatternNode asPatternNode() {
            return this;
        }

        @Override
        public String toString() {

            if(!pattern.startsWith("/")){
                return "/" + pattern + "/" + flags;
            } else {
                return pattern;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PatternNode)) return false;

            PatternNode that = (PatternNode) o;

            return !(compiledPattern != null ? !compiledPattern.equals(that.compiledPattern) : that.compiledPattern != null);

        }
    }

    class JsonNode extends ValueNode {
        private final Object json;
        private final boolean parsed;

        JsonNode(CharSequence charSequence) {
            json = charSequence.toString();
            parsed = false;
        }

        JsonNode(Object parsedJson) {
            json = parsedJson;
            parsed = true;
        }

        @Override
        public Class<?> type(Predicate.PredicateContext ctx) {
            if(isArray(ctx)) return List.class;
            else if(isMap(ctx)) return Map.class;
            else if(parse(ctx) instanceof Number) return Number.class;
            else if(parse(ctx) instanceof String) return String.class;
            else if(parse(ctx) instanceof Boolean) return Boolean.class;
            else return Void.class;
        }

        public boolean isJsonNode() {
            return true;
        }

        public JsonNode asJsonNode() {
            return this;
        }

        public ValueNode asValueListNode(Predicate.PredicateContext ctx){
            if(!isArray(ctx)){
                return UNDEFINED;
            } else {
                return new ValueListNode(Collections.unmodifiableList((List) parse(ctx)));
            }
        }

        public Object parse(Predicate.PredicateContext ctx){
            try {
              return parsed ? json : new JSONParser(JSONParser.MODE_PERMISSIVE).parse(json.toString());
            } catch (ParseException e) {
              throw new IllegalArgumentException(e);
            }
        }

        public boolean isParsed() {
            return parsed;
        }

        public Object getJson() {
            return json;
        }

        public boolean isArray(Predicate.PredicateContext ctx) {
            return parse(ctx) instanceof List;
        }

        public boolean isMap(Predicate.PredicateContext ctx) {
            return parse(ctx) instanceof Map;
        }

        public int length(Predicate.PredicateContext ctx) {
            return isArray(ctx) ? ((List<?>) parse(ctx)).size() : -1;
        }

        public boolean isEmpty(Predicate.PredicateContext ctx) {
            if (isArray(ctx) || isMap(ctx)) return ((Collection<?>) parse(ctx)).size() == 0;
            else if((parse(ctx) instanceof String)) return ((String)parse(ctx)).length() == 0;
            return true;
        }

        @Override
        public String toString() {
            return json.toString();
        }

        public boolean equals(JsonNode jsonNode, Predicate.PredicateContext ctx) {
            if (this == jsonNode) return true;
            return !(json != null ? !json.equals(jsonNode.parse(ctx)) : jsonNode.json != null);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof JsonNode)) return false;

            JsonNode jsonNode = (JsonNode) o;

            return !(json != null ? !json.equals(jsonNode.json) : jsonNode.json != null);
        }
    }

    class StringNode extends ValueNode {
        private final String string;
        private boolean useSingleQuote = true;

        StringNode(CharSequence charSequence, boolean escape) {
            if (escape && charSequence.length() > 1) {
                char open = charSequence.charAt(0);
                char close = charSequence.charAt(charSequence.length()-1);
                if (open == '\'' && close == '\'') {
                    charSequence = charSequence.subSequence(1, charSequence.length()-1);
                } else if (open == '"' && close == '"') {
                    charSequence = charSequence.subSequence(1, charSequence.length()-1);
                    useSingleQuote = false;
                }
                string = Utils.unescape(charSequence.toString());
            } else {
                string = charSequence.toString();
            }
        }

        @Override
        public NumberNode asNumberNode() {
            BigDecimal number = null;
            try {
                number = new BigDecimal(string);
            } catch (NumberFormatException nfe){
                return NumberNode.NAN;
            }
            return new NumberNode(number);
        }

        public String getString() {
            return string;
        }

        public int length(){
            return getString().length();
        }

        public boolean isEmpty(){
            return getString().isEmpty();
        }

        public boolean contains(String str) {
            return getString().contains(str);
        }

        @Override
        public Class<?> type(Predicate.PredicateContext ctx) {
            return String.class;
        }

        public boolean isStringNode() {
            return true;
        }

        public StringNode asStringNode() {
            return this;
        }

        @Override
        public String toString() {
            String quote = useSingleQuote ? "'" : "\"";
            return quote + Utils.escape(string, true) + quote;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StringNode) && !(o instanceof NumberNode)) return false;

            StringNode that = ((ValueNode) o).asStringNode();

            return !(string != null ? !string.equals(that.getString()) : that.getString() != null);

        }
    }

    class NumberNode extends ValueNode {

        public static NumberNode NAN = new NumberNode((BigDecimal)null);

        private final BigDecimal number;

        NumberNode(BigDecimal number) {
            this.number = number;
        }
        NumberNode(CharSequence num) {
            number = new BigDecimal(num.toString());
        }

        @Override
        public StringNode asStringNode() {
            return new StringNode(number.toString(), false);
        }

        public BigDecimal getNumber() {
            return number;
        }

        @Override
        public Class<?> type(Predicate.PredicateContext ctx) {
            return Number.class;
        }

        public boolean isNumberNode() {
            return true;
        }

        public NumberNode asNumberNode() {
            return this;
        }

        @Override
        public String toString() {
            return number.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NumberNode) && !(o instanceof StringNode)) return false;

            NumberNode that = ((ValueNode)o).asNumberNode();

            if(that == NumberNode.NAN){
                return false;
            } else {
                return number.compareTo(that.number) == 0;
            }
        }
    }

    class BooleanNode extends ValueNode {
        private final Boolean value;

        private BooleanNode(CharSequence boolValue) {
            value = Boolean.parseBoolean(boolValue.toString());
        }

        @Override
        public Class<?> type(Predicate.PredicateContext ctx) {
            return Boolean.class;
        }

        public boolean isBooleanNode() {
            return true;
        }

        public BooleanNode asBooleanNode() {
            return this;
        }

        public boolean getBoolean() {
            return value;
        }

        @Override
        public String toString() {
            return value.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BooleanNode)) return false;

            BooleanNode that = (BooleanNode) o;

            return !(value != null ? !value.equals(that.value) : that.value != null);
        }
    }

    class ClassNode extends ValueNode {
        private final Class clazz;

        ClassNode(Class clazz) {
            this.clazz = clazz;
        }

        @Override
        public Class<?> type(Predicate.PredicateContext ctx) {
            return Class.class;
        }

        public boolean isClassNode() {
            return true;
        }

        public ClassNode asClassNode() {
            return this;
        }

        public Class getClazz() {
            return clazz;
        }

        @Override
        public String toString() {
            return clazz.getName();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ClassNode)) return false;

            ClassNode that = (ClassNode) o;

            return !(clazz != null ? !clazz.equals(that.clazz) : that.clazz != null);
        }
    }

    class NullNode extends ValueNode {

        private NullNode() {}

        @Override
        public Class<?> type(Predicate.PredicateContext ctx) {
            return Void.class;
        }

        @Override
        public boolean isNullNode() {
            return true;
        }

        @Override
        public NullNode asNullNode() {
            return this;
        }

        @Override
        public String toString() {
            return "null";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NullNode)) return false;

            return true;
        }
    }

    class UndefinedNode extends ValueNode {

        @Override
        public Class<?> type(Predicate.PredicateContext ctx) {
            return Void.class;
        }

        public UndefinedNode asUndefinedNode() {
            return this;
        }

        public boolean isUndefinedNode() {
            return true;
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }

    class PredicateNode extends ValueNode {

        private final Predicate predicate;

        public PredicateNode(Predicate predicate) {
            this.predicate = predicate;
        }

        public Predicate getPredicate() {
            return predicate;
        }

        public PredicateNode asPredicateNode() {
            return this;
        }

        @Override
        public Class<?> type(Predicate.PredicateContext ctx) {
            return Void.class;
        }

        public boolean isPredicateNode() {
            return true;
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }

        @Override
        public String toString() {
            return predicate.toString();
        }
    }

    class ValueListNode extends ValueNode implements Iterable<ValueNode> {

        private List<ValueNode> nodes = new ArrayList<ValueNode>();

        public ValueListNode(Collection<?> values) {
            for (Object value : values) {
                nodes.add(toValueNode(value));
            }
        }

        public boolean contains(ValueNode node){
            return nodes.contains(node);
        }

        public boolean subsetof(ValueListNode right) {
            for (ValueNode leftNode : nodes) {
                if (!right.nodes.contains(leftNode)) {
                    return false;
                }
            }
            return true;
        }

        public List<ValueNode> getNodes() {
            return Collections.unmodifiableList(nodes);
        }

        @Override
        public Class<?> type(Predicate.PredicateContext ctx) {
            return List.class;
        }

        public boolean isValueListNode() {
            return true;
        }

        public ValueListNode asValueListNode() {
            return this;
        }

        @Override
        public String toString() {
            return "[" + Utils.join(",", nodes) + "]";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ValueListNode)) return false;

            ValueListNode that = (ValueListNode) o;

            return nodes.equals(that.nodes);
        }

        @Override
        public Iterator<ValueNode> iterator() {
            return nodes.iterator();
        }
    }

    class PathNode extends ValueNode {

        private final Path path;
        private final boolean existsCheck;
        private final boolean shouldExist;

        PathNode(Path path) {
            this(path, false, false);
        }

        PathNode(CharSequence charSequence, boolean existsCheck, boolean shouldExist) {
            this(PathCompiler.compile(charSequence.toString()), existsCheck, shouldExist);
        }

        PathNode(Path path, boolean existsCheck, boolean shouldExist) {
            this.path = path;
            this.existsCheck = existsCheck;
            this.shouldExist = shouldExist;
        }

        public Path getPath() {
            return path;
        }

        public boolean isExistsCheck() {
            return existsCheck;
        }

        public boolean shouldExists() {
            return shouldExist;
        }

        @Override
        public Class<?> type(Predicate.PredicateContext ctx) {
            return Void.class;
        }

        public boolean isPathNode() {
            return true;
        }

        public PathNode asPathNode() {
            return this;
        }

        public PathNode asExistsCheck(boolean shouldExist) {
            return new PathNode(path, true, shouldExist);
        }

        @Override
        public String toString() {
            return existsCheck && ! shouldExist ? Utils.concat("!" , path.toString()) : path.toString();
        }

        public ValueNode evaluate(Predicate.PredicateContext ctx) {
            if (isExistsCheck()) {
                try {
                    Configuration c = Configuration.builder().jsonProvider(ctx.configuration().jsonProvider()).options(Option.REQUIRE_PROPERTIES).build();
                    Object result = path.evaluate(ctx.item(), ctx.root(), c).getValue(false);
                    return result == JsonProvider.UNDEFINED ? FALSE : TRUE;
                } catch (PathNotFoundException e) {
                    return FALSE;
                }
            } else {
                try {
                    Object res;
                    if (ctx instanceof PredicateContextImpl) {
                        //This will use cache for document ($) queries
                        PredicateContextImpl ctxi = (PredicateContextImpl) ctx;
                        res = ctxi.evaluate(path);
                    } else {
                        Object doc = path.isRootPath() ? ctx.root() : ctx.item();
                        res = path.evaluate(doc, ctx.root(), ctx.configuration()).getValue();
                    }
                    res = ctx.configuration().jsonProvider().unwrap(res);

                    if (res instanceof Number) return ValueNode.createNumberNode(res.toString());
                    else if (res instanceof String) return ValueNode.createStringNode(res.toString(), false);
                    else if (res instanceof Boolean) return ValueNode.createBooleanNode(res.toString());
                    else if (res == null) return NULL_NODE;
                    else if (ctx.configuration().jsonProvider().isArray(res)) return ValueNode.createJsonNode(ctx.configuration().mappingProvider().map(res, List.class, ctx.configuration()));
                    else if (ctx.configuration().jsonProvider().isMap(res)) return ValueNode.createJsonNode(ctx.configuration().mappingProvider().map(res, Map.class, ctx.configuration()));
                    else throw new JsonPathException("Could not convert " + res.toString() + " to a ValueNode");
                } catch (PathNotFoundException e) {
                    return UNDEFINED;
                }
            }
        }


    }
}
