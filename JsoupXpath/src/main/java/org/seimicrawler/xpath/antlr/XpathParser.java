// Generated from resources/Xpath.g4 by ANTLR 4.7.2
package org.seimicrawler.xpath.antlr;

import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.RuntimeMetaData;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.VocabularyImpl;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.ParserATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class XpathParser extends Parser {
    static {
        RuntimeMetaData.checkVersion("4.7.2", RuntimeMetaData.VERSION);
    }

    protected static final DFA[] _decisionToDFA;
    protected static final PredictionContextCache _sharedContextCache =
            new PredictionContextCache();
    public static final int
            T__0 = 1, T__1 = 2, T__2 = 3, T__3 = 4, NodeType = 5, Number = 6, AxisName = 7, PATHSEP = 8,
            ABRPATH = 9, LPAR = 10, RPAR = 11, LBRAC = 12, RBRAC = 13, MINUS = 14, PLUS = 15, DOT = 16,
            MUL = 17, DIVISION = 18, MODULO = 19, DOTDOT = 20, AT = 21, COMMA = 22, PIPE = 23, LESS = 24,
            MORE_ = 25, LE = 26, GE = 27, EQUALITY = 28, INEQUALITY = 29, START_WITH = 30, END_WITH = 31,
            CONTAIN_WITH = 32, REGEXP_WITH = 33, REGEXP_NOT_WITH = 34, COLON = 35, CC = 36,
            APOS = 37, QUOT = 38, Literal = 39, Whitespace = 40, NCName = 41;
    public static final int
            RULE_main = 0, RULE_locationPath = 1, RULE_absoluteLocationPathNoroot = 2,
            RULE_relativeLocationPath = 3, RULE_step = 4, RULE_axisSpecifier = 5,
            RULE_nodeTest = 6, RULE_predicate = 7, RULE_abbreviatedStep = 8, RULE_expr = 9,
            RULE_primaryExpr = 10, RULE_functionCall = 11, RULE_unionExprNoRoot = 12,
            RULE_pathExprNoRoot = 13, RULE_filterExpr = 14, RULE_orExpr = 15, RULE_andExpr = 16,
            RULE_equalityExpr = 17, RULE_relationalExpr = 18, RULE_additiveExpr = 19,
            RULE_multiplicativeExpr = 20, RULE_unaryExprNoRoot = 21, RULE_qName = 22,
            RULE_functionName = 23, RULE_variableReference = 24, RULE_nameTest = 25,
            RULE_nCName = 26;

    private static String[] makeRuleNames() {
        return new String[]{
                "main", "locationPath", "absoluteLocationPathNoroot", "relativeLocationPath",
                "step", "axisSpecifier", "nodeTest", "predicate", "abbreviatedStep",
                "expr", "primaryExpr", "functionCall", "unionExprNoRoot", "pathExprNoRoot",
                "filterExpr", "orExpr", "andExpr", "equalityExpr", "relationalExpr",
                "additiveExpr", "multiplicativeExpr", "unaryExprNoRoot", "qName", "functionName",
                "variableReference", "nameTest", "nCName"
        };
    }

    public static final String[] ruleNames = makeRuleNames();

    private static String[] makeLiteralNames() {
        return new String[]{
                null, "'processing-instruction'", "'or'", "'and'", "'$'", null, null,
                null, "'/'", "'//'", "'('", "')'", "'['", "']'", "'-'", "'+'", "'.'",
                "'*'", "'`div`'", "'`mod`'", "'..'", "'@'", "','", "'|'", "'<'", "'>'",
                "'<='", "'>='", "'='", "'!='", "'^='", "'$='", "'*='", "'~='", "'!~'",
                "':'", "'::'", "'''", "'\"'"
        };
    }

    private static final String[] _LITERAL_NAMES = makeLiteralNames();

    private static String[] makeSymbolicNames() {
        return new String[]{
                null, null, null, null, null, "NodeType", "Number", "AxisName", "PATHSEP",
                "ABRPATH", "LPAR", "RPAR", "LBRAC", "RBRAC", "MINUS", "PLUS", "DOT",
                "MUL", "DIVISION", "MODULO", "DOTDOT", "AT", "COMMA", "PIPE", "LESS",
                "MORE_", "LE", "GE", "EQUALITY", "INEQUALITY", "START_WITH", "END_WITH",
                "CONTAIN_WITH", "REGEXP_WITH", "REGEXP_NOT_WITH", "COLON", "CC", "APOS",
                "QUOT", "Literal", "Whitespace", "NCName"
        };
    }

    private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
    public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

    /**
     * @deprecated Use {@link #VOCABULARY} instead.
     */
    @Deprecated
    public static final String[] tokenNames;

    static {
        tokenNames = new String[_SYMBOLIC_NAMES.length];
        for (int i = 0; i < tokenNames.length; i++) {
            tokenNames[i] = VOCABULARY.getLiteralName(i);
            if (tokenNames[i] == null) {
                tokenNames[i] = VOCABULARY.getSymbolicName(i);
            }

            if (tokenNames[i] == null) {
                tokenNames[i] = "<INVALID>";
            }
        }
    }

    @Override
    @Deprecated
    public String[] getTokenNames() {
        return tokenNames;
    }

    @Override

    public Vocabulary getVocabulary() {
        return VOCABULARY;
    }

    @Override
    public String getGrammarFileName() {
        return "Xpath.g4";
    }

    @Override
    public String[] getRuleNames() {
        return ruleNames;
    }

    @Override
    public String getSerializedATN() {
        return _serializedATN;
    }

    @Override
    public ATN getATN() {
        return _ATN;
    }

    public XpathParser(TokenStream input) {
        super(input);
        _interp = new ParserATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
    }

    public static class MainContext extends ParserRuleContext {
        public ExprContext expr() {
            return getRuleContext(ExprContext.class, 0);
        }

        public MainContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_main;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener) ((XpathListener) listener).enterMain(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener) ((XpathListener) listener).exitMain(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof XpathVisitor)
                return ((XpathVisitor<? extends T>) visitor).visitMain(this);
            else return visitor.visitChildren(this);
        }
    }

    public final MainContext main() throws RecognitionException {
        MainContext _localctx = new MainContext(_ctx, getState());
        enterRule(_localctx, 0, RULE_main);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(54);
                expr();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class LocationPathContext extends ParserRuleContext {
        public RelativeLocationPathContext relativeLocationPath() {
            return getRuleContext(RelativeLocationPathContext.class, 0);
        }

        public AbsoluteLocationPathNorootContext absoluteLocationPathNoroot() {
            return getRuleContext(AbsoluteLocationPathNorootContext.class, 0);
        }

        public LocationPathContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_locationPath;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).enterLocationPath(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).exitLocationPath(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof XpathVisitor)
                return ((XpathVisitor<? extends T>) visitor).visitLocationPath(this);
            else return visitor.visitChildren(this);
        }
    }

    public final LocationPathContext locationPath() throws RecognitionException {
        LocationPathContext _localctx = new LocationPathContext(_ctx, getState());
        enterRule(_localctx, 2, RULE_locationPath);
        try {
            setState(58);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case T__0:
                case NodeType:
                case AxisName:
                case DOT:
                case MUL:
                case DOTDOT:
                case AT:
                case NCName:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(56);
                    relativeLocationPath();
                }
                break;
                case PATHSEP:
                case ABRPATH:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(57);
                    absoluteLocationPathNoroot();
                }
                break;
                default:
                    throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class AbsoluteLocationPathNorootContext extends ParserRuleContext {
        public Token op;

        public RelativeLocationPathContext relativeLocationPath() {
            return getRuleContext(RelativeLocationPathContext.class, 0);
        }

        public TerminalNode PATHSEP() {
            return getToken(XpathParser.PATHSEP, 0);
        }

        public TerminalNode ABRPATH() {
            return getToken(XpathParser.ABRPATH, 0);
        }

        public AbsoluteLocationPathNorootContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_absoluteLocationPathNoroot;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).enterAbsoluteLocationPathNoroot(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).exitAbsoluteLocationPathNoroot(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof XpathVisitor)
                return ((XpathVisitor<? extends T>) visitor).visitAbsoluteLocationPathNoroot(this);
            else return visitor.visitChildren(this);
        }
    }

    public final AbsoluteLocationPathNorootContext absoluteLocationPathNoroot() throws RecognitionException {
        AbsoluteLocationPathNorootContext _localctx = new AbsoluteLocationPathNorootContext(_ctx, getState());
        enterRule(_localctx, 4, RULE_absoluteLocationPathNoroot);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(60);
                ((AbsoluteLocationPathNorootContext) _localctx).op = _input.LT(1);
                _la = _input.LA(1);
                if (!(_la == PATHSEP || _la == ABRPATH)) {
                    ((AbsoluteLocationPathNorootContext) _localctx).op = (Token) _errHandler.recoverInline(this);
                } else {
                    if (_input.LA(1) == Token.EOF) matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
                setState(61);
                relativeLocationPath();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class RelativeLocationPathContext extends ParserRuleContext {
        public Token op;

        public List<StepContext> step() {
            return getRuleContexts(StepContext.class);
        }

        public StepContext step(int i) {
            return getRuleContext(StepContext.class, i);
        }

        public List<TerminalNode> PATHSEP() {
            return getTokens(XpathParser.PATHSEP);
        }

        public TerminalNode PATHSEP(int i) {
            return getToken(XpathParser.PATHSEP, i);
        }

        public List<TerminalNode> ABRPATH() {
            return getTokens(XpathParser.ABRPATH);
        }

        public TerminalNode ABRPATH(int i) {
            return getToken(XpathParser.ABRPATH, i);
        }

        public RelativeLocationPathContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_relativeLocationPath;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).enterRelativeLocationPath(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).exitRelativeLocationPath(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof XpathVisitor)
                return ((XpathVisitor<? extends T>) visitor).visitRelativeLocationPath(this);
            else return visitor.visitChildren(this);
        }
    }

    public final RelativeLocationPathContext relativeLocationPath() throws RecognitionException {
        RelativeLocationPathContext _localctx = new RelativeLocationPathContext(_ctx, getState());
        enterRule(_localctx, 6, RULE_relativeLocationPath);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(63);
                step();
                setState(68);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == PATHSEP || _la == ABRPATH) {
                    {
                        {
                            setState(64);
                            ((RelativeLocationPathContext) _localctx).op = _input.LT(1);
                            _la = _input.LA(1);
                            if (!(_la == PATHSEP || _la == ABRPATH)) {
                                ((RelativeLocationPathContext) _localctx).op = (Token) _errHandler.recoverInline(this);
                            } else {
                                if (_input.LA(1) == Token.EOF) matchedEOF = true;
                                _errHandler.reportMatch(this);
                                consume();
                            }
                            setState(65);
                            step();
                        }
                    }
                    setState(70);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class StepContext extends ParserRuleContext {
        public AxisSpecifierContext axisSpecifier() {
            return getRuleContext(AxisSpecifierContext.class, 0);
        }

        public NodeTestContext nodeTest() {
            return getRuleContext(NodeTestContext.class, 0);
        }

        public List<PredicateContext> predicate() {
            return getRuleContexts(PredicateContext.class);
        }

        public PredicateContext predicate(int i) {
            return getRuleContext(PredicateContext.class, i);
        }

        public AbbreviatedStepContext abbreviatedStep() {
            return getRuleContext(AbbreviatedStepContext.class, 0);
        }

        public StepContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_step;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener) ((XpathListener) listener).enterStep(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener) ((XpathListener) listener).exitStep(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof XpathVisitor)
                return ((XpathVisitor<? extends T>) visitor).visitStep(this);
            else return visitor.visitChildren(this);
        }
    }

    public final StepContext step() throws RecognitionException {
        StepContext _localctx = new StepContext(_ctx, getState());
        enterRule(_localctx, 8, RULE_step);
        int _la;
        try {
            setState(80);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case T__0:
                case NodeType:
                case AxisName:
                case MUL:
                case AT:
                case NCName:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(71);
                    axisSpecifier();
                    setState(72);
                    nodeTest();
                    setState(76);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    while (_la == LBRAC) {
                        {
                            {
                                setState(73);
                                predicate();
                            }
                        }
                        setState(78);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                    }
                }
                break;
                case DOT:
                case DOTDOT:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(79);
                    abbreviatedStep();
                }
                break;
                default:
                    throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class AxisSpecifierContext extends ParserRuleContext {
        public TerminalNode AxisName() {
            return getToken(XpathParser.AxisName, 0);
        }

        public TerminalNode CC() {
            return getToken(XpathParser.CC, 0);
        }

        public TerminalNode AT() {
            return getToken(XpathParser.AT, 0);
        }

        public AxisSpecifierContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_axisSpecifier;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).enterAxisSpecifier(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).exitAxisSpecifier(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof XpathVisitor)
                return ((XpathVisitor<? extends T>) visitor).visitAxisSpecifier(this);
            else return visitor.visitChildren(this);
        }
    }

    public final AxisSpecifierContext axisSpecifier() throws RecognitionException {
        AxisSpecifierContext _localctx = new AxisSpecifierContext(_ctx, getState());
        enterRule(_localctx, 10, RULE_axisSpecifier);
        int _la;
        try {
            setState(87);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 5, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(82);
                    match(AxisName);
                    setState(83);
                    match(CC);
                }
                break;
                case 2:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(85);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (_la == AT) {
                        {
                            setState(84);
                            match(AT);
                        }
                    }

                }
                break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class NodeTestContext extends ParserRuleContext {
        public NameTestContext nameTest() {
            return getRuleContext(NameTestContext.class, 0);
        }

        public TerminalNode NodeType() {
            return getToken(XpathParser.NodeType, 0);
        }

        public TerminalNode LPAR() {
            return getToken(XpathParser.LPAR, 0);
        }

        public TerminalNode RPAR() {
            return getToken(XpathParser.RPAR, 0);
        }

        public TerminalNode Literal() {
            return getToken(XpathParser.Literal, 0);
        }

        public NodeTestContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_nodeTest;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener) ((XpathListener) listener).enterNodeTest(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener) ((XpathListener) listener).exitNodeTest(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof XpathVisitor)
                return ((XpathVisitor<? extends T>) visitor).visitNodeTest(this);
            else return visitor.visitChildren(this);
        }
    }

    public final NodeTestContext nodeTest() throws RecognitionException {
        NodeTestContext _localctx = new NodeTestContext(_ctx, getState());
        enterRule(_localctx, 12, RULE_nodeTest);
        try {
            setState(97);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case AxisName:
                case MUL:
                case NCName:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(89);
                    nameTest();
                }
                break;
                case NodeType:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(90);
                    match(NodeType);
                    setState(91);
                    match(LPAR);
                    setState(92);
                    match(RPAR);
                }
                break;
                case T__0:
                    enterOuterAlt(_localctx, 3);
                {
                    setState(93);
                    match(T__0);
                    setState(94);
                    match(LPAR);
                    setState(95);
                    match(Literal);
                    setState(96);
                    match(RPAR);
                }
                break;
                default:
                    throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class PredicateContext extends ParserRuleContext {
        public TerminalNode LBRAC() {
            return getToken(XpathParser.LBRAC, 0);
        }

        public ExprContext expr() {
            return getRuleContext(ExprContext.class, 0);
        }

        public TerminalNode RBRAC() {
            return getToken(XpathParser.RBRAC, 0);
        }

        public PredicateContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_predicate;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener) ((XpathListener) listener).enterPredicate(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener) ((XpathListener) listener).exitPredicate(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof XpathVisitor)
                return ((XpathVisitor<? extends T>) visitor).visitPredicate(this);
            else return visitor.visitChildren(this);
        }
    }

    public final PredicateContext predicate() throws RecognitionException {
        PredicateContext _localctx = new PredicateContext(_ctx, getState());
        enterRule(_localctx, 14, RULE_predicate);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(99);
                match(LBRAC);
                setState(100);
                expr();
                setState(101);
                match(RBRAC);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class AbbreviatedStepContext extends ParserRuleContext {
        public TerminalNode DOT() {
            return getToken(XpathParser.DOT, 0);
        }

        public TerminalNode DOTDOT() {
            return getToken(XpathParser.DOTDOT, 0);
        }

        public AbbreviatedStepContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_abbreviatedStep;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).enterAbbreviatedStep(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).exitAbbreviatedStep(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof XpathVisitor)
                return ((XpathVisitor<? extends T>) visitor).visitAbbreviatedStep(this);
            else return visitor.visitChildren(this);
        }
    }

    public final AbbreviatedStepContext abbreviatedStep() throws RecognitionException {
        AbbreviatedStepContext _localctx = new AbbreviatedStepContext(_ctx, getState());
        enterRule(_localctx, 16, RULE_abbreviatedStep);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(103);
                _la = _input.LA(1);
                if (!(_la == DOT || _la == DOTDOT)) {
                    _errHandler.recoverInline(this);
                } else {
                    if (_input.LA(1) == Token.EOF) matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class ExprContext extends ParserRuleContext {
        public OrExprContext orExpr() {
            return getRuleContext(OrExprContext.class, 0);
        }

        public ExprContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_expr;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener) ((XpathListener) listener).enterExpr(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener) ((XpathListener) listener).exitExpr(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof XpathVisitor)
                return ((XpathVisitor<? extends T>) visitor).visitExpr(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ExprContext expr() throws RecognitionException {
        ExprContext _localctx = new ExprContext(_ctx, getState());
        enterRule(_localctx, 18, RULE_expr);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(105);
                orExpr();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class PrimaryExprContext extends ParserRuleContext {
        public VariableReferenceContext variableReference() {
            return getRuleContext(VariableReferenceContext.class, 0);
        }

        public TerminalNode LPAR() {
            return getToken(XpathParser.LPAR, 0);
        }

        public ExprContext expr() {
            return getRuleContext(ExprContext.class, 0);
        }

        public TerminalNode RPAR() {
            return getToken(XpathParser.RPAR, 0);
        }

        public TerminalNode Literal() {
            return getToken(XpathParser.Literal, 0);
        }

        public TerminalNode Number() {
            return getToken(XpathParser.Number, 0);
        }

        public FunctionCallContext functionCall() {
            return getRuleContext(FunctionCallContext.class, 0);
        }

        public PrimaryExprContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_primaryExpr;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).enterPrimaryExpr(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener) ((XpathListener) listener).exitPrimaryExpr(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof XpathVisitor)
                return ((XpathVisitor<? extends T>) visitor).visitPrimaryExpr(this);
            else return visitor.visitChildren(this);
        }
    }

    public final PrimaryExprContext primaryExpr() throws RecognitionException {
        PrimaryExprContext _localctx = new PrimaryExprContext(_ctx, getState());
        enterRule(_localctx, 20, RULE_primaryExpr);
        try {
            setState(115);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case T__3:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(107);
                    variableReference();
                }
                break;
                case LPAR:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(108);
                    match(LPAR);
                    setState(109);
                    expr();
                    setState(110);
                    match(RPAR);
                }
                break;
                case Literal:
                    enterOuterAlt(_localctx, 3);
                {
                    setState(112);
                    match(Literal);
                }
                break;
                case Number:
                    enterOuterAlt(_localctx, 4);
                {
                    setState(113);
                    match(Number);
                }
                break;
                case AxisName:
                case NCName:
                    enterOuterAlt(_localctx, 5);
                {
                    setState(114);
                    functionCall();
                }
                break;
                default:
                    throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class FunctionCallContext extends ParserRuleContext {
        public FunctionNameContext functionName() {
            return getRuleContext(FunctionNameContext.class, 0);
        }

        public TerminalNode LPAR() {
            return getToken(XpathParser.LPAR, 0);
        }

        public TerminalNode RPAR() {
            return getToken(XpathParser.RPAR, 0);
        }

        public List<ExprContext> expr() {
            return getRuleContexts(ExprContext.class);
        }

        public ExprContext expr(int i) {
            return getRuleContext(ExprContext.class, i);
        }

        public List<TerminalNode> COMMA() {
            return getTokens(XpathParser.COMMA);
        }

        public TerminalNode COMMA(int i) {
            return getToken(XpathParser.COMMA, i);
        }

        public FunctionCallContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_functionCall;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).enterFunctionCall(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).exitFunctionCall(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof XpathVisitor)
                return ((XpathVisitor<? extends T>) visitor).visitFunctionCall(this);
            else return visitor.visitChildren(this);
        }
    }

    public final FunctionCallContext functionCall() throws RecognitionException {
        FunctionCallContext _localctx = new FunctionCallContext(_ctx, getState());
        enterRule(_localctx, 22, RULE_functionCall);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(117);
                functionName();
                setState(118);
                match(LPAR);
                setState(127);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__3) | (1L << NodeType) | (1L << Number) | (1L << AxisName) | (1L << PATHSEP) | (1L << ABRPATH) | (1L << LPAR) | (1L << MINUS) | (1L << DOT) | (1L << MUL) | (1L << DOTDOT) | (1L << AT) | (1L << Literal) | (1L << NCName))) != 0)) {
                    {
                        setState(119);
                        expr();
                        setState(124);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (_la == COMMA) {
                            {
                                {
                                    setState(120);
                                    match(COMMA);
                                    setState(121);
                                    expr();
                                }
                            }
                            setState(126);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                    }
                }

                setState(129);
                match(RPAR);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class UnionExprNoRootContext extends ParserRuleContext {
        public Token op;

        public PathExprNoRootContext pathExprNoRoot() {
            return getRuleContext(PathExprNoRootContext.class, 0);
        }

        public UnionExprNoRootContext unionExprNoRoot() {
            return getRuleContext(UnionExprNoRootContext.class, 0);
        }

        public TerminalNode PIPE() {
            return getToken(XpathParser.PIPE, 0);
        }

        public TerminalNode PATHSEP() {
            return getToken(XpathParser.PATHSEP, 0);
        }

        public UnionExprNoRootContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_unionExprNoRoot;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).enterUnionExprNoRoot(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).exitUnionExprNoRoot(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof XpathVisitor)
                return ((XpathVisitor<? extends T>) visitor).visitUnionExprNoRoot(this);
            else return visitor.visitChildren(this);
        }
    }

    public final UnionExprNoRootContext unionExprNoRoot() throws RecognitionException {
        UnionExprNoRootContext _localctx = new UnionExprNoRootContext(_ctx, getState());
        enterRule(_localctx, 24, RULE_unionExprNoRoot);
        int _la;
        try {
            setState(139);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 11, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(131);
                    pathExprNoRoot();
                    setState(134);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (_la == PIPE) {
                        {
                            setState(132);
                            ((UnionExprNoRootContext) _localctx).op = match(PIPE);
                            setState(133);
                            unionExprNoRoot();
                        }
                    }

                }
                break;
                case 2:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(136);
                    match(PATHSEP);
                    setState(137);
                    match(PIPE);
                    setState(138);
                    unionExprNoRoot();
                }
                break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class PathExprNoRootContext extends ParserRuleContext {
        public Token op;

        public LocationPathContext locationPath() {
            return getRuleContext(LocationPathContext.class, 0);
        }

        public FilterExprContext filterExpr() {
            return getRuleContext(FilterExprContext.class, 0);
        }

        public RelativeLocationPathContext relativeLocationPath() {
            return getRuleContext(RelativeLocationPathContext.class, 0);
        }

        public TerminalNode PATHSEP() {
            return getToken(XpathParser.PATHSEP, 0);
        }

        public TerminalNode ABRPATH() {
            return getToken(XpathParser.ABRPATH, 0);
        }

        public PathExprNoRootContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_pathExprNoRoot;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).enterPathExprNoRoot(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).exitPathExprNoRoot(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof XpathVisitor)
                return ((XpathVisitor<? extends T>) visitor).visitPathExprNoRoot(this);
            else return visitor.visitChildren(this);
        }
    }

    public final PathExprNoRootContext pathExprNoRoot() throws RecognitionException {
        PathExprNoRootContext _localctx = new PathExprNoRootContext(_ctx, getState());
        enterRule(_localctx, 26, RULE_pathExprNoRoot);
        int _la;
        try {
            setState(147);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 13, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(141);
                    locationPath();
                }
                break;
                case 2:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(142);
                    filterExpr();
                    setState(145);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (_la == PATHSEP || _la == ABRPATH) {
                        {
                            setState(143);
                            ((PathExprNoRootContext) _localctx).op = _input.LT(1);
                            _la = _input.LA(1);
                            if (!(_la == PATHSEP || _la == ABRPATH)) {
                                ((PathExprNoRootContext) _localctx).op = (Token) _errHandler.recoverInline(this);
                            } else {
                                if (_input.LA(1) == Token.EOF) matchedEOF = true;
                                _errHandler.reportMatch(this);
                                consume();
                            }
                            setState(144);
                            relativeLocationPath();
                        }
                    }

                }
                break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class FilterExprContext extends ParserRuleContext {
        public PrimaryExprContext primaryExpr() {
            return getRuleContext(PrimaryExprContext.class, 0);
        }

        public List<PredicateContext> predicate() {
            return getRuleContexts(PredicateContext.class);
        }

        public PredicateContext predicate(int i) {
            return getRuleContext(PredicateContext.class, i);
        }

        public FilterExprContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_filterExpr;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener) ((XpathListener) listener).enterFilterExpr(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener) ((XpathListener) listener).exitFilterExpr(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof XpathVisitor)
                return ((XpathVisitor<? extends T>) visitor).visitFilterExpr(this);
            else return visitor.visitChildren(this);
        }
    }

    public final FilterExprContext filterExpr() throws RecognitionException {
        FilterExprContext _localctx = new FilterExprContext(_ctx, getState());
        enterRule(_localctx, 28, RULE_filterExpr);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(149);
                primaryExpr();
                setState(153);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == LBRAC) {
                    {
                        {
                            setState(150);
                            predicate();
                        }
                    }
                    setState(155);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class OrExprContext extends ParserRuleContext {
        public List<AndExprContext> andExpr() {
            return getRuleContexts(AndExprContext.class);
        }

        public AndExprContext andExpr(int i) {
            return getRuleContext(AndExprContext.class, i);
        }

        public OrExprContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_orExpr;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener) ((XpathListener) listener).enterOrExpr(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener) ((XpathListener) listener).exitOrExpr(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof XpathVisitor)
                return ((XpathVisitor<? extends T>) visitor).visitOrExpr(this);
            else return visitor.visitChildren(this);
        }
    }

    public final OrExprContext orExpr() throws RecognitionException {
        OrExprContext _localctx = new OrExprContext(_ctx, getState());
        enterRule(_localctx, 30, RULE_orExpr);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(156);
                andExpr();
                setState(161);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == T__1) {
                    {
                        {
                            setState(157);
                            match(T__1);
                            setState(158);
                            andExpr();
                        }
                    }
                    setState(163);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class AndExprContext extends ParserRuleContext {
        public List<EqualityExprContext> equalityExpr() {
            return getRuleContexts(EqualityExprContext.class);
        }

        public EqualityExprContext equalityExpr(int i) {
            return getRuleContext(EqualityExprContext.class, i);
        }

        public AndExprContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_andExpr;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener) ((XpathListener) listener).enterAndExpr(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener) ((XpathListener) listener).exitAndExpr(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof XpathVisitor)
                return ((XpathVisitor<? extends T>) visitor).visitAndExpr(this);
            else return visitor.visitChildren(this);
        }
    }

    public final AndExprContext andExpr() throws RecognitionException {
        AndExprContext _localctx = new AndExprContext(_ctx, getState());
        enterRule(_localctx, 32, RULE_andExpr);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(164);
                equalityExpr();
                setState(169);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == T__2) {
                    {
                        {
                            setState(165);
                            match(T__2);
                            setState(166);
                            equalityExpr();
                        }
                    }
                    setState(171);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class EqualityExprContext extends ParserRuleContext {
        public Token op;

        public List<RelationalExprContext> relationalExpr() {
            return getRuleContexts(RelationalExprContext.class);
        }

        public RelationalExprContext relationalExpr(int i) {
            return getRuleContext(RelationalExprContext.class, i);
        }

        public List<TerminalNode> EQUALITY() {
            return getTokens(XpathParser.EQUALITY);
        }

        public TerminalNode EQUALITY(int i) {
            return getToken(XpathParser.EQUALITY, i);
        }

        public List<TerminalNode> INEQUALITY() {
            return getTokens(XpathParser.INEQUALITY);
        }

        public TerminalNode INEQUALITY(int i) {
            return getToken(XpathParser.INEQUALITY, i);
        }

        public EqualityExprContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_equalityExpr;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).enterEqualityExpr(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).exitEqualityExpr(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof XpathVisitor)
                return ((XpathVisitor<? extends T>) visitor).visitEqualityExpr(this);
            else return visitor.visitChildren(this);
        }
    }

    public final EqualityExprContext equalityExpr() throws RecognitionException {
        EqualityExprContext _localctx = new EqualityExprContext(_ctx, getState());
        enterRule(_localctx, 34, RULE_equalityExpr);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(172);
                relationalExpr();
                setState(177);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == EQUALITY || _la == INEQUALITY) {
                    {
                        {
                            setState(173);
                            ((EqualityExprContext) _localctx).op = _input.LT(1);
                            _la = _input.LA(1);
                            if (!(_la == EQUALITY || _la == INEQUALITY)) {
                                ((EqualityExprContext) _localctx).op = (Token) _errHandler.recoverInline(this);
                            } else {
                                if (_input.LA(1) == Token.EOF) matchedEOF = true;
                                _errHandler.reportMatch(this);
                                consume();
                            }
                            setState(174);
                            relationalExpr();
                        }
                    }
                    setState(179);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class RelationalExprContext extends ParserRuleContext {
        public Token op;

        public List<AdditiveExprContext> additiveExpr() {
            return getRuleContexts(AdditiveExprContext.class);
        }

        public AdditiveExprContext additiveExpr(int i) {
            return getRuleContext(AdditiveExprContext.class, i);
        }

        public List<TerminalNode> LESS() {
            return getTokens(XpathParser.LESS);
        }

        public TerminalNode LESS(int i) {
            return getToken(XpathParser.LESS, i);
        }

        public List<TerminalNode> MORE_() {
            return getTokens(XpathParser.MORE_);
        }

        public TerminalNode MORE_(int i) {
            return getToken(XpathParser.MORE_, i);
        }

        public List<TerminalNode> GE() {
            return getTokens(XpathParser.GE);
        }

        public TerminalNode GE(int i) {
            return getToken(XpathParser.GE, i);
        }

        public List<TerminalNode> START_WITH() {
            return getTokens(XpathParser.START_WITH);
        }

        public TerminalNode START_WITH(int i) {
            return getToken(XpathParser.START_WITH, i);
        }

        public List<TerminalNode> END_WITH() {
            return getTokens(XpathParser.END_WITH);
        }

        public TerminalNode END_WITH(int i) {
            return getToken(XpathParser.END_WITH, i);
        }

        public List<TerminalNode> CONTAIN_WITH() {
            return getTokens(XpathParser.CONTAIN_WITH);
        }

        public TerminalNode CONTAIN_WITH(int i) {
            return getToken(XpathParser.CONTAIN_WITH, i);
        }

        public List<TerminalNode> REGEXP_WITH() {
            return getTokens(XpathParser.REGEXP_WITH);
        }

        public TerminalNode REGEXP_WITH(int i) {
            return getToken(XpathParser.REGEXP_WITH, i);
        }

        public List<TerminalNode> REGEXP_NOT_WITH() {
            return getTokens(XpathParser.REGEXP_NOT_WITH);
        }

        public TerminalNode REGEXP_NOT_WITH(int i) {
            return getToken(XpathParser.REGEXP_NOT_WITH, i);
        }

        public RelationalExprContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_relationalExpr;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).enterRelationalExpr(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).exitRelationalExpr(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof XpathVisitor)
                return ((XpathVisitor<? extends T>) visitor).visitRelationalExpr(this);
            else return visitor.visitChildren(this);
        }
    }

    public final RelationalExprContext relationalExpr() throws RecognitionException {
        RelationalExprContext _localctx = new RelationalExprContext(_ctx, getState());
        enterRule(_localctx, 36, RULE_relationalExpr);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(180);
                additiveExpr();
                setState(185);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << LESS) | (1L << MORE_) | (1L << GE) | (1L << START_WITH) | (1L << END_WITH) | (1L << CONTAIN_WITH) | (1L << REGEXP_WITH) | (1L << REGEXP_NOT_WITH))) != 0)) {
                    {
                        {
                            setState(181);
                            ((RelationalExprContext) _localctx).op = _input.LT(1);
                            _la = _input.LA(1);
                            if (!((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << LESS) | (1L << MORE_) | (1L << GE) | (1L << START_WITH) | (1L << END_WITH) | (1L << CONTAIN_WITH) | (1L << REGEXP_WITH) | (1L << REGEXP_NOT_WITH))) != 0))) {
                                ((RelationalExprContext) _localctx).op = (Token) _errHandler.recoverInline(this);
                            } else {
                                if (_input.LA(1) == Token.EOF) matchedEOF = true;
                                _errHandler.reportMatch(this);
                                consume();
                            }
                            setState(182);
                            additiveExpr();
                        }
                    }
                    setState(187);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class AdditiveExprContext extends ParserRuleContext {
        public Token op;

        public List<MultiplicativeExprContext> multiplicativeExpr() {
            return getRuleContexts(MultiplicativeExprContext.class);
        }

        public MultiplicativeExprContext multiplicativeExpr(int i) {
            return getRuleContext(MultiplicativeExprContext.class, i);
        }

        public List<TerminalNode> PLUS() {
            return getTokens(XpathParser.PLUS);
        }

        public TerminalNode PLUS(int i) {
            return getToken(XpathParser.PLUS, i);
        }

        public List<TerminalNode> MINUS() {
            return getTokens(XpathParser.MINUS);
        }

        public TerminalNode MINUS(int i) {
            return getToken(XpathParser.MINUS, i);
        }

        public AdditiveExprContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_additiveExpr;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).enterAdditiveExpr(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).exitAdditiveExpr(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof XpathVisitor)
                return ((XpathVisitor<? extends T>) visitor).visitAdditiveExpr(this);
            else return visitor.visitChildren(this);
        }
    }

    public final AdditiveExprContext additiveExpr() throws RecognitionException {
        AdditiveExprContext _localctx = new AdditiveExprContext(_ctx, getState());
        enterRule(_localctx, 38, RULE_additiveExpr);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(188);
                multiplicativeExpr();
                setState(193);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == MINUS || _la == PLUS) {
                    {
                        {
                            setState(189);
                            ((AdditiveExprContext) _localctx).op = _input.LT(1);
                            _la = _input.LA(1);
                            if (!(_la == MINUS || _la == PLUS)) {
                                ((AdditiveExprContext) _localctx).op = (Token) _errHandler.recoverInline(this);
                            } else {
                                if (_input.LA(1) == Token.EOF) matchedEOF = true;
                                _errHandler.reportMatch(this);
                                consume();
                            }
                            setState(190);
                            multiplicativeExpr();
                        }
                    }
                    setState(195);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class MultiplicativeExprContext extends ParserRuleContext {
        public Token op;

        public UnaryExprNoRootContext unaryExprNoRoot() {
            return getRuleContext(UnaryExprNoRootContext.class, 0);
        }

        public MultiplicativeExprContext multiplicativeExpr() {
            return getRuleContext(MultiplicativeExprContext.class, 0);
        }

        public TerminalNode MUL() {
            return getToken(XpathParser.MUL, 0);
        }

        public TerminalNode DIVISION() {
            return getToken(XpathParser.DIVISION, 0);
        }

        public TerminalNode MODULO() {
            return getToken(XpathParser.MODULO, 0);
        }

        public MultiplicativeExprContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_multiplicativeExpr;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).enterMultiplicativeExpr(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).exitMultiplicativeExpr(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof XpathVisitor)
                return ((XpathVisitor<? extends T>) visitor).visitMultiplicativeExpr(this);
            else return visitor.visitChildren(this);
        }
    }

    public final MultiplicativeExprContext multiplicativeExpr() throws RecognitionException {
        MultiplicativeExprContext _localctx = new MultiplicativeExprContext(_ctx, getState());
        enterRule(_localctx, 40, RULE_multiplicativeExpr);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(196);
                unaryExprNoRoot();
                setState(199);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << MUL) | (1L << DIVISION) | (1L << MODULO))) != 0)) {
                    {
                        setState(197);
                        ((MultiplicativeExprContext) _localctx).op = _input.LT(1);
                        _la = _input.LA(1);
                        if (!((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << MUL) | (1L << DIVISION) | (1L << MODULO))) != 0))) {
                            ((MultiplicativeExprContext) _localctx).op = (Token) _errHandler.recoverInline(this);
                        } else {
                            if (_input.LA(1) == Token.EOF) matchedEOF = true;
                            _errHandler.reportMatch(this);
                            consume();
                        }
                        setState(198);
                        multiplicativeExpr();
                    }
                }

            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class UnaryExprNoRootContext extends ParserRuleContext {
        public Token sign;

        public UnionExprNoRootContext unionExprNoRoot() {
            return getRuleContext(UnionExprNoRootContext.class, 0);
        }

        public TerminalNode MINUS() {
            return getToken(XpathParser.MINUS, 0);
        }

        public UnaryExprNoRootContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_unaryExprNoRoot;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).enterUnaryExprNoRoot(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).exitUnaryExprNoRoot(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof XpathVisitor)
                return ((XpathVisitor<? extends T>) visitor).visitUnaryExprNoRoot(this);
            else return visitor.visitChildren(this);
        }
    }

    public final UnaryExprNoRootContext unaryExprNoRoot() throws RecognitionException {
        UnaryExprNoRootContext _localctx = new UnaryExprNoRootContext(_ctx, getState());
        enterRule(_localctx, 42, RULE_unaryExprNoRoot);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(202);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == MINUS) {
                    {
                        setState(201);
                        ((UnaryExprNoRootContext) _localctx).sign = match(MINUS);
                    }
                }

                setState(204);
                unionExprNoRoot();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class QNameContext extends ParserRuleContext {
        public List<NCNameContext> nCName() {
            return getRuleContexts(NCNameContext.class);
        }

        public NCNameContext nCName(int i) {
            return getRuleContext(NCNameContext.class, i);
        }

        public TerminalNode COLON() {
            return getToken(XpathParser.COLON, 0);
        }

        public QNameContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_qName;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener) ((XpathListener) listener).enterQName(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener) ((XpathListener) listener).exitQName(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof XpathVisitor)
                return ((XpathVisitor<? extends T>) visitor).visitQName(this);
            else return visitor.visitChildren(this);
        }
    }

    public final QNameContext qName() throws RecognitionException {
        QNameContext _localctx = new QNameContext(_ctx, getState());
        enterRule(_localctx, 44, RULE_qName);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(206);
                nCName();
                setState(209);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == COLON) {
                    {
                        setState(207);
                        match(COLON);
                        setState(208);
                        nCName();
                    }
                }

            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class FunctionNameContext extends ParserRuleContext {
        public QNameContext qName() {
            return getRuleContext(QNameContext.class, 0);
        }

        public FunctionNameContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_functionName;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).enterFunctionName(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).exitFunctionName(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof XpathVisitor)
                return ((XpathVisitor<? extends T>) visitor).visitFunctionName(this);
            else return visitor.visitChildren(this);
        }
    }

    public final FunctionNameContext functionName() throws RecognitionException {
        FunctionNameContext _localctx = new FunctionNameContext(_ctx, getState());
        enterRule(_localctx, 46, RULE_functionName);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(211);
                qName();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class VariableReferenceContext extends ParserRuleContext {
        public QNameContext qName() {
            return getRuleContext(QNameContext.class, 0);
        }

        public VariableReferenceContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_variableReference;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).enterVariableReference(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener)
                ((XpathListener) listener).exitVariableReference(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof XpathVisitor)
                return ((XpathVisitor<? extends T>) visitor).visitVariableReference(this);
            else return visitor.visitChildren(this);
        }
    }

    public final VariableReferenceContext variableReference() throws RecognitionException {
        VariableReferenceContext _localctx = new VariableReferenceContext(_ctx, getState());
        enterRule(_localctx, 48, RULE_variableReference);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(213);
                match(T__3);
                setState(214);
                qName();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class NameTestContext extends ParserRuleContext {
        public TerminalNode MUL() {
            return getToken(XpathParser.MUL, 0);
        }

        public NCNameContext nCName() {
            return getRuleContext(NCNameContext.class, 0);
        }

        public TerminalNode COLON() {
            return getToken(XpathParser.COLON, 0);
        }

        public QNameContext qName() {
            return getRuleContext(QNameContext.class, 0);
        }

        public NameTestContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_nameTest;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener) ((XpathListener) listener).enterNameTest(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener) ((XpathListener) listener).exitNameTest(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof XpathVisitor)
                return ((XpathVisitor<? extends T>) visitor).visitNameTest(this);
            else return visitor.visitChildren(this);
        }
    }

    public final NameTestContext nameTest() throws RecognitionException {
        NameTestContext _localctx = new NameTestContext(_ctx, getState());
        enterRule(_localctx, 50, RULE_nameTest);
        try {
            setState(222);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 23, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(216);
                    match(MUL);
                }
                break;
                case 2:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(217);
                    nCName();
                    setState(218);
                    match(COLON);
                    setState(219);
                    match(MUL);
                }
                break;
                case 3:
                    enterOuterAlt(_localctx, 3);
                {
                    setState(221);
                    qName();
                }
                break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class NCNameContext extends ParserRuleContext {
        public TerminalNode NCName() {
            return getToken(XpathParser.NCName, 0);
        }

        public TerminalNode AxisName() {
            return getToken(XpathParser.AxisName, 0);
        }

        public NCNameContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_nCName;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener) ((XpathListener) listener).enterNCName(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof XpathListener) ((XpathListener) listener).exitNCName(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof XpathVisitor)
                return ((XpathVisitor<? extends T>) visitor).visitNCName(this);
            else return visitor.visitChildren(this);
        }
    }

    public final NCNameContext nCName() throws RecognitionException {
        NCNameContext _localctx = new NCNameContext(_ctx, getState());
        enterRule(_localctx, 52, RULE_nCName);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(224);
                _la = _input.LA(1);
                if (!(_la == AxisName || _la == NCName)) {
                    _errHandler.recoverInline(this);
                } else {
                    if (_input.LA(1) == Token.EOF) matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static final String _serializedATN =
            "\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3+\u00e5\4\2\t\2\4" +
                    "\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t" +
                    "\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22" +
                    "\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31" +
                    "\4\32\t\32\4\33\t\33\4\34\t\34\3\2\3\2\3\3\3\3\5\3=\n\3\3\4\3\4\3\4\3" +
                    "\5\3\5\3\5\7\5E\n\5\f\5\16\5H\13\5\3\6\3\6\3\6\7\6M\n\6\f\6\16\6P\13\6" +
                    "\3\6\5\6S\n\6\3\7\3\7\3\7\5\7X\n\7\5\7Z\n\7\3\b\3\b\3\b\3\b\3\b\3\b\3" +
                    "\b\3\b\5\bd\n\b\3\t\3\t\3\t\3\t\3\n\3\n\3\13\3\13\3\f\3\f\3\f\3\f\3\f" +
                    "\3\f\3\f\3\f\5\fv\n\f\3\r\3\r\3\r\3\r\3\r\7\r}\n\r\f\r\16\r\u0080\13\r" +
                    "\5\r\u0082\n\r\3\r\3\r\3\16\3\16\3\16\5\16\u0089\n\16\3\16\3\16\3\16\5" +
                    "\16\u008e\n\16\3\17\3\17\3\17\3\17\5\17\u0094\n\17\5\17\u0096\n\17\3\20" +
                    "\3\20\7\20\u009a\n\20\f\20\16\20\u009d\13\20\3\21\3\21\3\21\7\21\u00a2" +
                    "\n\21\f\21\16\21\u00a5\13\21\3\22\3\22\3\22\7\22\u00aa\n\22\f\22\16\22" +
                    "\u00ad\13\22\3\23\3\23\3\23\7\23\u00b2\n\23\f\23\16\23\u00b5\13\23\3\24" +
                    "\3\24\3\24\7\24\u00ba\n\24\f\24\16\24\u00bd\13\24\3\25\3\25\3\25\7\25" +
                    "\u00c2\n\25\f\25\16\25\u00c5\13\25\3\26\3\26\3\26\5\26\u00ca\n\26\3\27" +
                    "\5\27\u00cd\n\27\3\27\3\27\3\30\3\30\3\30\5\30\u00d4\n\30\3\31\3\31\3" +
                    "\32\3\32\3\32\3\33\3\33\3\33\3\33\3\33\3\33\5\33\u00e1\n\33\3\34\3\34" +
                    "\3\34\2\2\35\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \"$&(*,.\60\62\64" +
                    "\66\2\t\3\2\n\13\4\2\22\22\26\26\3\2\36\37\5\2\32\33\35\35 $\3\2\20\21" +
                    "\3\2\23\25\4\2\t\t++\2\u00e6\28\3\2\2\2\4<\3\2\2\2\6>\3\2\2\2\bA\3\2\2" +
                    "\2\nR\3\2\2\2\fY\3\2\2\2\16c\3\2\2\2\20e\3\2\2\2\22i\3\2\2\2\24k\3\2\2" +
                    "\2\26u\3\2\2\2\30w\3\2\2\2\32\u008d\3\2\2\2\34\u0095\3\2\2\2\36\u0097" +
                    "\3\2\2\2 \u009e\3\2\2\2\"\u00a6\3\2\2\2$\u00ae\3\2\2\2&\u00b6\3\2\2\2" +
                    "(\u00be\3\2\2\2*\u00c6\3\2\2\2,\u00cc\3\2\2\2.\u00d0\3\2\2\2\60\u00d5" +
                    "\3\2\2\2\62\u00d7\3\2\2\2\64\u00e0\3\2\2\2\66\u00e2\3\2\2\289\5\24\13" +
                    "\29\3\3\2\2\2:=\5\b\5\2;=\5\6\4\2<:\3\2\2\2<;\3\2\2\2=\5\3\2\2\2>?\t\2" +
                    "\2\2?@\5\b\5\2@\7\3\2\2\2AF\5\n\6\2BC\t\2\2\2CE\5\n\6\2DB\3\2\2\2EH\3" +
                    "\2\2\2FD\3\2\2\2FG\3\2\2\2G\t\3\2\2\2HF\3\2\2\2IJ\5\f\7\2JN\5\16\b\2K" +
                    "M\5\20\t\2LK\3\2\2\2MP\3\2\2\2NL\3\2\2\2NO\3\2\2\2OS\3\2\2\2PN\3\2\2\2" +
                    "QS\5\22\n\2RI\3\2\2\2RQ\3\2\2\2S\13\3\2\2\2TU\7\t\2\2UZ\7&\2\2VX\7\27" +
                    "\2\2WV\3\2\2\2WX\3\2\2\2XZ\3\2\2\2YT\3\2\2\2YW\3\2\2\2Z\r\3\2\2\2[d\5" +
                    "\64\33\2\\]\7\7\2\2]^\7\f\2\2^d\7\r\2\2_`\7\3\2\2`a\7\f\2\2ab\7)\2\2b" +
                    "d\7\r\2\2c[\3\2\2\2c\\\3\2\2\2c_\3\2\2\2d\17\3\2\2\2ef\7\16\2\2fg\5\24" +
                    "\13\2gh\7\17\2\2h\21\3\2\2\2ij\t\3\2\2j\23\3\2\2\2kl\5 \21\2l\25\3\2\2" +
                    "\2mv\5\62\32\2no\7\f\2\2op\5\24\13\2pq\7\r\2\2qv\3\2\2\2rv\7)\2\2sv\7" +
                    "\b\2\2tv\5\30\r\2um\3\2\2\2un\3\2\2\2ur\3\2\2\2us\3\2\2\2ut\3\2\2\2v\27" +
                    "\3\2\2\2wx\5\60\31\2x\u0081\7\f\2\2y~\5\24\13\2z{\7\30\2\2{}\5\24\13\2" +
                    "|z\3\2\2\2}\u0080\3\2\2\2~|\3\2\2\2~\177\3\2\2\2\177\u0082\3\2\2\2\u0080" +
                    "~\3\2\2\2\u0081y\3\2\2\2\u0081\u0082\3\2\2\2\u0082\u0083\3\2\2\2\u0083" +
                    "\u0084\7\r\2\2\u0084\31\3\2\2\2\u0085\u0088\5\34\17\2\u0086\u0087\7\31" +
                    "\2\2\u0087\u0089\5\32\16\2\u0088\u0086\3\2\2\2\u0088\u0089\3\2\2\2\u0089" +
                    "\u008e\3\2\2\2\u008a\u008b\7\n\2\2\u008b\u008c\7\31\2\2\u008c\u008e\5" +
                    "\32\16\2\u008d\u0085\3\2\2\2\u008d\u008a\3\2\2\2\u008e\33\3\2\2\2\u008f" +
                    "\u0096\5\4\3\2\u0090\u0093\5\36\20\2\u0091\u0092\t\2\2\2\u0092\u0094\5" +
                    "\b\5\2\u0093\u0091\3\2\2\2\u0093\u0094\3\2\2\2\u0094\u0096\3\2\2\2\u0095" +
                    "\u008f\3\2\2\2\u0095\u0090\3\2\2\2\u0096\35\3\2\2\2\u0097\u009b\5\26\f" +
                    "\2\u0098\u009a\5\20\t\2\u0099\u0098\3\2\2\2\u009a\u009d\3\2\2\2\u009b" +
                    "\u0099\3\2\2\2\u009b\u009c\3\2\2\2\u009c\37\3\2\2\2\u009d\u009b\3\2\2" +
                    "\2\u009e\u00a3\5\"\22\2\u009f\u00a0\7\4\2\2\u00a0\u00a2\5\"\22\2\u00a1" +
                    "\u009f\3\2\2\2\u00a2\u00a5\3\2\2\2\u00a3\u00a1\3\2\2\2\u00a3\u00a4\3\2" +
                    "\2\2\u00a4!\3\2\2\2\u00a5\u00a3\3\2\2\2\u00a6\u00ab\5$\23\2\u00a7\u00a8" +
                    "\7\5\2\2\u00a8\u00aa\5$\23\2\u00a9\u00a7\3\2\2\2\u00aa\u00ad\3\2\2\2\u00ab" +
                    "\u00a9\3\2\2\2\u00ab\u00ac\3\2\2\2\u00ac#\3\2\2\2\u00ad\u00ab\3\2\2\2" +
                    "\u00ae\u00b3\5&\24\2\u00af\u00b0\t\4\2\2\u00b0\u00b2\5&\24\2\u00b1\u00af" +
                    "\3\2\2\2\u00b2\u00b5\3\2\2\2\u00b3\u00b1\3\2\2\2\u00b3\u00b4\3\2\2\2\u00b4" +
                    "%\3\2\2\2\u00b5\u00b3\3\2\2\2\u00b6\u00bb\5(\25\2\u00b7\u00b8\t\5\2\2" +
                    "\u00b8\u00ba\5(\25\2\u00b9\u00b7\3\2\2\2\u00ba\u00bd\3\2\2\2\u00bb\u00b9" +
                    "\3\2\2\2\u00bb\u00bc\3\2\2\2\u00bc\'\3\2\2\2\u00bd\u00bb\3\2\2\2\u00be" +
                    "\u00c3\5*\26\2\u00bf\u00c0\t\6\2\2\u00c0\u00c2\5*\26\2\u00c1\u00bf\3\2" +
                    "\2\2\u00c2\u00c5\3\2\2\2\u00c3\u00c1\3\2\2\2\u00c3\u00c4\3\2\2\2\u00c4" +
                    ")\3\2\2\2\u00c5\u00c3\3\2\2\2\u00c6\u00c9\5,\27\2\u00c7\u00c8\t\7\2\2" +
                    "\u00c8\u00ca\5*\26\2\u00c9\u00c7\3\2\2\2\u00c9\u00ca\3\2\2\2\u00ca+\3" +
                    "\2\2\2\u00cb\u00cd\7\20\2\2\u00cc\u00cb\3\2\2\2\u00cc\u00cd\3\2\2\2\u00cd" +
                    "\u00ce\3\2\2\2\u00ce\u00cf\5\32\16\2\u00cf-\3\2\2\2\u00d0\u00d3\5\66\34" +
                    "\2\u00d1\u00d2\7%\2\2\u00d2\u00d4\5\66\34\2\u00d3\u00d1\3\2\2\2\u00d3" +
                    "\u00d4\3\2\2\2\u00d4/\3\2\2\2\u00d5\u00d6\5.\30\2\u00d6\61\3\2\2\2\u00d7" +
                    "\u00d8\7\6\2\2\u00d8\u00d9\5.\30\2\u00d9\63\3\2\2\2\u00da\u00e1\7\23\2" +
                    "\2\u00db\u00dc\5\66\34\2\u00dc\u00dd\7%\2\2\u00dd\u00de\7\23\2\2\u00de" +
                    "\u00e1\3\2\2\2\u00df\u00e1\5.\30\2\u00e0\u00da\3\2\2\2\u00e0\u00db\3\2" +
                    "\2\2\u00e0\u00df\3\2\2\2\u00e1\65\3\2\2\2\u00e2\u00e3\t\b\2\2\u00e3\67" +
                    "\3\2\2\2\32<FNRWYcu~\u0081\u0088\u008d\u0093\u0095\u009b\u00a3\u00ab\u00b3" +
                    "\u00bb\u00c3\u00c9\u00cc\u00d3\u00e0";
    public static final ATN _ATN =
            new ATNDeserializer().deserialize(_serializedATN.toCharArray());

    static {
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
        for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
            _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
        }
    }
}