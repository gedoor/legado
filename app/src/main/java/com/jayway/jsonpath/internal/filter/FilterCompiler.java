package com.jayway.jsonpath.internal.filter;

import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.Predicate;
import com.jayway.jsonpath.internal.CharacterIndex;
import static com.jayway.jsonpath.internal.filter.ValueNodes.*;

import java.util.ArrayList;
import java.util.List;

public class FilterCompiler  {

    private static final char DOC_CONTEXT = '$';
    private static final char EVAL_CONTEXT = '@';

    private static final char OPEN_SQUARE_BRACKET = '[';
    private static final char CLOSE_SQUARE_BRACKET = ']';
    private static final char OPEN_PARENTHESIS = '(';
    private static final char CLOSE_PARENTHESIS = ')';
    private static final char OPEN_OBJECT = '{';
    private static final char CLOSE_OBJECT = '}';
    private static final char OPEN_ARRAY = '[';
    private static final char CLOSE_ARRAY = ']';

    private static final char SINGLE_QUOTE = '\'';
    private static final char DOUBLE_QUOTE = '"';

    private static final char SPACE = ' ';
    private static final char PERIOD = '.';

    private static final char AND = '&';
    private static final char OR = '|';

    private static final char MINUS = '-';
    private static final char LT = '<';
    private static final char GT = '>';
    private static final char EQ = '=';
    private static final char TILDE = '~';
    private static final char TRUE = 't';
    private static final char FALSE = 'f';
    private static final char NULL = 'n';
    private static final char NOT = '!';
    private static final char PATTERN = '/';
    private static final char IGNORE_CASE = 'i';

    private CharacterIndex filter;

    public static Filter compile(String filterString) {
        FilterCompiler compiler = new FilterCompiler(filterString);
        return new CompiledFilter(compiler.compile());
    }

    private FilterCompiler(String filterString) {
        filter = new CharacterIndex(filterString);
        filter.trim();
        if (!filter.currentCharIs('[') || !filter.lastCharIs(']')) {
            throw new InvalidPathException("Filter must start with '[' and end with ']'. " + filterString);
        }
        filter.incrementPosition(1);
        filter.decrementEndPosition(1);
        filter.trim();
        if (!filter.currentCharIs('?')) {
            throw new InvalidPathException("Filter must start with '[?' and end with ']'. " + filterString);
        }
        filter.incrementPosition(1);
        filter.trim();
        if (!filter.currentCharIs('(') || !filter.lastCharIs(')')) {
            throw new InvalidPathException("Filter must start with '[?(' and end with ')]'. " + filterString);
        }
    }

    public Predicate compile() {
        try {
             final ExpressionNode result = readLogicalOR();
             filter.skipBlanks();
             if (filter.inBounds()) {
                 throw new InvalidPathException(String.format("Expected end of filter expression instead of: %s",
                         filter.subSequence(filter.position(), filter.length())));
             }

             return result;
        } catch (InvalidPathException e){
            throw e;
        } catch (Exception e) {
            throw new InvalidPathException("Failed to parse filter: " + filter + ", error on position: " + filter.position() + ", char: " + filter.currentChar());
        }
    }

    private ValueNode readValueNode() {
        switch (filter.skipBlanks().currentChar()) {
            case DOC_CONTEXT  : return readPath();
            case EVAL_CONTEXT : return readPath();
            case NOT:
                filter.incrementPosition(1);
                switch (filter.skipBlanks().currentChar()) {
                    case DOC_CONTEXT  : return readPath();
                    case EVAL_CONTEXT : return readPath();
                    default: throw new InvalidPathException(String.format("Unexpected character: %c", NOT));
                }
            default : return readLiteral();
        }
    }

    private ValueNode readLiteral(){
        switch (filter.skipBlanks().currentChar()){
            case SINGLE_QUOTE:  return readStringLiteral(SINGLE_QUOTE);
            case DOUBLE_QUOTE: return readStringLiteral(DOUBLE_QUOTE);
            case TRUE:  return readBooleanLiteral();
            case FALSE: return readBooleanLiteral();
            case MINUS: return readNumberLiteral();
            case NULL:  return readNullLiteral();
            case OPEN_OBJECT: return readJsonLiteral();
            case OPEN_ARRAY: return readJsonLiteral();
            case PATTERN: return readPattern();
            default:    return readNumberLiteral();
        }
    }

    /*
     *  LogicalOR               = LogicalAND { '||' LogicalAND }
     *  LogicalAND              = LogicalANDOperand { '&&' LogicalANDOperand }
     *  LogicalANDOperand       = RelationalExpression | '(' LogicalOR ')' | '!' LogicalANDOperand
     *  RelationalExpression    = Value [ RelationalOperator Value ]
     */

    private ExpressionNode readLogicalOR() {
        final List<ExpressionNode> ops = new ArrayList<ExpressionNode>();
        ops.add(readLogicalAND());

        while (true) {
            int savepoint = filter.position();
            if (filter.hasSignificantSubSequence(LogicalOperator.OR.getOperatorString())) {
                ops.add(readLogicalAND());
            } else {
                filter.setPosition(savepoint);
                break;
            }
        }

        return 1 == ops.size() ? ops.get(0) : LogicalExpressionNode.createLogicalOr(ops);
    }

    private ExpressionNode readLogicalAND() {
        /// @fixme copy-pasted
        final List<ExpressionNode> ops = new ArrayList<ExpressionNode>();
        ops.add(readLogicalANDOperand());

        while (true) {
            int savepoint = filter.position();
            if (filter.hasSignificantSubSequence(LogicalOperator.AND.getOperatorString())) {
                ops.add(readLogicalANDOperand());
            } else {
                filter.setPosition(savepoint);
                break;
            }
        }

        return 1 == ops.size() ? ops.get(0) : LogicalExpressionNode.createLogicalAnd(ops);
    }

    private ExpressionNode readLogicalANDOperand() {
        int savepoint = filter.skipBlanks().position();
        if (filter.skipBlanks().currentCharIs(NOT)) {
            filter.readSignificantChar(NOT);
            switch (filter.skipBlanks().currentChar()) {
                case DOC_CONTEXT:
                case EVAL_CONTEXT:
                    filter.setPosition(savepoint);
                    break;
            default:
                final ExpressionNode op = readLogicalANDOperand();
                return LogicalExpressionNode.createLogicalNot(op);
            }
        }
        if (filter.skipBlanks().currentCharIs(OPEN_PARENTHESIS)) {
            filter.readSignificantChar(OPEN_PARENTHESIS);
            final ExpressionNode op = readLogicalOR();
            filter.readSignificantChar(CLOSE_PARENTHESIS);
            return op;
        }

        return readExpression();
    }

    private RelationalExpressionNode readExpression() {
        ValueNode left = readValueNode();
        int savepoint = filter.position();
        try {
            RelationalOperator operator = readRelationalOperator();
            ValueNode right = readValueNode();
            return new RelationalExpressionNode(left, operator, right);
        }
        catch (InvalidPathException exc) {
            filter.setPosition(savepoint);
        }

        PathNode pathNode = left.asPathNode();
        left = pathNode.asExistsCheck(pathNode.shouldExists());
        RelationalOperator operator = RelationalOperator.EXISTS;
        ValueNode right = left.asPathNode().shouldExists() ? ValueNodes.TRUE : ValueNodes.FALSE;
        return new RelationalExpressionNode(left, operator, right);
    }

    private LogicalOperator readLogicalOperator(){
        int begin = filter.skipBlanks().position();
        int end = begin+1;

        if(!filter.inBounds(end)){
            throw new InvalidPathException("Expected boolean literal");
        }
        CharSequence logicalOperator = filter.subSequence(begin, end+1);
        if(!logicalOperator.equals("||") && !logicalOperator.equals("&&")){
            throw new InvalidPathException("Expected logical operator");
        }
        filter.incrementPosition(logicalOperator.length());

        return LogicalOperator.fromString(logicalOperator.toString());
    }

    private RelationalOperator readRelationalOperator() {
        int begin = filter.skipBlanks().position();

        if(isRelationalOperatorChar(filter.currentChar())){
            while (filter.inBounds() && isRelationalOperatorChar(filter.currentChar())) {
                filter.incrementPosition(1);
            }
        } else {
            while (filter.inBounds() && filter.currentChar() != SPACE) {
                filter.incrementPosition(1);
            }
        }

        CharSequence operator = filter.subSequence(begin, filter.position());
        return RelationalOperator.fromString(operator.toString());
    }

    private NullNode readNullLiteral() {
        int begin = filter.position();
        if(filter.currentChar() == NULL && filter.inBounds(filter.position() + 3)){
            CharSequence nullValue = filter.subSequence(filter.position(), filter.position() + 4);
            if("null".equals(nullValue.toString())){
                filter.incrementPosition(nullValue.length());
                return ValueNode.createNullNode();
            }
        }
        throw new InvalidPathException("Expected <null> value");
    }

    private JsonNode readJsonLiteral(){
        int begin = filter.position();

        char openChar = filter.currentChar();

        assert openChar == OPEN_ARRAY || openChar == OPEN_OBJECT;

        char closeChar = openChar == OPEN_ARRAY ? CLOSE_ARRAY : CLOSE_OBJECT;

        int closingIndex = filter.indexOfMatchingCloseChar(filter.position(), openChar, closeChar, true, false);
        if (closingIndex == -1) {
            throw new InvalidPathException("String not closed. Expected " + SINGLE_QUOTE + " in " + filter);
        } else {
            filter.setPosition(closingIndex + 1);
        }
        CharSequence json = filter.subSequence(begin, filter.position());
        return ValueNode.createJsonNode(json);

    }

    private PatternNode readPattern() {
        int begin = filter.position();
        int closingIndex = filter.nextIndexOfUnescaped(PATTERN);
        if (closingIndex == -1) {
            throw new InvalidPathException("Pattern not closed. Expected " + PATTERN + " in " + filter);
        } else {
            if(filter.inBounds(closingIndex+1)) {
                int equalSignIndex = filter.nextIndexOf('=');
                int endIndex = equalSignIndex > closingIndex ? equalSignIndex : filter.nextIndexOfUnescaped(CLOSE_PARENTHESIS);
                CharSequence flags = filter.subSequence(closingIndex + 1, endIndex);
                closingIndex += flags.length();
            }
            filter.setPosition(closingIndex + 1);
        }
        CharSequence pattern = filter.subSequence(begin, filter.position());
        return ValueNode.createPatternNode(pattern);
    }

    private StringNode readStringLiteral(char endChar) {
        int begin = filter.position();

        int closingSingleQuoteIndex = filter.nextIndexOfUnescaped(endChar);
        if (closingSingleQuoteIndex == -1) {
            throw new InvalidPathException("String literal does not have matching quotes. Expected " + endChar + " in " + filter);
        } else {
            filter.setPosition(closingSingleQuoteIndex + 1);
        }
        CharSequence stringLiteral = filter.subSequence(begin, filter.position());
        return ValueNode.createStringNode(stringLiteral, true);
    }

    private NumberNode readNumberLiteral() {
        int begin = filter.position();

        while (filter.inBounds() && filter.isNumberCharacter(filter.position())) {
            filter.incrementPosition(1);
        }
        CharSequence numberLiteral = filter.subSequence(begin, filter.position());
        return ValueNode.createNumberNode(numberLiteral);
    }

    private BooleanNode readBooleanLiteral() {
        int begin = filter.position();
        int end = filter.currentChar() == TRUE ? filter.position() + 3 : filter.position() + 4;

        if(!filter.inBounds(end)){
            throw new InvalidPathException("Expected boolean literal");
        }
        CharSequence boolValue = filter.subSequence(begin, end+1);
        if(!boolValue.equals("true") && !boolValue.equals("false")){
            throw new InvalidPathException("Expected boolean literal");
        }
        filter.incrementPosition(boolValue.length());

        return ValueNode.createBooleanNode(boolValue);
    }

    private PathNode readPath() {
        char previousSignificantChar = filter.previousSignificantChar();
        int begin = filter.position();

        filter.incrementPosition(1); //skip $ and @
        while (filter.inBounds()) {
            if (filter.currentChar() == OPEN_SQUARE_BRACKET) {
                int closingSquareBracketIndex = filter.indexOfMatchingCloseChar(filter.position(), OPEN_SQUARE_BRACKET, CLOSE_SQUARE_BRACKET, true, false);
                if (closingSquareBracketIndex == -1) {
                    throw new InvalidPathException("Square brackets does not match in filter " + filter);
                } else {
                    filter.setPosition(closingSquareBracketIndex + 1);
                }
            }
            boolean closingFunctionBracket = (filter.currentChar() == CLOSE_PARENTHESIS && currentCharIsClosingFunctionBracket(begin));
            boolean closingLogicalBracket  = (filter.currentChar() == CLOSE_PARENTHESIS && !closingFunctionBracket);

            if (!filter.inBounds() || isRelationalOperatorChar(filter.currentChar()) || filter.currentChar() == SPACE || closingLogicalBracket) {
                break;
            } else {
                filter.incrementPosition(1);
            }
        }

        boolean shouldExists = !(previousSignificantChar == NOT);
        CharSequence path = filter.subSequence(begin, filter.position());
        return ValueNode.createPathNode(path, false, shouldExists);
    }

    private boolean expressionIsTerminated(){
        char c = filter.currentChar();
        if(c == CLOSE_PARENTHESIS || isLogicalOperatorChar(c)){
            return true;
        }
        c = filter.nextSignificantChar();
        if(c == CLOSE_PARENTHESIS || isLogicalOperatorChar(c)){
            return true;
        }
        return false;
    }

    private boolean currentCharIsClosingFunctionBracket(int lowerBound){
        if(filter.currentChar() != CLOSE_PARENTHESIS){
            return false;
        }
        int idx = filter.indexOfPreviousSignificantChar();
        if(idx == -1 || filter.charAt(idx) != OPEN_PARENTHESIS){
            return false;
        }
        idx--;
        while(filter.inBounds(idx) && idx > lowerBound){
            if(filter.charAt(idx) == PERIOD){
                return true;
            }
            idx--;
        }
        return false;
    }

    private boolean isLogicalOperatorChar(char c) {
        return c == AND || c == OR;
    }

    private boolean isRelationalOperatorChar(char c) {
        return c == LT || c == GT || c == EQ || c == TILDE || c == NOT;
    }

    private static final class CompiledFilter extends Filter {

        private final Predicate predicate;

        private CompiledFilter(Predicate predicate) {
            this.predicate = predicate;
        }

        @Override
        public boolean apply(Predicate.PredicateContext ctx) {
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
}
